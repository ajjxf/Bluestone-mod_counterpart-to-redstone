package com.bluestone.block;

import com.bluestone.power.BluePower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.TickPriority;

/**
 * Bluestone comparator - reimplemented from {@link DiodeBlock} (NOT ComparatorBlock,
 * to avoid the vanilla {@code ComparatorBlockEntity} output-storage dependency). The output level is
 * stored in the custom {@link #OUTPUT} block-state property; {@code getOutputSignal} returns it, and
 * {@link BluePower#weakTowards} reads OUTPUT toward the facing. Input reading is blue (via
 * {@link BluePower}); container/ComparatorOutput fullness is neutral (shared). Real emission is
 * suppressed (Option Y).
 */
@SuppressWarnings("deprecation")
public class BluestoneComparatorBlock extends DiodeBlock {
    public static final IntegerProperty OUTPUT = IntegerProperty.create("output", 0, 15);

    public BluestoneComparatorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.POWERED, false)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.MODE_COMPARATOR, ComparatorMode.COMPARE)
                .setValue(OUTPUT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.POWERED, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.MODE_COMPARATOR, OUTPUT);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    protected int getOutputSignal(BlockGetter level, BlockPos pos, BlockState state) {
        return state.getValue(OUTPUT);
    }

    @Override
    protected int getInputSignal(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos blockPos = pos.relative(direction);
        int i = BluePower.getEmittedBluePower(level, blockPos, direction);
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.hasAnalogOutputSignal()) {
            return blockState.getAnalogOutputSignal(level, blockPos);
        }
        return i;
    }

    @Override
    protected int getAlternateSignal(SignalGetter level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction d2 = direction.getClockWise();
        Direction d3 = direction.getCounterClockWise();
        return Math.max(
                BluePower.getEmittedBluePower(level, pos.relative(d2), d2),
                BluePower.getEmittedBluePower(level, pos.relative(d3), d3));
    }

    @Override
    protected boolean sideInputDiodesOnly() {
        return true;
    }

    @Override
    protected int getDelay(BlockState state) {
        return 2;
    }

    @Override
    protected boolean shouldTurnOn(Level level, BlockPos pos, BlockState state) {
        int i = this.getInputSignal(level, pos, state);
        if (i == 0) {
            return false;
        }
        int j = this.getAlternateSignal(level, pos, state);
        if (i > j) {
            return true;
        }
        return i == j && state.getValue(BlockStateProperties.MODE_COMPARATOR) == ComparatorMode.COMPARE;
    }

    private int calculateOutputSignal(Level level, BlockPos pos, BlockState state) {
        int i = this.getInputSignal(level, pos, state);
        if (i == 0) {
            return 0;
        }
        int j = this.getAlternateSignal(level, pos, state);
        if (j > i) {
            return 0;
        }
        if (state.getValue(BlockStateProperties.MODE_COMPARATOR) == ComparatorMode.SUBTRACT) {
            return i - j;
        }
        return i;
    }

    @Override
    protected void checkTickOnNeighbor(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockTicks().willTickThisTick(pos, this)) {
            return;
        }
        int i = this.calculateOutputSignal(level, pos, state);
        boolean powered = this.shouldTurnOn(level, pos, state);
        if (i != state.getValue(OUTPUT) || state.getValue(BlockStateProperties.POWERED) != powered) {
            // Match vanilla ComparatorBlock.updatePowered tick priority:
            //   HIGH when the output target (the block at FACING.getOpposite()) is a diode that is
            //   NOT facing back toward this comparator (i.e. "not aligned"), NORMAL otherwise.
            Direction outDir = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
            BlockState targetState = level.getBlockState(pos.relative(outDir));
            boolean targetNotAligned = DiodeBlock.isDiode(targetState)
                    && targetState.getValue(BlockStateProperties.HORIZONTAL_FACING) != outDir;
            TickPriority tickPriority = targetNotAligned ? TickPriority.HIGH : TickPriority.NORMAL;
            level.scheduleTick(pos, this, this.getDelay(state), tickPriority);
        }
    }

    /**
     * On placement, also re-evaluate the output: unlike a vanilla diode (which only schedules a
     * tick when the front input is live), a comparator's output depends on BOTH front and side
     * inputs. A side wire may already be powered when the comparator is placed, so we must check
     * {@link #checkTickOnNeighbor} here too — otherwise the comparator stays stale until some
     * unrelated block update nudges it.
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving); // notify output neighbour (vanilla updateTarget)
        if (!oldState.is(this) && !level.isClientSide) {
            this.checkTickOnNeighbor(level, pos, state);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int output = this.calculateOutputSignal(level, pos, state);
        boolean powered = this.shouldTurnOn(level, pos, state);
        if (output != state.getValue(OUTPUT) || powered != state.getValue(BlockStateProperties.POWERED)) {
            level.setBlock(pos, state.setValue(OUTPUT, output).setValue(BlockStateProperties.POWERED, powered), Block.UPDATE_ALL);
            // Notify the block on the output side (FACING.getOpposite()).
            Direction out = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
            level.updateNeighborsAt(pos.relative(out), this);
            level.updateNeighborsAt(pos, this);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block sourceBlock,
                                BlockPos sourcePos, boolean isMoving) {
        if (state.canSurvive(level, pos)) {
            this.checkTickOnNeighbor(level, pos, state);
        } else {
            Block.dropResources(state, level, pos);
            level.removeBlock(pos, false);
            for (Direction direction : Direction.values()) {
                level.updateNeighborsAt(pos.relative(direction), this);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        }
        BlockState cycled = state.cycle(BlockStateProperties.MODE_COMPARATOR);
        float pitch = cycled.getValue(BlockStateProperties.MODE_COMPARATOR) == ComparatorMode.SUBTRACT ? 0.55f : 0.5f;
        level.playSound(player, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3f, pitch);
        level.setBlock(pos, cycled, Block.UPDATE_ALL);
        this.checkTickOnNeighbor(level, pos, cycled);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
