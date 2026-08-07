package com.bluestone.mixin;

import com.bluestone.block.BluestoneComparatorBlock;
import com.bluestone.block.BluestoneRepeaterBlock;
import com.bluestone.block.ConverterRepeaterBlock;
import com.bluestone.block.ObserverProps;
import com.bluestone.power.ColorAttribution;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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

    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void bluestone$addObserved(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(ObserverProps.OBSERVED);
    }

    @Inject(method = "updateShape", at = @At("HEAD"))
    private void bluestone$trackOutputSide(BlockState state, Direction direction, BlockState neighborState,
                                           LevelAccessor level, BlockPos pos, BlockPos neighborPos,
                                           CallbackInfoReturnable<BlockState> cir) {
        if (level.isClientSide()) {
            return;
        }
        Direction outputSide = state.getValue(BlockStateProperties.FACING).getOpposite();
        if (direction != outputSide) {
            return;
        }
        int observed = outputSideIsBlue(neighborState, outputSide) ? 1 : 0;
        if (state.getValue(ObserverProps.OBSERVED) != observed) {
            ((Level) level).setBlock(pos, state.setValue(ObserverProps.OBSERVED, observed), 2);
        }
    }

    @Inject(method = "onPlace", at = @At("HEAD"))
    private void bluestone$initObserved(BlockState state, Level level, BlockPos pos, BlockState oldState,
                                        boolean isMoving, CallbackInfo ci) {
        if (level.isClientSide()) {
            return;
        }
        Direction outputSide = state.getValue(BlockStateProperties.FACING).getOpposite();
        BlockState outState = level.getBlockState(pos.relative(outputSide));
        int observed = outputSideIsBlue(outState, outputSide) ? 1 : 0;
        if (state.getValue(ObserverProps.OBSERVED) != observed) {
            level.setBlock(pos, state.setValue(ObserverProps.OBSERVED, observed), 2);
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
     * (that's gate&rarr;observer, not observer&rarr;gate).</p>
     */
    private static boolean outputSideIsBlue(BlockState state, Direction outputSide) {
        Block block = state.getBlock();
        // Gates: only blue when their INPUT (FACING) faces the observer's output
        if (block instanceof BluestoneRepeaterBlock || block instanceof BluestoneComparatorBlock) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING) == outputSide.getOpposite();
        }
        if (block instanceof ConverterRepeaterBlock) {
            // Converter input must face the observer; BLUE_TO_RED reads blue → mod texture
            if (state.getValue(BlockStateProperties.HORIZONTAL_FACING) != outputSide.getOpposite()) return false;
            return state.getValue(ConverterRepeaterBlock.CONVERTER_MODE) == ConverterRepeaterBlock.Mode.BLUE_TO_RED;
        }
        // Non-gate blue components (wire, torch, block): always blue
        return ColorAttribution.isBlueColored(state);
    }
}
