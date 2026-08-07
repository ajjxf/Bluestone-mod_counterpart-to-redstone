package com.bluestone.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Red-only power helper used by the redstone-torch mixins.
 *
 * <p>{@link com.bluestone.mixin.RedstoneViewMixin} overrides {@code Level.hasSignal} to OR in blue
 * power, so reactive blocks (pistons, ...) activate from blue. The redstone torch also reads
 * {@code level.hasSignal} for its inversion check; to keep it responding to RED only, its call is
 * redirected here. We replicate the vanilla {@code hasSignal} logic (red-only) by calling
 * {@code getSignal} directly — {@code getSignal} is NOT overridden by the mod and stays pure
 * (reads real redstone only), so this never sees blue.</p>
 */
public final class RedOnlyPower {
    private RedOnlyPower() {}

    /** Vanilla {@code SignalGetter.hasSignal}, blue-free. */
    public static boolean isEmittingRedstonePower(Level level, BlockPos pos, Direction direction) {
        BlockState state = level.getBlockState(pos);
        int weak = state.getSignal(level, pos, direction);
        int v = state.shouldCheckWeakPower(level, pos, direction)
                ? Math.max(weak, level.getDirectSignalTo(pos))
                : weak;
        return v > 0;
    }
}
