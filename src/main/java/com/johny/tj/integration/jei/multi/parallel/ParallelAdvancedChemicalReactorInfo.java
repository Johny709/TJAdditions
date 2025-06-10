package com.johny.tj.integration.jei.multi.parallel;

import com.johny.tj.machines.TJMetaTileEntities;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.unification.material.Materials.Steel;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.WEST;

public class ParallelAdvancedChemicalReactorInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.ADVANCED_PARALLEL_CHEMICAL_REACTOR;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();
        for (int shapeInfo = 1; shapeInfo <= 16; shapeInfo++) {
            GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(FRONT, RIGHT, DOWN);
            if (!(shapeInfo % 2 == 0)) {
                builder.aisle("C~~~C~CCCCC~~~~~~", "CCCCC~CCCCC~~~~~~", "C~~~C~CCCCC~~~~~~", "CCCCC~CCCCC~~~~~~", "C~~~C~CCCCC~~~~~~");
                builder.aisle("CCCCC~F~~~F~~~~~~", "CcccC~~~P~~~~~~~~", "CPPPPPPPpP~~~~~~~", "CcccC~~~P~~~~~~~~", "CCCCC~F~~~F~~~~~~");
                builder.aisle("C~~~C~F~~~F~~~~~~", "CPPPPPPPP~~~~~~~~", "CmmmC~~PpP~~~~~~~", "CPPPPPPPP~~~~~~~~", "C~~~C~F~~~F~~~~~~");
                builder.aisle("CCCCC~F~~~F~~~~~~", "CcccC~~~P~~~~~~~~", "CPPPPPPPpP~~~~~~~", "CcccC~~~P~~~~~~~~", "CCCCC~F~~~F~~~~~~");
            } else {
                builder.aisle("C~~~C~CCCCC~C~~~C", "CCCCC~CCCCC~CCCCC", "C~~~C~CCCCC~C~~~C", "CCCCC~CCCCC~CCCCC", "C~~~C~CCCCC~C~~~C");
                builder.aisle("CCCCC~F~~~F~CCCCC", "CcccC~~~P~~~CcccC", "CPPPPPPPpPPPPPPPC", "CcccC~~~P~~~CcccC", "CCCCC~F~~~F~CCCCC");
                builder.aisle("C~~~C~F~~~F~C~~~C", "CPPPPPPPPPPPPPPPC", "CmmmC~~PpP~~CmmmC", "CPPPPPPPPPPPPPPPC", "C~~~C~F~~~F~C~~~C");
                builder.aisle("CCCCC~F~~~F~CCCCC", "CcccC~~~P~~~CcccC", "CPPPPPPPpPPPPPPPC", "CcccC~~~P~~~CcccC", "CCCCC~F~~~F~CCCCC");
            }
            for (int layer = 1; layer < shapeInfo; layer++) {
                if (layer % 2 == 0) {
                    builder.aisle("C~~~C~F~~~F~C~~~C", "CCCCC~~~P~~~CCCCC", "C~~~C~~PpP~~C~~~C", "CCCCC~~~P~~~CCCCC", "C~~~C~F~~~F~C~~~C");
                    builder.aisle("CCCCC~F~~~F~CCCCC", "CcccC~~~P~~~CcccC", "CPPPPPPPpPPPPPPPC", "CcccC~~~P~~~CcccC", "CCCCC~F~~~F~CCCCC");
                    builder.aisle("C~~~C~F~~~F~C~~~C", "CPPPPPPPPPPPPPPPC", "CmmmC~~PpP~~CmmmC", "CPPPPPPPPPPPPPPPC", "C~~~C~F~~~F~C~~~C");
                    builder.aisle("CCCCC~F~~~F~CCCCC", "CcccC~~~P~~~CcccC", "CPPPPPPPpPPPPPPPC", "CcccC~~~P~~~CcccC", "CCCCC~F~~~F~CCCCC");
                }
            }
            String[] controller = shapeInfo > 1 ?
                    new String[]{"C~~~C~IiSOo~C~~~C", "CCCCC~CCCCC~CCCCC", "C~~~C~CCCCC~C~~~C", "CCCCC~CCCCC~CCCCC", "C~~~C~CMECC~C~~~C"} :
                    new String[]{"C~~~C~IiSOo~~~~~~", "CCCCC~CCCCC~~~~~~", "C~~~C~CCCCC~~~~~~", "CCCCC~CCCCC~~~~~~", "C~~~C~CMECC~~~~~~"};

            shapeInfos.add(builder.aisle(controller)
                    .where('S', this.getController(), WEST)
                    .where('C', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.CHEMICALLY_INERT))
                    .where('c', MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.CUPRONICKEL))
                    .where('P', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.PTFE_PIPE))
                    .where('F', MetaBlocks.FRAMES.get(Steel).getDefaultState())
                    .where('p', GAMetaBlocks.PUMP_CASING.getDefaultState())
                    .where('m', GAMetaBlocks.MOTOR_CASING.getDefaultState())
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], EAST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EAST)
                    .where('I', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.IV], WEST)
                    .where('i', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], WEST)
                    .where('O', MetaTileEntities.FLUID_EXPORT_HATCH[GTValues.IV], WEST)
                    .where('o', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], WEST)
                    .build());
        }
        return shapeInfos;
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.advanced_parallel_chemical_reactor.description"),
                I18n.format("tj.multiblock.parallel.description.parallel")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
