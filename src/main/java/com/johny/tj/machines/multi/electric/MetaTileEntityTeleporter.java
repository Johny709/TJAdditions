package com.johny.tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.builder.multicontrollers.MultiblockDisplayBuilder;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.capability.IParallelController;
import com.johny.tj.capability.LinkPos;
import com.johny.tj.capability.TJCapabilities;
import com.johny.tj.gui.TJGuiTextures;
import com.johny.tj.gui.TJWidgetGroup;
import com.johny.tj.gui.widgets.TJAdvancedTextWidget;
import com.johny.tj.gui.widgets.TJClickButtonWidget;
import com.johny.tj.gui.widgets.TJTextFieldWidget;
import gregicadditions.GAValues;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GAMultiblockCasing2;
import gregicadditions.item.components.FieldGenCasing;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.IWorkable;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
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
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.johny.tj.gui.TJGuiTextures.CASE_SENSITIVE_BUTTON;
import static com.johny.tj.gui.TJGuiTextures.SPACES_BUTTON;
import static com.johny.tj.textures.TJTextures.FUSION_MK2;
import static com.johny.tj.textures.TJTextures.TELEPORTER_OVERLAY;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate2;
import static gregtech.api.gui.GuiTextures.BUTTON_CLEAR_GRID;
import static gregtech.api.gui.GuiTextures.DISPLAY;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.IMPORT_FLUIDS;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.INPUT_ENERGY;

// TODO re-add the queue to check if it still crashes when marking then teleport mobs in subsequent ticks
public class MetaTileEntityTeleporter extends TJMultiblockDisplayBase implements IParallelController, IWorkable, LinkPos {

    private IEnergyContainer energyContainer;
    private IMultipleTankHandler inputFluidHandler;
    private NBTTagCompound linkData;
    private long energyDrain;
    private int tier;
    private boolean isActive;
    private int progress;
    private int maxProgress = 20;
    private String selectedPosName;
    private String searchPrompt = "";
    private boolean isCaseSensitive;
    private boolean hasSpaces;
    private int searchResults;
    private final Map<String, Pair<World, BlockPos>> posMap = new HashMap<>();
    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_FLUIDS, INPUT_ENERGY, MAINTENANCE_HATCH};
    private static final Random random = new Random();

    public MetaTileEntityTeleporter(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityTeleporter(metaTileEntityId);
    }

    @Override
    protected void updateFormedValid() {
        if (!this.isWorkingEnabled || this.getNumProblems() >= 6 || this.maxProgress < 1) {
            if (this.isActive)
                this.setActive(false);
            return;
        }

        this.calculateMaintenance(1);
        if (this.progress > 0 && !this.isActive)
            this.setActive(true);

        if (this.progress >= this.maxProgress) {
            this.progress = 0;
            if(this.isActive)
                this.setActive(false);
            Pair<World, BlockPos> worldPos = this.posMap.get(this.selectedPosName);
            if (worldPos == null) {
                return;
            }
            World world = worldPos.getLeft();
            BlockPos targetPos = worldPos.getValue();
            BlockPos pos = this.getPos().up();
            int worldID = world.provider.getDimension();
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            if (this.getWorld().provider.getDimension() != worldID && DimensionManager.getWorld(worldID) != null) {
                DimensionManager.initDimension(worldID);
                DimensionManager.keepDimensionLoaded(worldID, true);
            }
            List<ClassInheritanceMultiMap<Entity>> entityLists = Arrays.stream(this.getWorld().getChunk(pos).getEntityLists()).collect(Collectors.toCollection(ArrayList::new));
            for (ClassInheritanceMultiMap<Entity> entities : entityLists) {
                for (Entity entity : entities) {
                    BlockPos entityPos = entity.getPosition();
                    int entityX = entityPos.getX();
                    int entityY = entityPos.getY();
                    int entityZ = entityPos.getZ();
                    if (entityX == x && entityY == y && entityZ == z)
                        this.transportEntity(entity, world, targetPos);
                }
            }
        }

        if (this.progress <= 0) {
            this.progress = 1;
            if (!this.isActive)
                this.setActive(true);
        } else {
            this.progress++;
        }
    }

    private void transportEntity(Entity entity, World world, BlockPos pos) {
        int worldID = world.provider.getDimension();
        long energyX = Math.abs(pos.getX() - this.getPos().getX()) * 1000L;
        long energyY = Math.abs(pos.getY() - this.getPos().getY()) * 1000L;
        long energyZ = Math.abs(pos.getZ() - this.getPos().getZ()) * 1000L;
        boolean interDimensional = false;
        if (worldID != this.getWorld().provider.getDimension()) {
            interDimensional = true;
            energyX = Math.abs(pos.getX() * 1000);
            energyY = Math.abs(pos.getY() * 1000);
            energyZ = Math.abs(pos.getZ() * 1000);
        }

        long totalEnergyConsumption = 1000000 + energyX + energyY + energyZ;
        if (!hasEnoughEnergy(totalEnergyConsumption)) {
            entity.sendMessage(new TextComponentString(I18n.translateToLocal("gregtech.multiblock.not_enough_energy") + "\n" + I18n.translateToLocal("tj.multiblock.teleporter.fail")));
            return;
        }
        this.energyContainer.removeEnergy(totalEnergyConsumption);
        this.generateParticles(this.getWorld(), entity, this.getPos().getX(), this.getPos().getY(), this.getPos().getZ());

        if (interDimensional) {
            entity.setWorld(world);
            entity.changeDimension(worldID, new Teleporter((WorldServer) world) {
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
        this.generateParticles(world, entity, pos.getX(), pos.getY(), pos.getZ());
        entity.sendMessage(new TextComponentTranslation("tj.multiblock.teleporter.success"));
        DimensionManager.keepDimensionLoaded(worldID, false);
    }

    private void generateParticles(World world, Entity entity, int posX, int posY, int posZ) {
        for (int j = 0; j < 128; ++j) {
            double d6 = (double) j / 127.0D;
            float f = (random.nextFloat() - 0.5F) * 0.2F;
            float f1 = (random.nextFloat() - 0.5F) * 0.2F;
            float f2 = (random.nextFloat() - 0.5F) * 0.2F;
            double d3 = (double) posX + (posX - (double) posX) * d6 + (random.nextDouble() - 0.5D) * (double) entity.width * 2.0D;
            double d4 = (double) posY + (posY - (double) posY) * d6 + random.nextDouble() * (double) entity.height;
            double d5 = (double) posZ + (posZ - (double) posZ) * d6 + (random.nextDouble() - 0.5D) * (double) entity.width * 2.0D;
            world.spawnParticle(EnumParticleTypes.PORTAL, d3, d4, d5, f, f1, f2);
        }
    }

    private boolean hasEnoughEnergy(long amount) {
        return this.energyContainer.getEnergyStored() >= amount;
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
        int fieldGen = context.getOrDefault("FieldGen", FieldGenCasing.CasingType.FIELD_GENERATOR_LV).getTier();
        this.tier = Math.min(fieldGen, Math.max(framework, framework2));
        this.energyDrain = (long) (Math.pow(4, this.tier) * 8);
        this.energyContainer = new EnergyContainerList(this.getAbilities(INPUT_ENERGY));
        this.inputFluidHandler = new FluidTankList(true, this.getAbilities(IMPORT_FLUIDS));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("~CFC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("CHHHC", "~HHH~", "~C#C~", "~C#C~", "~C#C~", "~HHH~", "~~H~~")
                .aisle("FHfHF", "~HSH~", "~###~", "~###~", "~###~", "~HFH~", "~HfH~")
                .aisle("CHHHC", "~HHH~", "~C#C~", "~C#C~", "~C#C~", "~HHH~", "~~H~~")
                .aisle("~CFC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .where('S', this.selfPredicate())
                .where('C', statePredicate(this.getCasingState()))
                .where('H', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('F', frameworkPredicate().or(frameworkPredicate2()))
                .where('f', LargeSimpleRecipeMapMultiblockController.fieldGenPredicate())
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
    protected ModularUI.Builder createUITemplate(EntityPlayer player) {
        return this.createUI(player, 18);
    }

    @Override
    protected void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs) {
        super.addNewTabs(tabs);
        TJWidgetGroup widgetPosGroup = new TJWidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.pos", new ItemStack(Items.COMPASS), this.blockPosTab(widgetPosGroup::addWidgets)));
    }

    @Override
    protected AbstractWidgetGroup mainDisplayTab(Function<Widget, WidgetGroup> widgetGroup) {
        super.mainDisplayTab(widgetGroup);
        widgetGroup.apply(new ImageWidget(28, 112, 141, 18, DISPLAY));
        widgetGroup.apply(new TJTextFieldWidget(33, 117, 136, 18, false, this::getTickSpeed, this::setTickSpeed)
                .setTooltipText("machine.universal.tick.speed")
                .setTooltipFormat(this::getTickSpeedFormat)
                .setValidator(str -> Pattern.compile("\\*?[0-9_]*\\*?").matcher(str).matches()));
        widgetGroup.apply(new ClickButtonWidget(7, 112, 18, 18, "+", this::onIncrement));
        widgetGroup.apply(new ClickButtonWidget(172, 112, 18, 18, "-", this::onDecrement));
        return widgetGroup.apply(new ToggleButtonWidget(172, 151, 18, 18, TJGuiTextures.RESET_BUTTON, this::isReset, this::setReset)
                .setTooltipText("machine.universal.toggle.reset"));
    }

    private AbstractWidgetGroup blockPosTab(Function<Widget, WidgetGroup> widgetGroup) {
        ScrollableListWidget scrollWidget = new ScrollableListWidget(10, 12, 178, 97) {
            @Override
            public boolean isWidgetClickable(Widget widget) {
                return true; // this ScrollWidget will only add one widget so checks are unnecessary if position changes.
            }
        };
        scrollWidget.addWidget(new TJAdvancedTextWidget(0, 0, this::addPosDisplayText2, 0xFFFFFF)
                .setClickHandler(this::handlePosDisplayClick)
                .setMaxWidthLimit(1000));
        widgetGroup.apply(new AdvancedTextWidget(10, 0, this::addPosDisplayText, 0xFFFFFF));
        widgetGroup.apply(scrollWidget);
        widgetGroup.apply(new ToggleButtonWidget(172, 133, 18, 18, CASE_SENSITIVE_BUTTON, this::isCaseSensitive, this::setCaseSensitive)
                .setTooltipText("machine.universal.case_sensitive"));
        widgetGroup.apply(new ToggleButtonWidget(172, 151, 18, 18, SPACES_BUTTON, this::hasSpaces, this::setSpaces)
                .setTooltipText("machine.universal.spaces"));
        widgetGroup.apply(new ImageWidget(7, 112, 162, 18, DISPLAY));
        widgetGroup.apply(new TJClickButtonWidget(172, 112, 18, 18, "", this::onClear)
                .setTooltipText("machine.universal.toggle.clear")
                .setButtonTexture(BUTTON_CLEAR_GRID));
        return widgetGroup.apply(new TJTextFieldWidget(12, 117, 157, 18, false, this::getSearchPrompt, this::setSearchPrompt)
                .setBackgroundText("machine.universal.search")
                .setValidator(str -> Pattern.compile(".*").matcher(str).matches()));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        MultiblockDisplayBuilder.start(textList)
                .voltageTier(this.energyContainer)
                .custom(text -> {
                    Pair<World, BlockPos> selectedPos = this.posMap.get(this.selectedPosName);
                    if (selectedPos != null) {
                        World world = selectedPos.getLeft();
                        BlockPos pos = selectedPos.getRight();

                        text.add(new TextComponentTranslation("tj.multiblock.teleporter.selected.world", world.provider.getDimensionType().getName(), world.provider.getDimension()));
                        text.add(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.teleporter.selected.pos", pos.getX(), pos.getY(), pos.getZ())));
                    }
                })
                .isWorking(this.isWorkingEnabled, this.isActive, this.progress, this.maxProgress);
    }

    private void addPosDisplayText(List<ITextComponent> textList) {
        textList.add(new TextComponentString("§l" + I18n.translateToLocal("tj.multiblock.tab.pos") + "§r(§e" + this.searchResults + "§r/§e" + this.posMap.size() + "§r)"));
    }

    private void addPosDisplayText2(List<ITextComponent> textList) {
        int count = 0, searchResults = 0;
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
            searchResults++;
        }
        this.searchResults = searchResults;
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

            if (this.getWorld().provider.getDimension() != worldID && DimensionManager.getWorld(worldID) != null) {
                DimensionManager.initDimension(worldID);
                DimensionManager.keepDimensionLoaded(worldID, true);
            }
            WorldServer dimension = DimensionManager.getWorld(worldID);
            BlockPos blockPos = new BlockPos(posX, posY, posZ);
            this.transportEntity(player, dimension, blockPos);

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

    private String[] getTickSpeedFormat() {
        return ArrayUtils.toArray(String.valueOf(this.maxProgress));
    }

    private void onIncrement(Widget.ClickData clickData) {
        this.maxProgress = MathHelper.clamp(this.maxProgress * 2, 1, Integer.MAX_VALUE);
        this.markDirty();
    }

    private void onDecrement(Widget.ClickData clickData) {
        this.maxProgress = MathHelper.clamp(this.maxProgress / 2, 1, Integer.MAX_VALUE);
        this.markDirty();
    }

    private String getTickSpeed() {
        return String.valueOf(this.maxProgress);
    }

    private void setTickSpeed(String maxProgress) {
        this.maxProgress = maxProgress.isEmpty() ? 1 : Integer.parseInt(maxProgress);
        this.markDirty();
    }

    private String getSearchPrompt() {
        return this.searchPrompt;
    }

    private void setSearchPrompt(String searchPrompt) {
        this.searchPrompt = searchPrompt;
        this.markDirty();
    }

    private void onClear(Widget.ClickData clickData) {
        this.setSearchPrompt("");
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
        this.linkData.setInteger("I", this.getPosSize());
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
        buf.writeInt(this.maxProgress);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
        this.maxProgress = buf.readInt();
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
