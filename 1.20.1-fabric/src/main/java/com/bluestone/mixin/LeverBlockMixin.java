package com.bluestone.mixin;

import com.bluestone.power.ParticleColorResolver;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeverBlock;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tints the lever's activation particles based on surrounding signal types
 * (see {@link ParticleColorResolver}).
 */
@Mixin(LeverBlock.class)
public class LeverBlockMixin {

    @Redirect(method = "spawnParticles", at = @At(value = "NEW", target = "(Lorg/joml/Vector3f;F)Lnet/minecraft/particle/DustParticleEffect;"))
    private static DustParticleEffect bluestone$tintParticle(Vector3f color, float scale,
                                                               BlockState state, WorldAccess world, BlockPos pos, float alpha) {
        Vector3f tint = ParticleColorResolver.resolveColor(world, pos);
        return new DustParticleEffect(tint, scale);
    }
}
