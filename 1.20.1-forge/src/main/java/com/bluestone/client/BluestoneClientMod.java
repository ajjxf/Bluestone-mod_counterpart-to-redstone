package com.bluestone.client;

import com.bluestone.BluestoneMod;
import com.bluestone.block.BluestoneWireBlock;
import com.bluestone.client.particle.BluestoneDustParticle;
import com.bluestone.particle.BluestoneParticles;
import com.bluestone.registry.ModBlocks;
import com.bluestone.registry.ModItems;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client setup: registers a blue power-level colour provider for the bluestone wire (mirrors
 * vanilla {@code RedStoneWireBlock.getWireColor} but in blue hues) and a fixed blue item colour
 * for the dust item, plus render layers for the transparent/cutout bluestone blocks.
 *
 * <p>Without the render layers, the mod blocks default to the SOLID layer, which has no alpha
 * test, so transparent texels render as solid black. Vanilla registers redstone wire/torch as
 * cutout and repeaters/comparators as cutout-mipped; we mirror that exactly.</p>
 */
@Mod.EventBusSubscriber(modid = BluestoneMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BluestoneClientMod {

    private BluestoneClientMod() {}

    /** Register the bluestone-dust particle factory (texture: bluestone:particle/bluestone_dust). */
    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BluestoneParticles.BLUESTONE_DUST.get(), BluestoneDustParticle.Factory::new);
    }

    /** Register block colour providers (wire power gradient). */
    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        BlockColors colors = event.getBlockColors();
        colors.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) return -1;
            int power = state.getValue(BluestoneWireBlock.POWER);
            return blueColor(power);
        }, ModBlocks.BLUESTONE_WIRE.get());

        colors.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) return -1;
            int power = state.getValue(BluestoneWireBlock.POWER);
            return purpleColor(power);
        }, ModBlocks.PURPLESTONE_WIRE.get());
    }

    /** Register item colour providers (dust item flat colours at full power). */
    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColors colors = event.getItemColors();
        colors.register((stack, tintIndex) -> tintIndex == 0 ? blueColor(15) : -1, ModItems.BLUESTONE.get());
        colors.register((stack, tintIndex) -> tintIndex == 0 ? purpleColor(15) : -1, ModItems.PURPLESTONE.get());
    }

    /** Cutout / cutout-mipped render layers. */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Cutout (alpha < 0.1 discarded): wire + torches + ores (lit glow art has cutout edges).
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLUESTONE_WIRE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PURPLESTONE_WIRE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLUESTONE_TORCH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLUESTONE_WALL_TORCH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLUESTONE_ORE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DEEPSLATE_BLUESTONE_ORE.get(), RenderType.cutout());
            // Cutout-mipped: repeaters and comparators (mirrors vanilla redstone gates).
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLUESTONE_REPEATER.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLUESTONE_COMPARATOR.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CONVERTER_REPEATER.get(), RenderType.cutoutMipped());
        });
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
