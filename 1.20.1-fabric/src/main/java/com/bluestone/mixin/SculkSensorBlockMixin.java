package com.bluestone.mixin;

import com.bluestone.power.ParticleColorResolver;
import net.minecraft.block.BlockState;
import net.minecraft.block.SculkSensorBlock;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tints the sculk sensor's activation particles based on surrounding signal types
 * (see {@link ParticleColorResolver}).
 */
@Mixin(SculkSensorBlock.class)
public class SculkSensorBlockMixin {

    private static final Vector3f SCULK_BLUE = Vec3d.unpackRgb(3790560).toVector3f();

    @Redirect(method = "randomDisplayTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"))
    private void bluestone$tintParticle(World world, ParticleEffect particle, double x, double y, double z,
                                        double vx, double vy, double vz,
                                        BlockState state, World world2, net.minecraft.util.math.BlockPos pos, net.minecraft.util.math.random.Random random) {
        Vector3f toColor = ParticleColorResolver.resolveColor(world, pos);
        DustColorTransitionParticleEffect tinted = new DustColorTransitionParticleEffect(SCULK_BLUE, toColor, 1.0f);
        world.addParticle(tinted, x, y, z, vx, vy, vz);
    }
}
