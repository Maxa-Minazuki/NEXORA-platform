package com.nexora.inventory.entity;

/** Which replenishment Strategy a product uses to compute its reorder quantity. */
public enum ReplenishmentPolicy {
    TOP_UP_TO_TARGET, FIXED_LOT, DEMAND_BASED
}
