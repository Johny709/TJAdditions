package tj.integration.jei.multi;

import com.google.common.collect.Lists;
import gregicadditions.GAValues;
import gregicadditions.item.GAMetaItems;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregicadditions.machines.multi.impl.MetaTileEntityRotorHolderForNuclearCoolant;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.unification.material.Materials;
import gregtech.api.util.BlockInfo;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.items.behaviors.TurbineRotorBehavior;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import tj.machines.multi.electric.MetaTileEntityXLHotCoolantTurbine;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class XLHotCoolantTurbineInfo extends MultiblockInfoPage {

    public final MetaTileEntityXLHotCoolantTurbine turbine;

    public XLHotCoolantTurbineInfo(MetaTileEntityXLHotCoolantTurbine turbine) {
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
        holderNorth.setMetaTileEntity(GATileEntities.ROTOR_HOLDER[2]);
        holderNorth.getMetaTileEntity().setFrontFacing(EnumFacing.NORTH);
        holderSouth.setMetaTileEntity(GATileEntities.ROTOR_HOLDER[2]);
        holderSouth.getMetaTileEntity().setFrontFacing(EnumFacing.SOUTH);
        ItemStack rotorStack = GAMetaItems.HUGE_TURBINE_ROTOR.getStackForm();
        //noinspection ConstantConditions
        TurbineRotorBehavior.getInstanceFor(rotorStack).setPartMaterial(rotorStack, Materials.Darmstadtium);
        ((MetaTileEntityRotorHolderForNuclearCoolant) holderNorth.getMetaTileEntity()).getRotorInventory().setStackInSlot(0, rotorStack);
        ((MetaTileEntityRotorHolderForNuclearCoolant) holderSouth.getMetaTileEntity()).getRotorInventory().setStackInSlot(0, rotorStack);
        List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            GAMultiblockShapeInfo.Builder shapeInfo = GAMultiblockShapeInfo.builder(FRONT, UP, LEFT)
                    .aisle("CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCECCC", "CCCCCCC", "CCCCCCC", "CCCCCCC")
                    .aisle("CCCCCCC", "R#####T", "CCCCCCC", "CCCCCCC", "CCCCCCC", "R#####T", "CCCCCCC")
                    .aisle("CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC");
            for (int j = 0; j <= i; j++) {
                shapeInfo.aisle("CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC");
                shapeInfo.aisle("CCCCCCC", "R#####T", "CCCCCCC", "CCCCCCC", "CCCCCCC", "R#####T", "CCCCCCC");
                shapeInfo.aisle("CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC");
            }
            shapeInfo.aisle("CCCCCCC", "CCCCCCC", "CCCCCCC", "HCCCCCH", "CCCCCCC", "CCCCCCC", "CCCCCCC")
                    .aisle("CCCCCCC", "R#####T", "CCCCCCC", "HCCCCCH", "CCCCCCC", "R#####T", "CCCCCCC")
                    .aisle("CCCCCCC", "CCCCCCC", "CCCOCCC", "CCISJCC", "CCCMCCC", "CCCCCCC", "CCCCCCC")
                    .where('S', this.turbine, EnumFacing.WEST)
                    .where('C', this.turbine.turbineType.casingState)
                    .where('R', new BlockInfo(MetaBlocks.MACHINE.getDefaultState(), holderNorth))
                    .where('T', new BlockInfo(MetaBlocks.MACHINE.getDefaultState(), holderSouth))
                    .where('E', MetaTileEntities.ENERGY_OUTPUT_HATCH[GTValues.MAX], EnumFacing.EAST)
                    .where('I', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.MAX], EnumFacing.WEST)
                    .where('J', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.MAX], EnumFacing.WEST)
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST);
            if (this.turbine.turbineType.hasOutputHatch) {
                shapeInfo.where('O', MetaTileEntities.FLUID_EXPORT_HATCH[GAValues.EV], EnumFacing.WEST);
            } else {
                shapeInfo.where('O', this.turbine.turbineType.casingState);
            }
            shapeInfos.add(shapeInfo.build());
        }
        return shapeInfos;
    }

    @Override
    public String[] getDescription() {
        return new String[]{I18n.format("tj.multiblock.turbine.description"), I18n.format("tj.multiblock.turbine.fast_mode.description")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
