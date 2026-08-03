package com.bluestone.block;

import com.bluestone.particle.BluestoneParticles;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneOreBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Bluestone ore - mirrors vanilla {@link RedstoneOreBlock} exactly (glows on click/step/break,
 * 9-level light while lit, 4-5 dust drop with fortune, 1-5 XP, iron pickaxe), but its glow
 * particles use the mod's blue dust particle instead of the vanilla red one. Vanilla spawns its
 * particles from private static helpers, so every particle-emitting path is reimplemented here
 * with {@link BluestoneParticles#BLUESTONE_DUST}.
 */
public class BluestoneOreBlock extends RedstoneOreBlock {
    public BluestoneOreBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        light(state, world, pos);
        // NOTE: intentionally do NOT call super - RedstoneOreBlock.light() (private static) spawns
        // vanilla RED particles. We replicate only the LIT toggle + blue particles here.
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.bypassesSteppingEffects()) {
            light(state, world, pos);
        }
        // no super call (see onBlockBreakStart note)
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            spawnParticles(world, pos);
        } else {
            light(state, world, pos);
        }
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.getItem() instanceof BlockItem && new ItemPlacementContext(player, hand, itemStack, hit).canPlace()) {
            return ActionResult.PASS;
        }
        return ActionResult.SUCCESS;
    }

    private static void light(BlockState state, World world, BlockPos pos) {
        spawnParticles(world, pos);
        if (!state.get(LIT)) {
            world.setBlockState(pos, state.with(LIT, true), 3);
        }
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(LIT)) {
            spawnParticles(world, pos);
        }
    }

    private static void spawnParticles(World world, BlockPos pos) {
        double d = 0.5625;
        Random random = world.random;
        for (Direction direction : Direction.values()) {
            BlockPos blockPos = pos.offset(direction);
            if (world.getBlockState(blockPos).isOpaqueFullCube(world, blockPos)) {
                continue;
            }
            Direction.Axis axis = direction.getAxis();
            double e = axis == Direction.Axis.X ? 0.5 + 0.5625 * direction.getOffsetX() : random.nextFloat();
            double f = axis == Direction.Axis.Y ? 0.5 + 0.5625 * direction.getOffsetY() : random.nextFloat();
            double g = axis == Direction.Axis.Z ? 0.5 + 0.5625 * direction.getOffsetZ() : random.nextFloat();
            world.addParticle(BluestoneParticles.BLUESTONE_DUST, pos.getX() + e, pos.getY() + f, pos.getZ() + g, 0.0, 0.0, 0.0);
        }
    }
}
