package com.bluestone.mixin;

import net.minecraft.block.RedstoneWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the private {@code wiresGivePower} instance field of the vanilla
 * {@link RedstoneWireBlock} singleton. Purplestone (the red/blue bridge) needs to toggle this
 * field on the vanilla wire while computing its own received power, exactly as the vanilla wire
 * does internally - otherwise neighbouring redstone wires would each read the other at full power
 * (no -1 wire-to-wire decay) via {@code getEmittedRedstonePower}, forming a feedback loop at the
 * red/purple crossing.
 *
 * <p>The {@code @Accessor} annotation generates a setter named {@code bluestone$setWiresGivePower}
 * to avoid clashing with any bytecode-level field writes the compiler might synthesise.</p>
 */
@Mixin(RedstoneWireBlock.class)
public interface RedstoneWireBlockAccessor {

    @Accessor("wiresGivePower")
    void bluestone$setWiresGivePower(boolean value);

    @Accessor("wiresGivePower")
    boolean bluestone$getWiresGivePower();
}
