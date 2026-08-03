package com.bluestone.mixin;

import com.bluestone.power.BluePower;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RedstoneView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes vanilla reactive blocks (pistons via {@code isEmittingRedstonePower}, doors/lamps/etc. via
 * {@code isReceivingRedstonePower}) also see BLUE power. Blue components emit NO real redstone power
 * (Option Y), so without this they would not activate anything.
 *
 * <p>This is an interface mixin on {@link RedstoneView} (both methods are {@code default} there;
 * {@code World} does not override them). The redstone torch, which also uses
 * {@code isEmittingRedstonePower} for its inversion check, is exempted via a redirect in
 * {@code RedstoneTorchBlockMixin} so it keeps responding to RED only.</p>
 */
@Mixin(RedstoneView.class)
public interface RedstoneViewMixin {

    @Inject(method = "isEmittingRedstonePower(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z",
            at = @At("RETURN"), cancellable = true)
    default void bluestone$emitBlue(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())
                && BluePower.getEmittedBluePower((RedstoneView) (Object) this, pos, direction) > 0) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z",
            at = @At("RETURN"), cancellable = true)
    default void bluestone$receiveBlue(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())
                && BluePower.isReceivingBluePower((RedstoneView) (Object) this, pos)) {
            cir.setReturnValue(true);
        }
    }
}
