package com.bluestone.particle;

import com.bluestone.BluestoneMod;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers the mod's own bluestone-dust particle type (a NEW particle, not the vanilla redstone
 * dust particle). Behaviour (rising/fading dust) is provided client-side by
 * {@link com.bluestone.client.particle.BluestoneDustParticle}; its texture is a placeholder at
 * {@code assets/bluestone/particles/bluestone_dust.json} -> {@code bluestone:particle/bluestone_dust}.
 */
public class BluestoneParticles {
    public static final DefaultParticleType BLUESTONE_DUST = register("bluestone_dust");

    private static DefaultParticleType register(String name) {
        return Registry.register(Registries.PARTICLE_TYPE, new Identifier(BluestoneMod.MOD_ID, name), new DefaultParticleType(false) {});
    }

    public static void initialize() {
        // force class load so the particle type registers
    }
}
