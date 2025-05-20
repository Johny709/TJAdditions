package com.johny.tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.capability.IParallelController;
import com.johny.tj.capability.LinkPos;
import com.johny.tj.capability.TJCapabilities;
import com.johny.tj.gui.TJGuiTextures;
import com.johny.tj.gui.TJWidgetGroup;
import com.johny.tj.gui.widgets.TJAdvancedTextWidget;
import com.johny.tj.gui.widgets.TJTextFieldWidget;
import gregicadditions.GAValues;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GAMultiblockCasing2;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.IWorkable;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.AbstractWidgetGroup;
import gregtech.api.gui.widgets.ScrollableListWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.common.blocks.BlockMultiblockCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.DimensionType;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.johny.tj.gui.TJGuiTextures.CASE_SENSITIVE_BUTTON;
import static com.johny.tj.gui.TJGuiTextures.SPACES_BUTTON;
import static com.johny.tj.textures.TJTextures.FUSION_MK2;
import static com.johny.tj.textures.TJTextures.TELEPORTER_OVERLAY;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate2;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.IMPORT_FLUIDS;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.INPUT_ENERGY;
import static net.minecraft.util.text.TextFormatting.*;

// TODO WIP
public class MetaTileEntityTeleporter extends TJMultiblockDisplayBase implements IParallelController, IWorkable, LinkPos {

    private IEnergyContainer energyContainer;
    private IMultipleTankHandler inputFluidHandler;
    private NBTTagCompound linkData;
    private long energyDrain;
    private int tier;
    private boolean isActive;
    private int progress;
    private int maxProgress = 100;
    private String selectedPosName;
    private String searchPrompt = "";
    private boolean isCaseSensitive;
    private boolean hasSpaces;
    private int searchResults;
    private final Map<String, Pair<World, BlockPos>> posMap = new HashMap<>();
    private final Queue<Triple<Entity, World, BlockPos>> markEntitiesToTransport = new ArrayDeque<>();
    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_FLUIDS, INPUT_ENERGY, MAINTENANCE_HATCH};

    public MetaTileEntityTeleporter(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityTeleporter(metaTileEntityId);
    }

    @Override
    protected void updateFormedValid() {
        if (!this.isWorkingEnabled || this.getNumProblems() >= 6) {
            if (this.isActive)
                this.setActive(false);
            return;
        }

        this.calculateMaintenance(1);
        if (this.progress > 0 && !this.isActive)
            this.setActive(true);

        if (this.progress >= this.maxProgress) {
            this.progress = 0;
            if (!this.markEntitiesToTransport.isEmpty())
                this.transportEntity();
        }

        if (this.hasEnoughEnergy(this.energyDrain)) {
            if (this.progress <= 0) {
                this.progress = 1;
                if (!this.isActive)
                    this.setActive(true);
                Pair<World, BlockPos> worldPos = this.posMap.get(this.selectedPosName);
                if (worldPos == null) {
                    return;
                }
                World world = worldPos.getLeft();
                BlockPos targetPos = worldPos.getValue();
                FluidStack voidDew = FluidRegistry.getFluidStack("ender_distillation", 1000);
                BlockPos pos = getPos().offset(getFrontFacing().getOpposite()).up();
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();
                // cast both entity blockPos and this tile entity's blockPos to int so all entities within the same target block gets affected
                ClassInheritanceMultiMap<Entity>[] entityLists = getWorld().getChunk(pos).getEntityLists();
                for (ClassInheritanceMultiMap<Entity> entities : entityLists) {
                    for (Entity entity : entities) {
                        int entityX = (int) entity.posX;
                        int entityY = (int) entity.posY;
                        int entityZ = (int) entity.posZ;
                        if (entityX == x && entityY == y && entityZ == z && hasEnoughVoidDew(voidDew)) {
                            this.inputFluidHandler.drain(voidDew, true);
                            this.markEntitiesToTransport.add(new ImmutableTriple<>(entity, world, targetPos));
                        }
                    }
                }
            } else {
                this.progress++;
            }
            this.energyContainer.removeEnergy(this.energyDrain);
        } else {
            if (this.progress > 1)
                this.progress--;
        }
    }

    private void transportEntity() {
        Triple<Entity, World, BlockPos> entityPos = this.markEntitiesToTransport.poll();
        Entity entity = entityPos.getLeft();
        WorldServer dimension = (WorldServer) entityPos.getMiddle();
        BlockPos pos = entityPos.getRight();
        int worldID = dimension.provider.getDimension();

        if (getWorld().provider.getDimension() != worldID) {
            entity.setWorld(dimension);
            entity.changeDimension(worldID, new Teleporter(dimension) {
                @Override
                public void placeEntity(World world, Entity entity, float yaw) {
                    entity.setLocationAndAngles(pos.getX(), pos.getY(), pos.getZ(), entity.rotationYaw, 0.0F);
                    entity.motionX = 0.0D;
                    entity.motionY = 0.0D;
                    entity.motionZ = 0.0D;
                }
            });
        } else {
            entity.setPositionAndUpdate(pos.getX(), pos.getY(), pos.getZ());
        }
        entity.sendMessage(new TextComponentTranslation("tj.multiblock.teleporter.success"));
        DimensionManager.keepDimensionLoaded(worldID, false);
    }

    private boolean hasEnoughEnergy(long amount) {
        return this.energyContainer.getEnergyStored() >= amount;
    }

    private boolean hasEnoughVoidDew(FluidStack fluid) {
        FluidStack fluidStack = this.inputFluidHandler.drain(fluid, false);
        return fluidStack != null && fluidStack.amount == 1000;
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int framework = 0, framework2 = 0;
        if (context.get("framework") instanceof GAMultiblockCasing.CasingType) {
            framework = ((GAMultiblockCasing.CasingType) context.get("framework")).getTier();
        }
        if (context.get("framework2") instanceof GAMultiblockCasing2.CasingType) {
            framework2 = ((GAMultiblockCasing2.CasingType) context.get("framework2")).getTier();
        }
        this.tier = Math.max(framework, framework2);
        this.energyDrain = (long) (Math.pow(4, this.tier) * 8);
        this.energyContainer = new EnergyContainerList(getAbilities(INPUT_ENERGY));
        this.inputFluidHandler = new FluidTankList(true, getAbilities(IMPORT_FLUIDS));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("CCC", "C~C", "C~C", "C~C", "CCC")
                .aisle("CFC", "~#~", "~#~", "~#~", "CFC")
                .aisle("CSC", "C~C", "C~C", "C~C", "CCC")
                .where('S', selfPredicate())
                .where('C', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('F', frameworkPredicate().or(frameworkPredicate2()))
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.FUSION_CASING_MK2);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return FUSION_MK2;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        TELEPORTER_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), this.isActive);
    }

    @Override
    protected void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs) {
        super.addNewTabs(tabs);
        TJWidgetGroup widgetPosGroup = new TJWidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.pos", new ItemStack(Items.COMPASS), blockPosTab(widgetPosGroup::addWidgets)));
    }

    @Override
    protected AbstractWidgetGroup mainDisplayTab(Function<Widget, WidgetGroup> widgetGroup) {
        super.mainDisplayTab(widgetGroup);
        return widgetGroup.apply(new ToggleButtonWidget(172, 151, 18, 18, TJGuiTextures.RESET_BUTTON, this::isReset, this::setReset)
                .setTooltipText("machine.universal.toggle.reset"));
    }

    private AbstractWidgetGroup blockPosTab(Function<Widget, WidgetGroup> widgetGroup) {
        ScrollableListWidget scrollWidget = new ScrollableListWidget(10, 30, 178, 97) {
            @Override
            public boolean isWidgetClickable(Widget widget) {
                return true; // this ScrollWidget will only add one widget so checks are unnecessary if position changes.
            }
        };
        scrollWidget.addWidget(new TJAdvancedTextWidget(0, 0, this::addPosDisplayText, 0xFFFFFF)
                .setClickHandler(this::handlePosDisplayClick)
                .setMaxWidthLimit(1000));
        widgetGroup.apply(scrollWidget);
        widgetGroup.apply(new ToggleButtonWidget(172, 133, 18, 18, CASE_SENSITIVE_BUTTON, this::isCaseSensitive, this::setCaseSensitive)
                .setTooltipText("machine.universal.case_sensitive"));
        widgetGroup.apply(new ToggleButtonWidget(172, 151, 18, 18, SPACES_BUTTON, this::hasSpaces, this::setSpaces)
                .setTooltipText("machine.universal.spaces"));
        return widgetGroup.apply(new TJTextFieldWidget(10, 18, 180, 18, false, this::searchSupplier, this::searchResponder)
                .setBackgroundText("machine.universal.search")
                .setValidator(str -> Pattern.compile(".*").matcher(str).matches()));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        Pair<World, BlockPos> selectedPos = this.posMap.get(this.selectedPosName);
        int currentProgress = (int) Math.floor(this.progress / (this.maxProgress * 1.0) * 100);
        if (selectedPos != null) {
            World world = selectedPos.getLeft();
            BlockPos pos = selectedPos.getRight();

            textList.add(new TextComponentTranslation("tj.multiblock.teleporter.selected.world", world.provider.getDimensionType().getName(), world.provider.getDimension()));
            textList.add(new TextComponentTranslation("tj.multiblock.teleporter.selected.pos", pos.getX(), pos.getY(), pos.getZ()));
        }
        ITextComponent isWorkingText = !isWorkingEnabled ? new TextComponentTranslation("gregtech.multiblock.work_paused")
                : !isActive ? new TextComponentTranslation("gregtech.multiblock.idling")
                : new TextComponentTranslation("gregtech.multiblock.running");
        isWorkingText.getStyle().setColor(!isWorkingEnabled ? YELLOW : !isActive ? WHITE : GREEN);
        textList.add(isWorkingText);
        if (isActive)
            textList.add(new TextComponentTranslation("gregtech.multiblock.progress", currentProgress));
    }

    private void addPosDisplayText(List<ITextComponent> textList) {
        int count = 0, searchResults = 0;
        textList.add(new TextComponentString("§l" + I18n.translateToLocal("tj.multiblock.tab.pos") + "§r(§e" + this.searchResults + "§r/§e" + this.posMap.size() + "§r)"));
        for (Map.Entry<String, Pair<World, BlockPos>> posEntry : this.posMap.entrySet()) {
            String key = posEntry.getKey();
            String result = key, result2 = key;

            if (!this.isCaseSensitive) {
                result = result.toLowerCase();
                result2 = result2.toUpperCase();
            }

            if (!this.hasSpaces) {
                result = result.replace(" ", "");
                result2 = result2.replace(" ", "");
            }

            if (!result.isEmpty() && !result.contains(this.searchPrompt) && !result2.contains(this.searchPrompt))
                continue;

            DimensionType world = posEntry.getValue().getLeft().provider.getDimensionType();
            String worldName = world.getName();
            int worldID = world.getId();

            BlockPos pos = posEntry.getValue().getValue();

            String tp = "tp" + "w" + worldID + "x" + pos.getX() + "y" + pos.getY() + "z" + pos.getZ();
            String keyName = "select:" + key;
            String remove = "remove:" + key;
            String position = I18n.translateToLocal("machine.universal.linked.pos") + " X: §e" + pos.getX() + "§r Y: §e" + pos.getY() + "§r Z: §e" + pos.getZ();

            ITextComponent keyPos = new TextComponentString("[§e" + (++count) + "§r] " + key + "§r")
                    .appendText("\n")
                    .appendSibling(withButton(new TextComponentString("[TP]"), tp))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.select"), keyName))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), remove));

            ITextComponent blockPos = new TextComponentString(count + ": " + key + "\n")
                    .appendSibling(new TextComponentTranslation("machine.universal.linked.dimension", worldName, worldID))
                    .appendText("\n")
                    .appendSibling(new TextComponentString(position));

            keyPos.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, blockPos));
            textList.add(keyPos);
            this.searchResults = ++searchResults;
        }
    }

    private void handlePosDisplayClick(String componentData, Widget.ClickData clickData, EntityPlayer player) {
        if (componentData.startsWith("tp")) {
            String[] world = componentData.split("w");
            String[] pos = world[1].split("x");
            String[] x = pos[1].split("y");
            String[] yz = x[1].split("z");

            int worldID = Integer.parseInt(pos[0]);
            int posX = Integer.parseInt(x[0]);
            int posY = Integer.parseInt(yz[0]);
            int posZ = Integer.parseInt(yz[1]);

            if (getWorld().provider.getDimension() != worldID) {
                DimensionManager.initDimension(worldID);
                DimensionManager.keepDimensionLoaded(worldID, true);
            }
            WorldServer dimension = DimensionManager.getWorld(worldID);
            BlockPos blockPos = new BlockPos(posX, posY, posZ);
            player.sendMessage(new TextComponentTranslation("tj.multiblock.teleporter.queue", player.getName()));
            this.markEntitiesToTransport.add(new ImmutableTriple<>(player, dimension, blockPos));

        } else if (componentData.startsWith("select")) {
            String[] selectedPos = componentData.split(":");
            this.selectedPosName = selectedPos[1];

        } else if (componentData.startsWith("remove")) {
            String[] selectedName = componentData.split(":");
            this.posMap.remove(selectedName[1]);
            int index = this.linkData.getInteger("I");
            this.linkData.setInteger("I", index + 1);
        }
    }

    private String searchSupplier() {
        return this.searchPrompt;
    }

    private void searchResponder(String searchPrompt) {
        this.searchPrompt = searchPrompt;
        this.markDirty();
    }

    private boolean isCaseSensitive() {
        return this.isCaseSensitive;
    }

    private void setCaseSensitive(Boolean isCaseSensitive) {
        this.isCaseSensitive = isCaseSensitive;
       this.markDirty();
    }

    private boolean hasSpaces() {
        return this.hasSpaces;
    }

    private void setSpaces(Boolean hasSpaces) {
        this.hasSpaces = hasSpaces;
        this.markDirty();
    }

    private boolean isReset() {
        return false;
    }

    private void setReset(boolean reset) {
        this.posMap.clear();
        this.linkData.setInteger("I", getPosSize());
    }

    protected void setActive(boolean active) {
        this.isActive = active;
        if (!getWorld().isRemote) {
            this.writeCustomData(1, buf -> buf.writeBoolean(active));
            this.markDirty();
        }
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 1)
            this.isActive = buf.readBoolean();
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(this.isActive);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        NBTTagList posList = new NBTTagList();
        for (Map.Entry<String, Pair<World, BlockPos>> posEntry : this.posMap.entrySet()) {
            String name = posEntry.getKey();
            int worldID = posEntry.getValue().getLeft().provider.getDimension();
            int x = posEntry.getValue().getRight().getX();
            int y = posEntry.getValue().getRight().getY();
            int z = posEntry.getValue().getRight().getZ();

            NBTTagCompound posTag = new NBTTagCompound();
            posTag.setString("Name", name);
            posTag.setInteger("WorldID", worldID);
            posTag.setInteger("X", x);
            posTag.setInteger("Y", y);
            posTag.setInteger("Z", z);
            posList.appendTag(posTag);
        }
        data.setTag("PosList", posList);
        data.setBoolean("Active", this.isActive);
        data.setBoolean("CaseSensitive", this.isCaseSensitive);
        data.setBoolean("HasSpaces", this.hasSpaces);
        data.setInteger("Progress", this.progress);
        data.setInteger("MaxProgress", this.maxProgress);
        if (this.selectedPosName != null)
            data.setString("SelectedPos", this.selectedPosName);
        if (linkData != null)
            data.setTag("Link.XYZ", this.linkData);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.isActive = data.getBoolean("Active");
        this.isCaseSensitive = data.getBoolean("CaseSensitive");
        this.hasSpaces = data.getBoolean("HasSpaces");
        this.maxProgress = data.getInteger("MaxProgress");
        this.progress = data.getInteger("Progress");
        this.selectedPosName = data.getString("SelectedPos");
        if (data.hasKey("Link.XYZ"))
            this.linkData = data.getCompoundTag("Link.XYZ");
        if (!data.hasKey("PosList"))
            return;
        NBTTagList posList = data.getTagList("PosList", Constants.NBT.TAG_COMPOUND);
        for (NBTBase compound : posList) {
            NBTTagCompound tag = (NBTTagCompound) compound;
            String name = tag.getString("Name");
            World world = DimensionManager.getWorld(tag.getInteger("WorldID"));
            BlockPos pos = new BlockPos(tag.getInteger("X"), tag.getInteger("Y"), tag.getInteger("Z"));
            this.posMap.put(name, new ImmutablePair<>(world, pos));
        }
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == GregtechTileCapabilities.CAPABILITY_WORKABLE)
            return GregtechTileCapabilities.CAPABILITY_WORKABLE.cast(this);
        if (capability == TJCapabilities.CAPABILITY_LINK_POS)
            return TJCapabilities.CAPABILITY_LINK_POS.cast(this);
        return super.getCapability(capability, side);
    }

    @Override
    public long getMaxEUt() {
        return this.energyDrain;
    }

    @Override
    public long getTotalEnergyConsumption() {
        return this.energyDrain;
    }

    @Override
    public long getVoltageTier() {
        return GAValues.V[this.tier];
    }

    @Override
    public boolean isInterDimensional() {
        return true;
    }

    @Override
    public int dimensionID() {
        return this.getWorld().provider.getDimensionType().getId();
    }

    @Override
    public int getRange() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPosSize() {
        return 1;
    }

    @Override
    public void setPos(String name, BlockPos pos, EntityPlayer player, World world, int index) {
        this.posMap.put(name, new ImmutablePair<>(world, pos));
        this.linkData.setInteger("I", 1);
    }

    @Override
    public World world() {
        return this.getWorld();
    }

    @Override
    public NBTTagCompound getLinkData() {
        return this.linkData;
    }

    @Override
    public void setLinkData(NBTTagCompound linkData) {
        this.linkData = linkData;
    }

    @Override
    public int getProgress() {
        return this.progress;
    }

    @Override
    public int getMaxProgress() {
        return this.maxProgress;
    }
}
