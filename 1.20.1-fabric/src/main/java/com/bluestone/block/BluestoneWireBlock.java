package com.bluestone.block;

import com.bluestone.power.BluePower;
import com.bluestone.registry.ModBlocks;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bluestone wire - a faithful blue mirror of {@code RedstoneWireBlock}. Connection shapes (dot/line/
 * cross/up), outline shape, right-click dot<->cross toggle, update propagation, and the
 * power-decreases-by-1 rule all mirror vanilla; only the power SOURCE reading is blue (via
 * {@link BluePower}, which mirrors {@code RedstoneView}) and {@code connectsTo} is blue-only.
 * Emits NO real redstone power (Option Y).
 */
@SuppressWarnings("deprecation")
public class BluestoneWireBlock extends Block {
    public static final EnumProperty<WireConnection> NORTH = Properties.NORTH_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> EAST = Properties.EAST_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> SOUTH = Properties.SOUTH_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> WEST = Properties.WEST_WIRE_CONNECTION;
    public static final IntProperty POWER = Properties.POWER;
    public static final Map<Direction, EnumProperty<WireConnection>> DIRECTION_TO_WIRE_CONNECTION_PROPERTY =
            Maps.newEnumMap(ImmutableMap.of(
                    Direction.NORTH, NORTH, Direction.EAST, EAST,
                    Direction.SOUTH, SOUTH, Direction.WEST, WEST));

    private static final VoxelShape DOT_SHAPE = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 1.0, 13.0);
    private static final Map<Direction, VoxelShape> DIRECTION_TO_SIDE_SHAPE = Maps.newEnumMap(ImmutableMap.of(
            Direction.NORTH, Block.createCuboidShape(3.0, 0.0, 0.0, 13.0, 1.0, 13.0),
            Direction.SOUTH, Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 1.0, 16.0),
            Direction.EAST, Block.createCuboidShape(3.0, 0.0, 3.0, 16.0, 1.0, 13.0),
            Direction.WEST, Block.createCuboidShape(0.0, 0.0, 3.0, 13.0, 1.0, 13.0)));
    private static final Map<Direction, VoxelShape> DIRECTION_TO_UP_SHAPE = Maps.newEnumMap(ImmutableMap.of(
            Direction.NORTH, VoxelShapes.union(DIRECTION_TO_SIDE_SHAPE.get(Direction.NORTH), Block.createCuboidShape(3.0, 0.0, 0.0, 13.0, 16.0, 1.0)),
            Direction.SOUTH, VoxelShapes.union(DIRECTION_TO_SIDE_SHAPE.get(Direction.SOUTH), Block.createCuboidShape(3.0, 0.0, 15.0, 13.0, 16.0, 16.0)),
            Direction.EAST, VoxelShapes.union(DIRECTION_TO_SIDE_SHAPE.get(Direction.EAST), Block.createCuboidShape(15.0, 0.0, 3.0, 16.0, 16.0, 13.0)),
            Direction.WEST, VoxelShapes.union(DIRECTION_TO_SIDE_SHAPE.get(Direction.WEST), Block.createCuboidShape(0.0, 0.0, 3.0, 1.0, 16.0, 13.0))));
    private static final Map<BlockState, VoxelShape> SHAPES = Maps.newHashMap();
    private static final Vec3d[] COLORS = new Vec3d[16];
    static {
        for (int i = 0; i <= 15; i++) {
            float f = (float) i / 15.0f;
            float g = f * 0.6f + (f > 0.0f ? 0.4f : 0.3f);
            float h = net.minecraft.util.math.MathHelper.clamp(f * f * 0.7f - 0.5f, 0.0f, 1.0f);
            float j = net.minecraft.util.math.MathHelper.clamp(f * f * 0.6f - 0.7f, 0.0f, 1.0f);
            // blue gradient: swap so blue dominates (vanilla is red-dominant g>r,b)
            COLORS[i] = new Vec3d(h, j, g);
        }
    }

    private final BlockState dotState;
    private boolean wiresGivePower = true;

    public BluestoneWireBlock(AbstractBlock.Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(NORTH, WireConnection.NONE).with(EAST, WireConnection.NONE)
                .with(SOUTH, WireConnection.NONE).with(WEST, WireConnection.NONE).with(POWER, 0));
        this.dotState = getDefaultState().with(NORTH, WireConnection.SIDE).with(EAST, WireConnection.SIDE)
                .with(SOUTH, WireConnection.SIDE).with(WEST, WireConnection.SIDE);
        for (BlockState state : getStateManager().getStates()) {
            if (state.get(POWER) != 0) continue;
            SHAPES.put(state, getShapeForState(state));
        }
    }

    private VoxelShape getShapeForState(BlockState state) {
        VoxelShape shape = DOT_SHAPE;
        for (Direction dir : Direction.Type.HORIZONTAL) {
            WireConnection c = state.get(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(dir));
            if (c == WireConnection.SIDE) shape = VoxelShapes.union(shape, DIRECTION_TO_SIDE_SHAPE.get(dir));
            else if (c == WireConnection.UP) shape = VoxelShapes.union(shape, DIRECTION_TO_UP_SHAPE.get(dir));
        }
        return shape;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES.get(state.with(POWER, 0));
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) { return false; }
    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) { return 0; }
    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) { return 0; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, POWER);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getPlacementState(ctx.getWorld(), this.dotState, ctx.getBlockPos());
    }

    private BlockState getPlacementState(BlockView world, BlockState state, BlockPos pos) {
        boolean alone = isNotConnected(state);
        state = getDefaultWireState(world, getDefaultState().with(POWER, state.get(POWER)), pos);
        if (alone && isNotConnected(state)) return state;
        boolean n = state.get(NORTH).isConnected(), s = state.get(SOUTH).isConnected();
        boolean e = state.get(EAST).isConnected(), w = state.get(WEST).isConnected();
        boolean ns = !n && !s, ew = !e && !w;
        if (!w && ns) state = state.with(WEST, WireConnection.SIDE);
        if (!e && ns) state = state.with(EAST, WireConnection.SIDE);
        if (!n && ew) state = state.with(NORTH, WireConnection.SIDE);
        if (!s && ew) state = state.with(SOUTH, WireConnection.SIDE);
        return state;
    }

    private BlockState getDefaultWireState(BlockView world, BlockState state, BlockPos pos) {
        boolean aboveNonSolid = !world.getBlockState(pos.up()).isSolidBlock(world, pos);
        for (Direction dir : Direction.Type.HORIZONTAL) {
            if (state.get(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(dir)).isConnected()) continue;
            state = state.with(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(dir), getRenderConnectionType(world, pos, dir, aboveNonSolid));
        }
        return state;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) return state;
        if (direction == Direction.UP) return getPlacementState(world, state, pos);
        WireConnection c = getRenderConnectionType(world, pos, direction);
        if (c.isConnected() == state.get(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction)).isConnected() && !isFullyConnected(state)) {
            return state.with(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction), c);
        }
        return getPlacementState(world, dotState.with(POWER, state.get(POWER)).with(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction), c), pos);
    }

    /** Check if the block state is any type of redstone wire (vanilla, bluestone, or purplestone). */
    private static boolean isAnyWire(BlockState state) {
        Block b = state.getBlock();
        return b == Blocks.REDSTONE_WIRE
                || b instanceof BluestoneWireBlock; // covers bluestone + purplestone
    }

    private static boolean isFullyConnected(BlockState s) {
        return s.get(NORTH).isConnected() && s.get(SOUTH).isConnected() && s.get(EAST).isConnected() && s.get(WEST).isConnected();
    }
    private static boolean isNotConnected(BlockState s) {
        return !s.get(NORTH).isConnected() && !s.get(SOUTH).isConnected() && !s.get(EAST).isConnected() && !s.get(WEST).isConnected();
    }

    /**
     * Mirrors vanilla {@code RedstoneWireBlock.prepare}: when a wire's state changes, notifies
     * wires one block up/down in each horizontal direction so they recalculate their connection
     * shape (this is what makes an "up-running" wire return to flat when the upper wire is removed).
     *
     * <p>Unlike vanilla's {@code isOf(this)}, we check for ANY wire type (bluestone, purplestone,
     * or vanilla redstone) so cross-type UP connections propagate correctly.</p>
     */
    @Override
    public void prepare(BlockState state, WorldAccess world, BlockPos pos, int flags, int maxUpdateDepth) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (Direction direction : Direction.Type.HORIZONTAL) {
            WireConnection wireConnection = state.get(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction));
            if (wireConnection == WireConnection.NONE || isAnyWire(world.getBlockState(mutable.set((Vec3i) pos, direction)))) {
                continue;
            }
            mutable.move(Direction.DOWN);
            BlockState blockState = world.getBlockState(mutable);
            if (isAnyWire(blockState)) {
                Vec3i blockPos = mutable.offset(direction.getOpposite());
                world.replaceWithStateForNeighborUpdate(direction.getOpposite(), world.getBlockState((BlockPos) blockPos), mutable, (BlockPos) blockPos, flags, maxUpdateDepth);
            }
            mutable.set((Vec3i) pos, direction).move(Direction.UP);
            BlockState blockState2 = world.getBlockState(mutable);
            if (!isAnyWire(blockState2)) {
                continue;
            }
            Vec3i blockPos2 = mutable.offset(direction.getOpposite());
            world.replaceWithStateForNeighborUpdate(direction.getOpposite(), world.getBlockState((BlockPos) blockPos2), mutable, (BlockPos) blockPos2, flags, maxUpdateDepth);
        }
    }

    private WireConnection getRenderConnectionType(BlockView world, BlockPos pos, Direction direction) {
        return getRenderConnectionType(world, pos, direction, !world.getBlockState(pos.up()).isSolidBlock(world, pos));
    }

    private WireConnection getRenderConnectionType(BlockView world, BlockPos pos, Direction direction, boolean aboveNonSolid) {
        BlockPos nPos = pos.offset(direction);
        BlockState nState = world.getBlockState(nPos);
        if (aboveNonSolid) {
            boolean canRun = nState.getBlock() instanceof TrapdoorBlock || canRunOnTop(world, nPos, nState);
            if (canRun && connectsTo(world.getBlockState(nPos.up()))) {
                if (nState.isSideSolidFullSquare(world, nPos, direction.getOpposite())) return WireConnection.UP;
                return WireConnection.SIDE;
            }
        }
        if (connectsTo(nState, direction) || (!nState.isSolidBlock(world, nPos) && connectsTo(world.getBlockState(nPos.down())))) {
            return WireConnection.SIDE;
        }
        return WireConnection.NONE;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return canRunOnTop(world, pos.down(), world.getBlockState(pos.down()));
    }

    private boolean canRunOnTop(BlockView world, BlockPos pos, BlockState floor) {
        return floor.isSideSolidFullSquare(world, pos, Direction.UP) || floor.isOf(Blocks.HOPPER);
    }

    // --- blue connectsTo (blue components + neutral sources; never red) ---
    // protected + non-static so PurplestoneWireBlock can override to also accept red components.
    protected boolean connectsTo(BlockState state) { return connectsTo(state, null); }
    protected boolean connectsTo(BlockState state, Direction dir) {
        if (com.bluestone.power.ColorAttribution.isRedColored(state)) return false;
        if (state.isOf(ModBlocks.BLUESTONE_WIRE)) return true;
        if (state.isOf(ModBlocks.PURPLESTONE_WIRE)) return true;
        Block b = state.getBlock();
        if (b instanceof BluestoneRepeaterBlock || b instanceof BluestoneComparatorBlock
                || b instanceof ConverterRepeaterBlock) {
            Direction d = state.get(Properties.HORIZONTAL_FACING);
            return d == dir || d.getOpposite() == dir;
        }
        if (b instanceof BluestoneTorchBlock || b instanceof BluestoneWallTorchBlock || b instanceof BluestoneBlock) return dir != null;
        // Neutral sources connect (lever, button, etc.) — but NOT the observer (it shouldn't change wire shape)
        if (b == Blocks.OBSERVER) return false;
        return com.bluestone.power.ColorAttribution.isNeutralSource(state) && dir != null;
    }

    // --- power (blue mirror of vanilla getReceivedRedstonePower) ---
    protected int getReceivedBluePower(World world, BlockPos pos) {
        BluePower.wiresGivePower = false;
        int i = BluePower.getReceivedBluePower(world, pos);
        BluePower.wiresGivePower = true;
        int j = 0;
        if (i < 15) {
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos bPos = pos.offset(direction);
                BlockState bState = world.getBlockState(bPos);
                j = Math.max(j, increasePower(bState));
                BlockPos upPos = pos.up();
                if (bState.isSolidBlock(world, bPos) && !world.getBlockState(upPos).isSolidBlock(world, upPos)) {
                    j = Math.max(j, increasePower(world.getBlockState(bPos.up())));
                    continue;
                }
                if (bState.isSolidBlock(world, bPos)) continue;
                j = Math.max(j, increasePower(world.getBlockState(bPos.down())));
            }
        }
        return Math.max(i, j - 1);
    }

    protected int increasePower(BlockState state) {
        if (state.isOf(this)) return state.get(POWER);
        if (state.isOf(ModBlocks.PURPLESTONE_WIRE)) return state.get(POWER);
        return 0;
    }

    private void update(World world, BlockPos pos, BlockState state) {
        int i = getReceivedBluePower(world, pos);
        if (state.get(POWER) != i) {
            if (world.getBlockState(pos) == state) world.setBlockState(pos, state.with(POWER, i), Block.NOTIFY_ALL);
            Set<BlockPos> set = new HashSet<>();
            set.add(pos);
            for (Direction d : Direction.values()) set.add(pos.offset(d));
            for (BlockPos bp : set) world.updateNeighborsAlways(bp, this);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world.isClient) return;
        if (state.canPlaceAt(world, pos)) update(world, pos, state);
        else { dropStacks(state, world, pos); world.removeBlock(pos, false); }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) { update(world, pos, state); }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (oldState.isOf(state.getBlock()) || world.isClient) return;
        update(world, pos, state);
        for (Direction d : Direction.Type.VERTICAL) world.updateNeighborsAlways(pos.offset(d), this);
        updateOffsetNeighbors(world, pos);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (moved || state.isOf(newState.getBlock())) return;
        super.onStateReplaced(state, world, pos, newState, moved);
        if (world.isClient) return;
        for (Direction d : Direction.values()) world.updateNeighborsAlways(pos.offset(d), this);
        update(world, pos, state);
        updateOffsetNeighbors(world, pos);
    }

    private void updateOffsetNeighbors(World world, BlockPos pos) {
        for (Direction d : Direction.Type.HORIZONTAL) updateNeighbors(world, pos.offset(d));
        for (Direction d : Direction.Type.HORIZONTAL) {
            BlockPos bp = pos.offset(d);
            if (world.getBlockState(bp).isSolidBlock(world, bp)) updateNeighbors(world, bp.up());
            else updateNeighbors(world, bp.down());
        }
    }

    private void updateNeighbors(World world, BlockPos pos) {
        if (!world.getBlockState(pos).isOf(this)) return;
        world.updateNeighborsAlways(pos, this);
        for (Direction d : Direction.values()) world.updateNeighborsAlways(pos.offset(d), this);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, net.minecraft.util.Hand hand, net.minecraft.util.hit.BlockHitResult hit) {
        if (!player.getAbilities().allowModifyWorld) return ActionResult.PASS;
        if (isFullyConnected(state) || isNotConnected(state)) {
            BlockState ns = isFullyConnected(state) ? getDefaultState() : this.dotState;
            ns = ns.with(POWER, state.get(POWER));
            ns = getPlacementState(world, ns, pos);
            if (ns != state) {
                world.setBlockState(pos, ns, Block.NOTIFY_ALL);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    public static int getWireColor(int power) {
        Vec3d c = COLORS[power];
        return net.minecraft.util.math.MathHelper.packRgb((float) c.getX(), (float) c.getY(), (float) c.getZ());
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        int p = state.get(POWER);
        if (p == 0) return;
        for (Direction d : Direction.Type.HORIZONTAL) {
            WireConnection c = state.get(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(d));
            if (c == WireConnection.UP) spawnParticle(world, pos, COLORS[p], d, Direction.UP, -0.5f, 0.5f);
            else if (c == WireConnection.SIDE) spawnParticle(world, pos, COLORS[p], Direction.DOWN, d, 0.0f, 0.5f);
            else spawnParticle(world, pos, COLORS[p], Direction.DOWN, d, 0.0f, 0.3f);
        }
    }

    private void spawnParticle(World world, BlockPos pos, Vec3d color, Direction d, Direction d2, float f, float g) {
        float h = g - f;
        if (world.random.nextFloat() >= 0.2f * h) return;
        float j = f + h * world.random.nextFloat();
        double x = 0.5 + 0.4375 * d.getOffsetX() + j * d2.getOffsetX();
        double y = 0.5 + 0.4375 * d.getOffsetY() + j * d2.getOffsetY();
        double z = 0.5 + 0.4375 * d.getOffsetZ() + j * d2.getOffsetZ();
        // Blue dust particle (mod's own registered particle type).
        world.addParticle(com.bluestone.particle.BluestoneParticles.BLUESTONE_DUST, pos.getX() + x, pos.getY() + y, pos.getZ() + z, 0.0, 0.0, 0.0);
    }
}
