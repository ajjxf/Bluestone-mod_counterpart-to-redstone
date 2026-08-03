package com.bluestone.block;

import com.bluestone.power.BluePower;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

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

    public enum Mode implements StringIdentifiable {
        RED_TO_BLUE("red_to_blue"),
        BLUE_TO_RED("blue_to_red");

        private final String name;
        Mode(String name) { this.name = name; }
        @Override
        public String asString() { return this.name; }
    }

    public static final EnumProperty<Mode> CONVERTER_MODE = EnumProperty.of("converter_mode", Mode.class);

    private static final Vec3d PURPLE = new Vec3d(0.35, 0.10, 0.55);

    public ConverterRepeaterBlock(AbstractBlock.Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(CONVERTER_MODE, Mode.RED_TO_BLUE)
                .with(Properties.LOCKED, false)
                .with(Properties.POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(CONVERTER_MODE);
    }

    // --- toggle mode on right-click (like comparator) ---
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!player.getAbilities().allowModifyWorld) {
            return ActionResult.PASS;
        }
        Mode newMode = state.get(CONVERTER_MODE) == Mode.RED_TO_BLUE ? Mode.BLUE_TO_RED : Mode.RED_TO_BLUE;
        float pitch = newMode == Mode.BLUE_TO_RED ? 0.55f : 0.5f;
        world.playSound(player, pos, SoundEvents.BLOCK_COMPARATOR_CLICK, SoundCategory.BLOCKS, 0.3f, pitch);
        world.setBlockState(pos, state.with(CONVERTER_MODE, newMode), Block.NOTIFY_ALL);
        this.updatePowered(world, pos, state.with(CONVERTER_MODE, newMode));
        return ActionResult.success(world.isClient);
    }

    // --- emission depends on mode ---
    @Override
    public boolean emitsRedstonePower(BlockState state) {
        // RED_TO_BLUE: suppress (output via BluePower). BLUE_TO_RED: emit real redstone (vanilla).
        return state.get(CONVERTER_MODE) == Mode.BLUE_TO_RED;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        if (state.get(CONVERTER_MODE) != Mode.BLUE_TO_RED) return 0;
        return super.getStrongRedstonePower(state, world, pos, direction);
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        if (state.get(CONVERTER_MODE) != Mode.BLUE_TO_RED) return 0;
        return super.getWeakRedstonePower(state, world, pos, direction);
    }

    // --- input reading depends on mode ---
    @Override
    protected int getPower(World world, BlockPos pos, BlockState state) {
        if (state.get(CONVERTER_MODE) == Mode.RED_TO_BLUE) {
            // Read REDSTONE input (vanilla behaviour, inherited)
            return super.getPower(world, pos, state);
        }
        // BLUE_TO_RED: read BLUE input
        Direction direction = state.get(Properties.HORIZONTAL_FACING);
        BlockPos blockPos = pos.offset(direction);
        int i = BluePower.getEmittedBluePower(world, blockPos, direction);
        if (i >= 15) {
            return i;
        }
        BlockState blockState = world.getBlockState(blockPos);
        return Math.max(i, blockState.isOf(Blocks.REDSTONE_WIRE) ? blockState.get(RedstoneWireBlock.POWER) : 0);
    }

    // --- no delay cycling (DELAY fixed at 1) ---
    @Override
    protected int getUpdateDelayInternal(BlockState state) {
        return 2;
    }

    // --- no locking (converter never locks) ---
    @Override
    public boolean isLocked(WorldView world, BlockPos pos, BlockState state) {
        return false;
    }

    // --- purple particles ---
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(Properties.POWERED)) return;
        Direction direction = state.get(Properties.HORIZONTAL_FACING);
        double d = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        double e = pos.getY() + 0.4 + (random.nextDouble() - 0.5) * 0.2;
        double f = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        float g = -5.0f;
        if (random.nextBoolean()) {
            g = 3;
        }
        double h = (g /= 16.0f) * direction.getOffsetX();
        double i = g * direction.getOffsetZ();
        world.addParticle(new DustParticleEffect(PURPLE.toVector3f(), 1.0f),
                d + h, e, f + i, 0.0, 0.0, 0.0);
    }
}
