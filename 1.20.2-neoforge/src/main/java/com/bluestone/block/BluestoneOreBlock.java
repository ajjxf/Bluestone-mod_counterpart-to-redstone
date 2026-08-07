package com.bluestone.block;

import com.bluestone.particle.BluestoneParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Bluestone ore - mirrors vanilla {@link RedstoneOreBlock} exactly (glows on click/step/break,
 * 9-level light while lit, 4-5 dust drop with fortune, 1-5 XP, iron pickaxe), but its glow
 * particles use the mod's blue dust particle instead of the vanilla red one. Vanilla spawns its
 * particles from private static helpers, so every particle-emitting path is reimplemented here
 * with {@link BluestoneParticles#BLUESTONE_DUST}.
 */
public class BluestoneOreBlock extends RedStoneOreBlock {
    public BluestoneOreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        light(state, level, pos);
        // NOTE: intentionally do NOT call super - RedstoneOreBlock.interaction() (private static) spawns
        // vanilla RED particles. We replicate only the LIT toggle + blue particles here.
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully()) {
            light(state, level, pos);
        }
        // no super call (see attack note)
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            spawnParticles(level, pos);
        } else {
            light(state, level, pos);
        }
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.getItem() instanceof BlockItem && new BlockPlaceContext(player, hand, itemStack, hit).canPlace()) {
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    private static void light(BlockState state, Level level, BlockPos pos) {
        spawnParticles(level, pos);
        if (!state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, true), 3);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            spawnParticles(level, pos);
        }
    }

    private static void spawnParticles(Level level, BlockPos pos) {
        double d = 0.5625;
        RandomSource random = level.random;
        for (Direction direction : Direction.values()) {
            BlockPos blockPos = pos.relative(direction);
            if (level.getBlockState(blockPos).isSolidRender(level, blockPos)) {
                continue;
            }
            Direction.Axis axis = direction.getAxis();
            double e = axis == Direction.Axis.X ? 0.5 + 0.5625 * direction.getStepX() : random.nextFloat();
            double f = axis == Direction.Axis.Y ? 0.5 + 0.5625 * direction.getStepY() : random.nextFloat();
            double g = axis == Direction.Axis.Z ? 0.5 + 0.5625 * direction.getStepZ() : random.nextFloat();
            level.addParticle(BluestoneParticles.BLUESTONE_DUST.get(), pos.getX() + e, pos.getY() + f, pos.getZ() + g, 0.0, 0.0, 0.0);
        }
    }
}
