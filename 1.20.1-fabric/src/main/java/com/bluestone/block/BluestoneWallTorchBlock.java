package com.bluestone.block;

import com.bluestone.particle.BluestoneParticles;
import com.bluestone.power.BluePower;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallRedstoneTorchBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Bluestone wall torch - wall variant of {@link BluestoneTorchBlock}. Extends vanilla
 * {@link WallRedstoneTorchBlock}; overrides {@code shouldUnpower} to check BLUE power, and
 * {@code randomDisplayTick} to spawn the mod's own blue dust particle.
 */
public class BluestoneWallTorchBlock extends WallRedstoneTorchBlock {
    public BluestoneWallTorchBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    protected boolean shouldUnpower(World world, BlockPos pos, BlockState state) {
        Direction direction = state.get(Properties.HORIZONTAL_FACING).getOpposite();
        return BluePower.getEmittedBluePower(world, pos.offset(direction), direction) > 0;
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
        Direction direction = state.get(Properties.HORIZONTAL_FACING).getOpposite();
        double e = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2 + 0.27 * direction.getOffsetX();
        double f = pos.getY() + 0.7 + (random.nextDouble() - 0.5) * 0.2 + 0.22;
        double g = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2 + 0.27 * direction.getOffsetZ();
        world.addParticle(BluestoneParticles.BLUESTONE_DUST, e, f, g, 0.0, 0.0, 0.0);
    }
}
