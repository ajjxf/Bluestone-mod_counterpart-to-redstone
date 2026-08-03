package com.bluestone.mixin;

/**
 * Unused placeholder. Activation is handled by {@link RedstoneViewMixin} (interface mixin on
 * RedstoneView), not a World mixin (World does not declare isEmittingRedstonePower /
 * isReceivingRedstonePower - they are interface defaults).
 */
final class WorldMixin {
    private WorldMixin() {}
}
