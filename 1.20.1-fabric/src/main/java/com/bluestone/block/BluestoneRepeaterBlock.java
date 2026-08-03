package com.bluestone.block;

import com.bluestone.particle.BluestoneParticles;
import com.bluestone.power.BluePower;
import com.bluestone.registry.ModBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * Bluestone repeater - extends vanilla {@link RepeaterBlock} (inherits delay/facing/locking/
 * update scheduling, so behaviour & quirks match redstone). Only input reading is overridden to
 * read BLUE power via {@link BluePower#getEmittedBluePower} (mirrors
 * {@code AbstractRedstoneGateBlock.getPower} which uses {@code world.getEmittedRedstonePower}).
 * Real emission is suppressed (Option Y). Locked only by another BLUE repeater on a side.
 */
@SuppressWarnings("deprecation")
public class BluestoneRepeaterBlock extends RepeaterBlock {
    public BluestoneRepeaterBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return false;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    protected int getPower(World world, BlockPos pos, BlockState state) {
        Direction direction = state.get(Properties.HORIZONTAL_FACING);
        BlockPos blockPos = pos.offset(direction);
        int i = BluePower.getEmittedBluePower(world, blockPos, direction);
        if (i >= 15) {
            return i;
        }
        BlockState blockState = world.getBlockState(blockPos);
        return Math.max(i, blockState.isOf(ModBlocks.BLUESTONE_WIRE) ? blockState.get(BluestoneWireBlock.POWER) : 0);
    }

    /**
     * Locked when a BLUE GATE (bluestone repeater/comparator) on a perpendicular side emits blue
     * power toward us, mirroring vanilla {@code RepeaterBlock.isLocked} with
     * {@code getSideInputFromGatesOnly=true}. Neutral sources (observer, lever, etc.) do NOT lock.
     */
    @Override
    public boolean isLocked(WorldView world, BlockPos pos, BlockState state) {
        Direction facing = state.get(Properties.HORIZONTAL_FACING);
        Direction[] sides = new Direction[]{ facing.rotateYClockwise(), facing.rotateYCounterclockwise() };
        for (Direction side : sides) {
            BlockState sideState = world.getBlockState(pos.offset(side));
            // Only blue gates count (getSideInputFromGatesOnly=true for repeaters)
            if (!(sideState.getBlock() instanceof BluestoneRepeaterBlock)
                    && !(sideState.getBlock() instanceof BluestoneComparatorBlock)) {
                continue;
            }
            // Check if the gate emits blue power toward us
            if (BluePower.weakTowards(sideState, (RedstoneView) world, pos.offset(side), side) > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(Properties.POWERED)) return;
        Direction direction = state.get(Properties.HORIZONTAL_FACING);
        double d = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        double e = pos.getY() + 0.4 + (random.nextDouble() - 0.5) * 0.2;
        double f = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        float g = -5.0f;
        if (random.nextBoolean()) {
            g = state.get(Properties.DELAY) * 2 - 1;
        }
        double h = (g /= 16.0f) * direction.getOffsetX();
        double i = g * direction.getOffsetZ();
        world.addParticle(BluestoneParticles.BLUESTONE_DUST, d + h, e, f + i, 0.0, 0.0, 0.0);
    }
}
