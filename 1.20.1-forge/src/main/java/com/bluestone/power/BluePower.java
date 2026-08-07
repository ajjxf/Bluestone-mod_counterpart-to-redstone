package com.bluestone.power;

import com.bluestone.block.BluestoneBlock;
import com.bluestone.block.BluestoneComparatorBlock;
import com.bluestone.block.BluestoneRepeaterBlock;
import com.bluestone.block.BluestoneTorchBlock;
import com.bluestone.block.BluestoneWallTorchBlock;
import com.bluestone.block.BluestoneWireBlock;
import com.bluestone.block.ConverterRepeaterBlock;
import com.bluestone.block.PurplestoneWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Blue power model - a faithful blue mirror of {@code net.minecraft.world.level.SignalGetter}.
 *
 * <p>Direction convention is EXACTLY vanilla's: every method takes {@code direction} = the
 * direction <b>from the receiver to the source</b>, and returns the power the source emits
 * <b>toward the receiver</b> (i.e. in {@code direction.getOpposite()}). This matches
 * {@code BlockState.getSignal}/{@code getDirectSignal} and
 * {@code SignalGetter.getSignal}/{@code getBestNeighborSignal}.</p>
 *
 * <p>Blue components emit NO real redstone power (Option Y); redstone therefore ignores them for
 * free. Reactive blocks see blue through the {@code Level.hasNeighborSignal} mixin which
 * OR-s in {@link #isReceivingBluePower}. Red-coloured components are invisible here (return 0);
 * neutral sources are shared (their real emission is reused).</p>
 */
public final class BluePower {
    private BluePower() {}

    /**
     * While a blue wire computes its own received power, this is false so that neighbouring blue
     * wires contribute 0 via {@link #weakTowards} (mirrors vanilla {@code RedStoneWireBlock.shouldSignal});
     * wire-to-wire is instead handled by the wire reading neighbour POWER directly with the -1 step.
     */
    public static boolean wiresGivePower = true;

    /**
     * Faithful mirror of vanilla {@code RedStoneWireBlock.getWeakRedstonePower}: a blue/purple wire
     * only emits its POWER level toward directions where it actually has a connection (or toward UP
     * to power the block below it), and never toward DOWN. The {@code wiresGivePower} guard prevents
     * self-energising while the wire computes its own input.
     *
     * <p>Like vanilla, the connection is recomputed dynamically via
     * {@link BluestoneWireBlock#isConnectedTowards} rather than read from the stored block-state
     * property — so a gate placed beside an already-powered wire sees the signal at once, even
     * before the wire's stored connection property has been refreshed.</p>
     *
     * <p>{@code direction} follows the standard vanilla convention: from receiver to source.</p>
     */
    private static int wirePowerTowards(BluestoneWireBlock wire, BlockState state, SignalGetter level, BlockPos pos, Direction direction) {
        if (!wiresGivePower || direction == Direction.DOWN) return 0;
        int power = state.getValue(BluestoneWireBlock.POWER);
        if (power == 0) return 0;
        if (direction == Direction.UP) return power;
        // Horizontal: dynamically re-check the connection toward the receiver (vanilla behaviour).
        return wire.isConnectedTowards(level, pos, direction) ? power : 0;
    }

    // --- per-block weak/strong emission (direction = from receiver to source) ---

    public static int weakTowards(BlockState state, SignalGetter level, BlockPos pos, Direction direction) {
        if (ColorAttribution.isRedColored(state)) return 0;
        net.minecraft.world.level.block.Block block = state.getBlock();
        if (block instanceof BluestoneWireBlock) {
            return wirePowerTowards((BluestoneWireBlock) block, state, level, pos, direction);
        }
        if (block instanceof PurplestoneWireBlock) {
            return wirePowerTowards((BluestoneWireBlock) block, state, level, pos, direction);
        }
        if (block instanceof BluestoneRepeaterBlock) {
            return (state.getValue(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == direction) ? 15 : 0;
        }
        if (block instanceof BluestoneComparatorBlock) {
            return (state.getValue(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == direction)
                    ? state.getValue(BluestoneComparatorBlock.OUTPUT) : 0;
        }
        if (block instanceof ConverterRepeaterBlock) {
            // Converter emits blue 15 toward facing when powered AND in RED_TO_BLUE mode.
            if (state.getValue(ConverterRepeaterBlock.CONVERTER_MODE) != ConverterRepeaterBlock.Mode.RED_TO_BLUE) return 0;
            return (state.getValue(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == direction) ? 15 : 0;
        }
        if (block instanceof BluestoneTorchBlock) {
            return (state.getValue(BlockStateProperties.LIT) && direction != Direction.UP) ? 15 : 0;
        }
        if (block instanceof BluestoneWallTorchBlock) {
            return (state.getValue(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.HORIZONTAL_FACING) != direction) ? 15 : 0;
        }
        if (block instanceof BluestoneBlock) {
            return 15;
        }
        if (ColorAttribution.isNeutralSource(state)) {
            return state.getSignal(level, pos, direction);
        }
        return 0;
    }

    public static int strongTowards(BlockState state, SignalGetter level, BlockPos pos, Direction direction) {
        if (ColorAttribution.isRedColored(state)) return 0;
        net.minecraft.world.level.block.Block block = state.getBlock();
        if (block instanceof BluestoneWireBlock) {
            return wirePowerTowards((BluestoneWireBlock) block, state, level, pos, direction);
        }
        if (block instanceof PurplestoneWireBlock) {
            return wirePowerTowards((BluestoneWireBlock) block, state, level, pos, direction);
        }
        if (block instanceof BluestoneRepeaterBlock || block instanceof BluestoneComparatorBlock
                || (block instanceof ConverterRepeaterBlock
                        && state.getValue(ConverterRepeaterBlock.CONVERTER_MODE) == ConverterRepeaterBlock.Mode.RED_TO_BLUE)) {
            return weakTowards(state, level, pos, direction); // gates: strong == weak (vanilla)
        }
        if (block instanceof BluestoneTorchBlock) {
            return (state.getValue(BlockStateProperties.LIT) && direction == Direction.DOWN) ? 15 : 0;
        }
        if (block instanceof BluestoneWallTorchBlock) {
            // inherits RedstoneTorchBlock.getDirectSignal: only DOWN
            return (state.getValue(BlockStateProperties.LIT) && direction == Direction.DOWN) ? 15 : 0;
        }
        if (block instanceof BluestoneBlock) {
            return 15;
        }
        if (ColorAttribution.isNeutralSource(state)) {
            return state.getDirectSignal(level, pos, direction);
        }
        return 0;
    }

    // --- SignalGetter mirrors ---

    /** Mirror of {@code SignalGetter.getDirectSignalTo}. */
    public static int getReceivedStrongBluePower(SignalGetter level, BlockPos pos) {
        int i = 0;
        int j;
        if ((j = strongTowards(level, pos.below(), Direction.DOWN)) > i) { i = j; }
        if (i >= 15) return i;
        if ((j = strongTowards(level, pos.above(), Direction.UP)) > i) { i = j; }
        if (i >= 15) return i;
        if ((j = strongTowards(level, pos.north(), Direction.NORTH)) > i) { i = j; }
        if (i >= 15) return i;
        if ((j = strongTowards(level, pos.south(), Direction.SOUTH)) > i) { i = j; }
        if (i >= 15) return i;
        if ((j = strongTowards(level, pos.west(), Direction.WEST)) > i) { i = j; }
        if (i >= 15) return i;
        if ((j = strongTowards(level, pos.east(), Direction.EAST)) > i) { i = j; }
        return i;
    }

    private static int strongTowards(SignalGetter level, BlockPos pos, Direction direction) {
        return strongTowards(level.getBlockState(pos), level, pos, direction);
    }

    /** Mirror of {@code SignalGetter.getSignal(pos, direction)}. */
    public static int getEmittedBluePower(SignalGetter level, BlockPos pos, Direction direction) {
        BlockState state = level.getBlockState(pos);
        if (ColorAttribution.isRedColored(state)) return 0;
        int weak = weakTowards(state, level, pos, direction);
        if (state.isSolidRender(level, pos)) {
            return Math.max(weak, getReceivedStrongBluePower(level, pos));
        }
        return weak;
    }

    /** Mirror of {@code SignalGetter.getBestNeighborSignal}. */
    public static int getReceivedBluePower(SignalGetter level, BlockPos pos) {
        int i = 0;
        for (Direction direction : Direction.values()) {
            int j = getEmittedBluePower(level, pos.relative(direction), direction);
            if (j >= 15) return 15;
            if (j > i) i = j;
        }
        return i;
    }

    /** Mirror of {@code SignalGetter.hasNeighborSignal}. */
    public static boolean isReceivingBluePower(SignalGetter level, BlockPos pos) {
        return getEmittedBluePower(level, pos.below(), Direction.DOWN) > 0
                || getEmittedBluePower(level, pos.above(), Direction.UP) > 0
                || getEmittedBluePower(level, pos.north(), Direction.NORTH) > 0
                || getEmittedBluePower(level, pos.south(), Direction.SOUTH) > 0
                || getEmittedBluePower(level, pos.west(), Direction.WEST) > 0
                || getEmittedBluePower(level, pos.east(), Direction.EAST) > 0;
    }
}
