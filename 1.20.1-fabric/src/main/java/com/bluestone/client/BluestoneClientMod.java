package com.bluestone.client;

import com.bluestone.block.BluestoneWireBlock;
import com.bluestone.block.PurplestoneWireBlock;
import net.minecraft.block.RedstoneWireBlock;
import com.bluestone.client.particle.BluestoneDustParticle;
import com.bluestone.particle.BluestoneParticles;
import com.bluestone.registry.ModBlocks;
import com.bluestone.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.render.RenderLayer;

/**
 * Client setup: registers a blue power-level colour provider for the bluestone wire (mirrors
 * vanilla {@code RedstoneWireBlock.getWireColor} but in blue hues) and a fixed blue item colour
 * for the dust item, plus render layers for the transparent/cutout bluestone blocks.
 *
 * <p>Without the render layers, the mod blocks default to the SOLID layer, which has no alpha
 * test, so transparent texels render as solid black. Vanilla registers redstone wire/torch as
 * cutout and repeaters/comparators as cutout-mipped; we mirror that exactly.</p>
 */
public class BluestoneClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register the bluestone-dust particle factory (texture: bluestone:particle/bluestone_dust).
        ParticleFactoryRegistry.getInstance().register(BluestoneParticles.BLUESTONE_DUST, BluestoneDustParticle.Factory::new);

        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            if (tintIndex != 0) return -1;
            int power = state.get(BluestoneWireBlock.POWER);
            return blueColor(power);
        }, ModBlocks.BLUESTONE_WIRE);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 0 ? blueColor(15) : -1,
                ModItems.BLUESTONE);

        // Purplestone wire: purple gradient (tints the grey dust sprites purple by power level)
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            if (tintIndex != 0) return -1;
            int power = state.get(RedstoneWireBlock.POWER);
            return purpleColor(power);
        }, ModBlocks.PURPLESTONE_WIRE);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 0 ? purpleColor(15) : -1,
                ModItems.PURPLESTONE);

        // Cutout (alpha < 0.1 discarded): wire + torches + ores (lit glow art has cutout edges).
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLUESTONE_WIRE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PURPLESTONE_WIRE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLUESTONE_TORCH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLUESTONE_WALL_TORCH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLUESTONE_ORE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DEEPSLATE_BLUESTONE_ORE, RenderLayer.getCutout());
        // Cutout-mipped: repeaters and comparators (mirrors vanilla redstone gates).
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLUESTONE_REPEATER, RenderLayer.getCutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLUESTONE_COMPARATOR, RenderLayer.getCutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CONVERTER_REPEATER, RenderLayer.getCutoutMipped());
    }

    /** Blue gradient: power 0 = RGB(15,25,90) dark, power 15 = RGB(40,100,240) bright blue. Linear. */
    private static int blueColor(int power) {
        float t = Math.max(0, Math.min(15, power)) / 15f;
        int r = (int) (15 + t * 25);
        int g = (int) (25 + t * 75);
        int b = (int) (90 + t * 150);
        return (r << 16) | (g << 8) | b;
    }

    /** Purple gradient: power 0 = RGB(60,15,80) dark, power 15 = RGB(160,55,210) brighter+redder. Linear. */
    private static int purpleColor(int power) {
        float t = Math.max(0, Math.min(15, power)) / 15f;
        int r = (int) (60 + t * 100);
        int g = (int) (15 + t * 40);
        int b = (int) (80 + t * 130);
        return (r << 16) | (g << 8) | b;
    }
}
