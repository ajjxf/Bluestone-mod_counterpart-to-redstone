package com.bluestone.block;

import com.bluestone.particle.BluestoneParticles;
import com.bluestone.power.BluePower;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Bluestone torch (floor) - extends vanilla {@link RedstoneTorchBlock}; the lit/unlit toggle is
 * inherited (scheduledTick/neighborUpdate call {@code shouldUnpower}), which we override to check
 * BLUE power. Real emission is suppressed (Option Y). Overrides {@code randomDisplayTick} to spawn
 * the mod's own blue dust particle (NOT the vanilla redstone particle).
 */
public class BluestoneTorchBlock extends RedstoneTorchBlock {
    public BluestoneTorchBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    protected boolean shouldUnpower(World world, BlockPos pos, BlockState state) {
        return BluePower.getEmittedBluePower(world, pos.down(), Direction.DOWN) > 0;
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
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(Properties.LIT)) return;
        double d = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        double e = pos.getY() + 0.7 + (random.nextDouble() - 0.5) * 0.2;
        double f = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        world.addParticle(BluestoneParticles.BLUESTONE_DUST, d, e, f, 0.0, 0.0, 0.0);
    }
}
