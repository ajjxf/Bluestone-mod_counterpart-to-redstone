package com.bluestone.block;

import com.bluestone.particle.BluestoneParticles;
import com.bluestone.power.BluePower;
import com.bluestone.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Bluestone repeater - extends vanilla {@link RepeaterBlock} (inherits delay/facing/locking/
 * update scheduling, so behaviour & quirks match redstone). Only input reading is overridden to
 * read BLUE power via {@link BluePower#getEmittedBluePower} (mirrors
 * {@code DiodeBlock.getInputSignal} which uses {@code level.getSignal}).
 * Real emission is suppressed (Option Y). Locked only by another BLUE repeater on a side.
 */
@SuppressWarnings("deprecation")
public class BluestoneRepeaterBlock extends RepeaterBlock {
    public BluestoneRepeaterBlock(BlockBehaviour.Properties properties) {
        super(properties);
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
    protected int getInputSignal(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos blockPos = pos.relative(direction);
        int i = BluePower.getEmittedBluePower(level, blockPos, direction);
        if (i >= 15) {
            return i;
        }
        BlockState blockState = level.getBlockState(blockPos);
        return Math.max(i, blockState.is(ModBlocks.BLUESTONE_WIRE.get()) ? blockState.getValue(BluestoneWireBlock.POWER) : 0);
    }

    /**
     * Locked when a BLUE GATE (bluestone repeater/comparator) on a perpendicular side emits blue
     * power toward us, mirroring vanilla {@code RepeaterBlock.isLocked} with
     * {@code isAlternateSignal=true}. Neutral sources (observer, lever, etc.) do NOT lock.
     */
    @Override
    public boolean isLocked(LevelReader level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction[] sides = new Direction[]{ facing.getClockWise(), facing.getCounterClockWise() };
        for (Direction side : sides) {
            BlockState sideState = level.getBlockState(pos.relative(side));
            // Only blue gates count (isAlternateSignal=true for repeaters)
            if (!(sideState.getBlock() instanceof BluestoneRepeaterBlock)
                    && !(sideState.getBlock() instanceof BluestoneComparatorBlock)) {
                continue;
            }
            // Check if the gate emits blue power toward us
            if (BluePower.weakTowards(sideState, (SignalGetter) level, pos.relative(side), side) > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BlockStateProperties.POWERED)) return;
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        double d = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        double e = pos.getY() + 0.4 + (random.nextDouble() - 0.5) * 0.2;
        double f = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        float g = -5.0f;
        if (random.nextBoolean()) {
            g = state.getValue(BlockStateProperties.DELAY) * 2 - 1;
        }
        double h = (g /= 16.0f) * direction.getStepX();
        double i = g * direction.getStepZ();
        level.addParticle(BluestoneParticles.BLUESTONE_DUST.get(), d + h, e, f + i, 0.0, 0.0, 0.0);
    }
}
