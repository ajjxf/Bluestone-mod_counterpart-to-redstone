package com.bluestone.mixin;

import com.bluestone.power.RedOnlyPower;
import net.minecraft.block.WallRedstoneTorchBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Wall-torch counterpart of {@link RedstoneTorchBlockMixin}. */
@Mixin(WallRedstoneTorchBlock.class)
public class RedstoneWallTorchBlockMixin {
    @Redirect(method = "shouldUnpower",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;isEmittingRedstonePower(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"))
    private boolean bluestone$redOnlyEmit(World world, BlockPos pos, Direction direction) {
        return RedOnlyPower.isEmittingRedstonePower(world, pos, direction);
    }
}
