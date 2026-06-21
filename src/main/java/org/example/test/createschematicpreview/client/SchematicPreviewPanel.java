package org.example.test.createschematicpreview.client;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.client.SchematicRenderer;

import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.example.test.createschematicpreview.Config;
import org.example.test.createschematicpreview.Createschematicpreview;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SchematicPreviewPanel {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final int BORDER = 1;
	private static final int CHECKER_SIZE = 10;
	private static final int CHECKER_LIGHT = 0xFF7E90BD;
	private static final int CHECKER_DARK = 0xFF6874AD;
	private static final int FRAME_COLOR = 0xFFFFFFFF;
	private static final int TITLE_COLOR = 0xFF518DDB;

	private enum State {
		NONE, LOADING, OK, EMPTY, TOO_LARGE, FAILED
	}

	private String currentFile;
	private String pendingFile;
	private long pendingSince;
	private State state = State.NONE;

	private SchematicRenderer renderer;
	private Vec3i size = Vec3i.ZERO;

	private float yaw = (float) Config.defaultYaw;
	private float pitch = (float) Config.defaultPitch;
	private float zoom = 1.0F;

	private boolean dragging;
	private boolean wasDown;
	private double lastMouseX, lastMouseY;

	private int lastX, lastY, lastW, lastH;

	public void clear() {
		currentFile = null;
		pendingFile = null;
		renderer = null;
		size = Vec3i.ZERO;
		state = State.NONE;
	}

	public void setSelected(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			clear();
			return;
		}
		if (fileName.equals(currentFile) || fileName.equals(pendingFile))
			return;

		pendingFile = fileName;
		pendingSince = Util.getMillis();
		state = State.LOADING;
		currentFile = null;
		renderer = null;
		size = Vec3i.ZERO;
		yaw = (float) Config.defaultYaw;
		pitch = (float) Config.defaultPitch;
		zoom = 1.0F;
	}

	private void tickLoad() {
		if (pendingFile != null && Util.getMillis() - pendingSince >= Config.loadDelayMs)
			build(pendingFile);
	}

	private void build(String fileName) {
		currentFile = fileName;
		pendingFile = null;
		Minecraft mc = Minecraft.getInstance();
		Level level = mc.level;
		if (level == null || mc.player == null) {
			state = State.FAILED;
			return;
		}

		try {
			String owner = mc.player.getGameProfile().getName();
			ItemStack blueprint = AllItems.SCHEMATIC.asStack();
			blueprint.set(AllDataComponents.SCHEMATIC_OWNER, owner);
			blueprint.set(AllDataComponents.SCHEMATIC_FILE, fileName);

			StructureTemplate template = SchematicItem.loadSchematic(level, blueprint);
			Vec3i templateSize = template.getSize();
			if (templateSize.equals(Vec3i.ZERO)) {
				state = State.EMPTY;
				return;
			}

			long volume = (long) templateSize.getX() * templateSize.getY() * templateSize.getZ();
			if (volume > Config.maxBlockVolume) {
				size = templateSize;
				state = State.TOO_LARGE;
				return;
			}

			SchematicLevel fakeLevel = new SchematicLevel(level);
			template.placeInWorld(fakeLevel, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings(),
				fakeLevel.getRandom(), Block.UPDATE_CLIENTS);
			for (BlockEntity be : fakeLevel.getBlockEntities())
				be.setLevel(fakeLevel);

			renderer = new SchematicRenderer(fakeLevel);
			size = templateSize;
			state = State.OK;
		} catch (Exception e) {
			LOGGER.warn("[{}] Failed to build schematic preview for '{}'", Createschematicpreview.MODID, fileName, e);
			renderer = null;
			state = State.FAILED;
		}
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= lastX && mouseX < lastX + lastW && mouseY >= lastY && mouseY < lastY + lastH;
	}

	public void updateMouse(double mouseX, double mouseY, boolean leftDown) {
		if (leftDown && !wasDown && isMouseOver(mouseX, mouseY))
			dragging = true;
		if (!leftDown)
			dragging = false;
		if (dragging) {
			yaw += (float) (mouseX - lastMouseX);
			pitch = Mth.clamp(pitch + (float) (mouseY - lastMouseY), -90.0F, 90.0F);
		}
		lastMouseX = mouseX;
		lastMouseY = mouseY;
		wasDown = leftDown;
	}

	public void onScroll(double scrollDelta) {
		zoom = Mth.clamp(zoom * (scrollDelta > 0 ? 1.1F : 1.0F / 1.1F), 0.25F, 5.0F);
	}

	public void render(GuiGraphics graphics, int x, int y, int w, int h, int mouseX, int mouseY, float partialTicks) {
		int cols = Math.max(1, (w - 2 * BORDER) / CHECKER_SIZE);
		int rows = Math.max(1, (h - 2 * BORDER) / CHECKER_SIZE);
		w = cols * CHECKER_SIZE + 2 * BORDER;
		h = rows * CHECKER_SIZE + 2 * BORDER;

		lastX = x;
		lastY = y;
		lastW = w;
		lastH = h;

		tickLoad();

		int innerX = x + BORDER;
		int innerY = y + BORDER;
		int innerW = w - 2 * BORDER;
		int innerH = h - 2 * BORDER;

		drawCheckerboard(graphics, innerX, innerY, innerW, innerH);
		graphics.fill(x, y, x + w, y + BORDER, FRAME_COLOR);
		graphics.fill(x, y + h - BORDER, x + w, y + h, FRAME_COLOR);
		graphics.fill(x, y, x + BORDER, y + h, FRAME_COLOR);
		graphics.fill(x + w - BORDER, y, x + w, y + h, FRAME_COLOR);

		Minecraft mc = Minecraft.getInstance();

		if (state == State.OK && renderer != null) {
			renderPreview(graphics, innerX, innerY, innerW, innerH);
		} else {
			drawCenteredStatus(graphics, mc, innerX, innerY, innerW, innerH);
		}

		Component title = Component.translatable("gui." + Createschematicpreview.MODID + ".preview.title");
		graphics.drawString(mc.font, title, innerX + 3, innerY + 3, TITLE_COLOR);
	}

	private void drawCheckerboard(GuiGraphics graphics, int x, int y, int w, int h) {
		int cols = (w + CHECKER_SIZE - 1) / CHECKER_SIZE;
		int rows = (h + CHECKER_SIZE - 1) / CHECKER_SIZE;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				int cx = x + col * CHECKER_SIZE;
				int cy = y + row * CHECKER_SIZE;
				int cw = Math.min(CHECKER_SIZE, x + w - cx);
				int ch = Math.min(CHECKER_SIZE, y + h - cy);
				int color = ((row + col) & 1) == 0 ? CHECKER_LIGHT : CHECKER_DARK;
				graphics.fill(cx, cy, cx + cw, cy + ch, color);
			}
		}
	}

	private void drawCenteredStatus(GuiGraphics graphics, Minecraft mc, int x, int y, int w, int h) {
		String key;
		switch (state) {
			case LOADING -> key = "gui." + Createschematicpreview.MODID + ".preview.loading";
			case TOO_LARGE -> key = "gui." + Createschematicpreview.MODID + ".preview.too_large";
			case FAILED -> key = "gui." + Createschematicpreview.MODID + ".preview.failed";
			case EMPTY -> key = "gui." + Createschematicpreview.MODID + ".preview.empty";
			default -> key = "gui." + Createschematicpreview.MODID + ".preview.none";
		}
		Component text = Component.translatable(key);
		graphics.drawString(mc.font, text, x + (w - mc.font.width(text)) / 2, y + h / 2 - 4, 0xFFFFFFFF);
	}

	private void renderPreview(GuiGraphics graphics, int x, int y, int w, int h) {
		float centerX = x + w / 2.0F;
		float centerY = y + h / 2.0F;

		int maxDim = Math.max(1, Math.max(size.getX(), Math.max(size.getY(), size.getZ())));
		float fit = Math.min(w, h) * 0.6F;
		float scale = (fit / maxDim) * zoom;

		graphics.enableScissor(x, y, x + w, y + h);
		graphics.flush();

		PoseStack ms = graphics.pose();
		ms.pushPose();

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		Lighting.setupFor3DItems();

		ms.translate(centerX, centerY, 350.0F);
		ms.scale(scale, scale, scale);
		UIRenderHelper.flipForGuiRender(ms);
		ms.mulPose(Axis.XP.rotationDegrees(pitch));
		ms.mulPose(Axis.YP.rotationDegrees(yaw));
		ms.translate(-size.getX() / 2.0F, -size.getY() / 2.0F, -size.getZ() / 2.0F);

		SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
		renderer.render(ms, buffer);
		buffer.draw();

		ms.popPose();

		RenderSystem.disableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		graphics.disableScissor();
	}
}
