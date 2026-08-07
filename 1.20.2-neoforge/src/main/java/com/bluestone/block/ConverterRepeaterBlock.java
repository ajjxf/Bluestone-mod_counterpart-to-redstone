package com.bluestone.block;

import com.bluestone.power.BluePower;
import com.bluestone.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Unified red/blue converter repeater. Two modes toggled by right-click (like a comparator):
 * <ul>
 *   <li>{@link Mode#RED_TO_BLUE} (default on placement): reads <b>redstone</b> input,
 *       emits <b>blue</b> output. Suppresses real redstone emission.</li>
 *   <li>{@link Mode#BLUE_TO_RED}: reads <b>blue</b> input, emits <b>redstone</b> output
 *       via inherited vanilla emission.</li>
 * </ul>
 *
 * <p>No delay adjustment (DELAY fixed at 1), delay = 1 tick (2 game ticks).
 * Locked by a same-colour repeater on a perpendicular side.</p>
 */
@SuppressWarnings("deprecation")
public class ConverterRepeaterBlock extends RepeaterBlock {

    public enum Mode implements StringRepresentable {
        RED_TO_BLUE("red_to_blue"),
        BLUE_TO_RED("blue_to_red");

        private final String name;
        Mode(String name) { this.name = name; }
        @Override
        public String getSerializedName() { return this.name; }
    }

    public static final EnumProperty<Mode> CONVERTER_MODE = EnumProperty.create("converter_mode", Mode.class);

    private static final Vec3 PURPLE = new Vec3(0.35, 0.10, 0.55);

    public ConverterRepeaterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(CONVERTER_MODE, Mode.RED_TO_BLUE)
                .setValue(BlockStateProperties.LOCKED, false)
                .setValue(BlockStateProperties.POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONVERTER_MODE);
    }

    // --- toggle mode on right-click (like comparator) ---
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        }
        Mode newMode = state.getValue(CONVERTER_MODE) == Mode.RED_TO_BLUE ? Mode.BLUE_TO_RED : Mode.RED_TO_BLUE;
        float pitch = newMode == Mode.BLUE_TO_RED ? 0.55f : 0.5f;
        level.playSound(player, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3f, pitch);
        level.setBlock(pos, state.setValue(CONVERTER_MODE, newMode), Block.UPDATE_ALL);
        this.checkTickOnNeighbor(level, pos, state.setValue(CONVERTER_MODE, newMode));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // --- emission depends on mode ---
    @Override
    public boolean isSignalSource(BlockState state) {
        // RED_TO_BLUE: suppress (output via BluePower). BLUE_TO_RED: emit real redstone (vanilla).
        return state.getValue(CONVERTER_MODE) == Mode.BLUE_TO_RED;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (state.getValue(CONVERTER_MODE) != Mode.BLUE_TO_RED) return 0;
        return super.getDirectSignal(state, level, pos, direction);
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (state.getValue(CONVERTER_MODE) != Mode.BLUE_TO_RED) return 0;
        return super.getSignal(state, level, pos, direction);
    }

    // --- input reading depends on mode ---
    @Override
    protected int getInputSignal(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(CONVERTER_MODE) == Mode.RED_TO_BLUE) {
            // Read REDSTONE input (vanilla behaviour, inherited)
            return super.getInputSignal(level, pos, state);
        }
        // BLUE_TO_RED: read BLUE input
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos blockPos = pos.relative(direction);
        int i = BluePower.getEmittedBluePower(level, blockPos, direction);
        if (i >= 15) {
            return i;
        }
        BlockState blockState = level.getBlockState(blockPos);
        return Math.max(i, blockState.is(ModBlocks.BLUESTONE_WIRE.get()) ? blockState.getValue(BluestoneWireBlock.POWER)
                : blockState.is(ModBlocks.PURPLESTONE_WIRE.get()) ? blockState.getValue(BluestoneWireBlock.POWER) : 0);
    }


    /**
     * NeoForge hook ({@code IBlockExtension#canConnectRedstone}): vanilla redstone wire uses this
     * to decide its connection shape toward the converter. {@code direction} is the direction
     * <b>from the wire to the converter</b>. The converter only carries a REDSTONE signal on one
     * side, depending on mode:
     * <ul>
     *   <li>{@link Mode#RED_TO_BLUE}: red input on FACING side → wire sees converter at FACING.getOpposite()</li>
     *   <li>{@link Mode#BLUE_TO_RED}: red output on FACING.getOpposite() → wire sees converter at FACING</li>
     * </ul>
     * Without this override, vanilla redstone would connect to BOTH sides (inherited RepeaterBlock
     * behaviour), even when only one side actually carries redstone.
     */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @javax.annotation.Nullable Direction direction) {
        if (direction == null) return false;
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction redSide = state.getValue(CONVERTER_MODE) == Mode.RED_TO_BLUE ? facing.getOpposite() : facing;
        return redSide == direction;
    }

    // --- no delay cycling (DELAY fixed at 1) ---
    @Override
    protected int getDelay(BlockState state) {
        return 2;
    }

    // --- no locking (converter never locks) ---
    @Override
    public boolean isLocked(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }

    // --- purple particles ---
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BlockStateProperties.POWERED)) return;
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        double d = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        double e = pos.getY() + 0.4 + (random.nextDouble() - 0.5) * 0.2;
        double f = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        float g = -5.0f;
        if (random.nextBoolean()) {
            g = 3;
        }
        double h = (g /= 16.0f) * direction.getStepX();
        double i = g * direction.getStepZ();
        level.addParticle(new DustParticleOptions(PURPLE.toVector3f(), 1.0f),
                d + h, e, f + i, 0.0, 0.0, 0.0);
    }
}
