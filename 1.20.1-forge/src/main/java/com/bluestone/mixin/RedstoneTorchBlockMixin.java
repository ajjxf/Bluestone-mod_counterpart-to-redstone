package com.bluestone.mixin;

import com.bluestone.power.RedOnlyPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The redstone (floor) torch's {@code hasNeighborSignal} calls {@code level.hasSignal}, which now
 * (via {@link RedstoneViewMixin}'s injection into {@code BlockState.getSignal}) also reports blue
 * power. To keep the red torch responding to RED only (so blue cannot turn a red torch off),
 * redirect that call to {@link RedOnlyPower} (blue-free).
 *
 * <p>Blue torches override {@code hasNeighborSignal} themselves and never call {@code level.hasSignal},
 * so they are unaffected.</p>
 */
@Mixin(RedstoneTorchBlock.class)
public class RedstoneTorchBlockMixin {
    @Redirect(method = "hasNeighborSignal",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;hasSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
    private boolean bluestone$redOnlyEmit(Level level, BlockPos pos, Direction direction) {
        return RedOnlyPower.isEmittingRedstonePower(level, pos, direction);
    }
}
