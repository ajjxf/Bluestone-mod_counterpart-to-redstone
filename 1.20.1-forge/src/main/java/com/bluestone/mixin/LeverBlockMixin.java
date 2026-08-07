package com.bluestone.mixin;

import com.bluestone.power.ParticleColorResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tints the lever's activation particles based on surrounding signal types
 * (see {@link ParticleColorResolver}).
 *
 * <p>Mojmap 1.20.2 {@code LeverBlock} emits its redstone dust particle from the private static
 * helper {@code makeParticle(BlockState, LevelAccessor, BlockPos, float)}, which constructs a
 * {@code new DustParticleOptions(Vector3f, float)}. We redirect that constructor call to swap in
 * the colour resolved from the surrounding signal mix (blue/purple/red).</p>
 */
@Mixin(net.minecraft.world.level.block.LeverBlock.class)
public class LeverBlockMixin {

    @Redirect(method = "makeParticle", at = @At(value = "NEW", target = "(Lorg/joml/Vector3f;F)Lnet/minecraft/core/particles/DustParticleOptions;"))
    private static DustParticleOptions bluestone$tintParticle(Vector3f color, float scale,
                                                               BlockState state, LevelAccessor level, BlockPos pos, float alpha) {
        Vector3f tint = ParticleColorResolver.resolveColor(level, pos);
        return new DustParticleOptions(tint, scale);
    }
}
