package com.simibubi.create.content.curiosities.zapper.terrainzapper;

import com.simibubi.create.foundation.item.render.CreateCustomRenderedItemModel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

public class WorldshaperModel extends CreateCustomRenderedItemModel {

	public WorldshaperModel(BakedModel template) {
		super(template, "handheld_worldshaper");
		addPartials("core", "core_glow", "accelerator");
	}

	@Override
	public BlockEntityWithoutLevelRenderer createRenderer() {
		Minecraft minecraft = Minecraft.getInstance();
		return new WorldshaperItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
	}

}
