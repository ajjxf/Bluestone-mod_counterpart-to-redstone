package com.bluestone.block;

import com.bluestone.particle.BluestoneParticles;
import com.bluestone.power.BluePower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Bluestone torch (floor) - extends vanilla {@link RedstoneTorchBlock}; the lit/unlit toggle is
 * inherited (tick/neighborChanged call {@code hasNeighborSignal}), which we override to check
 * BLUE power. Real emission is suppressed (Option Y). Overrides {@code animateTick} to spawn
 * the mod's own blue dust particle (NOT the vanilla redstone particle).
 */
public class BluestoneTorchBlock extends RedstoneTorchBlock {
    public BluestoneTorchBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean hasNeighborSignal(Level level, BlockPos pos, BlockState state) {
        return BluePower.getEmittedBluePower(level, pos.below(), Direction.DOWN) > 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BlockStateProperties.LIT)) return;
        double d = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        double e = pos.getY() + 0.7 + (random.nextDouble() - 0.5) * 0.2;
        double f = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        level.addParticle(BluestoneParticles.BLUESTONE_DUST.get(), d, e, f, 0.0, 0.0, 0.0);
    }
}
