package com.bluestone.power;

import com.bluestone.block.BluestoneBlock;
import com.bluestone.block.BluestoneComparatorBlock;
import com.bluestone.block.BluestoneRepeaterBlock;
import com.bluestone.block.BluestoneTorchBlock;
import com.bluestone.block.BluestoneWallTorchBlock;
import com.bluestone.block.BluestoneWireBlock;
import com.bluestone.block.ConverterRepeaterBlock;
import com.bluestone.block.PurplestoneWireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;

/**
 * Blue power model - a faithful blue mirror of {@code net.minecraft.world.RedstoneView}.
 *
 * <p>Direction convention is EXACTLY vanilla's: every method takes {@code direction} = the
 * direction <b>from the receiver to the source</b>, and returns the power the source emits
 * <b>toward the receiver</b> (i.e. in {@code direction.getOpposite()}). This matches
 * {@code BlockState.getWeakRedstonePower}/{@code getStrongRedstonePower} and
 * {@code RedstoneView.getEmittedRedstonePower}/{@code getReceivedRedstonePower}.</p>
 *
 * <p>Blue components emit NO real redstone power (Option Y); redstone therefore ignores them for
 * free. Reactive blocks see blue through the {@code World.isReceivingRedstonePower} mixin which
 * OR-s in {@link #isReceivingBluePower}. Red-coloured components are invisible here (return 0);
 * neutral sources are shared (their real emission is reused).</p>
 */
public final class BluePower {
    private BluePower() {}

    /**
     * While a blue wire computes its own received power, this is false so that neighbouring blue
     * wires contribute 0 via {@link #weakTowards} (mirrors vanilla {@code RedstoneWireBlock.wiresGivePower});
     * wire-to-wire is instead handled by the wire reading neighbour POWER directly with the -1 step.
     */
    public static boolean wiresGivePower = true;

    /**
     * Faithful mirror of vanilla {@code RedstoneWireBlock.getWeakRedstonePower}: a blue/purple wire
     * only emits its POWER level toward directions where it actually has a connection (or toward UP
     * to power the block below it), and never toward DOWN. The {@code wiresGivePower} guard prevents
     * self-energising while the wire computes its own input.
     *
     * <p>{@code direction} follows the standard vanilla convention: from receiver to source.
     * The wire's connection in the transmission direction ({@code direction.getOpposite()}) is checked,
     * exactly as vanilla does.</p>
     */
    private static int wirePowerTowards(BluestoneWireBlock wire, BlockState state, Direction direction) {
        if (!wiresGivePower || direction == Direction.DOWN) return 0;
        int power = state.get(BluestoneWireBlock.POWER);
        if (power == 0) return 0;
        if (direction == Direction.UP) return power;
        // Horizontal: only emit if this wire has a connection toward the receiver.
        // The connection property is keyed by the direction FROM the wire (source) TO the receiver,
        // which is direction.getOpposite() since direction is receiver->source.
        return state.get(BluestoneWireBlock.DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction.getOpposite())).isConnected()
                ? power : 0;
    }

    // --- per-block weak/strong emission (direction = from receiver to source) ---

    public static int weakTowards(BlockState state, RedstoneView world, BlockPos pos, Direction direction) {
        if (ColorAttribution.isRedColored(state)) return 0;
        Block block = state.getBlock();
        if (block instanceof BluestoneWireBlock) {
            return wirePowerTowards((BluestoneWireBlock) block, state, direction);
        }
        if (block instanceof PurplestoneWireBlock) {
            return wirePowerTowards((BluestoneWireBlock) block, state, direction);
        }
        if (block instanceof BluestoneRepeaterBlock) {
            return (state.get(Properties.POWERED) && state.get(Properties.HORIZONTAL_FACING) == direction) ? 15 : 0;
        }
        if (block instanceof BluestoneComparatorBlock) {
            return (state.get(Properties.POWERED) && state.get(Properties.HORIZONTAL_FACING) == direction)
                    ? state.get(BluestoneComparatorBlock.OUTPUT) : 0;
        }
        if (block instanceof ConverterRepeaterBlock) {
            // Converter emits blue 15 toward facing when powered AND in RED_TO_BLUE mode.
            if (state.get(ConverterRepeaterBlock.CONVERTER_MODE) != ConverterRepeaterBlock.Mode.RED_TO_BLUE) return 0;
            return (state.get(Properties.POWERED) && state.get(Properties.HORIZONTAL_FACING) == direction) ? 15 : 0;
        }
        if (block instanceof BluestoneTorchBlock) {
            return (state.get(Properties.LIT) && direction != Direction.UP) ? 15 : 0;
        }
        if (block instanceof BluestoneWallTorchBlock) {
            return (state.get(Properties.LIT) && state.get(Properties.HORIZONTAL_FACING) != direction) ? 15 : 0;
        }
        if (block instanceof BluestoneBlock) {
            return 15;
        }
        if (ColorAttribution.isNeutralSource(state)) {
            return state.getWeakRedstonePower(world, pos, direction);
        }
        return 0;
    }

    public static int strongTowards(BlockState state, RedstoneView world, BlockPos pos, Direction direction) {
        if (ColorAttribution.isRedColored(state)) return 0;
        Block block = state.getBlock();
        if (block instanceof BluestoneWireBlock) {
            return wirePowerTowards((BluestoneWireBlock) block, state, direction);
        }
        if (block instanceof PurplestoneWireBlock) {
            return wirePowerTowards((BluestoneWireBlock) block, state, direction);
        }
        if (block instanceof BluestoneRepeaterBlock || block instanceof BluestoneComparatorBlock
                || (block instanceof ConverterRepeaterBlock
                        && state.get(ConverterRepeaterBlock.CONVERTER_MODE) == ConverterRepeaterBlock.Mode.RED_TO_BLUE)) {
            return weakTowards(state, world, pos, direction); // gates: strong == weak (vanilla)
        }
        if (block instanceof BluestoneTorchBlock) {
            return (state.get(Properties.LIT) && direction == Direction.DOWN) ? 15 : 0;
        }
        if (block instanceof BluestoneWallTorchBlock) {
            // inherits RedstoneTorchBlock.getStrongRedstonePower: only DOWN
            return (state.get(Properties.LIT) && direction == Direction.DOWN) ? 15 : 0;
        }
        if (block instanceof BluestoneBlock) {
            return 15;
        }
        if (ColorAttribution.isNeutralSource(state)) {
            return state.getStrongRedstonePower(world, pos, direction);
        }
        return 0;
    }

    // --- RedstoneView mirrors ---

    /** Mirror of {@code RedstoneView.getReceivedStrongRedstonePower}. */
    public static int getReceivedStrongBluePower(RedstoneView world, BlockPos pos) {
        int i = 0;
        int j;
        if ((j = strongTowards(world, pos.down(), Direction.DOWN)) > i) { i = j; }
        if (i >= 15) return i;
        if ((j = strongTowards(world, pos.up(), Direction.UP)) > i) { i = j; }
        if (i >= 15) return i;
        if ((j = strongTowards(world, pos.north(), Direction.NORTH)) > i) { i = j; }
        if (i >= 15) return i;
        if ((j = strongTowards(world, pos.south(), Direction.SOUTH)) > i) { i = j; }
        if (i >= 15) return i;
        if ((j = strongTowards(world, pos.west(), Direction.WEST)) > i) { i = j; }
        if (i >= 15) return i;
        if ((j = strongTowards(world, pos.east(), Direction.EAST)) > i) { i = j; }
        return i;
    }

    private static int strongTowards(RedstoneView world, BlockPos pos, Direction direction) {
        return strongTowards(world.getBlockState(pos), world, pos, direction);
    }

    /** Mirror of {@code RedstoneView.getEmittedRedstonePower(pos, direction)}. */
    public static int getEmittedBluePower(RedstoneView world, BlockPos pos, Direction direction) {
        BlockState state = world.getBlockState(pos);
        if (ColorAttribution.isRedColored(state)) return 0;
        int weak = weakTowards(state, world, pos, direction);
        if (state.isSolidBlock(world, pos)) {
            return Math.max(weak, getReceivedStrongBluePower(world, pos));
        }
        return weak;
    }

    /** Mirror of {@code RedstoneView.getReceivedRedstonePower}. */
    public static int getReceivedBluePower(RedstoneView world, BlockPos pos) {
        int i = 0;
        for (Direction direction : Direction.values()) {
            int j = getEmittedBluePower(world, pos.offset(direction), direction);
            if (j >= 15) return 15;
            if (j > i) i = j;
        }
        return i;
    }

    /** Mirror of {@code RedstoneView.isReceivingRedstonePower}. */
    public static boolean isReceivingBluePower(RedstoneView world, BlockPos pos) {
        return getEmittedBluePower(world, pos.down(), Direction.DOWN) > 0
                || getEmittedBluePower(world, pos.up(), Direction.UP) > 0
                || getEmittedBluePower(world, pos.north(), Direction.NORTH) > 0
                || getEmittedBluePower(world, pos.south(), Direction.SOUTH) > 0
                || getEmittedBluePower(world, pos.west(), Direction.WEST) > 0
                || getEmittedBluePower(world, pos.east(), Direction.EAST) > 0;
    }
}
