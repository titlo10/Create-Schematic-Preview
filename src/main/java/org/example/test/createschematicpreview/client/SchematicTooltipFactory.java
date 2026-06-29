package org.example.test.createschematicpreview.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

import org.example.test.createschematicpreview.Createschematicpreview;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = Createschematicpreview.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class SchematicTooltipFactory {

	@SubscribeEvent
	static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
		event.register(SchematicPreviewTooltip.class, ClientSchematicPreviewTooltip::new);
	}
}
