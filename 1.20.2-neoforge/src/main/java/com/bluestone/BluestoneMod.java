package com.bluestone;

import com.bluestone.particle.BluestoneParticles;
import com.bluestone.registry.ModBlocks;
import com.bluestone.registry.ModItemGroups;
import com.bluestone.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bluestone mod entrypoint for NeoForge.
 *
 * <p>Bluestone is a fully isolated parallel power system to redstone. Blue components
 * (wire/repeater/comparator/torch/block) emit REAL redstone power so they activate vanilla
 * reactive blocks (pistons, doors, lamps, ...) for free, but red and blue COMPONENTS filter
 * each other out when reading their inputs, so the two systems never interact.</p>
 */
@Mod(BluestoneMod.MOD_ID)
public class BluestoneMod {
    public static final String MOD_ID = "bluestone";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public BluestoneMod(IEventBus modEventBus) {
        // Register every DeferredRegister on the mod event bus (blocks, items, creative tabs, particles).
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModItemGroups.register(modEventBus);
        BluestoneParticles.register(modEventBus);

        // Append entries to vanilla creative tabs (game-event-bus listener; see ModItemGroups).
        modEventBus.addListener(ModItemGroups::onBuildTabContents);

        // Ore generation is data-driven: data/bluestone/neoforge/biome_modifier/bluestone_ore.json
        // injects bluestone:bluestone_ore (same params as vanilla redstone ore) into overworld biomes.

        LOGGER.info("[Bluestone] Initialized - a parallel blue power system.");
    }

    /** Convenience id helper. */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
