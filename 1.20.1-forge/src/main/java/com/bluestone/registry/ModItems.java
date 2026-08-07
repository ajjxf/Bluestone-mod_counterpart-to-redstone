package com.bluestone.registry;

import com.bluestone.BluestoneMod;
import com.bluestone.item.BluestoneTorchItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BluestoneMod.MOD_ID);

    // The dust item is registered under id "bluestone" (matching vanilla redstone/redstone_wire split:
    // block id is bluestone_wire, item id is bluestone). No recipe (creative-only seed item).
    public static final RegistryObject<BlockItem> BLUESTONE = registerBlockItem("bluestone", ModBlocks.BLUESTONE_WIRE);
    public static final RegistryObject<BlockItem> PURPLESTONE = registerBlockItem("purplestone", ModBlocks.PURPLESTONE_WIRE);
    public static final RegistryObject<BluestoneTorchItem> BLUESTONE_TORCH = registerTorchItem("bluestone_torch", ModBlocks.BLUESTONE_TORCH);
    public static final RegistryObject<BlockItem> BLUESTONE_REPEATER = registerBlockItem("bluestone_repeater", ModBlocks.BLUESTONE_REPEATER);
    public static final RegistryObject<BlockItem> BLUESTONE_COMPARATOR = registerBlockItem("bluestone_comparator", ModBlocks.BLUESTONE_COMPARATOR);
    public static final RegistryObject<BlockItem> CONVERTER_REPEATER = registerBlockItem("converter_repeater", ModBlocks.CONVERTER_REPEATER);
    public static final RegistryObject<BlockItem> BLUESTONE_BLOCK = registerBlockItem("bluestone_block", ModBlocks.BLUESTONE_BLOCK);
    public static final RegistryObject<BlockItem> BLUESTONE_ORE = registerBlockItem("bluestone_ore", ModBlocks.BLUESTONE_ORE);
    public static final RegistryObject<BlockItem> DEEPSLATE_BLUESTONE_ORE = registerBlockItem("deepslate_bluestone_ore", ModBlocks.DEEPSLATE_BLUESTONE_ORE);

    private static RegistryObject<BlockItem> registerBlockItem(String name, RegistryObject<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<BluestoneTorchItem> registerTorchItem(String name, RegistryObject<? extends Block> block) {
        return ITEMS.register(name, () -> new BluestoneTorchItem(block.get(), new Item.Properties()));
    }

    private ModItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
