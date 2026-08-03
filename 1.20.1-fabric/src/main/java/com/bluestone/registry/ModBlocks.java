package com.bluestone.registry;

import com.bluestone.BluestoneMod;
import com.bluestone.block.BluestoneBlock;
import com.bluestone.block.BluestoneComparatorBlock;
import com.bluestone.block.BluestoneOreBlock;
import com.bluestone.block.BluestoneRepeaterBlock;
import com.bluestone.block.BluestoneTorchBlock;
import com.bluestone.block.BluestoneWallTorchBlock;
import com.bluestone.block.ConverterRepeaterBlock;
import com.bluestone.block.PurplestoneWireBlock;
import com.bluestone.block.BluestoneWireBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.Instrument;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final BluestoneWireBlock BLUESTONE_WIRE = register("bluestone_wire",
            new BluestoneWireBlock(AbstractBlock.Settings.copy(Blocks.REDSTONE_WIRE).mapColor(MapColor.BLUE)));
    public static final PurplestoneWireBlock PURPLESTONE_WIRE = register("purplestone_wire",
            new PurplestoneWireBlock(AbstractBlock.Settings.copy(Blocks.REDSTONE_WIRE).mapColor(MapColor.PURPLE)));
    public static final BluestoneRepeaterBlock BLUESTONE_REPEATER = register("bluestone_repeater",
            new BluestoneRepeaterBlock(AbstractBlock.Settings.copy(Blocks.REPEATER).mapColor(MapColor.BLUE)));
    public static final BluestoneComparatorBlock BLUESTONE_COMPARATOR = register("bluestone_comparator",
            new BluestoneComparatorBlock(AbstractBlock.Settings.copy(Blocks.COMPARATOR).mapColor(MapColor.BLUE)));
    public static final ConverterRepeaterBlock CONVERTER_REPEATER = register("converter_repeater",
            new ConverterRepeaterBlock(AbstractBlock.Settings.copy(Blocks.REPEATER).mapColor(MapColor.PURPLE)));
    public static final BluestoneTorchBlock BLUESTONE_TORCH = register("bluestone_torch",
            new BluestoneTorchBlock(AbstractBlock.Settings.copy(Blocks.REDSTONE_TORCH).mapColor(MapColor.BLUE)));
    public static final BluestoneWallTorchBlock BLUESTONE_WALL_TORCH = register("bluestone_wall_torch",
            new BluestoneWallTorchBlock(AbstractBlock.Settings.copy(Blocks.REDSTONE_WALL_TORCH).mapColor(MapColor.BLUE)));
    public static final BluestoneBlock BLUESTONE_BLOCK = register("bluestone_block",
            new BluestoneBlock(AbstractBlock.Settings.copy(Blocks.REDSTONE_BLOCK).mapColor(MapColor.BLUE)));
    // Ores - exact mirrors of vanilla redstone ore registration (lit light 9, iron pickaxe, 3.0 / 4.5 strength).
    public static final BluestoneOreBlock BLUESTONE_ORE = register("bluestone_ore",
            new BluestoneOreBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.STONE_GRAY)
                    .instrument(Instrument.BASEDRUM)
                    .requiresTool()
                    .ticksRandomly()
                    .luminance(Blocks.createLightLevelFromLitBlockState(9))
                    .strength(3.0f, 3.0f)));
    public static final BluestoneOreBlock DEEPSLATE_BLUESTONE_ORE = register("deepslate_bluestone_ore",
            new BluestoneOreBlock(AbstractBlock.Settings.copy(BLUESTONE_ORE)
                    .mapColor(MapColor.DEEPSLATE_GRAY)
                    .strength(4.5f, 3.0f)
                    .sounds(BlockSoundGroup.DEEPSLATE)));

    private static <T extends Block> T register(String name, T block) {
        return Registry.register(Registries.BLOCK, new Identifier(BluestoneMod.MOD_ID, name), block);
    }

    public static void initialize() {
        // Force class load so blocks register; items are registered in ModItems.
        ModItems.initialize();
    }
}
