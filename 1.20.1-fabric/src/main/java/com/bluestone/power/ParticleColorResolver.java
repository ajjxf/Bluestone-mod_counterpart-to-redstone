package com.bluestone.power;

import com.bluestone.block.ConverterRepeaterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.joml.Vector3f;

/**
 * Shared logic for tinting activation particles (lever, sculk sensor) based on surrounding
 * signal types.
 *
 * <p><b>Direction convention</b>: for a gate facing north, input = FACING (north),
 * output = FACING.getOpposite() (south). A block's input "points toward" the source block
 * (lever/sensor) when FACING == direction-from-neighbour-to-source.</p>
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>Only blue inputs (genuine blue components + BLUE_TO_RED converter input facing source):
 *       → blue (RGB 0.15, 0.4, 1.0).</li>
 *   <li>Blue + other (redstone dust/repeater/comparator, powered/activator rails, purplestone,
 *       RED_TO_BLUE converter input facing source) mixed: → purple (RGB 0.55, 0.15, 0.8).</li>
 *   <li>No blue inputs → vanilla red.</li>
 * </ul>
 * Neutral sources (observer, button, pressure plate, etc.) are ignored.
 */
public final class ParticleColorResolver {
    private ParticleColorResolver() {}

    public static final Vector3f RED = DustParticleEffect.RED;
    public static final Vector3f BLUE = new Vec3d(0.15, 0.4, 1.0).toVector3f();
    public static final Vector3f PURPLE = new Vec3d(0.55, 0.15, 0.8).toVector3f();

    public static Vector3f resolveColor(BlockView world, BlockPos pos) {
        boolean hasBlue = false;
        boolean hasOther = false;
        for (Direction dir : Direction.values()) {
            BlockPos nPos = pos.offset(dir);
            BlockState nState = world.getBlockState(nPos);
            // towardSource = direction from neighbour back to the source block (lever/sensor)
            Direction towardSource = dir.getOpposite();
            // For gates: input = FACING. Input "points toward source" when FACING == towardSource.
            if (!inputFacesToward(nState, towardSource)) continue;
            if (isBlueInput(nState, towardSource)) {
                hasBlue = true;
            } else if (isOtherInput(nState, towardSource)) {
                hasOther = true;
            }
        }
        if (hasBlue && hasOther) return PURPLE;
        if (hasBlue) return BLUE;
        return RED;
    }

    /**
     * Check if the neighbour's input side faces toward the source block.
     * For gates: input = FACING, so FACING must equal towardSource.
     * For non-gate blocks (wire, torch, etc.): always true (they accept signals from any side).
     */
    private static boolean inputFacesToward(BlockState state, Direction towardSource) {
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            return state.get(Properties.HORIZONTAL_FACING) == towardSource;
        }
        return true;
    }

    private static boolean isBlueInput(BlockState state, Direction towardSource) {
        // Converter in BLUE_TO_RED mode: reads blue on input (FACING). FACING == towardSource checked above.
        if (state.getBlock() instanceof ConverterRepeaterBlock) {
            return state.get(ConverterRepeaterBlock.CONVERTER_MODE) == ConverterRepeaterBlock.Mode.BLUE_TO_RED;
        }
        return ColorAttribution.isBlueColored(state);
    }

    private static boolean isOtherInput(BlockState state, Direction towardSource) {
        Block b = state.getBlock();
        // Converter in RED_TO_BLUE mode: reads red on input
        if (b instanceof ConverterRepeaterBlock) {
            return state.get(ConverterRepeaterBlock.CONVERTER_MODE) == ConverterRepeaterBlock.Mode.RED_TO_BLUE;
        }
        return b == Blocks.REDSTONE_WIRE
                || b == Blocks.REPEATER
                || b == Blocks.COMPARATOR
                || b == Blocks.POWERED_RAIL
                || b == Blocks.ACTIVATOR_RAIL
                || ColorAttribution.isPurpleColored(state);
    }
}
