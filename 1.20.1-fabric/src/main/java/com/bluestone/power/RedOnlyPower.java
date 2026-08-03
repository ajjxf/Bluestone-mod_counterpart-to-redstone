package com.bluestone.power;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Red-only power helper used by the redstone-torch mixins.
 *
 * <p>The {@link com.bluestone.mixin.RedstoneViewMixin} OR-s blue into
 * {@code isEmittingRedstonePower} so reactive blocks (pistons, ...) activate from blue. The redstone
 * torch also uses {@code isEmittingRedstonePower} for its inversion check; to keep it responding to
 * RED only, its call is redirected here, which replicates the vanilla blue-free logic (blue
 * components emit NO real redstone power in Option Y, so vanilla {@code getWeakRedstonePower} /
 * {@code getReceivedStrongRedstonePower} are already blue-free).</p>
 */
public final class RedOnlyPower {
    private RedOnlyPower() {}

    /** Vanilla {@code RedstoneView.isEmittingRedstonePower}, blue-free. */
    public static boolean isEmittingRedstonePower(World world, BlockPos pos, Direction direction) {
        BlockState state = world.getBlockState(pos);
        int weak = state.getWeakRedstonePower(world, pos, direction);
        int v = state.isSolidBlock(world, pos)
                ? Math.max(weak, world.getReceivedStrongRedstonePower(pos))
                : weak;
        return v > 0;
    }
}
