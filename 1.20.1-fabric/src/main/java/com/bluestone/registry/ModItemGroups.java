package com.bluestone.registry;

import com.bluestone.BluestoneMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final RegistryKey<ItemGroup> BLUESTONE_KEY =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, new Identifier(BluestoneMod.MOD_ID, "bluestone"));

    public static void initialize() {
        Registry.register(Registries.ITEM_GROUP, BLUESTONE_KEY,
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemgroup.bluestone.bluestone"))
                        .icon(() -> new ItemStack(ModItems.BLUESTONE))
                        .entries((displayContext, entries) -> {
                            entries.add(ModItems.BLUESTONE);
                            entries.add(ModItems.PURPLESTONE);
                            entries.add(ModItems.BLUESTONE_TORCH);
                            entries.add(ModItems.BLUESTONE_BLOCK);
                            entries.add(ModItems.BLUESTONE_REPEATER);
                            entries.add(ModItems.BLUESTONE_COMPARATOR);
                            entries.add(ModItems.BLUESTONE_ORE);
                            entries.add(ModItems.CONVERTER_REPEATER);
                        })
                        .build());
        // Both ores join the vanilla "Natural Blocks" page, right after the deepslate redstone ore
        // (like redstone ore); the deepslate variant lives ONLY there, the regular ore also in the
        // bluestone page. The bluestone block also joins "Building Blocks" after the redstone block.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries -> {
            entries.addAfter(new ItemStack(Items.DEEPSLATE_REDSTONE_ORE),
                    ModItems.BLUESTONE_ORE, ModItems.DEEPSLATE_BLUESTONE_ORE);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.addAfter(new ItemStack(Items.REDSTONE_BLOCK), ModItems.BLUESTONE_BLOCK);
        });
    }
}
