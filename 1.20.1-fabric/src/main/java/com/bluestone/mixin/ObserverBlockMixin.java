package com.bluestone.mixin;

import com.bluestone.block.BluestoneComparatorBlock;
import com.bluestone.block.BluestoneRepeaterBlock;
import com.bluestone.block.ConverterRepeaterBlock;
import com.bluestone.block.ObserverProps;
import com.bluestone.power.ColorAttribution;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ObserverBlock;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Observer texture state: the output-side (back) uses bluestone-styled textures
 * ({@code observed=1}) when the block on the output side is a genuine blue component
 * or a BlueToRed converter's input; otherwise vanilla textures ({@code observed=0}).
 *
 * <p>The observer is a neutral source - its signal is shared by both redstone and bluestone
 * systems. The texture choice is purely cosmetic, based only on what the output side connects to.</p>
 */
@Mixin(ObserverBlock.class)
public class ObserverBlockMixin {

    @Inject(method = "appendProperties", at = @At("TAIL"))
    private void bluestone$addObserved(StateManager.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(ObserverProps.OBSERVED);
    }

    @Inject(method = "getStateForNeighborUpdate", at = @At("HEAD"))
    private void bluestone$trackOutputSide(BlockState state, Direction direction, BlockState neighborState,
                                           WorldAccess world, BlockPos pos, BlockPos neighborPos,
                                           CallbackInfoReturnable<BlockState> cir) {
        if (world.isClient()) {
            return;
        }
        Direction outputSide = state.get(Properties.FACING).getOpposite();
        if (direction != outputSide) {
            return;
        }
        int observed = outputSideIsBlue(neighborState, outputSide) ? 1 : 0;
        if (state.get(ObserverProps.OBSERVED) != observed) {
            ((World) world).setBlockState(pos, state.with(ObserverProps.OBSERVED, observed), 2);
        }
    }

    @Inject(method = "onBlockAdded", at = @At("HEAD"))
    private void bluestone$initObserved(BlockState state, World world, BlockPos pos, BlockState oldState,
                                        boolean notify, CallbackInfo ci) {
        if (world.isClient()) {
            return;
        }
        Direction outputSide = state.get(Properties.FACING).getOpposite();
        BlockState outState = world.getBlockState(pos.offset(outputSide));
        int observed = outputSideIsBlue(outState, outputSide) ? 1 : 0;
        if (state.get(ObserverProps.OBSERVED) != observed) {
            world.setBlockState(pos, state.with(ObserverProps.OBSERVED, observed), 2);
        }
    }

    /**
     * A block on the output side is "blue" for texture purposes if it receives the observer's
     * output signal on its INPUT side (FACING faces toward the observer's output = FACING == outputSide.getOpposite()).
     *
     * <p>Definition: for a gate facing north, input = FACING (north), output = FACING.getOpposite() (south).
     * The observer feeds the gate's input when gate.FACING points toward the observer.</p>
     *
     * <p>Blue gates (repeater/comparator/converter) whose OUTPUT faces the observer do NOT count
     * (that's gate→observer, not observer→gate).</p>
     */
    private static boolean outputSideIsBlue(BlockState state, Direction outputSide) {
        Block block = state.getBlock();
        // Gates: only blue when their INPUT (FACING) faces the observer's output
        if (block instanceof BluestoneRepeaterBlock || block instanceof BluestoneComparatorBlock) {
            return state.get(Properties.HORIZONTAL_FACING) == outputSide.getOpposite();
        }
        if (block instanceof ConverterRepeaterBlock) {
            // Converter input must face the observer; BLUE_TO_RED reads blue → mod texture
            if (state.get(Properties.HORIZONTAL_FACING) != outputSide.getOpposite()) return false;
            return state.get(ConverterRepeaterBlock.CONVERTER_MODE) == ConverterRepeaterBlock.Mode.BLUE_TO_RED;
        }
        // Non-gate blue components (wire, torch, block): always blue
        return ColorAttribution.isBlueColored(state);
    }
}
