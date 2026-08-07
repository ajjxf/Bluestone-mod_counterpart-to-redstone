package com.bluestone.block;

import com.bluestone.particle.BluestoneParticles;
import com.bluestone.power.BluePower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Bluestone wall torch - wall variant of {@link BluestoneTorchBlock}. Extends vanilla
 * {@link RedstoneWallTorchBlock}; overrides {@code hasNeighborSignal} to check BLUE power, and
 * {@code animateTick} to spawn the mod's own blue dust particle.
 */
public class BluestoneWallTorchBlock extends RedstoneWallTorchBlock {
    public BluestoneWallTorchBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean hasNeighborSignal(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        return BluePower.getEmittedBluePower(level, pos.relative(direction), direction) > 0;
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
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        double e = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2 + 0.27 * direction.getStepX();
        double f = pos.getY() + 0.7 + (random.nextDouble() - 0.5) * 0.2 + 0.22;
        double g = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2 + 0.27 * direction.getStepZ();
        level.addParticle(BluestoneParticles.BLUESTONE_DUST.get(), e, f, g, 0.0, 0.0, 0.0);
    }
}
