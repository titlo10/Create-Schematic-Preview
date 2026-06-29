package org.example.test.createschematicpreview.client;

import java.util.List;

import com.mojang.datafixers.util.Either;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import org.example.test.createschematicpreview.Config;
import org.example.test.createschematicpreview.Createschematicpreview;

@EventBusSubscriber(modid = Createschematicpreview.MODID, value = Dist.CLIENT)
public class SchematicTooltipEvents {

	private static final int MIN_PREVIEW_SIZE = 48;
	private static final int SCREEN_EDGE_PADDING = 16;
	private static final int TOOLTIP_TEXT_HEIGHT_ALLOWANCE = 48;

	@SubscribeEvent(priority = EventPriority.LOWEST)
	static void addPreviewHint(ItemTooltipEvent event) {
		if (!Config.previewEnabled || event.getEntity() == null || !hasSchematicFile(event.getItemStack()))
			return;

		String fileName = event.getItemStack().get(AllDataComponents.SCHEMATIC_FILE);
		if (fileName == null || fileName.isEmpty())
			return;

		List<Component> tooltip = event.getToolTip();
		int index = previewHintIndex(tooltip, fileName);
		tooltip.add(index, previewHint());
	}

	@SubscribeEvent
	static void addPreviewComponent(RenderTooltipEvent.GatherComponents event) {
		ItemStack stack = event.getItemStack();
		if (!Config.previewEnabled || !Screen.hasAltDown() || !hasSchematicFile(stack))
			return;

		String fileName = stack.get(AllDataComponents.SCHEMATIC_FILE);
		if (fileName == null || fileName.isEmpty())
			return;

		int width = Math.min(Config.panelWidth, Math.max(MIN_PREVIEW_SIZE,
			event.getScreenWidth() - SCREEN_EDGE_PADDING));
		int height = Math.min(Config.panelHeight, Math.max(MIN_PREVIEW_SIZE,
			event.getScreenHeight() - TOOLTIP_TEXT_HEIGHT_ALLOWANCE));

		List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
		elements.add(previewComponentIndex(elements), Either.right(new SchematicPreviewTooltip(fileName, width, height)));
		event.setMaxWidth(Math.max(event.getMaxWidth(), width));
	}

	private static boolean hasSchematicFile(ItemStack stack) {
		return !stack.isEmpty() && AllItems.SCHEMATIC.isIn(stack) && stack.has(AllDataComponents.SCHEMATIC_FILE);
	}

	private static Component previewHint() {
		boolean alt = Screen.hasAltDown();
		MutableComponent hint = Component.empty();
		hint.append(Component.translatable("gui." + Createschematicpreview.MODID + ".tooltip.hold_preview.prefix")
			.withStyle(ChatFormatting.DARK_GRAY));
		hint.append(Component.translatable("gui." + Createschematicpreview.MODID + ".tooltip.key_alt")
			.withStyle(alt ? ChatFormatting.WHITE : ChatFormatting.GRAY));
		hint.append(Component.translatable("gui." + Createschematicpreview.MODID + ".tooltip.hold_preview.suffix")
			.withStyle(ChatFormatting.DARK_GRAY));
		return hint;
	}

	private static int previewComponentIndex(List<Either<FormattedText, TooltipComponent>> elements) {
		int index = previewHintElementIndex(elements);
		if (index == -1)
			index = Math.min(1, elements.size());
		else
			index++;

		if (index < elements.size() && elements.get(index).map(SchematicTooltipEvents::isEmptyText, component -> false))
			index++;
		return index;
	}

	private static int previewHintIndex(List<Component> tooltip, String fileName) {
		for (int i = 1; i < tooltip.size(); i++) {
			if (tooltip.get(i).getString().equals(fileName))
				return i == 1 ? 1 : 2;
		}
		return Math.min(2, tooltip.size());
	}

	private static int previewHintElementIndex(List<Either<FormattedText, TooltipComponent>> elements) {
		for (int i = 1; i < elements.size(); i++) {
			if (elements.get(i).map(SchematicTooltipEvents::isPreviewHint, component -> false))
				return i;
		}
		return -1;
	}

	private static boolean isPreviewHint(FormattedText text) {
		return text instanceof Component component && component.getString().contains("Preview");
	}

	private static boolean isEmptyText(FormattedText text) {
		return text instanceof Component component && component.getString().isEmpty();
	}
}
