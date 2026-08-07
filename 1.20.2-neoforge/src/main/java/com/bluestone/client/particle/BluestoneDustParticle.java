package com.bluestone.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Client particle for {@link com.bluestone.particle.BluestoneParticles#BLUESTONE_DUST}. A faithful
 * blue mirror of vanilla {@code AbstractDustParticle}/{@code RedDustParticle}: same random colour
 * darkening (each particle gets a random brightness factor), same velocity damping (x0.1), same
 * random lifetime formula, same size grow-in and sprite-for-age animation. Only the base colour
 * differs: pure blue (0,0,1) = red&lt;-&gt;blue swap of vanilla redstone's (1,0,0).
 *
 * <p>Cannot extend {@code AbstractDustParticle} because it requires an {@code AbstractDustParticleEffect}
 * parameter; our particle type is a plain {@code SimpleParticleType}, so the relevant logic is
 * replicated here with a fixed blue colour.</p>
 */
public class BluestoneDustParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;

    private BluestoneDustParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.spriteSet = spriteSet;
        this.friction = 0.96f;
        // base colour = pure blue (red<->blue swap of vanilla redstone red)
        float bx = 0.0f, by = 0.0f, bz = 1.0f;
        // velocity damping (same as vanilla dust)
        this.xd *= 0.1;
        this.yd *= 0.1;
        this.zd *= 0.1;
        // random colour darkening (two random factors, exactly like AbstractDustParticle.darken)
        float f = this.random.nextFloat() * 0.4f + 0.6f;
        this.rCol = this.darken(bx, f);
        this.gCol = this.darken(by, f);
        this.bCol = this.darken(bz, f);
        // scale and lifetime (scale param = 1.0 like vanilla DustParticleEffect.DEFAULT)
        this.quadSize *= 0.75f;
        int i = (int) (8.0 / (this.random.nextDouble() * 0.8 + 0.2));
        this.lifetime = i;
        this.setSpriteFromAge(spriteSet);
    }

    private float darken(float colorComponent, float multiplier) {
        return (this.random.nextFloat() * 0.2f + 0.8f) * colorComponent * multiplier;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public float getQuadSize(float tickDelta) {
        return this.quadSize * Mth.clamp(((float) this.age + tickDelta) / (float) this.lifetime * 32.0f, 0.0f, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.spriteSet);
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new BluestoneDustParticle(level, x, y, z, xd, yd, zd, this.spriteSet);
        }
    }
}
