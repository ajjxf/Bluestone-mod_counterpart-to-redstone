package com.bluestone.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Bluestone block - the blue counterpart of the redstone block. A constant blue power source
 * (level 15). Emits NO real redstone power (Option Y); {@link com.bluestone.power.BluePower}
 * reports it as 15 in every direction, so it activates reactive blocks via the
 * {@code World.isReceivingRedstonePower} mixin and is ignored by redstone (which sees 0).
 */
public class BluestoneBlock extends Block {
    public BluestoneBlock(AbstractBlock.Settings settings) {
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
}
