package tj.integration.jei.multi;

import com.google.common.collect.Lists;
import gregicadditions.GAValues;
import gregicadditions.item.GAMetaItems;
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

import java.util.List;

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
        MultiblockShapeInfo.Builder shapeInfo = MultiblockShapeInfo.builder()
                .aisle("CCCCCCCCC", "CRCCRCCRC", "CCCCCCCCC", "CCCCCCCCC", "CCCCCCCCC", "CRCCRCCRC", "CCCCCCCCC")
                .aisle("CCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC", "CCCCCCCCC", "CCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC")
                .aisle("CCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC", "ICCCCCCCC", "CCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC")
                .aisle("CCCCCCCCC", "C#CC#CC#C", "JCCCCCCCC", "SCCCCCCCE", "MCCCCCCCC", "C#CC#CC#C", "CCCCCCCCC")
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
                .where('J', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.MAX], EnumFacing.WEST)
                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST);
        if (turbine.turbineType.hasOutputHatch) {
            shapeInfo.where('O', MetaTileEntities.FLUID_EXPORT_HATCH[GAValues.EV], EnumFacing.WEST);
        } else {
            shapeInfo.where('O', turbine.turbineType.casingState);
        }
        return Lists.newArrayList(shapeInfo.build());
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
