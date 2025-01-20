package com.johny.tj.machines.multi.electric;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.google.common.collect.Lists;
import com.johny.tj.blocks.BlockSolidCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.gui.TJGuiTextures;
import com.johny.tj.gui.TJHorizontoalTabListRenderer;
import com.johny.tj.gui.TJTabGroup;
import com.johny.tj.machines.ExtendedItemFilter;
import com.johny.tj.machines.TJMiner;
import com.johny.tj.textures.TJTextures;
import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.gui.widgets.tab.ItemTabInfo;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import gregtech.api.util.Position;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.items.MetaItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static gregicadditions.GAMaterials.Taranium;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.unification.material.Materials.DrillingFluid;
import static gregtech.api.unification.material.Materials.Duranium;


public class MetaTileEntityEliteLargeMiner extends TJMultiblockDisplayBase implements TJMiner {

    protected static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.INPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};

    public final Type type;
    protected AtomicLong x = new AtomicLong(Long.MAX_VALUE), y = new AtomicLong(Long.MAX_VALUE), z = new AtomicLong(Long.MAX_VALUE);
    protected AtomicInteger currentChunk = new AtomicInteger(0);
    protected IEnergyContainer energyContainer;
    protected IMultipleTankHandler importFluidHandler;
    protected IItemHandlerModifiable outputInventory;
    protected List<Chunk> chunks = new ArrayList<>();
    protected boolean isActive = false;
    protected boolean done = false;
    protected boolean silktouch = false;
    protected boolean canRestart = false;
    protected final ExtendedItemFilter blockFilter;
    public Map<Integer, IBlockState> blocksToFilter;
    protected boolean enableFilter;
    protected boolean blackListFilter;


    public MetaTileEntityEliteLargeMiner(ResourceLocation metaTileEntityId, Type type) {
        super(metaTileEntityId);
        this.type = type;
        blocksToFilter = new HashMap<>();
        blockFilter = new ExtendedItemFilter(blocksToFilter);
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        resetTileAbilities();
        if (isActive)
            setActive(false);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        initializeAbilities();
    }

    private void initializeAbilities() {
        this.importFluidHandler = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.outputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.EXPORT_ITEMS));
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
    }

    private void resetTileAbilities() {
        this.importFluidHandler = new FluidTankList(true);
        this.outputInventory = new ItemStackHandler(0);
        this.energyContainer = new EnergyContainerList(Lists.newArrayList());
    }

    public boolean drainEnergy() {
        long energyDrain = GAValues.V[Math.max(GAValues.EV, GAUtility.getTierByVoltage(energyContainer.getInputVoltage()))];

        FluidStack drillingFluid;
        if (this.type != Type.CREATIVE) {
            drillingFluid = DrillingFluid.getFluid(type.drillingFluidConsumePerTick);
        }
        else {
            drillingFluid = Taranium.getFluid(type.drillingFluidConsumePerTick);
        }
        FluidStack canDrain = importFluidHandler.drain(drillingFluid, false);
        if (energyContainer.getEnergyStored() >= energyDrain && canDrain != null && canDrain.amount == type.drillingFluidConsumePerTick) {
            energyContainer.removeEnergy(energyContainer.getInputVoltage());
            importFluidHandler.drain(drillingFluid, true);
            return true;
        }
        return false;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public long getNbBlock() {
        int tierDifference = GAUtility.getTierByVoltage(energyContainer.getInputVoltage()) - GAValues.MV;
        return (long) Math.floor(Math.pow(2, tierDifference));
    }

    @Override
    protected void updateFormedValid() {
        if (!getWorld().isRemote) {
            if (isWorkingEnabled) {
                if (done || !drainEnergy()) {
                    if (isActive)
                        setActive(false);
                    return;
                }

                if (!isActive)
                    setActive(true);

                WorldServer world = (WorldServer) this.getWorld();
                Chunk chunkMiner = world.getChunk(getPos());
                Chunk origin;
                if (chunks.isEmpty() && type.chunk / 2.0 > 1.0) {
                    int tmp = Math.floorDiv(type.chunk, 2);
                    origin = world.getChunk(chunkMiner.x - tmp, chunkMiner.z - tmp);
                    for (int i = 0; i < type.chunk; i++) {
                        for (int j = 0; j < type.chunk; j++) {
                            chunks.add(world.getChunk(origin.x + i, origin.z + j));
                        }
                    }
                } else if (chunks.isEmpty() && type.chunk == 1) {
                    origin = world.getChunk(chunkMiner.x, chunkMiner.z);
                    chunks.add(origin);
                }

                if (currentChunk.intValue() == chunks.size()) {
                    setActive(false);
                    return;
                }

                Chunk chunk = chunks.get(currentChunk.intValue());

                if (x.get() == Long.MAX_VALUE) {
                    x.set(chunk.getPos().getXStart());
                }
                if (z.get() == Long.MAX_VALUE) {
                    z.set(chunk.getPos().getZStart());
                }
                if (y.get() == Long.MAX_VALUE) {
                    y.set(getPos().getY());
                }

                List<BlockPos> blockPos = TJMiner.getBlockToMinePerChunk(this, x, y, z, chunk.getPos());
                blockPos.forEach(blockPos1 -> {
                    NonNullList<ItemStack> itemStacks = NonNullList.create();
                    IBlockState blockState = this.getWorld().getBlockState(blockPos1);
                    int meta = blockState.getBlock().getMetaFromState(blockState);
                    int maxState = blockState.getBlock().getBlockState().getValidStates().size();
                    IBlockState actualState = blockState.getBlock().getBlockState().getValidStates().get(Math.min(meta, maxState));
                    if (isEnableFilter()) {
                        if (isBlackListFilter()) {
                            if (blocksToFilter.containsValue(actualState)) {
                                return;
                            }
                        } else {
                            if (!blocksToFilter.containsValue(actualState)) {
                                return;
                            }
                        }
                    }
                    if (!silktouch) {
                        GAUtility.applyHammerDrops(world.rand, actualState, itemStacks, type.fortune, null, this.energyContainer.getInputVoltage());
                    } else {
                        itemStacks.add(new ItemStack(actualState.getBlock(), 1, Math.min(meta, maxState)));
                    }
                    if (addItemsToItemHandler(outputInventory, true, itemStacks)) {
                        addItemsToItemHandler(outputInventory, false, itemStacks);
                        if (this.getType() != Type.CREATIVE) {
                            world.setBlockState(blockPos1, Blocks.COBBLESTONE.getDefaultState());
                        }
                    }
                });

                if (y.get() < 0) {
                    if (type != Type.CREATIVE) {
                        currentChunk.incrementAndGet();
                        if (currentChunk.get() >= chunks.size()) {
                            if (canRestart) {
                                currentChunk.set(0);
                                x.set(chunks.get(currentChunk.intValue()).getPos().getXStart());
                                z.set(chunks.get(currentChunk.intValue()).getPos().getZStart());
                                y.set(getPos().getY());
                            } else
                                done = true;
                        } else {
                            x.set(chunks.get(currentChunk.intValue()).getPos().getXStart());
                            z.set(chunks.get(currentChunk.intValue()).getPos().getZStart());
                            y.set(getPos().getY());
                        }
                    } else {
                        x.set(chunks.get(currentChunk.intValue()).getPos().getXStart());
                        z.set(chunks.get(currentChunk.intValue()).getPos().getZStart());
                        y.set(getPos().getY());
                    }
                }


                if (!getWorld().isRemote && getOffsetTimer() % 5 == 0) {
                    pushItemsIntoNearbyHandlers(getFrontFacing());
                }
            }
        }
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("F###F", "F###F", "PQQQP", "#####", "#####", "#####", "#####", "#####", "#####", "#####")
                .aisle("#####", "#####", "QPPPQ", "#CCC#", "#####", "#####", "#####", "#####", "#####", "#####")
                .aisle("#####", "#####", "QPPPQ", "#CPC#", "#FFF#", "#FFF#", "#FFF#", "##F##", "##F##", "##F##")
                .aisle("#####", "#####", "CPPPQ", "#CSC#", "#####", "#####", "#####", "#####", "#####", "#####")
                .aisle("F###F", "F###F", "PQQQP", "#####", "#####", "#####", "#####", "#####", "#####", "#####")
                .setAmountAtLeast('L', 3)
                .where('S', selfPredicate())
                .where('L', statePredicate(getCasingState()))
                .where('C', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('P', statePredicate(getCasingState()))
                .where('Q', statePredicate(getCasingState()).or(abilityPartPredicate(MultiblockAbility.EXPORT_ITEMS)))
                .where('F', statePredicate(getFrameState()))
                .where('#', blockWorldState -> true)
                .build();
    }

    public IBlockState getFrameState() {
        return MetaBlocks.FRAMES.get(Duranium).getDefaultState();
    }

    public IBlockState getCasingState() {
        return TJMetaBlocks.SOLID_CASING.getState(BlockSolidCasings.SolidCasingType.DURANIUM_CASING);
    }

    @Override
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        //basically check minimal requirements for inputs count
        int itemOutputsCount = abilities.getOrDefault(MultiblockAbility.EXPORT_ITEMS, Collections.emptyList())
                .stream().map(it -> (IItemHandler) it).mapToInt(IItemHandler::getSlots).sum();
        int fluidInputsCount = abilities.getOrDefault(MultiblockAbility.IMPORT_FLUIDS, Collections.emptyList()).size();
        return itemOutputsCount >= 1 &&
                fluidInputsCount >= 1 &&
                abilities.containsKey(MultiblockAbility.INPUT_ENERGY);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        if (this.type != Type.CREATIVE) {
            tooltip.add(I18n.format("gtadditions.machine.miner.multi.description", type.chunk, type.chunk, type.fortuneString));

            tooltip.add(I18n.format("gtadditions.machine.miner.fluid_usage", type.drillingFluidConsumePerTick, I18n.format(DrillingFluid.getFluid(0).getUnlocalizedName())));
        }
        else {
            tooltip.add(I18n.format("gtadditions.machine.miner.multi.description2", type.chunk, type.chunk, type.fortuneString));

            tooltip.add(I18n.format("gtadditions.machine.miner.fluid_usage", type.drillingFluidConsumePerTick, I18n.format(Taranium.getFluid(0).getUnlocalizedName())));
        }
    }

    @Override
    protected ModularUI.Builder createUITemplate(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI.extendedBuilder();
        builder.image(-10, 0, 195, 217, TJGuiTextures.NEW_MULTIBLOCK_DISPLAY);
        builder.bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT ,-3, 134);

        TJTabGroup tabGroup = new TJTabGroup(() -> new TJHorizontoalTabListRenderer(TJHorizontoalTabListRenderer.HorizontalStartCorner.LEFT, TJHorizontoalTabListRenderer.VerticalLocation.BOTTOM), new Position(-10, 1));
        tabGroup.addTab(new ItemTabInfo("tj.multiblock.tab.display", this.getStackForm()), mainDisplayTab());
        tabGroup.addTab(new ItemTabInfo("tj.multiblock.tab.filter", MetaItems.ITEM_FILTER.getStackForm()), filterTab());
        builder.widget(tabGroup);
        return builder;
    }

    private AbstractWidgetGroup mainDisplayTab() {
        WidgetGroup widgetGroup = new WidgetGroup();
        widgetGroup.addWidget(new AdvancedTextWidget(10, 18, this::addDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180).setClickHandler(this::handleDisplayClick));
        widgetGroup.addWidget(new LabelWidget(11, 7, getMetaFullName(), 0xFFFFFF));
        widgetGroup.addWidget(new ToggleButtonWidget(172, 168, 18, 18, TJGuiTextures.POWER_BUTTON, this::isWorkingEnabled, this::setWorkingEnabled)
                .setTooltipText("machine.universal.toggle.run.mode"));
        widgetGroup.addWidget(new ToggleButtonWidget(172, 132, 18, 18, TJGuiTextures.CAUTION_BUTTON, this::getDoStructureCheck, this::setDoStructureCheck)
                .setTooltipText("machine.universal.toggle.check.mode"));
        return widgetGroup;
    }

    private AbstractWidgetGroup filterTab() {
        WidgetGroup widgetGroup = new WidgetGroup();
        this.blockFilter.initUI(widgetGroup::addWidget);
        widgetGroup.addWidget(new ToggleButtonWidget(172, 132, 18, 18, GuiTextures.TOGGLE_BUTTON_BACK, this::isEnableFilter, this::setEnableFilter)
                .setTooltipText("machine.universal.toggle.filter"));
        widgetGroup.addWidget(new ToggleButtonWidget(172, 150, 18, 18, GuiTextures.BUTTON_BLACKLIST, this::isBlackListFilter, this::setBlackListFilter)
                .setTooltipText("cover.filter.blacklist"));
        widgetGroup.addWidget(new ImageWidget(173, 133, 16, 16, TJGuiTextures.ITEM_FILTER));
        return widgetGroup;
    }

    public void setEnableFilter(boolean enableFilter) {
        this.enableFilter = enableFilter;
    }

    public boolean isEnableFilter() {
        return enableFilter;
    }

    public void setBlackListFilter(boolean blackListFilter) {
        this.blackListFilter = blackListFilter;
    }

    public boolean isBlackListFilter() {
        return blackListFilter;
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        if (this.isStructureFormed()) {
            if(x.get() == Long.MAX_VALUE) {
                textList.add(new TextComponentString(String.format("X: Not Active")));
                textList.add(new TextComponentString(String.format("Y: Not Active")));
                textList.add(new TextComponentString(String.format("Z: Not Active")));
            }
            else{
                textList.add(new TextComponentString(String.format("X: %d", x.get())));
                textList.add(new TextComponentString(String.format("Y: %d", y.get())));
                textList.add(new TextComponentString(String.format("Z: %d", z.get())));
            }
            textList.add(new TextComponentTranslation("gregtech.multiblock.large_miner.chunk", currentChunk.get()));
            textList.add(new TextComponentTranslation("gregtech.multiblock.large_miner.nb_chunk", chunks.size()));
            textList.add(new TextComponentTranslation("gregtech.multiblock.large_miner.block_per_tick", getNbBlock()));
            if (this.type != Type.CREATIVE) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.large_miner.silktouch", silktouch));
                textList.add(new TextComponentTranslation("gregtech.multiblock.large_miner.mode"));
            }
            ITextComponent toggleContinous = new TextComponentTranslation("gregtech.multiblock.elite_large_miner.restart");
            toggleContinous.appendText(" ");

            if (canRestart)
                toggleContinous.appendSibling(withButton(new TextComponentTranslation("gregtech.multiblock.elite_large_miner.restart.true"), "true"));
            else
                toggleContinous.appendSibling(withButton(new TextComponentTranslation("gregtech.multiblock.elite_large_miner.restart.false"), "false"));

            textList.add(toggleContinous);

            if (done)
                textList.add(new TextComponentTranslation("gregtech.multiblock.large_miner.done", getNbBlock()).setStyle(new Style().setColor(TextFormatting.GREEN)));
        }

        super.addDisplayText(textList);
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        canRestart = !componentData.equals("true");
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return TJTextures.DURANIUM;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityEliteLargeMiner(metaTileEntityId, getType());
    }

    @Override
    public boolean onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        if (this.type != Type.CREATIVE) {
            this.silktouch = !silktouch;
            return true;
        }
        return false;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        NBTTagList blockList = new NBTTagList();
        NBTTagList indexList = new NBTTagList();
        int j = 0;
        for (Integer i : blocksToFilter.keySet()) {
            NBTTagCompound indexCompound = new NBTTagCompound();
            indexCompound.setInteger("BlockIndex" + j++, i);
            indexList.appendTag(indexCompound);
            ItemStack stack = new ItemStack(blocksToFilter.get(i).getBlock(), 1, blocksToFilter.get(i).getBlock().getMetaFromState(blocksToFilter.get(i)));
            blockList.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        data.setTag("BlockList", blockList);
        data.setTag("IndexList", indexList);
        data.setBoolean("EnableFilter", enableFilter);
        data.setBoolean("BlackListFilter", blackListFilter);
        data.setTag("xPos", new NBTTagLong(x.get()));
        data.setTag("yPos", new NBTTagLong(y.get()));
        data.setTag("zPos", new NBTTagLong(z.get()));
        data.setTag("chunk", new NBTTagInt(currentChunk.get()));
        data.setTag("done", new NBTTagInt(done ? 1 : 0));
        data.setTag("silktouch", new NBTTagInt(silktouch ? 1 : 0));
        data.setTag("restart", new NBTTagInt(canRestart ? 1 : 0));
        data.setTag("working", new NBTTagInt(isWorkingEnabled ? 1 : 0));
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        NBTTagList blockList = data.getTagList("BlockList", Constants.NBT.TAG_COMPOUND);
        NBTTagList indexList = data.getTagList("IndexList", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < indexList.tagCount(); i++) {
            int index = indexList.getCompoundTagAt(i).getInteger("BlockIndex" + i);
            ItemStack stack = new ItemStack(blockList.getCompoundTagAt(i));
            blockFilter.getItemFilterSlots().insertItem(index, stack, false);
        }
        blackListFilter = data.getBoolean("BlackListFilter");
        enableFilter = data.getBoolean("EnableFilter");
        x.set(data.getLong("xPos"));
        y.set(data.getLong("yPos"));
        z.set(data.getLong("zPos"));
        currentChunk.set(data.getInteger("chunk"));
        done = data.getInteger("done") != 0;
        silktouch = data.getInteger("silktouch") != 0;
        canRestart = data.getInteger("restart") != 0;
        isWorkingEnabled = data.getInteger("working") != 0;
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(isActive);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), isActive);
    }

    protected void setActive(boolean active) {
        this.isActive = active;
        markDirty();
        if (!getWorld().isRemote) {
            writeCustomData(1, buf -> buf.writeBoolean(active));
        }
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 1) {
            this.isActive = buf.readBoolean();
            getHolder().scheduleChunkForRenderUpdate();
        }
    }
}
