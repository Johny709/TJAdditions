package com.johny.tj.integration.jei.multi;

import com.johny.tj.machines.multi.electric.MetaTileEntityIndustrialFusionReactor;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class IndustrialFusionReactorInfo extends MultiblockInfoPage {

    MetaTileEntityIndustrialFusionReactor fusionReactor;

    public IndustrialFusionReactorInfo(MetaTileEntityIndustrialFusionReactor fusionReactor) {
        this.fusionReactor = fusionReactor;
    }

    @Override
    public MultiblockControllerBase getController() {
        return fusionReactor;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapes = new ArrayList<>();
        int tier = fusionReactor.getTier();
        for (int index = 0; index < 16; index++) {
            GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(LEFT, FRONT, DOWN);
            for (int num = 0; num < index; num++) {
                builder.aisle("###############", "######CCC######", "####CC###CC####", "###C#######C###", "##C#########C##", "##C#########C##", "#C###########C#", "#C###########C#", "#C###########C#", "##C#########C##", "##C#########C##", "###C#######C###", "####CC###CC####", "######CCC######", "###############");
                builder.aisle("######CCC######", "####CCcccCC####", "###CccCCCccC###", "##CcCC###CCcC##", "#CcC#######CcC#", "#CcC#######CcC#", "CcC#########CcC", "CcC#########CcC", "CcC#########CcC", "#CcC#######CcC#", "#CcC#######CcC#", "##CcCC###CCcC##", "###CccCCCccC###", "####CCcccCC####", "######CCC######");
            }
            builder.aisle("###############", "######ICI######", "####CC###CC####", "###C#######C###", "##C#########C##", "##C#########C##", "#I###########I#", "#C###########C#", "#I###########I#", "##C#########C##", "##C#########C##", "###C#######C###", "####CC###CC####", "######ICI######", "###############");
            builder.aisle("######OCO######", "####CCcccCC####", "###EccOCOccE###", "##EcEC###CEcE##", "#CcE#######EcC#", "#CcC#######CcC#", "OcO#########OcO", "CcC#########CcS", "OcO#########OcO", "#CcC#######CcC#", "#CcE#######EcC#", "##EcEC###CEcE##", "###EccOCOccE###", "####CCcccCC####", "######OCO######");
            builder.aisle("###############", "######ICI######", "####CC###CC####", "###C#######C###", "##C#########C##", "##C#########C##", "#I###########I#", "#C###########C#", "#I###########I#", "##C#########C##", "##C#########C##", "###C#######C###", "####CC###CC####", "######ICI######", "###############");
                    builder.where('S', fusionReactor, EnumFacing.WEST)
                    .where('C', fusionReactor.getCasingState())
                    .where('c', fusionReactor.getCoilState())
                    .where('E', tier < 9 ? MetaTileEntities.ENERGY_INPUT_HATCH[tier] : GATileEntities.ENERGY_INPUT[tier - 9], EnumFacing.WEST)
                    .where('I', MetaTileEntities.FLUID_IMPORT_HATCH[Math.min(tier, 9)], EnumFacing.WEST)
                    .where('O', MetaTileEntities.FLUID_EXPORT_HATCH[Math.min(tier, 9)], EnumFacing.WEST);
            shapes.add(builder.build());
        }
        return shapes;
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.industrial_fusion_reactor.description")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
