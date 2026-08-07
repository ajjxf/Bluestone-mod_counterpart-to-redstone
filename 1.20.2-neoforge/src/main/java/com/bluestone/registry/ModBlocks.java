package com.bluestone.registry;

import com.bluestone.BluestoneMod;
import com.bluestone.block.BluestoneBlock;
import com.bluestone.block.BluestoneComparatorBlock;
import com.bluestone.block.BluestoneOreBlock;
import com.bluestone.block.BluestoneRepeaterBlock;
import com.bluestone.block.BluestoneTorchBlock;
import com.bluestone.block.BluestoneWallTorchBlock;
import com.bluestone.block.BluestoneWireBlock;
import com.bluestone.block.ConverterRepeaterBlock;
import com.bluestone.block.PurplestoneWireBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * All Bluestone blocks, registered via a NeoForge {@link DeferredRegister}.
 *
 * <p>The blue dust item ({@code bluestone:bluestone}) is registered against the wire block
 * ({@code bluestone_wire}) — matching the vanilla redstone/redstone_wire split. Item registration
 * happens in {@link ModItems}.</p>
 */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, BluestoneMod.MOD_ID);

    public static final Supplier<BluestoneWireBlock> BLUESTONE_WIRE =
            BLOCKS.register("bluestone_wire", () -> new BluestoneWireBlock(
                    BlockBehaviour.Properties.copy(Blocks.REDSTONE_WIRE).mapColor(MapColor.COLOR_BLUE)));
    public static final Supplier<PurplestoneWireBlock> PURPLESTONE_WIRE =
            BLOCKS.register("purplestone_wire", () -> new PurplestoneWireBlock(
                    BlockBehaviour.Properties.copy(Blocks.REDSTONE_WIRE).mapColor(MapColor.COLOR_PURPLE)));
    public static final Supplier<BluestoneRepeaterBlock> BLUESTONE_REPEATER =
            BLOCKS.register("bluestone_repeater", () -> new BluestoneRepeaterBlock(
                    BlockBehaviour.Properties.copy(Blocks.REPEATER).mapColor(MapColor.COLOR_BLUE)));
    public static final Supplier<BluestoneComparatorBlock> BLUESTONE_COMPARATOR =
            BLOCKS.register("bluestone_comparator", () -> new BluestoneComparatorBlock(
                    BlockBehaviour.Properties.copy(Blocks.COMPARATOR).mapColor(MapColor.COLOR_BLUE)));
    public static final Supplier<ConverterRepeaterBlock> CONVERTER_REPEATER =
            BLOCKS.register("converter_repeater", () -> new ConverterRepeaterBlock(
                    BlockBehaviour.Properties.copy(Blocks.REPEATER).mapColor(MapColor.COLOR_PURPLE)));
    public static final Supplier<BluestoneTorchBlock> BLUESTONE_TORCH =
            BLOCKS.register("bluestone_torch", () -> new BluestoneTorchBlock(
                    BlockBehaviour.Properties.copy(Blocks.REDSTONE_TORCH).mapColor(MapColor.COLOR_BLUE)));
    public static final Supplier<BluestoneWallTorchBlock> BLUESTONE_WALL_TORCH =
            BLOCKS.register("bluestone_wall_torch", () -> new BluestoneWallTorchBlock(
                    BlockBehaviour.Properties.copy(Blocks.REDSTONE_WALL_TORCH).mapColor(MapColor.COLOR_BLUE)));
    public static final Supplier<BluestoneBlock> BLUESTONE_BLOCK =
            BLOCKS.register("bluestone_block", () -> new BluestoneBlock(
                    BlockBehaviour.Properties.copy(Blocks.REDSTONE_BLOCK).mapColor(MapColor.COLOR_BLUE)));
    // Ores - exact mirrors of vanilla redstone ore registration (lit light 9, iron pickaxe, 3.0 / 4.5 strength).
    public static final Supplier<BluestoneOreBlock> BLUESTONE_ORE =
            BLOCKS.register("bluestone_ore", () -> new BluestoneOreBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .requiresCorrectToolForDrops()
                            .randomTicks()
                            .lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 9 : 0)
                            .strength(3.0f, 3.0f)));
    public static final Supplier<BluestoneOreBlock> DEEPSLATE_BLUESTONE_ORE =
            BLOCKS.register("deepslate_bluestone_ore", () -> new BluestoneOreBlock(
                    BlockBehaviour.Properties.copy(BLUESTONE_ORE.get())
                            .mapColor(MapColor.DEEPSLATE)
                            .strength(4.5f, 3.0f)
                            .sound(SoundType.DEEPSLATE)));

    private ModBlocks() {}

    /** Called from the mod constructor to attach the deferred register to the mod event bus. */
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
