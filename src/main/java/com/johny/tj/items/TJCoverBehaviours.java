package com.johny.tj.items;

import com.johny.tj.TJ;
import com.johny.tj.items.covers.CoverCreativeEnergy;
import com.johny.tj.items.covers.CoverCreativeFluid;
import com.johny.tj.items.covers.CoverCreativeItem;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.CoverDefinition;
import gregtech.api.cover.ICoverable;
import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.util.GTLog;
import gregtech.common.items.behaviors.CoverPlaceBehavior;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import java.util.function.BiFunction;

public class TJCoverBehaviours {

    public static void init() {
        GTLog.logger.info("Registering Covers from TJ...");

        registerBehavior(126, new ResourceLocation(TJ.MODID, "creative.fluid.cover"), TJMetaItems.CREATIVE_FLUID_COVER, CoverCreativeFluid::new);
        registerBehavior(127, new ResourceLocation(TJ.MODID, "creative.item.cover"), TJMetaItems.CREATIVE_ITEM_COVER, CoverCreativeItem::new);
        registerBehavior(128, new ResourceLocation(TJ.MODID, "creative.energy.cover"), TJMetaItems.CREATIVE_ENERGY_COVER, CoverCreativeEnergy::new);
    }

    public static void registerBehavior(int coverNetworkId, ResourceLocation coverId, MetaItem<?>.MetaValueItem placerItem, BiFunction<ICoverable, EnumFacing, CoverBehavior> behaviorCreator) {
        CoverDefinition coverDefinition = new CoverDefinition(coverId, behaviorCreator, placerItem.getStackForm());
        CoverDefinition.registerCover(coverNetworkId, coverDefinition);
        placerItem.addComponents(new CoverPlaceBehavior(coverDefinition));
    }
}
