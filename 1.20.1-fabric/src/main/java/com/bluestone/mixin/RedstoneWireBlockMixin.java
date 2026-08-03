package com.bluestone.mixin;

import com.bluestone.power.BluePower;
import com.bluestone.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes vanilla redstone wire treat purplestone wire as a same-color wire:
 * <ul>
 *   <li>{@code connectsTo} (static): purplestone is accepted as a wire connection (like REDSTONE_WIRE).</li>
 *   <li>{@code increasePower} (private): reads purplestone's POWER property (wire-to-wire -1 decay).</li>
 *   <li>{@code getReceivedRedstonePower}: toggles the shared {@code BluePower.wiresGivePower} flag so
 *       that purplestone (a separate instance) also stays silent while redstone computes its input,
 *       preventing full-power feedback at red/purple crossings.</li>
 * </ul>
 * Without this, red wire would only connect to other REDSTONE_WIRE blocks, ignoring purplestone.
 */
@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin {

    /** Accept purplestone as a wire-to-wire connection (same as REDSTONE_WIRE). */
    @Inject(method = "connectsTo(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void bluestone$connectPurplestone(BlockState state, @Nullable Direction dir,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (state.isOf(ModBlocks.PURPLESTONE_WIRE)) {
            cir.setReturnValue(true);
        }
    }

    /** Read purplestone POWER when vanilla wire scans neighbours for wire-to-wire power.
     *  increasePower returns 0 for non-REDSTONE_WIRE; we override that for purplestone. */
    @Inject(method = "increasePower", at = @At("RETURN"), cancellable = true)
    private void bluestone$readPurplePower(BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() == 0 && state.isOf(ModBlocks.PURPLESTONE_WIRE)) {
            cir.setReturnValue(state.get(Properties.POWER));
        }
    }

    /**
     * Synchronise the shared {@code BluePower.wiresGivePower} flag with vanilla's per-instance
     * {@code wiresGivePower} field while vanilla redstone computes its own received power.
     * Vanilla toggles its OWN instance field, but purplestone is a different instance and checks
     * {@code BluePower.wiresGivePower} - without syncing, purplestone would emit full power back,
     * creating a no-decay feedback loop at the red/purple boundary.
     */
    @Inject(method = "getReceivedRedstonePower(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)I",
            at = @At("HEAD"))
    private void bluestone$syncWiresGivePowerOff(World world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        BluePower.wiresGivePower = false;
    }

    @Inject(method = "getReceivedRedstonePower(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)I",
            at = @At("RETURN"))
    private void bluestone$syncWiresGivePowerOn(World world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        BluePower.wiresGivePower = true;
    }

    /**
     * In {@code prepare}, vanilla checks {@code state.isOf(this)} to decide whether to propagate
     * shape-update notifications to wires up/down in each horizontal direction. Redirect these
     * checks so bluestone/purplestone wires also receive the notification (cross-type UP connections).
     */
    @Redirect(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
    private boolean bluestone$isAnyWire(BlockState state, Block originalBlock) {
        return state.isOf(originalBlock)
                || state.isOf(ModBlocks.BLUESTONE_WIRE)
                || state.isOf(ModBlocks.PURPLESTONE_WIRE);
    }
}
