package tj.machines.multi.steam;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.IWorkable;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.BlockFireboxCasing;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;
import tj.blocks.AbilityBlocks;
import tj.blocks.TJMetaBlocks;
import tj.builder.multicontrollers.MultiblockDisplayBuilder;
import tj.builder.multicontrollers.TJMultiblockDisplayBase;
import tj.capability.IHeatInfo;
import tj.capability.TJCapabilities;
import tj.multiblockpart.TJMultiblockAbility;

import java.util.*;
import java.util.function.Predicate;

import static gregicadditions.capabilities.GregicAdditionsCapabilities.MUFFLER_HATCH;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withHoverTextTranslate;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.IMPORT_FLUIDS;
import static gregtech.api.unification.material.Materials.*;

public class MetaTileEntityLargeSolarBoiler extends TJMultiblockDisplayBase implements IWorkable, IHeatInfo {

    private static final FluidStack WATER = Water.getFluid(1);
    private static final FluidStack DISTILLED_WATER = DistilledWater.getFluid(1);
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private IMultipleTankHandler waterTank;
    private IMultipleTankHandler steamTank;
    private BlockPos offSetPos;
    private boolean isActive;
    private boolean hadWater;
    private int steamProduction;
    private int waterConsumption;
    private int calcification;
    private int temp;

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_FLUIDS, GregicAdditionsCapabilities.MAINTENANCE_HATCH, GregicAdditionsCapabilities.MUFFLER_HATCH};
    private final Set<BlockPos> activeStates = new HashSet<>();

    public MetaTileEntityLargeSolarBoiler(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeSolarBoiler(this.metaTileEntityId);
    }

    @Override
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        boolean hasInputFluid = abilities.containsKey(IMPORT_FLUIDS);
        boolean hasSteamOutput = abilities.containsKey(TJMultiblockAbility.STEAM_OUTPUT);
        boolean hasOutputFluid = abilities.containsKey(MultiblockAbility.EXPORT_FLUIDS);
        int mufflerCount = abilities.getOrDefault(MUFFLER_HATCH, Collections.emptyList()).size();

        return mufflerCount == 1 && hasInputFluid && (hasOutputFluid || hasSteamOutput) && super.checkStructureComponents(parts, abilities);
    }

    @Override
    protected void updateFormedValid() {
        if (((this.getProblems() >> 5) & 1) == 0) return;
        if (this.getOffsetTimer() % 20 == 0) {
            if (this.isWorkingEnabled && this.canBurn()) {
                if (!this.isActive)
                    setActive(true);
                this.temp = MathHelper.clamp(this.temp + 20, 0, 12000);
            } else {
                if (this.isActive)
                    setActive(false);
                this.temp = MathHelper.clamp(this.temp - 10, 0, 12000);
            }
        }
        if (!this.canGenerateSteam() || this.getOffsetTimer() < 20) {
            this.hadWater = false;
            return;
        }
        int waterToConsume = Math.round((900 * this.getTempPercent()) / 160);
        FluidStack waterStack = this.waterTank.drain(waterToConsume, false);
        boolean hasEnoughWater = waterStack != null && (waterStack.isFluidEqual(WATER) || waterStack.isFluidEqual(DISTILLED_WATER)) && waterStack.amount == waterToConsume || waterToConsume == 0;
        if (hasEnoughWater && this.hadWater) {
            this.getWorld().setBlockToAir(this.getPos());
            this.getWorld().createExplosion(null,
                    this.getPos().getX() + 0.5, this.getPos().getY() + 0.5, this.getPos().getZ() + 0.5,
                    2.0f, true);
        } else if (hasEnoughWater) {
            this.steamProduction = this.steamTank.fill(Steam.getFluid(waterToConsume * 160), true);
            this.waterConsumption = this.waterTank.drain(waterToConsume, true).amount;
            if (!waterStack.isFluidEqual(DISTILLED_WATER))
                this.calcification = MathHelper.clamp(this.calcification + 1, 0, 240000);
        } else this.hadWater = true;
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (this.isStructureFormed()) {
            MultiblockDisplayBuilder.start(textList)
                    .temperature(this.heat(), this.maxHeat())
                    .fluidInput(true, Water.getFluid(this.waterConsumption))
                    .custom(text -> {
                        text.add(new TextComponentTranslation("gregtech.multiblock.large_boiler.steam_output", this.steamProduction, 900));

                        ITextComponent heatEffText = new TextComponentTranslation("gregtech.multiblock.large_boiler.heat_efficiency",100);
                        withHoverTextTranslate(heatEffText, "gregtech.multiblock.large_boiler.heat_efficiency.tooltip");
                        text.add(heatEffText);
                        if (!this.canBurn())
                            text.add(new TextComponentTranslation("tj.multiblock.large_solar_boiler.obstructed").setStyle(new Style().setColor(TextFormatting.RED)));
                    }).isWorking(this.isWorkingEnabled(), this.isActive(), this.getProgress(), this.getMaxProgress());
        }
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("XXX", "CCC", "CCC", "sss")
                .aisle("XXX", "CPC", "CPC", "sss")
                .aisle("XXX", "CSC", "CCC", "sss")
                .where('S', this.selfPredicate())
                .where('X', this.fireboxStatePredicate(GTUtility.getAllPropertyValues(MetaBlocks.BOILER_FIREBOX_CASING.getState(BlockFireboxCasing.FireboxCasingType.STEEL_FIREBOX), BlockFireboxCasing.ACTIVE))
                        .or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('C', statePredicate(MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.STEEL_SOLID))
                        .or(abilityPartPredicate(MultiblockAbility.EXPORT_FLUIDS, TJMultiblockAbility.STEAM_OUTPUT)))
                .where('P', statePredicate(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.STEEL_PIPE)))
                .where('s', statePredicate(TJMetaBlocks.ABILITY_BLOCKS.getState(AbilityBlocks.AbilityType.SOLAR_COLLECTOR)))
                .build();
    }

    public Predicate<BlockWorldState> fireboxStatePredicate(IBlockState... allowedStates) {
        return (blockWorldState) -> {
            IBlockState state = blockWorldState.getBlockState();
            if (ArrayUtils.contains(allowedStates, state)) {
                if (blockWorldState.getWorld() != null)
                    this.activeStates.add(blockWorldState.getPos());
                return true;
            }
            return false;
        };
    }

    private void replaceFireboxAsActive(boolean isActive) {
        this.activeStates.forEach(pos -> {
            IBlockState state = this.getWorld().getBlockState(pos);
            if (state.getBlock() instanceof BlockFireboxCasing) {
                state = state.withProperty(BlockFireboxCasing.ACTIVE, isActive);
                this.getWorld().setBlockState(pos, state);
            }
        });
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        List<IFluidTank> fluidTanks = new ArrayList<>();
        fluidTanks.addAll(this.getAbilities(MultiblockAbility.EXPORT_FLUIDS));
        fluidTanks.addAll(this.getAbilities(TJMultiblockAbility.STEAM_OUTPUT));

        this.waterTank = new FluidTankList(true, this.getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.steamTank = new FluidTankList(true, fluidTanks);
        this.offSetPos = this.getPos().offset(this.getFrontFacing().getOpposite(), 1);
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.waterTank = new FluidTankList(true);
        this.steamTank = new FluidTankList(true);
    }

    @Override
    public void onRemoval() {
        super.onRemoval();
        if (!this.getWorld().isRemote) {
            this.replaceFireboxAsActive(false);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, this.getFrontFacing(), this.isActive());
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        if (sourcePart instanceof IMultiblockAbilityPart) {
            MultiblockAbility<?> ability = ((IMultiblockAbilityPart<?>) sourcePart).getAbility();
            if (ability == MultiblockAbility.EXPORT_FLUIDS || ability == TJMultiblockAbility.STEAM_OUTPUT)
                return Textures.SOLID_STEEL_CASING;
        }
        return sourcePart == null ? Textures.SOLID_STEEL_CASING : this.isActive ? Textures.STEEL_FIREBOX_ACTIVE : Textures.STEEL_FIREBOX;
    }

    @Override
    public int getLightValueForPart(IMultiblockPart sourcePart) {
        if (sourcePart instanceof IMultiblockAbilityPart) {
            MultiblockAbility<?> ability = ((IMultiblockAbilityPart<?>) sourcePart).getAbility();
            if (ability == MultiblockAbility.EXPORT_FLUIDS || ability == TJMultiblockAbility.STEAM_OUTPUT)
                return 0;
        }
        return sourcePart == null ? 0 : this.isActive ? 15 : 0;
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 1) {
            this.isActive = buf.readBoolean();
            this.scheduleRenderUpdate();
        }
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

    protected void setActive(boolean active) {
        this.isActive = active;
        if (!this.getWorld().isRemote) {
            this.replaceFireboxAsActive(active);
            this.writeCustomData(1, buf -> buf.writeBoolean(active));
            this.markDirty();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("Temp", this.temp);
        data.setBoolean("HadWater", this.hadWater);
        data.setBoolean("IsActive", this.isActive);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.temp = data.getInteger("Temp");
        this.hadWater = data.getBoolean("HadWater");
        this.isActive = data.getBoolean("IsActive");
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == GregtechTileCapabilities.CAPABILITY_WORKABLE)
            return GregtechTileCapabilities.CAPABILITY_WORKABLE.cast(this);
        if (capability == TJCapabilities.CAPABILITY_HEAT)
            return TJCapabilities.CAPABILITY_HEAT.cast(this);
        return super.getCapability(capability, side);
    }

    private boolean canBurn() {
        return this.getWorld().isDaytime() && this.canSeeSky() && !this.getWorld().isRaining();
    }

    private boolean canSeeSky() {
        int startY = this.offSetPos.getY() + 3;
        for (int x = -1; x < 2; x++) {
            for (int y = startY; y <= this.getWorld().getHeight(); y++) {
                for (int z = -1; z < 2; z++) {
                    this.pos.setPos(this.offSetPos.getX() + x, y, this.offSetPos.getZ() + z);
                    if (this.getWorld().getBlockState(this.pos).getBlock() != Blocks.AIR)
                        return false;
                }
            }
        }
        return true;
    }

    public float getTempPercent() {
        return this.temp / (12000 * 1.00F);
    }

    @Override
    public int getProgress() {
        return this.canBurn() ? (int) this.getWorld().getWorldTime() % 24000 : 0;
    }

    @Override
    public int getMaxProgress() {
        return 12540;
    }

    @Override
    public boolean isActive() {
        return this.isActive;
    }

    public boolean canGenerateSteam() {
        return this.temp >= 2400;
    }

    @Override
    public long heat() {
        return (long) this.temp / 24;
    }

    @Override
    public long maxHeat() {
        return 500;
    }
}
