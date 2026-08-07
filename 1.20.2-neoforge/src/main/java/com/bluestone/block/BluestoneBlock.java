package com.bluestone.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bluestone block - the blue counterpart of the redstone block. A constant blue power source
 * (level 15). Emits NO real redstone power (Option Y); {@link com.bluestone.power.BluePower}
 * reports it as 15 in every direction, so it activates reactive blocks via the
 * {@code Level.hasNeighborSignal} mixin and is ignored by redstone (which sees 0).
 */
public class BluestoneBlock extends Block {
    public BluestoneBlock(BlockBehaviour.Properties properties) {
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
}
