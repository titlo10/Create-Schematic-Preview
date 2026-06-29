package org.example.test.createschematicpreview.mixin;

import com.simibubi.create.content.schematics.table.SchematicTableScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.Mth;

import org.example.test.createschematicpreview.Config;
import org.example.test.createschematicpreview.client.PreviewScreenAccess;
import org.example.test.createschematicpreview.client.SchematicPreviewPanel;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SchematicTableScreen.class)
public abstract class SchematicTableScreenMixin implements PreviewScreenAccess {

	@Unique private static final int createschematicpreview$PANEL_GAP = 6;
	@Unique private static final int createschematicpreview$SCREEN_MARGIN = 4;
	@Unique private static final int createschematicpreview$MIN_PANEL_SIZE = 48;

	@Shadow private ScrollInput schematicsArea;
	@Shadow private Label schematicsLabel;
	@Shadow protected AllGuiTextures background;

	@Unique private SchematicPreviewPanel createschematicpreview$panel;

	@Override
	@Nullable
	public SchematicPreviewPanel createschematicpreview$getPanel() {
		return createschematicpreview$panel;
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void createschematicpreview$initPanel(CallbackInfo ci) {
		createschematicpreview$panel = new SchematicPreviewPanel();
	}

	@Inject(method = "renderBg", at = @At("TAIL"))
	private void createschematicpreview$renderPanel(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY,
													 CallbackInfo ci) {
		if (!Config.previewEnabled || createschematicpreview$panel == null)
			return;

		SchematicTableScreen self = (SchematicTableScreen) (Object) this;
		Minecraft mc = Minecraft.getInstance();

		String file = null;
		if (schematicsArea != null && schematicsLabel != null && schematicsLabel.text != null) {
			String text = schematicsLabel.text.getString();
			if (!text.isEmpty())
				file = text;
		}
		createschematicpreview$panel.setSelected(file);

		int screenW = mc.getWindow().getGuiScaledWidth();
		int screenH = mc.getWindow().getGuiScaledHeight();
		int panelW = Math.min(Config.panelWidth,
			Math.max(1, screenW - createschematicpreview$SCREEN_MARGIN * 2));
		int panelH = Math.min(Config.panelHeight,
			Math.max(1, screenH - createschematicpreview$SCREEN_MARGIN * 2));
		int minPanelW = Math.min(createschematicpreview$MIN_PANEL_SIZE, panelW);
		int minPanelH = Math.min(createschematicpreview$MIN_PANEL_SIZE, panelH);
		int leftPos = self.getGuiLeft();
		int topPos = self.getGuiTop();

		int occupiedLeft = leftPos;
		int occupiedTop = topPos;
		int occupiedRight = leftPos + background.getWidth();
		int occupiedBottom = topPos + background.getHeight() + 4 + AllGuiTextures.PLAYER_INVENTORY.getHeight();
		for (Rect2i area : self.getExtraAreas()) {
			occupiedLeft = Math.min(occupiedLeft, area.getX());
			occupiedTop = Math.min(occupiedTop, area.getY());
			occupiedRight = Math.max(occupiedRight, area.getX() + area.getWidth());
			occupiedBottom = Math.max(occupiedBottom, area.getY() + area.getHeight());
		}

		int leftRoom = Math.max(0, occupiedLeft - createschematicpreview$PANEL_GAP
			- createschematicpreview$SCREEN_MARGIN);
		int rightRoom = Math.max(0, screenW - createschematicpreview$SCREEN_MARGIN
			- occupiedRight - createschematicpreview$PANEL_GAP);
		int aboveRoom = Math.max(0, occupiedTop - createschematicpreview$PANEL_GAP
			- createschematicpreview$SCREEN_MARGIN);
		int belowRoom = Math.max(0, screenH - createschematicpreview$SCREEN_MARGIN
			- occupiedBottom - createschematicpreview$PANEL_GAP);

		int px;
		int py;
		if (leftRoom >= panelW) {
			px = occupiedLeft - createschematicpreview$PANEL_GAP - panelW;
			py = createschematicpreview$clamp(topPos, createschematicpreview$SCREEN_MARGIN,
				screenH - createschematicpreview$SCREEN_MARGIN - panelH);
		} else if (rightRoom >= panelW) {
			px = occupiedRight + createschematicpreview$PANEL_GAP;
			py = createschematicpreview$clamp(topPos, createschematicpreview$SCREEN_MARGIN,
				screenH - createschematicpreview$SCREEN_MARGIN - panelH);
		} else if (belowRoom >= minPanelH || aboveRoom >= minPanelH) {
			boolean useBelow = belowRoom >= minPanelH && (belowRoom >= aboveRoom || aboveRoom < minPanelH);
			int verticalRoom = useBelow ? belowRoom : aboveRoom;
			panelH = Math.min(panelH, verticalRoom);
			px = createschematicpreview$clamp((occupiedLeft + occupiedRight - panelW) / 2,
				createschematicpreview$SCREEN_MARGIN, screenW - createschematicpreview$SCREEN_MARGIN - panelW);
			py = useBelow ? occupiedBottom + createschematicpreview$PANEL_GAP
				: occupiedTop - createschematicpreview$PANEL_GAP - panelH;
		} else {
			int sideRoom = Math.max(leftRoom, rightRoom);
			if (sideRoom < minPanelW)
				return;

			panelW = Math.min(panelW, sideRoom);
			py = createschematicpreview$clamp(topPos, createschematicpreview$SCREEN_MARGIN,
				screenH - createschematicpreview$SCREEN_MARGIN - panelH);
			px = leftRoom >= rightRoom ? occupiedLeft - createschematicpreview$PANEL_GAP - panelW
				: occupiedRight + createschematicpreview$PANEL_GAP;
		}

		long window = mc.getWindow().getWindow();
		boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		createschematicpreview$panel.updateMouse(mouseX, mouseY, leftDown);

		createschematicpreview$panel.render(graphics, px, py, panelW, panelH, mouseX, mouseY, partialTicks);
	}

	@Unique
	private int createschematicpreview$clamp(int value, int min, int max) {
		if (max < min)
			return min;
		return Mth.clamp(value, min, max);
	}
}
