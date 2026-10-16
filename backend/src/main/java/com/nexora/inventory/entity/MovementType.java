package com.nexora.inventory.entity;

public enum MovementType {
    /** Goods received from a supplier (+). */
    RECEIPT,
    /** Damaged, expired or consumed goods written off (-). */
    WRITE_OFF,
    /** Manual correction after a stock count (+/-). */
    ADJUSTMENT,
    /** Opening balance when a product is created. */
    INITIAL
}
