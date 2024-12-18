package com.johny.tj.integration.jei.multi;

import com.google.common.collect.Lists;
import com.johny.tj.machines.multi.electric.MetaTileEntityXLTurbine;
import gregicadditions.item.GAMetaItems;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.unification.material.Materials;
import gregtech.api.util.BlockInfo;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.items.behaviors.TurbineRotorBehavior;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityRotorHolder;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import java.util.List;

public class XLTurbineInfo extends MultiblockInfoPage {

    public final MetaTileEntityXLTurbine turbine;

    public XLTurbineInfo(MetaTileEntityXLTurbine turbine) {
        this.turbine = turbine;
    }

    @Override
    public MultiblockControllerBase getController() {
        return turbine;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        MetaTileEntityHolder holderNorth = new MetaTileEntityHolder();
        MetaTileEntityHolder holderSouth = new MetaTileEntityHolder();
        holderNorth.setMetaTileEntity(MetaTileEntities.ROTOR_HOLDER[2]);
        holderNorth.getMetaTileEntity().setFrontFacing(EnumFacing.NORTH);
        holderSouth.setMetaTileEntity(MetaTileEntities.ROTOR_HOLDER[2]);
        holderSouth.getMetaTileEntity().setFrontFacing(EnumFacing.SOUTH);
        ItemStack rotorStack = GAMetaItems.HUGE_TURBINE_ROTOR.getStackForm();
        //noinspection ConstantConditions
        TurbineRotorBehavior.getInstanceFor(rotorStack).setPartMaterial(rotorStack, Materials.Darmstadtium);
        ((MetaTileEntityRotorHolder) holderNorth.getMetaTileEntity()).getRotorInventory().setStackInSlot(0, rotorStack);
        ((MetaTileEntityRotorHolder) holderSouth.getMetaTileEntity()).getRotorInventory().setStackInSlot(0, rotorStack);
        MultiblockShapeInfo.Builder shapeInfo = MultiblockShapeInfo.builder()
                .aisle("CCCCCCCCC", "CRCCRCCRC", "CCCCCCCCC", "CCCCCCCCC", "CCCCCCCCC", "CRCCRCCRC", "CCCCCCCCC")
                .aisle("CCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC", "CCCCCCCCC", "CCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC")
                .aisle("CCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC", "ICCCCCCCC", "CCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC")
                .aisle("CCCCCCCCC", "C#CC#CC#C", "JCCCCCCCC", "SCCCCCCCE", "CCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC")
                .aisle("CCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC", "OCCCCCCCC", "CCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC")
                .aisle("CCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC", "CCCCCCCCC", "CCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC")
                .aisle("CCCCCCCCC", "CTCCTCCTC", "CCCCCCCCC", "CCCCCCCCC", "CCCCCCCCC", "CTCCTCCTC", "CCCCCCCCC")
                .where('S', turbine, EnumFacing.WEST)
                .where('C', turbine.turbineType.casingState)
                .where('R', new BlockInfo(MetaBlocks.MACHINE.getDefaultState(), holderNorth))
                .where('T', new BlockInfo(MetaBlocks.MACHINE.getDefaultState(), holderSouth))
                .where('E', MetaTileEntities.ENERGY_OUTPUT_HATCH[GTValues.MAX], EnumFacing.EAST)
                .where('#', Blocks.AIR.getDefaultState())
                .where('I', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.MAX], EnumFacing.WEST)
                .where('J', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.MAX], EnumFacing.WEST);
        if (turbine.turbineType.hasOutputHatch) {
            shapeInfo.where('O', MetaTileEntities.FLUID_EXPORT_HATCH[GTValues.MAX], EnumFacing.WEST);
        } else {
            shapeInfo.where('O', turbine.turbineType.casingState);
        }
        return Lists.newArrayList(shapeInfo.build());
    }

    @Override
    public String[] getDescription() {
        return new String[]{I18n.format("tj.multiblock.turbine.description")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
