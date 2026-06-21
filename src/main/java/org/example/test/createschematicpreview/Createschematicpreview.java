package org.example.test.createschematicpreview;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import org.slf4j.Logger;

@Mod(Createschematicpreview.MODID)
public class Createschematicpreview {

	public static final String MODID = "createschematicpreview";
	private static final Logger LOGGER = LogUtils.getLogger();

	public Createschematicpreview(IEventBus modEventBus, ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
		LOGGER.info("Create: Schematics Preview loaded");
	}
}
