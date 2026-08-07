package com.bluestone.mixin;

import com.bluestone.power.BluePower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes vanilla reactive blocks (pistons via {@code hasSignal}, doors/lamps/etc. via
 * {@code hasNeighborSignal}) also see BLUE power. Blue components emit NO real redstone power
 * (Option Y), so without this they would not activate anything.
 *
 * <p><b>Approach</b>: {@code Level} implements {@link SignalGetter} but does not override the
 * {@code hasSignal}/{@code hasNeighborSignal} default methods. By declaring these methods on a
 * {@code Level} mixin (as plain methods, not {@code @Inject}), Mixin adds them to the {@code Level}
 * class. The JVM's interface method dispatch then resolves calls to
 * {@code SignalGetter.hasSignal}/{@code hasNeighborSignal} on any {@code Level} to our blue-aware
 * override, because a concrete class method shadows the interface default.</p>
 *
 * <p>This keeps {@code getSignal} (the numeric method) <b>pure</b>: vanilla redstone wire reads
 * {@code getSignal} for its power level and must NOT see blue, so we must not OR blue in there.
 * Only the boolean "is there any signal" checks (used by pistons, lamps, doors) get the blue OR-in.
 * This mirrors the original Fabric design that hooked {@code RedstoneView.isEmittingRedstonePower}
 * (boolean) rather than {@code getEmittedRedstonePower} (int).</p>
 *
 * <p>The redstone torch exemption (must respond to RED only) is handled by
 * {@code RedstoneTorchBlockMixin}, which redirects its {@code hasSignal} call to
 * {@code RedOnlyPower}.</p>
 */
@Mixin(Level.class)
public abstract class RedstoneViewMixin {

    /**
     * Override of {@code SignalGetter.hasSignal}: OR in blue power so reactive blocks that check
     * "is a signal emitted toward me in this direction" also see blue.
     *
     * <p>{@code hasSignal(pos,dir)} is defined as {@code getSignal(pos,dir) > 0} in the interface.
     * We do NOT override {@code getSignal} (so it stays pure for vanilla redstone wire numeric
     * reads), so calling it here does not recurse. We just inline {@code getSignal > 0} for the
     * red half, then OR in blue.</p>
     */
    public boolean hasSignal(BlockPos pos, Direction direction) {
        SignalGetter self = (SignalGetter) (Object) this;
        if (self.getSignal(pos, direction) > 0) {
            return true;
        }
        return BluePower.getEmittedBluePower(self, pos, direction) > 0;
    }

    /**
     * Override of {@code SignalGetter.hasNeighborSignal}: OR in blue power so reactive blocks that
     * check "am I receiving any signal from any neighbour" also see blue.
     *
     * <p>{@code hasNeighborSignal} scans all 6 directions via {@code getSignal}. We inline that scan
     * for the red half (no recursion since {@code getSignal} is not overridden), then OR in blue.</p>
     */
    public boolean hasNeighborSignal(BlockPos pos) {
        SignalGetter self = (SignalGetter) (Object) this;
        if (self.getSignal(pos.below(), Direction.DOWN) > 0
                || self.getSignal(pos.above(), Direction.UP) > 0
                || self.getSignal(pos.north(), Direction.NORTH) > 0
                || self.getSignal(pos.south(), Direction.SOUTH) > 0
                || self.getSignal(pos.west(), Direction.WEST) > 0
                || self.getSignal(pos.east(), Direction.EAST) > 0) {
            return true;
        }
        return BluePower.isReceivingBluePower(self, pos);
    }
}
