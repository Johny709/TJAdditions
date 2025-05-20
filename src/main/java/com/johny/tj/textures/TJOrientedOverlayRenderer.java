package com.johny.tj.textures;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.texture.TextureUtils;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

public class TJOrientedOverlayRenderer implements TextureUtils.IIconRegister {

    public enum OverlayFace {
        FRONT, BACK, TOP, BOTTOM, SIDE;

        public static OverlayFace bySide(EnumFacing side, EnumFacing frontFacing) {
            if (side == frontFacing) {
                return FRONT;
            } else if (side.getOpposite() == frontFacing) {
                return BACK;
            } else if (side == EnumFacing.UP) {
                return TOP;
            } else if (side == EnumFacing.DOWN) {
                return BOTTOM;
            } else return SIDE;
        }
    }

    private final String basePath;
    private final OverlayFace[] faces;
    private final String modID;
    private final String overlay;

    @SideOnly(Side.CLIENT)
    private Map<OverlayFace, ActivePredicate> sprites;

    @SideOnly(Side.CLIENT)
    private static class ActivePredicate {

        private final TextureAtlasSprite normalSprite;
        private final TextureAtlasSprite activeSprite;

        public ActivePredicate(TextureAtlasSprite normalSprite, TextureAtlasSprite activeSprite) {
            this.normalSprite = normalSprite;
            this.activeSprite = activeSprite;
        }

        public TextureAtlasSprite getSprite(boolean active) {
            return active ? activeSprite : normalSprite;
        }
    }

    public TJOrientedOverlayRenderer(String modID, String basePath, String overlay, OverlayFace... faces) {
        this.modID = modID;
        this.basePath = basePath;
        this.faces = faces;
        this.overlay = overlay;
        TJTextures.iconRegisters.add(this);
    }

    public TJOrientedOverlayRenderer(String modID, String basePath, OverlayFace... faces) {
        this(modID, basePath, null, faces);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(TextureMap textureMap) {
        this.sprites = new HashMap<>();
        for (OverlayFace overlayFace : faces) {
            String faceName = this.overlay != null ? this.overlay : overlayFace.name().toLowerCase();
            ResourceLocation normalLocation = new ResourceLocation(modID, String.format("blocks/%s/overlay_%s", basePath, faceName));
            ResourceLocation activeLocation = new ResourceLocation(modID, String.format("blocks/%s/overlay_%s_active", basePath, faceName));
            TextureAtlasSprite normalSprite = textureMap.registerSprite(normalLocation);
            TextureAtlasSprite activeSprite = textureMap.registerSprite(activeLocation);
            sprites.put(overlayFace, new ActivePredicate(normalSprite, activeSprite));
        }
    }

    @SideOnly(Side.CLIENT)
    public void render(CCRenderState renderState, Matrix4 translation, IVertexOperation[] ops, Cuboid6 bounds, EnumFacing frontFacing, boolean isActive) {
        for (EnumFacing renderSide : EnumFacing.VALUES) {
            OverlayFace overlayFace = OverlayFace.bySide(renderSide, frontFacing);
            if (sprites.containsKey(overlayFace)) {
                TextureAtlasSprite renderSprite = sprites.get(overlayFace).getSprite(isActive);
                TJTextures.renderFace(renderState, translation, ops, renderSide, bounds, renderSprite);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void render(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline, EnumFacing frontFacing, boolean isActive) {
        render(renderState, translation, pipeline, Cuboid6.full, frontFacing, isActive);
    }
}
