package me.jraynor.inventory

import net.minecraftforge.items.IItemHandler


class SlotRange(
    val first: Int, val lastInc: Int, val lastExc: Int
) {
    constructor(start: Int, numSlots: Int) : this(start, start + numSlots - 1, start + numSlots)

    constructor(inv: IItemHandler) : this(0, inv.slots)

    operator fun contains(slot: Int): Boolean {
        return slot in first..lastInc
    }

    override fun toString(): String {
        return String.format("SlotRange: {first: %d, lastInc: %d, lastExc: %d}", first, lastInc, lastExc)
    }
}