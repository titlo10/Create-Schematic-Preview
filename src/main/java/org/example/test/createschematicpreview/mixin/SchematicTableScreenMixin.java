package org.example.test.createschematicpreview.mixin;

import com.simibubi.create.content.schematics.table.SchematicTableScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

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

		int panelW = Config.panelWidth;
		int panelH = Config.panelHeight;
		int leftPos = self.getGuiLeft();
		int topPos = self.getGuiTop();

		int px = leftPos - panelW - 6;
		if (px < 0)
			px = leftPos + background.getWidth() + 6;
		int py = topPos;

		long window = mc.getWindow().getWindow();
		boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		createschematicpreview$panel.updateMouse(mouseX, mouseY, leftDown);

		createschematicpreview$panel.render(graphics, px, py, panelW, panelH, mouseX, mouseY, partialTicks);
	}
}
