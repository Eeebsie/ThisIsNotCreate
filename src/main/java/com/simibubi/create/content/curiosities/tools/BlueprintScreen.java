package com.simibubi.create.content.curiosities.tools;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.logistics.item.filter.FilterScreenPacket;
import com.simibubi.create.content.logistics.item.filter.FilterScreenPacket.Option;
import com.simibubi.create.foundation.gui.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.GuiGameElement;
import com.simibubi.create.foundation.gui.widgets.IconButton;
import com.simibubi.create.foundation.networking.AllPackets;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import net.minecraftforge.fmlclient.gui.GuiUtils;

import org.apache.logging.log4j.core.pattern.TextRenderer;

public class BlueprintScreen extends AbstractSimiContainerScreen<BlueprintContainer> {

	protected AllGuiTextures background;
	private List<Rect2i> extraAreas = Collections.emptyList();

	private IconButton resetButton;
	private IconButton confirmButton;

	public BlueprintScreen(BlueprintContainer container, Inventory inv, Component title) {
		super(container, inv, title);
		this.background = AllGuiTextures.BLUEPRINT;
	}

	@Override
	protected void init() {
		setWindowSize(background.width, background.height + 4 + PLAYER_INVENTORY.height);
		setWindowOffset(2 + (width % 2 == 0 ? 0 : -1), 0);
		super.init();
		widgets.clear();

		int x = leftPos;
		int y = topPos;

		resetButton = new IconButton(x + background.width - 62, y + background.height - 24, AllIcons.I_TRASH);
		confirmButton = new IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM);

		widgets.add(resetButton);
		widgets.add(confirmButton);

		extraAreas = ImmutableList.of(
			new Rect2i(x + background.width, y + background.height - 36, 56, 44)
		);
	}

	@Override
	protected void renderWindow(PoseStack ms, int mouseX, int mouseY, float partialTicks) {
		int invX = getLeftOfCentered(PLAYER_INVENTORY.width);
		int invY = topPos + background.height + 4;
		renderPlayerInventory(ms, invX, invY);

		int x = leftPos;
		int y = topPos;

		background.draw(ms, this, x, y);
		font.draw(ms, title, x + 15, y + 4, 0xFFFFFF);

		GuiGameElement.of(AllBlockPartials.CRAFTING_BLUEPRINT_1x1)
			.<GuiGameElement.GuiRenderBuilder>at(x + background.width + 20, y + background.height - 32, 0)
			.rotate(45, -45, 22.5f)
			.scale(40)
			.render(ms);
	}

	@Override
	protected void renderTooltip(PoseStack ms, int x, int y) {
		if (!this.minecraft.player.getInventory().getSelected()
			.isEmpty() || this.hoveredSlot == null || this.hoveredSlot.hasItem()
			|| hoveredSlot.container == menu.playerInventory) {
			super.renderTooltip(ms, x, y);
			return;
		}
		renderWrappedToolTip(ms, addToTooltip(new LinkedList<>(), hoveredSlot.getSlotIndex(), true), x, y,
			font);
	}

	@Override
	public List<Component> getTooltipFromItem(ItemStack stack) {
		List<Component> list = super.getTooltipFromItem(stack);
		if (hoveredSlot.container == menu.playerInventory)
			return list;
		return hoveredSlot != null ? addToTooltip(list, hoveredSlot.getSlotIndex(), false) : list;
	}

	private List<Component> addToTooltip(List<Component> list, int slot, boolean isEmptySlot) {
		if (slot < 0 || slot > 10)
			return list;

		if (slot < 9) {
			list.add(Lang.createTranslationTextComponent("crafting_blueprint.crafting_slot")
				.withStyle(ChatFormatting.GOLD));
			if (isEmptySlot)
				list.add(Lang.createTranslationTextComponent("crafting_blueprint.filter_items_viable")
					.withStyle(ChatFormatting.GRAY));

		} else if (slot == 9) {
			list.add(Lang.createTranslationTextComponent("crafting_blueprint.display_slot")
				.withStyle(ChatFormatting.GOLD));
			if (!isEmptySlot)
				list.add(Lang
					.createTranslationTextComponent("crafting_blueprint."
						+ (menu.contentHolder.inferredIcon ? "inferred" : "manually_assigned"))
					.withStyle(ChatFormatting.GRAY));

		} else if (slot == 10) {
			list.add(Lang.createTranslationTextComponent("crafting_blueprint.secondary_display_slot")
				.withStyle(ChatFormatting.GOLD));
			if (isEmptySlot)
				list.add(Lang.createTranslationTextComponent("crafting_blueprint.optional")
					.withStyle(ChatFormatting.GRAY));
		}

		return list;
	}

	@Override
	public void tick() {
//		handleTooltips();
		super.tick();

		if (!menu.contentHolder.isEntityAlive())
			minecraft.player.closeContainer();
	}

//	protected void handleTooltips() {
//		List<IconButton> tooltipButtons = getTooltipButtons();
//
//		for (IconButton button : tooltipButtons) {
//			if (!button.getToolTip()
//				.isEmpty()) {
//				button.setToolTip(button.getToolTip()
//					.get(0));
//				button.getToolTip()
//					.add(TooltipHelper.holdShift(Palette.Yellow, hasShiftDown()));
//			}
//		}
//
//		if (hasShiftDown()) {
//			List<IFormattableTextComponent> tooltipDescriptions = getTooltipDescriptions();
//			for (int i = 0; i < tooltipButtons.size(); i++)
//				fillToolTip(tooltipButtons.get(i), tooltipDescriptions.get(i));
//		}
//	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		boolean mouseClicked = super.mouseClicked(x, y, button);

		if (button == 0) {
			if (confirmButton.isHovered()) {
				minecraft.player.closeContainer();
				return true;
			}
			if (resetButton.isHovered()) {
				menu.clearContents();
				contentsCleared();
				menu.sendClearPacket();
				return true;
			}
		}

		return mouseClicked;
	}

	protected void contentsCleared() {}

	protected void sendOptionUpdate(Option option) {
		AllPackets.channel.sendToServer(new FilterScreenPacket(option));
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

}
