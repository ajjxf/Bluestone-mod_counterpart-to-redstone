package com.bluestone.mixin;

import com.bluestone.power.ParticleColorResolver;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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

    private static final Vector3f SCULK_BLUE = Vec3.fromRGB24(3790560).toVector3f();

    @Redirect(method = "animateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void bluestone$tintParticle(Level level, ParticleOptions particle, double x, double y, double z,
                                        double vx, double vy, double vz,
                                        BlockState state, Level level2, BlockPos pos, RandomSource random) {
        Vector3f toColor = ParticleColorResolver.resolveColor(level, pos);
        DustColorTransitionOptions tinted = new DustColorTransitionOptions(SCULK_BLUE, toColor, 1.0f);
        level.addParticle(tinted, x, y, z, vx, vy, vz);
    }
}
