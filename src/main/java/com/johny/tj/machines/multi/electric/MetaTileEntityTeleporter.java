package com.johny.tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.capability.IParallelController;
import com.johny.tj.capability.LinkPos;
import com.johny.tj.gui.TJWidgetGroup;
import com.johny.tj.gui.widgets.TJAdvancedTextWidget;
import com.johny.tj.gui.widgets.TJTextFieldWidget;
import gregicadditions.GAValues;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GAMultiblockCasing2;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.AbstractWidgetGroup;
import gregtech.api.gui.widgets.ScrollableListWidget;
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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.johny.tj.textures.TJTextures.FUSION_MK2;
import static com.johny.tj.textures.TJTextures.TELEPORTER_OVERLAY;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate2;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.IMPORT_FLUIDS;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.INPUT_ENERGY;

// TODO WIP
public class MetaTileEntityTeleporter extends TJMultiblockDisplayBase implements IParallelController, LinkPos {

    private IEnergyContainer energyContainer;
    private IMultipleTankHandler inputFluidHandler;
    private NBTTagCompound linkData = new NBTTagCompound();
    private long energyDrain;
    private int tier;
    private boolean isActive;
    private int progress;
    private int maxProgress = 20;
    private String selectedPosName;
    private int selectedPosWorldID;
    private BlockPos selectedPos;
    private String searchPrompt = "";
    private final Map<String, Pair<World, BlockPos>> posMap = new HashMap<>();
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
        if (!this.isWorkingEnabled || getNumProblems() >= 6) {
            if (this.isActive)
                setActive(false);
            return;
        }

        if (this.progress >= this.maxProgress) {
            this.progress = 0;
        }

        if (hasEnoughEnergy(this.energyDrain)) {
            if (this.progress <= 0) {
                this.progress = 1;
                Pair<World, BlockPos> worldPos = this.posMap.get(this.selectedPosName);
                BlockPos targetPos = worldPos.getValue();
                if (targetPos == null) {
                    return;
                }
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
                            entity.setWorld(worldPos.getKey());
                            entity.setPosition(targetPos.getX(), targetPos.getY(), targetPos.getZ());
                        }
                    }
                }
            }
            this.energyContainer.removeEnergy(this.energyDrain);
        } else {
            if (this.progress > 1)
                this.progress--;
        }
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

    private AbstractWidgetGroup blockPosTab(Function<Widget, WidgetGroup> widgetGroup) {
        ScrollableListWidget scrollWidget = new ScrollableListWidget(10, 38, 180, 80) {
            @Override
            public boolean isWidgetClickable(Widget widget) {
                return true; // this ScrollWidget will only add one widget so checks are unnecessary if position changes.
            }
        };
        scrollWidget.addWidget(new TJAdvancedTextWidget(0, 0, this::addPosDisplayText, 0xFFFFFF)
                .setClickHandler(this::handlePosDisplayClick)
                .setMaxWidthLimit(180));
        widgetGroup.apply(scrollWidget);
        return widgetGroup.apply(new TJTextFieldWidget(10, 18, 180, 18, false, this::searchSupplier, this::searchResponder)
                .setValidator(str -> Pattern.compile("\\*?[a-zA-Z0-9_]*\\*?").matcher(str).matches()));
    }

    private void addPosDisplayText(List<ITextComponent> textList) {
        for (Map.Entry<String, Pair<World, BlockPos>> posEntry : this.posMap.entrySet()) {
            String key = posEntry.getKey();

            if (key.isEmpty() || key.contains(this.searchPrompt)) {
                DimensionType world = posEntry.getValue().getKey().provider.getDimensionType();
                String worldName = world.getName();
                int worldID = world.getId();

                BlockPos pos = posEntry.getValue().getValue();

                String tp = "tp" + "w" + worldID + "x" + pos.getX() + "y" + pos.getY() + "z" + pos.getZ();
                String keyName = "select:" + key;
                String remove = "remove:" + key;

                ITextComponent keyPos = new TextComponentString("[§b" + key + "§r]")
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentString("[TP]"), tp))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentString("[O]"), keyName))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), remove));

                ITextComponent blockPos = new TextComponentTranslation("machine.universal.linked.dimension", worldName, worldID)
                        .appendSibling(new TextComponentString("X: §e" + pos.getX() + "§r Y: §e" + pos.getY() + "§r Z: §e" + pos.getZ()));

                keyPos.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, blockPos));
                textList.add(keyPos);
            }
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

            World dimension = DimensionManager.getWorld(worldID);
            dimension.getChunk(new BlockPos(posX, posY, posZ));
            player.setWorld(dimension);
            this.teleport(player, posX, posY, posZ);

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

    private void teleport(EntityPlayer player, int x, int y, int z) {
        if (!player.attemptTeleport(x, y, z))
            this.teleport(player, x, ++y, z);
    }

    private String searchSupplier() {
        return this.searchPrompt;
    }

    private void searchResponder(String searchPrompt) {
        this.searchPrompt = searchPrompt;
        markDirty();
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
        data.setBoolean("Active", this.isActive);
        data.setInteger("Progress", this.progress);
        data.setInteger("MaxProgress", this.maxProgress);
        data.setTag("Link.XYZ", this.linkData);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.isActive = data.getBoolean("Active");
        this.maxProgress = data.getInteger("MaxProgress");
        this.progress = data.getInteger("Progress");
        if (data.hasKey("Link.XYZ"))
            this.linkData = data.getCompoundTag("Link.XYZ");
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
        return getWorld().provider.getDimensionType().getId();
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
        return getWorld();
    }

    @Override
    public NBTTagCompound getLinkData() {
        return this.linkData;
    }

    @Override
    public void setLinkData(NBTTagCompound linkData) {
        this.linkData = linkData;
    }
}
