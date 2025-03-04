package com.johny.tj.machines.multi.electric;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import com.johny.tj.TJConfig;
import com.johny.tj.blocks.BlockSolidCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import com.johny.tj.machines.TJMiner;
import com.johny.tj.textures.TJTextures;
import gregicadditions.item.components.MotorCasing;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.items.MetaItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import java.util.List;

import static gregicadditions.GAMaterials.TungstenTitaniumCarbide;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.motorPredicate;
import static gregtech.api.unification.material.Materials.DrillingFluid;

public class MetaTileEntityWorldDestroyer extends MetaTileEntityEliteLargeMiner {

    private int chunkArea = 1, chunkAreaLimit;

    public MetaTileEntityWorldDestroyer(ResourceLocation metaTileEntityId, Type type) {
        super(metaTileEntityId, type);
        isWorkingEnabled = false;
    }

    @Override
    public IBlockState getFrameState() {
        return MetaBlocks.FRAMES.get(TungstenTitaniumCarbide).getDefaultState();
    }

    @Override
    public IBlockState getCasingState() {
        return TJMetaBlocks.SOLID_CASING.getState(BlockSolidCasings.SolidCasingType.TUNGSTEN_TITANIUM_CARBIDE_CASING);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return TJTextures.TUNGSTEN_TITANIUM_CARBIDE;
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("F###F", "F###F", "PQQQP", "#####", "#####", "#####", "#####", "#####", "#####", "#####")
                .aisle("#####", "#####", "QPPPQ", "#CCC#", "#####", "#####", "#####", "#####", "#####", "#####")
                .aisle("#####", "#####", "QPMPQ", "#CPC#", "#FFF#", "#FFF#", "#FFF#", "##F##", "##F##", "##F##")
                .aisle("#####", "#####", "CPPPQ", "#CSC#", "#####", "#####", "#####", "#####", "#####", "#####")
                .aisle("F###F", "F###F", "PQQQP", "#####", "#####", "#####", "#####", "#####", "#####", "#####")
                .setAmountAtLeast('L', 3)
                .where('S', selfPredicate())
                .where('L', statePredicate(getCasingState()))
                .where('C', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('P', statePredicate(getCasingState()))
                .where('M', motorPredicate())
                .where('Q', statePredicate(getCasingState()).or(abilityPartPredicate(MultiblockAbility.EXPORT_ITEMS)))
                .where('F', statePredicate(getFrameState()))
                .where('#', blockWorldState -> true)
                .build();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityWorldDestroyer(metaTileEntityId, getType());
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("tj.multiblock.elite_large_miner.description", type.chunk, type.chunk, type.fortuneString));
        tooltip.add(I18n.format("gtadditions.machine.miner.fluid_usage", type.drillingFluidConsumePerTick, I18n.format(DrillingFluid.getFluid(0).getUnlocalizedName())));
        tooltip.add(I18n.format("tj.multiblock.elite_large_miner.chunk.info"));
    }

    @Override
    public boolean onRightClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        if (playerIn.getHeldItemMainhand().isItemEqual(MetaItems.HARD_HAMMER.getStackForm())) {
            if (!getWorld().isRemote) {
                if (!playerIn.isSneaking()) {
                    if (chunkArea < chunkAreaLimit) {
                        chunkArea++;
                        playerIn.sendMessage(new TextComponentTranslation("tj.multiblock.elite_large_miner.chunk.increase")
                                .appendSibling(new TextComponentString(" " + chunkArea + "x" + chunkArea).setStyle(new Style().setColor(TextFormatting.YELLOW))));
                    } else {
                        playerIn.sendMessage(new TextComponentTranslation("tj.multiblock.elite_large_miner.chunk.max")
                                .appendSibling(new TextComponentString(" " + chunkArea + "x" + chunkArea).setStyle(new Style().setColor(TextFormatting.YELLOW))));
                    }
                } else {
                    if (chunkArea > 1) {
                        chunkArea--;
                        playerIn.sendMessage(new TextComponentTranslation("tj.multiblock.elite_large_miner.chunk.decrease")
                                .appendSibling(new TextComponentString(" " + chunkArea + "x" + chunkArea).setStyle(new Style().setColor(TextFormatting.YELLOW))));
                    } else {
                        playerIn.sendMessage(new TextComponentTranslation("tj.multiblock.elite_large_miner.chunk.min")
                                .appendSibling(new TextComponentString(" " + chunkArea + "x" + chunkArea).setStyle(new Style().setColor(TextFormatting.YELLOW))));
                    }
                }
                done = false;
                isWorkingEnabled = false;
                if (!chunks.isEmpty()) {
                    currentChunk.set(0);
                    x.set(chunks.get(currentChunk.intValue()).getPos().getXStart());
                    z.set(chunks.get(currentChunk.intValue()).getPos().getZStart());
                    y.set(getPos().getY() - 5);
                    chunks.clear();
                }
            }
            return true;
        }
        return super.onRightClick(playerIn, hand, facing, hitResult);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        chunkAreaLimit = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier() * TJConfig.worldDestroyerMiner.worldDestroyerChunkMultiplier;
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        chunkAreaLimit = 0;
        chunkArea = 1;

    }

    @Override
    protected void updateFormedValid() {
        if (isWorkingEnabled) {
            if (getNumProblems() < 6) {
                if (getOffsetTimer() % (1 + getNumProblems()) == 0) {
                    if (done || !drainEnergy()) {
                        if (isActive)
                            setActive(false);
                        return;
                    }

                    if (!isActive)
                        setActive(true);

                    calculateMaintenance(1 + getNumProblems());
                    WorldServer world = (WorldServer) this.getWorld();
                    Chunk chunkMiner = world.getChunk(getPos());
                    Chunk origin;
                    int chunkSize = type.chunk * chunkArea;
                    if (chunks.isEmpty() && chunkSize / 2.0 > 1.0) {
                        int tmp = Math.floorDiv(chunkSize, 2);
                        origin = world.getChunk(chunkMiner.x - tmp, chunkMiner.z - tmp);
                        for (int i = 0; i < chunkSize; i++) {
                            for (int j = 0; j < chunkSize; j++) {
                                chunks.add(world.getChunk(origin.x + i, origin.z + j));
                            }
                        }
                    } else if (chunks.isEmpty() && chunkSize == 1) {
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
                        y.set(getPos().getY() - 5);
                    }

                    List<BlockPos> blockPos = TJMiner.getBlockToMinePerChunk(this, x, y, z, chunk.getPos());
                    blockPos.forEach(blockPos1 -> {
                        NonNullList<ItemStack> itemStacks = NonNullList.create();
                        IBlockState blockState = this.getWorld().getBlockState(blockPos1);
                        int meta = blockState.getBlock().getMetaFromState(blockState);
                        int maxState = blockState.getBlock().getBlockState().getValidStates().size() - 1;
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
                            itemStacks.add(new ItemStack(actualState.getBlock().getItemDropped(actualState, world.rand, type.fortune), 1, Math.min(meta, maxState)));
                        } else {
                            itemStacks.add(new ItemStack(actualState.getBlock(), 1, Math.min(meta, maxState)));
                        }
                        if (addItemsToItemHandler(outputInventory, true, itemStacks)) {
                            addItemsToItemHandler(outputInventory, false, itemStacks);
                            if (this.getType() != Type.CREATIVE) {
                                world.setBlockState(blockPos1, Blocks.AIR.getDefaultState());
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
                                    y.set(getPos().getY() - 5);
                                } else
                                    done = true;
                            } else {
                                x.set(chunks.get(currentChunk.intValue()).getPos().getXStart());
                                z.set(chunks.get(currentChunk.intValue()).getPos().getZStart());
                                y.set(getPos().getY() - 5);
                            }
                        } else {
                            x.set(chunks.get(currentChunk.intValue()).getPos().getXStart());
                            z.set(chunks.get(currentChunk.intValue()).getPos().getZStart());
                            y.set(getPos().getY() - 5);
                        }
                    }

                    if (!getWorld().isRemote && getOffsetTimer() % 5 == 0) {
                        pushItemsIntoNearbyHandlers(getFrontFacing());
                    }
                }
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("ChunkLimit", chunkAreaLimit);
        data.setInteger("ChunkMultiplier", chunkArea);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        chunkAreaLimit = data.getInteger("ChunkLimit");
        chunkArea = data.getInteger("ChunkMultiplier");
    }
}
