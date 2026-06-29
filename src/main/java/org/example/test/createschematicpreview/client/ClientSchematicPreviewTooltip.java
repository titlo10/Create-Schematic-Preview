package org.example.test.createschematicpreview.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public class ClientSchematicPreviewTooltip implements ClientTooltipComponent {

	private static final SchematicPreviewPanel PANEL = new SchematicPreviewPanel();

	private final SchematicPreviewTooltip preview;

	public ClientSchematicPreviewTooltip(SchematicPreviewTooltip preview) {
		this.preview = preview;
	}

	@Override
	public int getHeight() {
		return preview.height();
	}

	@Override
	public int getWidth(Font font) {
		return preview.width();
	}

	@Override
	public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
		PANEL.setSelected(preview.fileName());
		PANEL.render(graphics, x, y, preview.width(), preview.height(), -1, -1, 0);
	}
}
