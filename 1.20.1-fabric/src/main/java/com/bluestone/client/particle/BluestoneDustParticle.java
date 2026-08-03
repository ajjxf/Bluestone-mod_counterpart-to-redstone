package com.bluestone.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/**
 * Client particle for {@link com.bluestone.particle.BluestoneParticles#BLUESTONE_DUST}. A faithful
 * blue mirror of vanilla {@code AbstractDustParticle}/{@code RedDustParticle}: same random colour
 * darkening (each particle gets a random brightness factor), same velocity damping (x0.1), same
 * random lifetime formula, same size grow-in and sprite-for-age animation. Only the base colour
 * differs: pure blue (0,0,1) = red&lt;-&gt;blue swap of vanilla redstone's (1,0,0).
 *
 * <p>Cannot extend {@code AbstractDustParticle} because it requires an {@code AbstractDustParticleEffect}
 * parameter; our particle type is a plain {@code DefaultParticleType}, so the relevant logic is
 * replicated here with a fixed blue colour.
 */
public class BluestoneDustParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;

    private BluestoneDustParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;
        this.velocityMultiplier = 0.96f;
        this.ascending = true;
        // base colour = pure blue (red<->blue swap of vanilla redstone red)
        float bx = 0.0f, by = 0.0f, bz = 1.0f;
        // velocity damping (same as vanilla dust)
        this.velocityX *= 0.1;
        this.velocityY *= 0.1;
        this.velocityZ *= 0.1;
        // random colour darkening (two random factors, exactly like AbstractDustParticle.darken)
        float f = this.random.nextFloat() * 0.4f + 0.6f;
        this.red = this.darken(bx, f);
        this.green = this.darken(by, f);
        this.blue = this.darken(bz, f);
        // scale and lifetime (scale param = 1.0 like vanilla DustParticleEffect.DEFAULT)
        this.scale *= 0.75f;
        int i = (int) (8.0 / (this.random.nextDouble() * 0.8 + 0.2));
        this.maxAge = i;
        this.setSpriteForAge(spriteProvider);
    }

    private float darken(float colorComponent, float multiplier) {
        return (this.random.nextFloat() * 0.2f + 0.8f) * colorComponent * multiplier;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public float getSize(float tickDelta) {
        return this.scale * MathHelper.clamp(((float) this.age + tickDelta) / (float) this.maxAge * 32.0f, 0.0f, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteForAge(this.spriteProvider);
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new BluestoneDustParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
