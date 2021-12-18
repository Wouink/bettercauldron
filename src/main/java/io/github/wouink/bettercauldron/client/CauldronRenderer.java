package io.github.wouink.bettercauldron.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import io.github.wouink.bettercauldron.block.tileentity.CauldronTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.ResourceLocation;

public class CauldronRenderer extends TileEntityRenderer<CauldronTileEntity> {
	public CauldronRenderer(TileEntityRendererDispatcher dispatcher) {
		super(dispatcher);
	}

	public static float[] HEIGHTS = {0.5625f, 0.75f, 0.9375f};

	public static void renderFluid(float height, int color, int luminosity, ResourceLocation texture, MatrixStack ms, IRenderTypeBuffer buffer, int light) {
		ms.pushPose();

		float opacity = 1;
		if(luminosity != 0) light = light & 15728640 | luminosity << 4;
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(AtlasTexture.LOCATION_BLOCKS).apply(texture);
		IVertexBuilder builder = buffer.getBuffer(RenderType.translucentMovingBlock());
		ms.translate(0.5, 0, 0.5);

		int lu = light & '\uffff';
		int lv = light >> 16 & '\uffff';
		float r = (float) ((color >> 16 & 255)) / 255.0f;
		float g = (float) ((color >> 8 & 255)) / 255.0f;
		float b = (float) (color & 255) / 255.0f;

		// half width of the texture to be rendered inside the cauldron
		float hw = 0.375f;

		float atlasScaleU = sprite.getU1() - sprite.getU0();
		float atlasScaleV = sprite.getV1() - sprite.getV0();
		float minU = sprite.getU0();
		float minV = sprite.getV0();
		float maxU = minU + atlasScaleU * 0.5f;
		float maxV = minV + atlasScaleV * 0.5f;

		builder.vertex(ms.last().pose(), -hw, height, hw).color(r, g, b, opacity).uv(minU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lu, lv).normal(ms.last().normal(), 0, 1, 0).endVertex();
		builder.vertex(ms.last().pose(), hw, height, hw).color(r, g, b, opacity).uv(maxU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lu, lv).normal(ms.last().normal(), 0, 1, 0).endVertex();
		builder.vertex(ms.last().pose(), hw, height, -hw).color(r, g, b, opacity).uv(maxU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lu, lv).normal(ms.last().normal(), 0, 1, 0).endVertex();
		builder.vertex(ms.last().pose(), -hw, height, -hw).color(r, g, b, opacity).uv(minU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lu, lv).normal(ms.last().normal(), 0, 1, 0).endVertex();

		ms.popPose();
	}

	@Override
	public void render(CauldronTileEntity cauldron, float partialTicks, MatrixStack ms, IRenderTypeBuffer buffer, int light, int overlay) {
		if(!cauldron.isEmpty()) {
			renderFluid(HEIGHTS[cauldron.getFluidLevel() - 1], cauldron.getFluid().getAttributes().getColor(), cauldron.getFluid().getAttributes().getLuminosity(), cauldron.getFluid().getAttributes().getStillTexture(), ms, buffer, light);
		}
	}
}
