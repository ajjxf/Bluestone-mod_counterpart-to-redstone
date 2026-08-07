package com.bluestone.block;

import com.bluestone.mixin.RedstoneWireBlockAccessor;
import com.bluestone.power.BluePower;
import com.bluestone.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.Vec3;

/**
 * Purplestone wire - a BRIDGE between redstone and bluestone. Extends {@link BluestoneWireBlock}
 * (so blue wire logic, shape rendering, blue power propagation all work), but ALSO:
 * <ul>
 *   <li>Emits REAL redstone power ({@code isSignalSource=true}, weak/strong return POWER),
 *       so vanilla redstone wire connects to it and redstone systems read its signal.</li>
 *   <li>{@code shouldConnectTo} overridden to ALSO accept red-coloured components (redstone wire,
 *       repeater, comparator, torch, block) - the bridge connection.</li>
 *   <li>{@code getReceivedBluePower} overridden to ALSO read redstone power (vanilla), not just
 *       blue power - so a redstone signal next to purplestone energises it.</li>
 * </ul>
 * The {@link com.bluestone.mixin.RedstoneWireBlockMixin} makes vanilla red wire treat purple as a
 * same-color wire (shouldConnectTo + getWireSignal), completing the red-side bridge.
 */
public class PurplestoneWireBlock extends BluestoneWireBlock {

    public PurplestoneWireBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    // --- emit REAL redstone power, exactly mirroring vanilla RedStoneWireBlock ---
    // wiresGivePower: set false while computing own received power (prevents self-energising);
    // wire-to-wire uses getWireSignal (reads POWER directly) which bypasses this flag.
    private boolean wiresGivePower = true;

    @Override
    public boolean isSignalSource(BlockState state) {
        return this.wiresGivePower;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Check BOTH our own instance flag AND the shared BluePower flag: when ANY wire (blue, purple,
        // or vanilla redstone via the global toggle below) is computing its own received power, all
        // wires must stay silent to prevent cross-instance feedback loops at mixed-wire crossings.
        if (!this.wiresGivePower || !BluePower.wiresGivePower || direction == Direction.DOWN) {
            return 0;
        }
        int i = state.getValue(BlockStateProperties.POWER);
        if (i == 0) {
            return 0;
        }
        // Only emit toward UP, or toward horizontal directions where this wire has a connection.
        // Like vanilla RedStoneWireBlock.getWeakRedstonePower, recompute the connection DYNAMICALLY
        // (not from the stored property) so a gate placed beside an already-powered purplestone
        // wire reads the signal immediately, before updateShape refreshes the stored property.
        if (direction == Direction.UP || this.isConnectedTowards(level, pos, direction)) {
            return i;
        }
        return 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!this.wiresGivePower || !BluePower.wiresGivePower) {
            return 0;
        }
        return state.getSignal(level, pos, direction);
    }

    // --- getWireSignal: read POWER from BOTH blue wire and purple wire (the bridge) ---
    @Override
    protected int getWireSignal(BlockState state) {
        if (state.is(this)) return state.getValue(BlockStateProperties.POWER);
        if (state.is(ModBlocks.BLUESTONE_WIRE.get())) return state.getValue(BlockStateProperties.POWER);
        if (state.is(Blocks.REDSTONE_WIRE)) return state.getValue(BlockStateProperties.POWER);
        return 0;
    }

    // --- shouldConnectTo: blue base + ALSO accept red-coloured components (the bridge) ---
    @Override
    protected boolean shouldConnectTo(BlockState state, Direction dir) {
        if (state.is(Blocks.REDSTONE_WIRE)) return true;
        // Vanilla repeater: connect only on input/output sides
        if (state.is(Blocks.REPEATER)) {
            Direction d = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return d == dir || d.getOpposite() == dir;
        }
        // Vanilla comparator: connect from all sides
        if (state.is(Blocks.COMPARATOR)) return dir != null;
        // Converter: purplestone carries BOTH red and blue, so it connects on BOTH the red side
        // and the blue side of the converter (i.e. both input and output, gate-facing rule).
        if (state.getBlock() instanceof ConverterRepeaterBlock) {
            Direction d = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return d == dir || d.getOpposite() == dir;
        }
        if (state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH)
                || state.is(Blocks.REDSTONE_BLOCK)) {
            return dir != null;
        }
        return super.shouldConnectTo(state, dir);
    }

    // --- power: blue system (inherited) + ALSO read vanilla redstone power ---
    // BluePower filters out red-coloured components (returns 0 for redstone wire/torch/etc),
    // so purplestone must ALSO check the vanilla redstone level and take the max.
    @Override
    protected int getReceivedBluePower(Level level, BlockPos pos) {
        // Suppress ALL wire emission while computing our own input (prevents self-energising):
        //  - this.wiresGivePower: our own instance flag (suppresses our redstone emission)
        //  - BluePower.wiresGivePower: shared flag (suppresses blue wire emission via weakTowards,
        //    AND suppresses other purplestone instances' redstone emission, since they check it too)
        //  - vanilla REDSTONE_WIRE.shouldSignal: suppresses redstone wire emission (different instance,
        //    not covered by the shared flag) - toggled via the accessor mixin.
        // Wire-to-wire contribution (with -1 decay) is added manually below for all three wire types.
        this.wiresGivePower = false;
        RedstoneWireBlockAccessor acc = (RedstoneWireBlockAccessor) (Object) Blocks.REDSTONE_WIRE;
        boolean prevRed = acc.bluestone$getWiresGivePower();
        acc.bluestone$setWiresGivePower(false);
        // super.getReceivedBluePower toggles BluePower.wiresGivePower internally and resets it to true.
        // We need it to STAY false for the redPower read below, so save and re-assert.
        boolean prevBlue = BluePower.wiresGivePower;
        try {
            int bluePower = super.getReceivedBluePower(level, pos);
            // Re-suppress the shared flag (parent reset it to true) for the vanilla redstone read.
            BluePower.wiresGivePower = false;
            int redPower = level.getBestNeighborSignal(pos);
            // wire-to-wire: scan horizontal neighbours for wire POWER directly with -1 decay.
            // (vanilla getWireSignal does this for redstone<->redstone and redstone<->purplestone;
            //  we replicate it here for all wire types since emission was suppressed above.)
            int wireMax = 0;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos nPos = pos.relative(direction);
                BlockState nState = level.getBlockState(nPos);
                if (nState.is(Blocks.REDSTONE_WIRE) || nState.is(this) || nState.is(ModBlocks.BLUESTONE_WIRE.get())) {
                    wireMax = Math.max(wireMax, nState.getValue(BlockStateProperties.POWER));
                }
            }
            return Math.max(Math.max(bluePower, redPower), Math.max(wireMax - 1, 0));
        } finally {
            BluePower.wiresGivePower = prevBlue;
            acc.bluestone$setWiresGivePower(prevRed);
            this.wiresGivePower = true;
        }
    }

    // --- purple particles (vanilla DustParticleOptions with purple colour) ---
    private static final Vec3 PURPLE = new Vec3(0.35, 0.10, 0.55);

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int p = state.getValue(BlockStateProperties.POWER);
        if (p == 0) return;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            RedstoneSide c = state.getValue(BluestoneWireBlock.DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(d));
            if (c == RedstoneSide.UP) spawnPurpleParticle(level, pos, d, Direction.UP, -0.5f, 0.5f, random);
            else if (c == RedstoneSide.SIDE) spawnPurpleParticle(level, pos, Direction.DOWN, d, 0.0f, 0.5f, random);
            else spawnPurpleParticle(level, pos, Direction.DOWN, d, 0.0f, 0.3f, random);
        }
    }

    private void spawnPurpleParticle(Level level, BlockPos pos, Direction d, Direction d2, float f, float g, RandomSource random) {
        float h = g - f;
        if (random.nextFloat() >= 0.2f * h) return;
        float j = f + h * random.nextFloat();
        double x = 0.5 + 0.4375 * d.getStepX() + j * d2.getStepX();
        double y = 0.5 + 0.4375 * d.getStepY() + j * d2.getStepY();
        double z = 0.5 + 0.4375 * d.getStepZ() + j * d2.getStepZ();
        level.addParticle(new DustParticleOptions(PURPLE.toVector3f(), 1.0f),
                pos.getX() + x, pos.getY() + y, pos.getZ() + z, 0.0, 0.0, 0.0);
    }
}
