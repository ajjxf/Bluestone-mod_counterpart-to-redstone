package com.bluestone.block;

import net.minecraft.state.property.IntProperty;

/**
 * State property added to the vanilla observer block (see
 * {@link com.bluestone.mixin.ObserverBlockMixin}).
 *
 * <p>Kept in a plain class outside the mixin package (mixin-package classes cannot be referenced
 * directly), and not as a field on the mixin itself (Mixin rejects non-private static fields on
 * mixin classes). Referenced by the mixin (server-side tracking) and the overridden
 * {@code assets/minecraft/blockstates/observer.json} (24 variants keyed by
 * {@code facing,powered,observed}).</p>
 */
public final class ObserverProps {
    /** 1 = the observed block at {@code pos + FACING} is a bluestone component; 0 = anything else. */
    public static final IntProperty OBSERVED = IntProperty.of("observed", 0, 1);

    private ObserverProps() {}
}
