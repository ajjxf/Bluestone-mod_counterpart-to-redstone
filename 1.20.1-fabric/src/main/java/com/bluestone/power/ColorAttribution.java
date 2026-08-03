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
import net.minecraft.block.Blocks;

/**
 * Classifies a block state as red-coloured, blue-coloured, or neutral.
 *
 * <p>The "coloured" set (mutually exclusive between red and blue) is exactly:
 * redstone wire / repeater / comparator / redstone torch (+ wall torch) / redstone block,
 * plus their blue counterparts. Everything else that emits redstone power is a <b>neutral</b>
 * source shared by both systems (lever, button, pressure plate, observer, daylight detector,
 * target block, sculk sensor, calibrated sculk sensor, lectern, chiseled bookshelf, detector
 * rail, tripwire hook, weighted pressure plate, ...).</p>
 *
 * <p>Identification of neutral sources is <b>generic</b>: any block that
 * {@code emitsRedstonePower()} and is not a coloured component is treated as neutral, so sources
 * not explicitly listed are still covered automatically.</p>
 */
public final class ColorAttribution {
    private ColorAttribution() {}

    public static boolean isRedColored(BlockState state) {
        Block b = state.getBlock();
        return b == Blocks.REDSTONE_WIRE
                || b == Blocks.REPEATER
                || b == Blocks.COMPARATOR
                || b == Blocks.REDSTONE_TORCH
                || b == Blocks.REDSTONE_WALL_TORCH
                || b == Blocks.REDSTONE_BLOCK;
    }

    public static boolean isBlueColored(BlockState state) {
        Block b = state.getBlock();
        // PurplestoneWireBlock extends BluestoneWireBlock but is NOT blue-coloured (it's a bridge)
        if (b instanceof PurplestoneWireBlock) return false;
        return b instanceof BluestoneWireBlock
                || b instanceof BluestoneRepeaterBlock
                || b instanceof BluestoneComparatorBlock
                || b instanceof BluestoneTorchBlock
                || b instanceof BluestoneWallTorchBlock
                || b instanceof BluestoneBlock
                // Converter emits blue output only in RED_TO_BLUE mode
                || (b instanceof ConverterRepeaterBlock
                        && state.get(ConverterRepeaterBlock.CONVERTER_MODE) == ConverterRepeaterBlock.Mode.RED_TO_BLUE);
    }

    /** A coloured component belongs to exactly one system. Purplestone is a BRIDGE (not coloured). */
    public static boolean isColored(BlockState state) {
        return isRedColored(state) || isBlueColored(state);
    }

    /** Purplestone wire bridges red and blue - neither red-coloured nor blue-coloured. */
    public static boolean isPurpleColored(BlockState state) {
        return state.getBlock() instanceof PurplestoneWireBlock;
    }

    /** A converter repeater. Neither pure-coloured nor neutral. */
    public static boolean isConverter(BlockState state) {
        return state.getBlock() instanceof ConverterRepeaterBlock;
    }

    /**
     * A neutral source: emits redstone power and is neither red- nor blue-coloured NOR purple
     * (purple is a bridge wire, not a neutral source - it must NOT be read via the neutral-source
     * path in BluePower, which would bypass the wiresGivePower guard and cause self-energising).
     * Converter repeaters are also excluded: blue-to-red emits real redstone but must NOT leak
     * into the blue system via the neutral-source delegation path.
     */
    public static boolean isNeutralSource(BlockState state) {
        return state.emitsRedstonePower() && !isColored(state) && !isPurpleColored(state) && !isConverter(state);
    }
}
