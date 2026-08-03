package com.bluestone;

import com.bluestone.particle.BluestoneParticles;
import com.bluestone.registry.ModBlocks;
import com.bluestone.registry.ModItemGroups;
import com.bluestone.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bluestone mod entrypoint.
 *
 * <p>Bluestone is a fully isolated parallel power system to redstone. Blue components
 * (wire/repeater/comparator/torch/block) emit REAL redstone power so they activate vanilla
 * reactive blocks (pistons, doors, lamps, ...) for free, but red and blue COMPONENTS filter
 * each other out when reading their inputs, so the two systems never interact.</p>
 */
public class BluestoneMod implements ModInitializer {
    public static final String MOD_ID = "bluestone";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModBlocks.initialize();
        ModItems.initialize();
        ModItemGroups.initialize();
        BluestoneParticles.initialize();
        registerOreGeneration();
        LOGGER.info("[Bluestone] Initialized - a parallel blue power system.");
    }

    /**
     * Bluestone ore generates like vanilla redstone ore: overworld only, underground-ores step,
     * 4 veins of size 8 per chunk between y 0 and y 16 (see
     * {@code data/bluestone/worldgen/placed_feature/bluestone_ore.json}).
     */
    private static void registerOreGeneration() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                RegistryKey.of(RegistryKeys.PLACED_FEATURE, id("bluestone_ore")));
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
