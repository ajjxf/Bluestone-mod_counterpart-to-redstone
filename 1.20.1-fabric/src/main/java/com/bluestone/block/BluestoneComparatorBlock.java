package com.bluestone.block;

import com.bluestone.power.BluePower;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * Bluestone comparator - reimplemented from {@link AbstractRedstoneGateBlock} (NOT ComparatorBlock,
 * to avoid the vanilla {@code ComparatorBlockEntity} output-storage dependency). The output level is
 * stored in the custom {@link #OUTPUT} block-state property; {@code getOutputLevel} returns it, and
 * {@link BluePower#weakTowards} reads OUTPUT toward the facing. Input reading is blue (via
 * {@link BluePower}); container/ComparatorOutput fullness is neutral (shared). Real emission is
 * suppressed (Option Y).
 */
@SuppressWarnings("deprecation")
public class BluestoneComparatorBlock extends AbstractRedstoneGateBlock {
    public static final IntProperty OUTPUT = IntProperty.of("output", 0, 15);

    public BluestoneComparatorBlock(AbstractBlock.Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(Properties.POWERED, false)
                .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
                .with(Properties.COMPARATOR_MODE, ComparatorMode.COMPARE)
                .with(OUTPUT, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.POWERED, Properties.HORIZONTAL_FACING, Properties.COMPARATOR_MODE, OUTPUT);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return false;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    protected int getOutputLevel(BlockView world, BlockPos pos, BlockState state) {
        return state.get(OUTPUT);
    }

    @Override
    protected int getPower(World world, BlockPos pos, BlockState state) {
        Direction direction = state.get(Properties.HORIZONTAL_FACING);
        BlockPos blockPos = pos.offset(direction);
        int i = BluePower.getEmittedBluePower(world, blockPos, direction);
        BlockState blockState = world.getBlockState(blockPos);
        if (blockState.hasComparatorOutput()) {
            return blockState.getComparatorOutput(world, blockPos);
        }
        return i;
    }

    @Override
    protected int getMaxInputLevelSides(RedstoneView world, BlockPos pos, BlockState state) {
        Direction direction = state.get(Properties.HORIZONTAL_FACING);
        Direction d2 = direction.rotateYClockwise();
        Direction d3 = direction.rotateYCounterclockwise();
        return Math.max(
                BluePower.getEmittedBluePower(world, pos.offset(d2), d2),
                BluePower.getEmittedBluePower(world, pos.offset(d3), d3));
    }

    @Override
    protected boolean getSideInputFromGatesOnly() {
        return true;
    }

    @Override
    protected int getUpdateDelayInternal(BlockState state) {
        return 2;
    }

    @Override
    protected boolean hasPower(World world, BlockPos pos, BlockState state) {
        int i = this.getPower(world, pos, state);
        if (i == 0) {
            return false;
        }
        int j = this.getMaxInputLevelSides(world, pos, state);
        if (i > j) {
            return true;
        }
        return i == j && state.get(Properties.COMPARATOR_MODE) == ComparatorMode.COMPARE;
    }

    private int calculateOutputSignal(World world, BlockPos pos, BlockState state) {
        int i = this.getPower(world, pos, state);
        if (i == 0) {
            return 0;
        }
        int j = this.getMaxInputLevelSides(world, pos, state);
        if (j > i) {
            return 0;
        }
        if (state.get(Properties.COMPARATOR_MODE) == ComparatorMode.SUBTRACT) {
            return i - j;
        }
        return i;
    }

    @Override
    protected void updatePowered(World world, BlockPos pos, BlockState state) {
        if (world.getBlockTickScheduler().isTicking(pos, this)) {
            return;
        }
        int i = this.calculateOutputSignal(world, pos, state);
        boolean powered = this.hasPower(world, pos, state);
        if (i != state.get(OUTPUT) || state.get(Properties.POWERED) != powered) {
            // Use the same TickPriority ladder as vanilla AbstractRedstoneGateBlock.updatePowered
            net.minecraft.world.tick.TickPriority tickPriority = net.minecraft.world.tick.TickPriority.HIGH;
            if (this.isTargetNotAligned(world, pos, state)) {
                tickPriority = net.minecraft.world.tick.TickPriority.EXTREMELY_HIGH;
            } else if (powered) {
                tickPriority = net.minecraft.world.tick.TickPriority.VERY_HIGH;
            }
            world.scheduleBlockTick(pos, this, 2, tickPriority);
        }
    }

    protected void updateTarget(World world, BlockPos pos, BlockState state) {
        Direction out = state.get(Properties.HORIZONTAL_FACING).getOpposite();
        world.updateNeighborsAlways(pos.offset(out), this);
        world.updateNeighborsAlways(pos, this);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int output = this.calculateOutputSignal(world, pos, state);
        boolean powered = this.hasPower(world, pos, state);
        if (output != state.get(OUTPUT) || powered != state.get(Properties.POWERED)) {
            world.setBlockState(pos, state.with(OUTPUT, output).with(Properties.POWERED, powered), Block.NOTIFY_ALL);
            this.updateTarget(world, pos, state);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock,
                               BlockPos sourcePos, boolean notify) {
        if (state.canPlaceAt(world, pos)) {
            this.updatePowered(world, pos, state);
        } else {
            dropStacks(state, world, pos);
            world.removeBlock(pos, false);
            for (Direction direction : Direction.values()) {
                world.updateNeighborsAlways(pos.offset(direction), this);
            }
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return AbstractRedstoneGateBlock.hasTopRim(world, pos.down());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!player.getAbilities().allowModifyWorld) {
            return ActionResult.PASS;
        }
        BlockState cycled = state.cycle(Properties.COMPARATOR_MODE);
        float pitch = cycled.get(Properties.COMPARATOR_MODE) == ComparatorMode.SUBTRACT ? 0.55f : 0.5f;
        world.playSound(player, pos, SoundEvents.BLOCK_COMPARATOR_CLICK, SoundCategory.BLOCKS, 0.3f, pitch);
        world.setBlockState(pos, cycled, Block.NOTIFY_ALL);
        this.updatePowered(world, pos, cycled);
        return ActionResult.success(world.isClient);
    }
}
