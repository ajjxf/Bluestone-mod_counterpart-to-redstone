package com.bluestone.item;

import com.bluestone.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

/**
 * Places either a floor bluestone torch (on ground) or a wall bluestone torch (on a wall side),
 * mirroring how vanilla's single redstone-torch item places both RedstoneTorchBlock and
 * WallRedstoneTorchBlock. Without this, the torch could only be placed on the floor.
 */
public class BluestoneTorchItem extends BlockItem {
    public BluestoneTorchItem(Block floorBlock, Settings settings) {
        super(floorBlock, settings);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        BlockPos pos = ctx.getBlockPos();
        WorldView world = ctx.getWorld();
        if (side == Direction.UP) {
            BlockState floor = ModBlocks.BLUESTONE_TORCH.getDefaultState();
            return floor.canPlaceAt(world, pos) ? floor : null;
        }
        if (side.getAxis().isHorizontal()) {
            // Borrow vanilla wall-torch facing resolution, then apply it to the bluestone wall torch.
            BlockState vanilla = Blocks.WALL_TORCH.getPlacementState(ctx);
            if (vanilla == null) return null;
            BlockState wall = ModBlocks.BLUESTONE_WALL_TORCH.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, vanilla.get(WallTorchBlock.FACING));
            return wall.canPlaceAt(world, pos) ? wall : null;
        }
        return null;
    }
}
