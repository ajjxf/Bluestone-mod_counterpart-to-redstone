package com.bluestone.item;

import com.bluestone.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;

/**
 * Places either a floor bluestone torch (on ground) or a wall bluestone torch (on a wall side),
 * mirroring how vanilla's single redstone-torch item places both RedstoneTorchBlock and
 * RedstoneWallTorchBlock. Without this, the torch could only be placed on the floor.
 */
public class BluestoneTorchItem extends BlockItem {
    public BluestoneTorchItem(Block floorBlock, Item.Properties properties) {
        super(floorBlock, properties);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(BlockPlaceContext ctx) {
        Direction side = ctx.getClickedFace();
        BlockPos pos = ctx.getClickedPos();
        LevelReader level = ctx.getLevel();
        if (side == Direction.UP) {
            BlockState floor = ModBlocks.BLUESTONE_TORCH.get().defaultBlockState();
            return floor.canSurvive(level, pos) ? floor : null;
        }
        if (side.getAxis().isHorizontal()) {
            // Borrow vanilla wall-torch facing resolution, then apply it to the bluestone wall torch.
            BlockState vanilla = Blocks.WALL_TORCH.getStateForPlacement(ctx);
            if (vanilla == null) return null;
            BlockState wall = ModBlocks.BLUESTONE_WALL_TORCH.get().defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, vanilla.getValue(WallTorchBlock.FACING));
            return wall.canSurvive(level, pos) ? wall : null;
        }
        return null;
    }
}
