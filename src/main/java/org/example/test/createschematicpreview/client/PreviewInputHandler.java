package org.example.test.createschematicpreview.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import org.example.test.createschematicpreview.Config;
import org.example.test.createschematicpreview.Createschematicpreview;

@EventBusSubscriber(modid = Createschematicpreview.MODID, value = Dist.CLIENT)
public class PreviewInputHandler {

	@SubscribeEvent
	static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
		if (!Config.previewEnabled)
			return;
		if (!(event.getScreen() instanceof PreviewScreenAccess access))
			return;

		SchematicPreviewPanel panel = access.createschematicpreview$getPanel();
		if (panel != null && panel.isMouseOver(event.getMouseX(), event.getMouseY())) {
			panel.onScroll(event.getScrollDeltaY());
			event.setCanceled(true);
		}
	}
}
