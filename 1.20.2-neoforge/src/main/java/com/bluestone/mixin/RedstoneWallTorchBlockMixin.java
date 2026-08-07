package com.bluestone.mixin;

import com.bluestone.power.RedOnlyPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Wall-torch counterpart of {@link RedstoneTorchBlockMixin}. */
@Mixin(RedstoneWallTorchBlock.class)
public class RedstoneWallTorchBlockMixin {
    @Redirect(method = "hasNeighborSignal",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;hasSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
    private boolean bluestone$redOnlyEmit(Level level, BlockPos pos, Direction direction) {
        return RedOnlyPower.isEmittingRedstonePower(level, pos, direction);
    }
}
