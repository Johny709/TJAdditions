package com.johny.tj.machines.multi.electric;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.machines.LinkPos;
import com.johny.tj.machines.acceleratorBlacklist;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.EmitterCasing;
import gregicadditions.item.components.FieldGenCasing;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.block.machines.BlockMachine;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.pipenet.block.material.TileEntityMaterialPipeBase;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static gregtech.api.unification.material.Materials.UUMatter;

public class MetaTileEntityLargeWorldAccelerator extends TJMultiblockDisplayBase implements acceleratorBlacklist, LinkPos {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.INPUT_ENERGY, MultiblockAbility.IMPORT_FLUIDS, GregicAdditionsCapabilities.MAINTENANCE_HATCH};

    private long energyPerTick = 0;
    private boolean isActive = false;
    private AcceleratorMode acceleratorMode;
    private IMultipleTankHandler importFluidHandler;
    private IEnergyContainer energyContainer;
    private int tier;
    private int gtAcceleratorTier;
    private int energyMultiplier = 1;
    private BlockPos[] entityLinkBlockPos;
    private int fluidConsumption;

    public MetaTileEntityLargeWorldAccelerator(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.acceleratorMode = AcceleratorMode.RANDOM_TICK;
        this.isWorkingEnabled = false;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeWorldAccelerator(metaTileEntityId);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("tj.multiblock.large_world_accelerator.description"));
        tooltip.add("§f§n" + I18n.format("gregtech.machine.world_accelerator.mode.entity") + "§r§e§l -> §r" + I18n.format("tj.multiblock.world_accelerator.mode.entity.description"));
        tooltip.add("§f§n" + I18n.format("gregtech.machine.world_accelerator.mode.tile") + "§r§e§l -> §r" + I18n.format("tj.multiblock.world_accelerator.mode.tile.description"));
        tooltip.add("§f§n" + I18n.format("tj.multiblock.large_world_accelerator.mode.GT") + "§r§e§l -> §r" + I18n.format("tj.multiblock.large_world_accelerator.mode.GT.description"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        if (isStructureFormed()) {
            WorldServer world = (WorldServer) this.getWorld();
            long maxVoltage = energyContainer.getInputVoltage();
            textList.add(hasEnoughEnergy(energyPerTick) ? new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", maxVoltage)
                    : new TextComponentTranslation("gregtech.multiblock.not_enough_energy")
                        .setStyle(new Style().setColor(TextFormatting.RED)));
            textList.add(isWorkingEnabled ? (isActive ? new TextComponentTranslation("gregtech.multiblock.running").setStyle(new Style().setColor(TextFormatting.GREEN))
                    : new TextComponentTranslation("gregtech.multiblock.idling"))
                    : new TextComponentTranslation("gregtech.multiblock.work_paused").setStyle(new Style().setColor(TextFormatting.YELLOW)));

            switch (acceleratorMode) {
                case TILE_ENTITY:
                    textList.add(new TextComponentTranslation("gregtech.machine.world_accelerator.mode.tile"));
                    textList.add(new TextComponentTranslation("tj.multiblock.large_world_accelerator.linked")
                            .setStyle(new Style().setBold(true).setUnderlined(true)));
                    Arrays.stream(entityLinkBlockPos).filter(Objects::nonNull).forEach(blockPos -> textList.add(new TextComponentString(world.getTileEntity(blockPos).getBlockType().getLocalizedName())
                            .appendSibling(new TextComponentString(" X: ").appendSibling(new TextComponentString("" + blockPos.getX()).setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                            .appendSibling(new TextComponentString(" Y: ").appendSibling(new TextComponentString("" + blockPos.getY()).setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                            .appendSibling(new TextComponentString(" Z: ").appendSibling(new TextComponentString("" + blockPos.getZ()).setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))));
                    break;

                case GT_TILE_ENTITY:
                    textList.add(hasEnoughFluid(fluidConsumption) ? new TextComponentTranslation("tj.multiblock.enough_fluid")
                            .appendSibling(new TextComponentString(" " + UUMatter.getLocalizedName() + ": " + fluidConsumption + "/t")
                                    .setStyle(new Style().setColor(TextFormatting.YELLOW))) :
                            new TextComponentTranslation("tj.multiblock.not_enough_fluid").setStyle(new Style().setColor(TextFormatting.RED)));
                    textList.add(new TextComponentTranslation("tj.multiblock.large_world_accelerator.mode.GT"));
                    if (gtAcceleratorTier > 0) {
                        textList.add(new TextComponentTranslation("tj.multiblock.large_world_accelerator.linked")
                                .setStyle(new Style().setBold(true).setUnderlined(true)));
                        Arrays.stream(entityLinkBlockPos).filter(Objects::nonNull).forEach(blockPos -> textList.add(new TextComponentTranslation(BlockMachine.getMetaTileEntity(world, blockPos).getMetaFullName())
                                .appendSibling(new TextComponentString(" X: ").appendSibling(new TextComponentString("" + blockPos.getX()).setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                                .appendSibling(new TextComponentString(" Y: ").appendSibling(new TextComponentString("" + blockPos.getY()).setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                                .appendSibling(new TextComponentString(" Z: ").appendSibling(new TextComponentString("" + blockPos.getZ()).setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))));
                    } else {
                        textList.add(new TextComponentTranslation("tj.multiblock.large_world_accelerator.tier", GAValues.VN[10]));
                    }
                    break;

                case RANDOM_TICK:
                    textList.add(new TextComponentTranslation("gregtech.machine.world_accelerator.mode.entity"));

            }
        }
        super.addDisplayText(textList);
    }

    @Override
    protected void updateFormedValid() {
        if (getOffsetTimer() > 100) {
            if (!isWorkingEnabled) {
                if (isActive)
                    setActive(false);
                return;
            }
            if (getNumProblems() < 6) {
                if (getOffsetTimer() % (1 + getNumProblems()) == 0) {
                    if (hasEnoughEnergy(energyPerTick)) {
                        if (!isActive)
                            setActive(true);

                        energyContainer.removeEnergy(energyPerTick);
                        WorldServer world = (WorldServer) this.getWorld();
                        switch (acceleratorMode) {

                            case TILE_ENTITY:
                                for (BlockPos pos : entityLinkBlockPos) {
                                    if (pos == null) {
                                        continue;
                                    }
                                    TileEntity targetTE = world.getTileEntity(pos);
                                    if (targetTE == null || targetTE instanceof TileEntityMaterialPipeBase || targetTE instanceof MetaTileEntityHolder) {
                                        continue;
                                    }
                                    boolean horror = false;
                                    if (clazz != null && targetTE instanceof ITickable) {
                                        horror = clazz.isInstance(targetTE);
                                    }
                                    if (targetTE instanceof ITickable && (!horror || !world.isRemote)) {
                                        IntStream.range(0, (int) Math.pow(2, tier)).forEach(value -> ((ITickable) targetTE).update());
                                    }
                                }
                                return;

                            case GT_TILE_ENTITY:
                                if (gtAcceleratorTier < 1) {
                                    return;
                                }
                                if (hasEnoughFluid(fluidConsumption)) {
                                    importFluidHandler.drain(UUMatter.getFluid(fluidConsumption), true);
                                    if (entityLinkBlockPos[0] != null) {
                                        MetaTileEntity targetGTTE = BlockMachine.getMetaTileEntity(world, entityLinkBlockPos[0]);
                                        if (targetGTTE == null || targetGTTE instanceof acceleratorBlacklist) {
                                            return;
                                        }
                                        IntStream.range(0, (int) Math.pow(2, gtAcceleratorTier)).forEach(value -> targetGTTE.update());
                                    }
                                }
                                return;

                            case RANDOM_TICK:
                                BlockPos worldAcceleratorPos = getPos().offset(getFrontFacing().getOpposite());
                                BlockPos upperConner = worldAcceleratorPos.north(tier).east(tier);
                                for (int x = 0; x < getArea(); x++) {
                                    BlockPos row = upperConner.south(x);
                                    for (int y = 0; y < getArea(); y++) {
                                        BlockPos cell = row.west(y);
                                        IBlockState targetBlock = world.getBlockState(cell);
                                        IntStream.range(0, (int) Math.pow(2, tier)).forEach(value -> {
                                            if (world.rand.nextInt(100) == 0) {
                                                if (targetBlock.getBlock().getTickRandomly()) {
                                                    targetBlock.getBlock().randomTick(world, cell, targetBlock, world.rand);
                                                }
                                            }
                                        });
                                    }
                                }
                        }
                    } else {
                        if (isActive)
                            setActive(false);
                    }
                }
            }
        }
    }

    private boolean hasEnoughEnergy(long amount) {
        return energyContainer.getEnergyStored() >= amount;
    }

    private boolean hasEnoughFluid(int amount) {
        FluidStack fluidStack = importFluidHandler.drain(UUMatter.getFluid(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    public int getArea() {
        return (tier * 2) + 1;
    }

    static Class clazz;

    static {
        try {
            clazz = Class.forName("cofh.core.block.TileCore");
        } catch (Exception ignored) {

        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.importFluidHandler = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        FieldGenCasing.CasingType fieldGen = context.getOrDefault("FieldGen", FieldGenCasing.CasingType.FIELD_GENERATOR_LV);
        EmitterCasing.CasingType emitter = context.getOrDefault("Emitter", EmitterCasing.CasingType.EMITTER_LV);
        tier = Math.min(fieldGen.getTier(), emitter.getTier());
        gtAcceleratorTier = tier - GAValues.UHV;
        energyPerTick = (long) (Math.pow(4, tier) * 8) * energyMultiplier;
        fluidConsumption = (int) Math.pow(4, gtAcceleratorTier - 1) * 1000;
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("#C#", "CEC", "#C#")
                .aisle("CEC", "EFE", "CEC")
                .aisle("#C#", "CSC", "#C#")
                .where('S', selfPredicate())
                .where('C', statePredicate(GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.TRITANIUM)).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('F', LargeSimpleRecipeMapMultiblockController.fieldGenPredicate())
                .where('E', LargeSimpleRecipeMapMultiblockController.emitterPredicate())
                .where('#', (tile) -> true)
                .build();
    }


    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart iMultiblockPart) {
        return ClientHandler.TRITANIUM_CASING;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.AMPLIFAB_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), isActive);
    }

    protected void setActive(boolean active) {
        this.isActive = active;
        markDirty();
        if (!getWorld().isRemote) {
            writeCustomData(1, buf -> buf.writeBoolean(active));
        }
    }

    @Override
    public boolean onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        if (!getWorld().isRemote) {
            String tileMode = "null";
            switch (acceleratorMode) {
                case RANDOM_TICK:
                    acceleratorMode = AcceleratorMode.TILE_ENTITY;
                    energyMultiplier = 6;
                    entityLinkBlockPos = new BlockPos[tier];
                    tileMode = "gregtech.machine.world_accelerator.mode.tile";
                    break;
                case TILE_ENTITY:
                    acceleratorMode = AcceleratorMode.GT_TILE_ENTITY;
                    energyMultiplier = 256;
                    entityLinkBlockPos = new BlockPos[1];
                    tileMode = "tj.multiblock.large_world_accelerator.mode.GT";
                    break;
                case GT_TILE_ENTITY:
                    acceleratorMode = AcceleratorMode.RANDOM_TICK;
                    energyMultiplier = 1;
                    tileMode = "gregtech.machine.world_accelerator.mode.entity";
            }
            energyPerTick = (long) (Math.pow(4, tier) * 8) * energyMultiplier;
            playerIn.sendStatusMessage(new TextComponentTranslation(tileMode), false);
        }
        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        for (int i = 0; i < entityLinkBlockPos.length; i++) {
            if (entityLinkBlockPos[i] != null) {
                data.setDouble("EntityLinkX" + i, entityLinkBlockPos[i].getX());
                data.setDouble("EntityLinkY" + i, entityLinkBlockPos[i].getY());
                data.setDouble("EntityLinkZ" + i, entityLinkBlockPos[i].getZ());
            }
        }
        data.setInteger("EnergyMultiplier", energyMultiplier);
        data.setInteger("AcceleratorMode", acceleratorMode.ordinal());
        data.setInteger("BlockPosSize", entityLinkBlockPos.length);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        energyMultiplier = data.getInteger("EnergyModifier");
        acceleratorMode = AcceleratorMode.values()[data.getInteger("AcceleratorMode")];
        entityLinkBlockPos = new BlockPos[data.getInteger("BlockPosSize")];
        for (int i = 0; i < entityLinkBlockPos.length; i++) {
            if (data.hasKey("EntityLinkX" + i) && data.hasKey("EntityLinkY" + i) && data.hasKey("EntityLinkY" + i)) {
                entityLinkBlockPos[i] = new BlockPos(data.getDouble("EntityLinkX" + i), data.getDouble("EntityLinkY" + i), data.getDouble("EntityLinkZ" + i));
            }
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

    @Override
    public int getBlockPosSize() {
        return entityLinkBlockPos.length;
    }

    @Override
    public BlockPos getBlockPos(int i) {
        return entityLinkBlockPos[i];
    }

    @Override
    public void setBlockPos(double x, double y, double z, boolean connect, int i) {
        entityLinkBlockPos[i] = connect ? new BlockPos(x, y, z) : null;
    }

    public enum AcceleratorMode {
        RANDOM_TICK,
        TILE_ENTITY,
        GT_TILE_ENTITY
    }

}
