package com.bluestone.block;

import com.bluestone.power.BluePower;
import com.bluestone.power.ColorAttribution;
import com.bluestone.registry.ModBlocks;
import com.bluestone.mixin.RedstoneWireBlockAccessor;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Purplestone wire - a BRIDGE between redstone and bluestone. Extends {@link BluestoneWireBlock}
 * (so blue wire logic, shape rendering, blue power propagation all work), but ALSO:
 * <ul>
 *   <li>Emits REAL redstone power ({@code emitsRedstonePower=true}, weak/strong return POWER),
 *       so vanilla redstone wire connects to it and redstone systems read its signal.</li>
 *   <li>{@code connectsTo} overridden to ALSO accept red-coloured components (redstone wire,
 *       repeater, comparator, torch, block) - the bridge connection.</li>
 *   <li>{@code getReceivedBluePower} overridden to ALSO read redstone power (vanilla), not just
 *       blue power - so a redstone signal next to purplestone energises it.</li>
 * </ul>
 * The {@link com.bluestone.mixin.RedstoneWireBlockMixin} makes vanilla red wire treat purple as a
 * same-color wire (connectsTo + increasePower), completing the red-side bridge.
 */
public class PurplestoneWireBlock extends BluestoneWireBlock {

    public PurplestoneWireBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    // --- emit REAL redstone power, exactly mirroring vanilla RedstoneWireBlock ---
    // wiresGivePower: set false while computing own received power (prevents self-energising);
    // wire-to-wire uses increasePower (reads POWER directly) which bypasses this flag.
    private boolean wiresGivePower = true;

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return this.wiresGivePower;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        // Check BOTH our own instance flag AND the shared BluePower flag: when ANY wire (blue, purple,
        // or vanilla redstone via the global toggle below) is computing its own received power, all
        // wires must stay silent to prevent cross-instance feedback loops at mixed-wire crossings.
        if (!this.wiresGivePower || !BluePower.wiresGivePower || direction == Direction.DOWN) {
            return 0;
        }
        int i = state.get(Properties.POWER);
        if (i == 0) {
            return 0;
        }
        // Only emit toward UP, or toward horizontal directions where this wire has a connection
        if (direction == Direction.UP || state.get(BluestoneWireBlock.DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction.getOpposite())).isConnected()) {
            return i;
        }
        return 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        if (!this.wiresGivePower || !BluePower.wiresGivePower) {
            return 0;
        }
        return state.getWeakRedstonePower(world, pos, direction);
    }

    // --- increasePower: read POWER from BOTH blue wire and purple wire (the bridge) ---
    @Override
    protected int increasePower(BlockState state) {
        if (state.isOf(this)) return state.get(Properties.POWER);
        if (state.isOf(ModBlocks.BLUESTONE_WIRE)) return state.get(Properties.POWER);
        if (state.isOf(Blocks.REDSTONE_WIRE)) return state.get(Properties.POWER);
        return 0;
    }

    // --- connectsTo: blue base + ALSO accept red-coloured components (the bridge) ---
    @Override
    protected boolean connectsTo(BlockState state, Direction dir) {
        if (state.isOf(Blocks.REDSTONE_WIRE)) return true;
        // Vanilla repeater: connect only on input/output sides
        if (state.isOf(Blocks.REPEATER)) {
            Direction d = state.get(Properties.HORIZONTAL_FACING);
            return d == dir || d.getOpposite() == dir;
        }
        // Vanilla comparator: connect from all sides
        if (state.isOf(Blocks.COMPARATOR)) return dir != null;
        // Converter repeater: same gate-facing rule
        if (state.getBlock() instanceof ConverterRepeaterBlock) {
            Direction d = state.get(Properties.HORIZONTAL_FACING);
            return d == dir || d.getOpposite() == dir;
        }
        if (state.isOf(Blocks.REDSTONE_TORCH) || state.isOf(Blocks.REDSTONE_WALL_TORCH)
                || state.isOf(Blocks.REDSTONE_BLOCK)) {
            return dir != null;
        }
        return super.connectsTo(state, dir);
    }

    // --- power: blue system (inherited) + ALSO read vanilla redstone power ---
    // BluePower filters out red-coloured components (returns 0 for redstone wire/torch/etc),
    // so purplestone must ALSO check the vanilla redstone level and take the max.
    @Override
    protected int getReceivedBluePower(World world, BlockPos pos) {
        // Suppress ALL wire emission while computing our own input (prevents self-energising):
        //  - this.wiresGivePower: our own instance flag (suppresses our redstone emission)
        //  - BluePower.wiresGivePower: shared flag (suppresses blue wire emission via weakTowards,
        //    AND suppresses other purplestone instances' redstone emission, since they check it too)
        //  - vanilla REDSTONE_WIRE.wiresGivePower: suppresses redstone wire emission (different instance,
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
            int bluePower = super.getReceivedBluePower(world, pos);
            // Re-suppress the shared flag (parent reset it to true) for the vanilla redstone read.
            BluePower.wiresGivePower = false;
            int redPower = world.getReceivedRedstonePower(pos);
            // wire-to-wire: scan horizontal neighbours for wire POWER directly with -1 decay.
            // (vanilla increasePower does this for redstone<->redstone and redstone<->purplestone;
            //  we replicate it here for all wire types since emission was suppressed above.)
            int wireMax = 0;
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos nPos = pos.offset(direction);
                BlockState nState = world.getBlockState(nPos);
                if (nState.isOf(Blocks.REDSTONE_WIRE) || nState.isOf(this) || nState.isOf(ModBlocks.BLUESTONE_WIRE)) {
                    wireMax = Math.max(wireMax, nState.get(Properties.POWER));
                }
            }
            return Math.max(Math.max(bluePower, redPower), Math.max(wireMax - 1, 0));
        } finally {
            BluePower.wiresGivePower = prevBlue;
            acc.bluestone$setWiresGivePower(prevRed);
            this.wiresGivePower = true;
        }
    }

    // --- purple particles (vanilla DustParticleEffect with purple colour) ---
    private static final Vec3d PURPLE = new Vec3d(0.35, 0.10, 0.55);

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        int p = state.get(Properties.POWER);
        if (p == 0) return;
        for (Direction d : Direction.Type.HORIZONTAL) {
            WireConnection c = state.get(BluestoneWireBlock.DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(d));
            if (c == WireConnection.UP) spawnPurpleParticle(world, pos, d, Direction.UP, -0.5f, 0.5f, random);
            else if (c == WireConnection.SIDE) spawnPurpleParticle(world, pos, Direction.DOWN, d, 0.0f, 0.5f, random);
            else spawnPurpleParticle(world, pos, Direction.DOWN, d, 0.0f, 0.3f, random);
        }
    }

    private void spawnPurpleParticle(World world, BlockPos pos, Direction d, Direction d2, float f, float g, Random random) {
        float h = g - f;
        if (random.nextFloat() >= 0.2f * h) return;
        float j = f + h * random.nextFloat();
        double x = 0.5 + 0.4375 * d.getOffsetX() + j * d2.getOffsetX();
        double y = 0.5 + 0.4375 * d.getOffsetY() + j * d2.getOffsetY();
        double z = 0.5 + 0.4375 * d.getOffsetZ() + j * d2.getOffsetZ();
        world.addParticle(new DustParticleEffect(PURPLE.toVector3f(), 1.0f),
                pos.getX() + x, pos.getY() + y, pos.getZ() + z, 0.0, 0.0, 0.0);
    }
}
