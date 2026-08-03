package com.bluestone.registry;

import com.bluestone.BluestoneMod;
import com.bluestone.item.BluestoneTorchItem;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    // The dust item is registered under id "bluestone" (matching vanilla redstone/redstone_wire split:
    // block id is bluestone_wire, item id is bluestone). No recipe (creative-only seed item).
    public static final BlockItem BLUESTONE = registerBlockItem("bluestone", ModBlocks.BLUESTONE_WIRE);
    public static final BlockItem PURPLESTONE = registerBlockItem("purplestone", ModBlocks.PURPLESTONE_WIRE);
    public static final BluestoneTorchItem BLUESTONE_TORCH = registerTorchItem("bluestone_torch", ModBlocks.BLUESTONE_TORCH);
    public static final BlockItem BLUESTONE_REPEATER = registerBlockItem("bluestone_repeater", ModBlocks.BLUESTONE_REPEATER);
    public static final BlockItem BLUESTONE_COMPARATOR = registerBlockItem("bluestone_comparator", ModBlocks.BLUESTONE_COMPARATOR);
    public static final BlockItem CONVERTER_REPEATER = registerBlockItem("converter_repeater", ModBlocks.CONVERTER_REPEATER);
    public static final BlockItem BLUESTONE_BLOCK = registerBlockItem("bluestone_block", ModBlocks.BLUESTONE_BLOCK);
    public static final BlockItem BLUESTONE_ORE = registerBlockItem("bluestone_ore", ModBlocks.BLUESTONE_ORE);
    public static final BlockItem DEEPSLATE_BLUESTONE_ORE = registerBlockItem("deepslate_bluestone_ore", ModBlocks.DEEPSLATE_BLUESTONE_ORE);

    private static BlockItem registerBlockItem(String name, Block block) {
        BlockItem item = new BlockItem(block, new Item.Settings());
        Registry.register(Registries.ITEM, new Identifier(BluestoneMod.MOD_ID, name), item);
        return item;
    }

    private static BluestoneTorchItem registerTorchItem(String name, Block block) {
        BluestoneTorchItem item = new BluestoneTorchItem(block, new Item.Settings());
        Registry.register(Registries.ITEM, new Identifier(BluestoneMod.MOD_ID, name), item);
        return item;
    }

    public static void initialize() {
        // Force class load so items register.
    }
}
