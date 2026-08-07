package com.bluestone.particle;

import com.bluestone.BluestoneMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the mod's own bluestone-dust particle type (a NEW particle, not the vanilla redstone
 * dust particle). Behaviour (rising/fading dust) is provided client-side by
 * {@link com.bluestone.client.particle.BluestoneDustParticle}; its texture is a placeholder at
 * {@code assets/bluestone/particles/bluestone_dust.json} -> {@code bluestone:particle/bluestone_dust}.
 */
public final class BluestoneParticles {
    /** NeoForge deferred register bound to the vanilla particle-type registry. */
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.PARTICLE_TYPE, BluestoneMod.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLUESTONE_DUST =
            PARTICLE_TYPES.register("bluestone_dust", () -> new SimpleParticleType(false));

    private BluestoneParticles() {}

    /** Called from the mod constructor to attach the deferred register to the mod event bus. */
    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
