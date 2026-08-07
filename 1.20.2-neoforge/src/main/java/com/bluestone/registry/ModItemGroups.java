package com.bluestone.registry;

import com.bluestone.BluestoneMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registers the custom "Bluestone" creative tab and appends entries to the vanilla Natural/Building
 * Blocks tabs.
 *
 * <p>The custom tab is built with vanilla {@link CreativeModeTab#builder()} (NeoForge 1.20.2 lets
 * mod tabs live in a {@link DeferredRegister} just like blocks/items). Vanilla-tab append happens
 * via the {@link BuildCreativeModeTabContentsEvent}.</p>
 */
public final class ModItemGroups {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BluestoneMod.MOD_ID);

    public static final Supplier<CreativeModeTab> BLUESTONE = CREATIVE_MODE_TABS.register("bluestone",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemgroup.bluestone.bluestone"))
                    .icon(() -> new ItemStack(ModItems.BLUESTONE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BLUESTONE.get());
                        output.accept(ModItems.PURPLESTONE.get());
                        output.accept(ModItems.BLUESTONE_TORCH.get());
                        output.accept(ModItems.BLUESTONE_BLOCK.get());
                        output.accept(ModItems.BLUESTONE_REPEATER.get());
                        output.accept(ModItems.BLUESTONE_COMPARATOR.get());
                        output.accept(ModItems.BLUESTONE_ORE.get());
                        output.accept(ModItems.CONVERTER_REPEATER.get());
                    })
                    .build());

    private ModItemGroups() {}

    /** Attach the deferred register to the mod event bus. */
    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    /** Append our ores/block to the relevant vanilla tabs. Call from a MOD-bus subscriber. */
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        // Both ores join the vanilla "Natural Blocks" page (appended at end; the deepslate variant
        // lives ONLY here, the regular ore also appears in the bluestone page). The bluestone block
        // also joins "Building Blocks".
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(new ItemStack(ModItems.BLUESTONE_ORE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(new ItemStack(ModItems.DEEPSLATE_BLUESTONE_ORE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(new ItemStack(ModItems.BLUESTONE_BLOCK.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
