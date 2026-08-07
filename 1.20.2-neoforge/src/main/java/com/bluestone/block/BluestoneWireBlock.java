package com.bluestone.block;

import com.bluestone.particle.BluestoneParticles;
import com.bluestone.power.BluePower;
import com.bluestone.registry.ModBlocks;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bluestone wire - a faithful blue mirror of {@code RedStoneWireBlock}. Connection shapes (dot/line/
 * cross/up), outline shape, right-click dot&lt;-&gt;cross toggle, update propagation, and the
 * power-decreases-by-1 rule all mirror vanilla; only the power SOURCE reading is blue (via
 * {@link BluePower}, which mirrors {@code SignalGetter}) and {@code shouldConnectTo} is blue-only.
 * Emits NO real redstone power (Option Y).
 */
@SuppressWarnings("deprecation")
public class BluestoneWireBlock extends Block {
    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final Map<Direction, EnumProperty<RedstoneSide>> DIRECTION_TO_WIRE_CONNECTION_PROPERTY =
            Maps.newEnumMap(ImmutableMap.of(
                    Direction.NORTH, NORTH, Direction.EAST, EAST,
                    Direction.SOUTH, SOUTH, Direction.WEST, WEST));

    private static final VoxelShape DOT_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 13.0);
    private static final Map<Direction, VoxelShape> DIRECTION_TO_SIDE_SHAPE = Maps.newEnumMap(ImmutableMap.of(
            Direction.NORTH, Block.box(3.0, 0.0, 0.0, 13.0, 1.0, 13.0),
            Direction.SOUTH, Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 16.0),
            Direction.EAST, Block.box(3.0, 0.0, 3.0, 16.0, 1.0, 13.0),
            Direction.WEST, Block.box(0.0, 0.0, 3.0, 13.0, 1.0, 13.0)));
    private static final Map<Direction, VoxelShape> DIRECTION_TO_UP_SHAPE = Maps.newEnumMap(ImmutableMap.of(
            Direction.NORTH, Shapes.or(DIRECTION_TO_SIDE_SHAPE.get(Direction.NORTH), Block.box(3.0, 0.0, 0.0, 13.0, 16.0, 1.0)),
            Direction.SOUTH, Shapes.or(DIRECTION_TO_SIDE_SHAPE.get(Direction.SOUTH), Block.box(3.0, 0.0, 15.0, 13.0, 16.0, 16.0)),
            Direction.EAST, Shapes.or(DIRECTION_TO_SIDE_SHAPE.get(Direction.EAST), Block.box(15.0, 0.0, 3.0, 16.0, 16.0, 13.0)),
            Direction.WEST, Shapes.or(DIRECTION_TO_SIDE_SHAPE.get(Direction.WEST), Block.box(0.0, 0.0, 3.0, 1.0, 16.0, 13.0))));
    private static final Map<BlockState, VoxelShape> SHAPES = Maps.newHashMap();
    private static final Vec3[] COLORS = new Vec3[16];
    static {
        for (int i = 0; i <= 15; i++) {
            float f = (float) i / 15.0f;
            float g = f * 0.6f + (f > 0.0f ? 0.4f : 0.3f);
            float h = Mth.clamp(f * f * 0.7f - 0.5f, 0.0f, 1.0f);
            float j = Mth.clamp(f * f * 0.6f - 0.7f, 0.0f, 1.0f);
            // blue gradient: swap so blue dominates (vanilla is red-dominant g>r,b)
            COLORS[i] = new Vec3(h, j, g);
        }
    }

    private final BlockState dotState;
    private boolean wiresGivePower = true;

    public BluestoneWireBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, RedstoneSide.NONE).setValue(EAST, RedstoneSide.NONE)
                .setValue(SOUTH, RedstoneSide.NONE).setValue(WEST, RedstoneSide.NONE).setValue(POWER, 0));
        this.dotState = defaultBlockState().setValue(NORTH, RedstoneSide.SIDE).setValue(EAST, RedstoneSide.SIDE)
                .setValue(SOUTH, RedstoneSide.SIDE).setValue(WEST, RedstoneSide.SIDE);
        for (BlockState state : stateDefinition.getPossibleStates()) {
            if (state.getValue(POWER) != 0) continue;
            SHAPES.put(state, getShapeForState(state));
        }
    }

    private VoxelShape getShapeForState(BlockState state) {
        VoxelShape shape = DOT_SHAPE;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            RedstoneSide c = state.getValue(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(dir));
            if (c == RedstoneSide.SIDE) shape = Shapes.or(shape, DIRECTION_TO_SIDE_SHAPE.get(dir));
            else if (c == RedstoneSide.UP) shape = Shapes.or(shape, DIRECTION_TO_UP_SHAPE.get(dir));
        }
        return shape;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.setValue(POWER, 0));
    }

    @Override
    public boolean isSignalSource(BlockState state) { return false; }
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return 0; }
    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return 0; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, POWER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return getPlacementState(ctx.getLevel(), this.dotState, ctx.getClickedPos());
    }

    private BlockState getPlacementState(BlockGetter level, BlockState state, BlockPos pos) {
        boolean alone = isNotConnected(state);
        state = getDefaultWireState(level, defaultBlockState().setValue(POWER, state.getValue(POWER)), pos);
        if (alone && isNotConnected(state)) return state;
        boolean n = state.getValue(NORTH).isConnected(), s = state.getValue(SOUTH).isConnected();
        boolean e = state.getValue(EAST).isConnected(), w = state.getValue(WEST).isConnected();
        boolean ns = !n && !s, ew = !e && !w;
        if (!w && ns) state = state.setValue(WEST, RedstoneSide.SIDE);
        if (!e && ns) state = state.setValue(EAST, RedstoneSide.SIDE);
        if (!n && ew) state = state.setValue(NORTH, RedstoneSide.SIDE);
        if (!s && ew) state = state.setValue(SOUTH, RedstoneSide.SIDE);
        return state;
    }

    private BlockState getDefaultWireState(BlockGetter level, BlockState state, BlockPos pos) {
        boolean aboveNonSolid = !level.getBlockState(pos.above()).isSolidRender(level, pos);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (state.getValue(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(dir)).isConnected()) continue;
            state = state.setValue(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(dir), getRenderConnectionType(level, pos, dir, aboveNonSolid));
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) return state;
        if (direction == Direction.UP) return getPlacementState(level, state, pos);
        RedstoneSide c = getRenderConnectionType(level, pos, direction);
        if (c.isConnected() == state.getValue(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction)).isConnected() && !isFullyConnected(state)) {
            return state.setValue(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction), c);
        }
        return getPlacementState(level, dotState.setValue(POWER, state.getValue(POWER)).setValue(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction), c), pos);
    }

    /** Check if the block state is any type of redstone wire (vanilla, bluestone, or purplestone). */
    private static boolean isAnyWire(BlockState state) {
        Block b = state.getBlock();
        return b == Blocks.REDSTONE_WIRE
                || b instanceof BluestoneWireBlock; // covers bluestone + purplestone
    }

    private static boolean isFullyConnected(BlockState s) {
        return s.getValue(NORTH).isConnected() && s.getValue(SOUTH).isConnected() && s.getValue(EAST).isConnected() && s.getValue(WEST).isConnected();
    }
    private static boolean isNotConnected(BlockState s) {
        return !s.getValue(NORTH).isConnected() && !s.getValue(SOUTH).isConnected() && !s.getValue(EAST).isConnected() && !s.getValue(WEST).isConnected();
    }

    /**
     * Mirrors vanilla {@code RedStoneWireBlock.updateIndirectNeighbourShapes}: when a wire's state changes,
     * notifies wires one block up/down in each horizontal direction so they recalculate their connection
     * shape (this is what makes an "up-running" wire return to flat when the upper wire is removed).
     *
     * <p>Unlike vanilla's {@code is(this)}, we check for ANY wire type (bluestone, purplestone,
     * or vanilla redstone) so cross-type UP connections propagate correctly.</p>
     */
    @Override
    public void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, int flags, int maxUpdateDepth) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            RedstoneSide wireConnection = state.getValue(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction));
            if (wireConnection == RedstoneSide.NONE || isAnyWire(level.getBlockState(mutable.setWithOffset(pos, direction)))) {
                continue;
            }
            mutable.move(Direction.DOWN);
            BlockState blockState = level.getBlockState(mutable);
            if (isAnyWire(blockState)) {
                BlockPos blockPos = mutable.relative(direction.getOpposite());
                level.neighborShapeChanged(direction.getOpposite(), level.getBlockState(blockPos), mutable, blockPos, flags, maxUpdateDepth);
            }
            mutable.setWithOffset(pos, direction).move(Direction.UP);
            BlockState blockState2 = level.getBlockState(mutable);
            if (!isAnyWire(blockState2)) {
                continue;
            }
            BlockPos blockPos2 = mutable.relative(direction.getOpposite());
            level.neighborShapeChanged(direction.getOpposite(), level.getBlockState(blockPos2), mutable, blockPos2, flags, maxUpdateDepth);
        }
    }

    private RedstoneSide getRenderConnectionType(BlockGetter level, BlockPos pos, Direction direction) {
        return getRenderConnectionType(level, pos, direction, !level.getBlockState(pos.above()).isSolidRender(level, pos));
    }

    /**
     * Dynamically (re)compute whether this wire has a connection toward {@code direction} (the
     * direction FROM the wire TO the receiver), without relying on the stored block-state property.
     * This mirrors vanilla {@code RedStoneWireBlock.getWeakRedstonePower} which calls
     * {@code getPlacementState(...).get(PROPERTY).isConnected()} — so that a gate placed next to an
     * already-powered wire reads the signal immediately, even before the wire's stored connection
     * property has been refreshed by an {@code updateShape} pass.
     *
     * @param receiverDir the direction from the receiver to this wire (vanilla convention);
     *                    the connection toward the receiver is {@code receiverDir.getOpposite()}.
     */
    public boolean isConnectedTowards(BlockGetter level, BlockPos pos, Direction receiverDir) {
        if (receiverDir == Direction.DOWN) return false;
        if (receiverDir == Direction.UP) return true;
        Direction wireToReceiver = receiverDir.getOpposite();
        return getRenderConnectionType(level, pos, wireToReceiver).isConnected();
    }

    private RedstoneSide getRenderConnectionType(BlockGetter level, BlockPos pos, Direction direction, boolean aboveNonSolid) {
        BlockPos nPos = pos.relative(direction);
        BlockState nState = level.getBlockState(nPos);
        if (aboveNonSolid) {
            boolean canRun = nState.getBlock() instanceof TrapDoorBlock || canRunOnTop(level, nPos, nState);
            if (canRun && shouldConnectTo(level.getBlockState(nPos.above()))) {
                if (nState.isFaceSturdy(level, nPos, direction.getOpposite())) return RedstoneSide.UP;
                return RedstoneSide.SIDE;
            }
        }
        if (shouldConnectTo(nState, direction) || (!nState.isSolidRender(level, nPos) && shouldConnectTo(level.getBlockState(nPos.below())))) {
            return RedstoneSide.SIDE;
        }
        return RedstoneSide.NONE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canRunOnTop(level, pos.below(), level.getBlockState(pos.below()));
    }

    /**
     * NeoForge hook ({@code IBlockExtension#canConnectRedstone}): vanilla redstone wire uses
     * {@code canRedstoneConnectTo} (which calls this) to decide its connection shape, including the
     * UP/"climb the wall" shape. The default implementation only recognises REDSTONE_WIRE, so it
     * would not form an UP connection toward purplestone wire (the red/blue bridge).
     *
     * <p><b>Blue-coloured wires must be invisible to redstone</b> (Option Y isolation), so only
     * {@link PurplestoneWireBlock} reports itself as connectable here. Plain bluestone wire returns
     * false, keeping redstone and bluestone fully separated.</p>
     */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @javax.annotation.Nullable Direction direction) {
        // Purplestone (the bridge) is connectable; plain bluestone is invisible to redstone.
        return this instanceof PurplestoneWireBlock;
    }

    private boolean canRunOnTop(BlockGetter level, BlockPos pos, BlockState floor) {
        return floor.isFaceSturdy(level, pos, Direction.UP) || floor.is(Blocks.HOPPER);
    }

    // --- blue shouldConnectTo (blue components + neutral sources; never red) ---
    // protected + non-static so PurplestoneWireBlock can override to also accept red components.
    protected boolean shouldConnectTo(BlockState state) { return shouldConnectTo(state, null); }
    protected boolean shouldConnectTo(BlockState state, Direction dir) {
        if (com.bluestone.power.ColorAttribution.isRedColored(state)) return false;
        if (state.is(ModBlocks.BLUESTONE_WIRE.get())) return true;
        if (state.is(ModBlocks.PURPLESTONE_WIRE.get())) return true;
        Block b = state.getBlock();
        // Bluestone repeater: connect only on input/output sides (like vanilla repeater)
        if (b instanceof BluestoneRepeaterBlock) {
            Direction d = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return d == dir || d.getOpposite() == dir;
        }
        // Converter: bluestone connects only on the side that carries the BLUE signal.
        //   RED_TO_BLUE  → blue OUTPUT on FACING.getOpposite() → wire sees converter at FACING
        //   BLUE_TO_RED  → blue INPUT  on FACING             → wire sees converter at FACING.getOpposite()
        if (b instanceof ConverterRepeaterBlock) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            ConverterRepeaterBlock.Mode mode = state.getValue(ConverterRepeaterBlock.CONVERTER_MODE);
            Direction blueSide = mode == ConverterRepeaterBlock.Mode.RED_TO_BLUE ? facing : facing.getOpposite();
            return blueSide == dir;
        }
        // Comparator: connect from all sides (like vanilla comparator)
        if (b instanceof BluestoneComparatorBlock) return dir != null;
        if (b instanceof BluestoneTorchBlock || b instanceof BluestoneWallTorchBlock || b instanceof BluestoneBlock) return dir != null;
        // Observer: connect only on its output side (FACING), matching vanilla redstone wire.
        if (b == Blocks.OBSERVER) return dir == state.getValue(BlockStateProperties.FACING);
        return com.bluestone.power.ColorAttribution.isNeutralSource(state) && dir != null;
    }

    // --- power (blue mirror of vanilla calculateTargetStrength) ---
    protected int getReceivedBluePower(Level level, BlockPos pos) {
        BluePower.wiresGivePower = false;
        int i = BluePower.getReceivedBluePower(level, pos);
        BluePower.wiresGivePower = true;
        int j = 0;
        if (i < 15) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos bPos = pos.relative(direction);
                BlockState bState = level.getBlockState(bPos);
                j = Math.max(j, getWireSignal(bState));
                BlockPos upPos = pos.above();
                if (bState.isSolidRender(level, bPos) && !level.getBlockState(upPos).isSolidRender(level, upPos)) {
                    j = Math.max(j, getWireSignal(level.getBlockState(bPos.above())));
                    continue;
                }
                if (bState.isSolidRender(level, bPos)) continue;
                j = Math.max(j, getWireSignal(level.getBlockState(bPos.below())));
            }
        }
        return Math.max(i, j - 1);
    }

    protected int getWireSignal(BlockState state) {
        if (state.is(this)) return state.getValue(POWER);
        if (state.is(ModBlocks.PURPLESTONE_WIRE.get())) return state.getValue(POWER);
        return 0;
    }

    private void update(Level level, BlockPos pos, BlockState state) {
        int i = getReceivedBluePower(level, pos);
        if (state.getValue(POWER) != i) {
            if (level.getBlockState(pos) == state) level.setBlock(pos, state.setValue(POWER, i), Block.UPDATE_ALL);
            Set<BlockPos> set = new HashSet<>();
            set.add(pos);
            for (Direction d : Direction.values()) set.add(pos.relative(d));
            for (BlockPos bp : set) level.updateNeighborsAt(bp, this);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean isMoving) {
        if (level.isClientSide) return;
        if (state.canSurvive(level, pos)) update(level, pos, state);
        else { Block.dropResources(state, level, pos); level.removeBlock(pos, false); }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { update(level, pos, state); }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (oldState.is(state.getBlock()) || level.isClientSide) return;
        update(level, pos, state);
        for (Direction d : Direction.Plane.VERTICAL) level.updateNeighborsAt(pos.relative(d), this);
        updateOffsetNeighbors(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (isMoving || state.is(newState.getBlock())) return;
        super.onRemove(state, level, pos, newState, isMoving);
        if (level.isClientSide) return;
        for (Direction d : Direction.values()) level.updateNeighborsAt(pos.relative(d), this);
        update(level, pos, state);
        updateOffsetNeighbors(level, pos);
    }

    private void updateOffsetNeighbors(Level level, BlockPos pos) {
        for (Direction d : Direction.Plane.HORIZONTAL) updateNeighbors(level, pos.relative(d));
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos bp = pos.relative(d);
            if (level.getBlockState(bp).isSolidRender(level, bp)) updateNeighbors(level, bp.above());
            else updateNeighbors(level, bp.below());
        }
    }

    private void updateNeighbors(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(this)) return;
        level.updateNeighborsAt(pos, this);
        for (Direction d : Direction.values()) level.updateNeighborsAt(pos.relative(d), this);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (isFullyConnected(state) || isNotConnected(state)) {
            BlockState ns = isFullyConnected(state) ? defaultBlockState() : this.dotState;
            ns = ns.setValue(POWER, state.getValue(POWER));
            ns = getPlacementState(level, ns, pos);
            if (ns != state) {
                level.setBlock(pos, ns, Block.UPDATE_ALL);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    public static int getWireColor(int power) {
        Vec3 c = COLORS[power];
        return Mth.color((float) c.x(), (float) c.y(), (float) c.z());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int p = state.getValue(POWER);
        if (p == 0) return;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            RedstoneSide c = state.getValue(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(d));
            if (c == RedstoneSide.UP) spawnParticle(level, pos, COLORS[p], d, Direction.UP, -0.5f, 0.5f);
            else if (c == RedstoneSide.SIDE) spawnParticle(level, pos, COLORS[p], Direction.DOWN, d, 0.0f, 0.5f);
            else spawnParticle(level, pos, COLORS[p], Direction.DOWN, d, 0.0f, 0.3f);
        }
    }

    private void spawnParticle(Level level, BlockPos pos, Vec3 color, Direction d, Direction d2, float f, float g) {
        float h = g - f;
        if (level.random.nextFloat() >= 0.2f * h) return;
        float j = f + h * level.random.nextFloat();
        double x = 0.5 + 0.4375 * d.getStepX() + j * d2.getStepX();
        double y = 0.5 + 0.4375 * d.getStepY() + j * d2.getStepY();
        double z = 0.5 + 0.4375 * d.getStepZ() + j * d2.getStepZ();
        // Blue dust particle (mod's own registered particle type).
        level.addParticle(BluestoneParticles.BLUESTONE_DUST.get(), pos.getX() + x, pos.getY() + y, pos.getZ() + z, 0.0, 0.0, 0.0);
    }
}
