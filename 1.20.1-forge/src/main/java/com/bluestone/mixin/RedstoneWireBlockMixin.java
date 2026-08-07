package com.bluestone.mixin;

import com.bluestone.power.BluePower;
import com.bluestone.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes vanilla redstone wire treat purplestone wire as a same-color wire:
 * <ul>
 *   <li>{@code shouldConnectTo} (static): purplestone is accepted as a wire connection (like REDSTONE_WIRE).</li>
 *   <li>{@code getWireSignal} (private): reads purplestone's POWER property (wire-to-wire -1 decay).</li>
 *   <li>{@code calculateTargetStrength}: toggles the shared {@code BluePower.wiresGivePower} flag so
 *       that purplestone (a separate instance) also stays silent while redstone computes its input,
 *       preventing full-power feedback at red/purple crossings.</li>
 * </ul>
 * Without this, red wire would only connect to other REDSTONE_WIRE blocks, ignoring purplestone.
 */
@Mixin(RedStoneWireBlock.class)
public abstract class RedstoneWireBlockMixin {

    /** Accept purplestone as a wire-to-wire connection (same as REDSTONE_WIRE). */
    @Inject(method = "shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void bluestone$connectPurplestone(BlockState state, @Nullable Direction dir,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (state.is(ModBlocks.PURPLESTONE_WIRE.get())) {
            cir.setReturnValue(true);
        }
    }

    /** Read purplestone POWER when vanilla wire scans neighbours for wire-to-wire power.
     *  getWireSignal returns 0 for non-REDSTONE_WIRE; we override that for purplestone. */
    @Inject(method = "getWireSignal", at = @At("RETURN"), cancellable = true)
    private void bluestone$readPurplePower(BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() == 0 && state.is(ModBlocks.PURPLESTONE_WIRE.get())) {
            cir.setReturnValue(state.getValue(BlockStateProperties.POWER));
        }
    }

    /**
     * Synchronise the shared {@code BluePower.wiresGivePower} flag with vanilla's per-instance
     * {@code shouldSignal} field while vanilla redstone computes its own received power.
     * Vanilla toggles its OWN instance field, but purplestone is a different instance and checks
     * {@code BluePower.wiresGivePower} - without syncing, purplestone would emit full power back,
     * creating a no-decay feedback loop at the red/purple boundary.
     */
    @Inject(method = "calculateTargetStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"))
    private void bluestone$syncWiresGivePowerOff(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        BluePower.wiresGivePower = false;
    }

    @Inject(method = "calculateTargetStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"))
    private void bluestone$syncWiresGivePowerOn(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        BluePower.wiresGivePower = true;
    }

    /**
     * In {@code updateIndirectNeighbourShapes}, vanilla checks {@code state.is(this)} to decide whether
     * to propagate shape-update notifications to wires up/down in each horizontal direction. Redirect
     * these checks so bluestone/purplestone wires also receive the notification (cross-type UP connections).
     */
    @Redirect(method = "updateIndirectNeighbourShapes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean bluestone$isAnyWire(BlockState state, Block originalBlock) {
        return state.is(originalBlock)
                || state.is(ModBlocks.BLUESTONE_WIRE.get())
                || state.is(ModBlocks.PURPLESTONE_WIRE.get());
    }
}
