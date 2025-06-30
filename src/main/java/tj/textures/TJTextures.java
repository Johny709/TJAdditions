package tj.textures;

import codechicken.lib.render.BlockRenderer;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.texture.TextureUtils;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.TransformationList;
import codechicken.lib.vec.uv.IconTransformation;
import codechicken.lib.vec.uv.UVTransformationList;
import tj.TJ;
import gregtech.api.GTValues;
import gregtech.api.render.UVMirror;
import gregtech.api.util.GTLog;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

import static tj.textures.TJOrientedOverlayRenderer.OverlayFace.FRONT;
import static tj.textures.TJOrientedOverlayRenderer.OverlayFace.TOP;

public class TJTextures {

    private static final ThreadLocal<BlockRenderer.BlockFace> blockFaces = ThreadLocal.withInitial(BlockRenderer.BlockFace::new);
    public static final List<TextureUtils.IIconRegister> iconRegisters = new ArrayList<>();

    public static final TJSimpleCubeRenderer DRACONIC = new TJSimpleCubeRenderer(TJ.MODID, "casings/solid/draconiccasing");
    public static final TJSimpleCubeRenderer AWAKENED = new TJSimpleCubeRenderer(TJ.MODID, "casings/solid/awakenedcasing");
    public static final TJSimpleCubeRenderer CHOATIC = new TJSimpleCubeRenderer(TJ.MODID, "casings/solid/chaoticcasing");
    public static final TJSimpleCubeRenderer ETERNITY = new TJSimpleCubeRenderer(TJ.MODID, "casings/solid/eternityblock");
    public static final TJSimpleCubeRenderer SOUL = new TJSimpleCubeRenderer(TJ.MODID, "casings/solid/soulcasing");
    public static final TJSimpleCubeRenderer DURANIUM = new TJSimpleCubeRenderer(TJ.MODID, "casings/solid/duranium");
    public static final TJSimpleCubeRenderer SEABORGIUM = new TJSimpleCubeRenderer(TJ.MODID,"casings/solid/seaborgium");
    public static final TJSimpleCubeRenderer HEAVY_QUARK_DEGENERATE_MATTER = new TJSimpleCubeRenderer(TJ.MODID, "casings/solid/heavy_quark_degenerate_matter");
    public static final TJSimpleCubeRenderer STAINLESS_PIPE = new TJSimpleCubeRenderer(TJ.MODID, "pipe/machine_casing_pipe_stainless");
    public static final TJSimpleCubeRenderer TUNGSTEN_TITANIUM_CARBIDE = new TJSimpleCubeRenderer(TJ.MODID, "casings/solid/tungsten_titanium_carbide");
    public static final TJSimpleOverlayRenderer COVER_CREATIVE_FLUID = new TJSimpleOverlayRenderer(TJ.MODID, "cover/creative_fluid_cover_overlay");
    public static final TJSimpleOverlayRenderer COVER_CREATIVE_ENERGY = new TJSimpleOverlayRenderer(TJ.MODID, "cover/creative_energy_cover_overlay");
    public static final TJSimpleCubeRenderer FIELD_GENERATOR_CORE = new TJSimpleCubeRenderer(TJ.MODID, "items/field_generator_core");
    public static final TJSimpleOverlayRenderer FIELD_GENERATOR_SPIN = new TJSimpleOverlayRenderer(TJ.MODID, "items/field_generator_overlay");
    public static final TJOrientedOverlayRenderer BOILER_OVERLAY = new TJOrientedOverlayRenderer(GTValues.MODID, "generators/boiler/coal", FRONT);
    public static final TJSimpleOverlayRenderer OUTSIDE_OVERLAY_BASE = new TJSimpleOverlayRenderer(TJ.MODID, "cover/outside_overlay_base");
    public static final TJSimpleOverlayRenderer INSIDE_OVERLAY_BASE = new TJSimpleOverlayRenderer(TJ.MODID, "cover/inside_overlay_base");
    public static final TJSimpleOverlayRenderer PORTAL_OVERLAY = new TJSimpleOverlayRenderer("minecraft", "portal");
    public static final TJOrientedOverlayRenderer TELEPORTER_OVERLAY = new TJOrientedOverlayRenderer(GTValues.MODID, "machines/teleporter", "teleporter", TOP);

    public static final TJSimpleCubeRenderer FUSION_MK2 = new TJSimpleCubeRenderer(GTValues.MODID, "casings/fusion/machine_casing_fusion_2");
    public static final TJSimpleCubeRenderer FUSION_PORT_LUV = new TJSimpleCubeRenderer(TJ.MODID, "casings/ability/fusion_energy_port_luv");
    public static final TJSimpleCubeRenderer FUSION_PORT_ZPM = new TJSimpleCubeRenderer(TJ.MODID, "casings/ability/fusion_energy_port_zpm");
    public static final TJSimpleCubeRenderer FUSION_PORT_UV = new TJSimpleCubeRenderer(TJ.MODID, "casings/ability/fusion_energy_port_uv");
    public static final TJSimpleCubeRenderer FUSION_PORT_UHV = new TJSimpleCubeRenderer(TJ.MODID, "casings/ability/fusion_energy_port_uhv");
    public static final TJSimpleCubeRenderer FUSION_PORT_UEV = new TJSimpleCubeRenderer(TJ.MODID, "casings/ability/fusion_energy_port_uev");

    @SideOnly(Side.CLIENT)
    public static void register(TextureMap textureMap) {
        GTLog.logger.info("Loading meta tile entity texture sprites...");
        for (TextureUtils.IIconRegister iconRegister : iconRegisters) {
            iconRegister.registerIcons(textureMap);
        }
    }

    @SideOnly(Side.CLIENT)
    public static void renderFace(CCRenderState renderState, Matrix4 translation, IVertexOperation[] ops, EnumFacing face, Cuboid6 bounds, TextureAtlasSprite sprite) {
        BlockRenderer.BlockFace blockFace = blockFaces.get();
        blockFace.loadCuboidFace(bounds, face.getIndex());
        UVTransformationList uvList = new UVTransformationList(new IconTransformation(sprite));
        if (face.getIndex() == 0) {
            uvList.prepend(new UVMirror(0, 0, bounds.min.z, bounds.max.z));
        }
        renderState.setPipeline(blockFace, 0, blockFace.verts.length,
                ArrayUtils.addAll(ops, new TransformationList(translation), uvList));
        renderState.render();
    }
}
