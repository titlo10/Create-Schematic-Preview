package org.example.test.createschematicpreview.client;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record SchematicPreviewTooltip(String fileName, int width, int height) implements TooltipComponent {
}
