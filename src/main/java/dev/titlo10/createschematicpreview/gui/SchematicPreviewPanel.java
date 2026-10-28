package dev.titlo10.createschematicpreview.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

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

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.Locale;

import static dev.titlo10.createschematicpreview.CSPConfig.CONFIG;
import static dev.titlo10.createschematicpreview.CreateSchematicPreview.*;

@OnlyIn(Dist.CLIENT)
public class SchematicPreviewPanel {

	private static final int BORDER = 1, CHECKER_SIZE = 10;
	private static final int CHECKER_LIGHT = 0xFF7E90BD;
	private static final int CHECKER_DARK = 0xFF6874AD;
	private static final int FRAME_COLOR = 0xFFFFFFFF;

	private enum State {
		NONE, LOADING, OK, EMPTY, TOO_LARGE, FAILED;

		private final Component component;

		State () {
			String key = this.toString().toLowerCase(Locale.ENGLISH);
			component = translatable("gui.preview." + key);
		}

		public Component getComponent() {
			return component;
		}
	}

	private String currentFile;
	private String pendingFile;
	private long pendingSince;
	private State state = State.NONE;

	private SchematicRenderer renderer;
	private Vec3i size = Vec3i.ZERO;

	private float yaw = CONFIG.defaultYaw.getF();
	private float pitch = CONFIG.defaultPitch.getF();
	private float zoom = CONFIG.defaultZoom.getF();

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
		if (fileName.equals(currentFile) || fileName.equals(pendingFile)) return;

		pendingFile = fileName;
		pendingSince = Util.getMillis();
		state = State.LOADING;
		currentFile = null;
		renderer = null;
		size = Vec3i.ZERO;
		yaw = CONFIG.defaultYaw.getF();
		pitch = CONFIG.defaultPitch.getF();
		zoom = CONFIG.defaultZoom.getF();
	}

	private void tickLoad() {
		int loadDelay = CONFIG.loadDelayMs.get();
		if (pendingFile != null && Util.getMillis() - pendingSince >= loadDelay) build(pendingFile);
	}

	private void build(String fileName) {
		Minecraft mc = Minecraft.getInstance();
		Level level = mc.level;
		if (level == null || mc.player == null) {
			state = State.FAILED;
			return;
		}

		currentFile = fileName;
		pendingFile = null;

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
			if (volume > CONFIG.maxBlockVolume.get()) {
				size = templateSize;
				state = State.TOO_LARGE;
				return;
			}

			var fakeSchematicLevel = new SchematicLevel(level);
			template.placeInWorld(fakeSchematicLevel, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings(),
				fakeSchematicLevel.getRandom(), Block.UPDATE_CLIENTS);
			for (BlockEntity be : fakeSchematicLevel.getBlockEntities())
				be.setLevel(fakeSchematicLevel);
			fixControllerBlockEntities(fakeSchematicLevel);

			renderer = new SchematicRenderer(fakeSchematicLevel);
			size = templateSize;
			state = State.OK;
		} catch (Exception e) {
			LOGGER.warn("Failed to build schematic preview for '{}'", fileName, e);
			renderer = null;
			state = State.FAILED;
		}
	}

	private void fixControllerBlockEntities(SchematicLevel schematicLevel) {
		for (BlockEntity blockEntity : schematicLevel.getBlockEntities()) {
			if (!(blockEntity instanceof IMultiBlockEntityContainer multiBlock))
				continue;

			BlockPos lastKnownPos = multiBlock.getLastKnownPos();
			BlockPos currentPos = blockEntity.getBlockPos();
			if (lastKnownPos == null || currentPos == null || multiBlock.isController() || lastKnownPos.equals(currentPos))
				continue;

			BlockPos adjustedController = multiBlock.getController()
				.offset(currentPos.subtract(lastKnownPos));
			if (multiBlock instanceof SmartBlockEntity smartBlockEntity)
				smartBlockEntity.markVirtual();
			multiBlock.setController(adjustedController);
		}
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= lastX && mouseX < lastX + lastW && mouseY >= lastY && mouseY < lastY + lastH;
	}

	public void updateMouse(double mouseX, double mouseY, boolean leftDown) {
		if (leftDown && !wasDown && isMouseOver(mouseX, mouseY)) dragging = true;
		if (!leftDown) dragging = false;
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

		if (state == State.OK && renderer != null)
			renderPreview(graphics, innerX, innerY, innerW, innerH);
		else
			drawCenteredStatus(graphics, mc, innerX, innerY, innerW, innerH);
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
		Component text = state.getComponent();
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

		int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);

		SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
		renderer.render(ms, buffer);
		restoreFramebuffer(drawFramebuffer, readFramebuffer);
		buffer.draw();
		restoreFramebuffer(drawFramebuffer, readFramebuffer);

		ms.popPose();

		RenderSystem.disableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		graphics.disableScissor();
	}

	private static void restoreFramebuffer(int drawFramebuffer, int readFramebuffer) {
		if (GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) != drawFramebuffer)
			GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
		if (GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING) != readFramebuffer)
			GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
	}
}
