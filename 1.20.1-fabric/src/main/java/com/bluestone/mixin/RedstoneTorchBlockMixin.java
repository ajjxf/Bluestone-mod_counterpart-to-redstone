package com.bluestone.mixin;

import com.bluestone.power.RedOnlyPower;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The redstone (floor) torch's {@code shouldUnpower} calls {@code world.isEmittingRedstonePower},
 * which the {@link RedstoneViewMixin} now OR-s blue into. To keep the red torch responding to RED
 * only (so blue cannot turn a red torch off), redirect that call to {@link RedOnlyPower} (blue-free).
 * Blue torches override {@code shouldUnpower} themselves, so they are unaffected.
 */
@Mixin(RedstoneTorchBlock.class)
public class RedstoneTorchBlockMixin {
    @Redirect(method = "shouldUnpower",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;isEmittingRedstonePower(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"))
    private boolean bluestone$redOnlyEmit(World world, BlockPos pos, Direction direction) {
        return RedOnlyPower.isEmittingRedstonePower(world, pos, direction);
    }
}
