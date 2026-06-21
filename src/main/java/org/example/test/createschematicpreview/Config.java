package org.example.test.createschematicpreview;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Createschematicpreview.MODID)
public class Config {
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	private static final ModConfigSpec.BooleanValue PREVIEW_ENABLED = BUILDER
		.comment("Master toggle for the 3D schematic preview panel.")
		.define("previewEnabled", true);

	private static final ModConfigSpec.IntValue MAX_BLOCK_VOLUME = BUILDER
		.comment("Maximum schematic bounding-box volume (width * height * length) that will be previewed.",
			"Larger schematics show a 'too large' message instead, to avoid render hitches.")
		.defineInRange("maxBlockVolume", 216000, 1, Integer.MAX_VALUE);

	private static final ModConfigSpec.IntValue PANEL_WIDTH = BUILDER
		.comment("Width of the preview panel, in GUI pixels.")
		.defineInRange("panelWidth", 156, 48, 512);

	private static final ModConfigSpec.IntValue PANEL_HEIGHT = BUILDER
		.comment("Height of the preview panel, in GUI pixels.")
		.defineInRange("panelHeight", 156, 48, 512);

	private static final ModConfigSpec.IntValue LOAD_DELAY_MS = BUILDER
		.comment("How long (milliseconds) a schematic must stay highlighted before its 3D preview is",
			"built. This makes loading lazy/on-demand: schematics you merely scroll past are never",
			"built, only the one you actually settle on. 0 = build immediately on selection.")
		.defineInRange("previewLoadDelayMs", 150, 0, 5000);

	private static final ModConfigSpec.DoubleValue DEFAULT_YAW = BUILDER
		.comment("Initial horizontal rotation (degrees) of a freshly selected schematic.")
		.defineInRange("defaultYaw", 45.0, -180.0, 180.0);

	private static final ModConfigSpec.DoubleValue DEFAULT_PITCH = BUILDER
		.comment("Initial vertical tilt (degrees) of a freshly selected schematic.")
		.defineInRange("defaultPitch", 30.0, -90.0, 90.0);

	static final ModConfigSpec SPEC = BUILDER.build();

	public static boolean previewEnabled;
	public static int maxBlockVolume;
	public static int panelWidth;
	public static int panelHeight;
	public static int loadDelayMs;
	public static double defaultYaw;
	public static double defaultPitch;

	@SubscribeEvent
	static void onLoad(final ModConfigEvent event) {
		if (event.getConfig().getSpec() != SPEC)
			return;
		previewEnabled = PREVIEW_ENABLED.get();
		maxBlockVolume = MAX_BLOCK_VOLUME.get();
		panelWidth = PANEL_WIDTH.get();
		panelHeight = PANEL_HEIGHT.get();
		loadDelayMs = LOAD_DELAY_MS.get();
		defaultYaw = DEFAULT_YAW.get();
		defaultPitch = DEFAULT_PITCH.get();
	}
}
