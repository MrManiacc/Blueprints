@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package me.jraynor.api.utilities.inventory

import me.jraynor.inventory.InvResult
import me.jraynor.inventory.SlotRange
import net.minecraft.item.Item

import net.minecraftforge.items.IItemHandler
import net.minecraft.item.ItemStack.areItemStacksEqual

import net.minecraft.item.ItemStack
import net.minecraft.item.ItemStack.EMPTY
import net.minecraftforge.items.ItemHandlerHelper

/**
 * This stores various utils for the inventory
 */
object InventoryUtils {
    const val SLOT_ITER_LIMIT = 256
    private val countsBuffer = HashMap<Item, Int>()

    /**
     * Tries to insert the given ItemStack stack to the target inventory.
     * The return value is a stack of the remaining items that couldn't be inserted.
     * If all items were successfully inserted, then null is returned.
     */
    fun tryInsertItemStackToInventory(inv: IItemHandler, stackIn: ItemStack): ItemStack? {
        return tryInsertItemStackToInventoryWithinSlotRange(inv, stackIn, SlotRange(inv))
    }

    /**
     * Tries to insert the given ItemStack stack to the target inventory, inside the given slot range.
     * The return value is a stack of the remaining items that couldn't be inserted.
     * If all items were successfully inserted, then null is returned.
     */
    fun tryInsertItemStackToInventoryWithinSlotRange(
        inv: IItemHandler,
        stackIn: ItemStack,
        slotRange: SlotRange
    ): ItemStack {
        var stack = stackIn
        val lastSlot = Math.min(slotRange.lastInc, inv.slots - 1)

        // First try to add to existing stacks
        for (slot in slotRange.first..lastSlot) {
            if (!inv.getStackInSlot(slot).isEmpty) {
                stack = inv.insertItem(slot, stack, false)
                if (stack.isEmpty) {
                    return EMPTY
                }
            }
        }

        // Second round, try to add to any slot
        for (slot in slotRange.first..lastSlot) {
            stack = inv.insertItem(slot, stack, false)
            if (stack.isEmpty) {
                return EMPTY
            }
        }
        return stack
    }

    /**
     * Tries to move all items from the inventory invSrc into invDst.
     */
    fun tryMoveAllItems(invSrc: IItemHandler, invDst: IItemHandler): InvResult? {
        return tryMoveAllItemsWithinSlotRange(invSrc, invDst, SlotRange(invSrc), SlotRange(invDst))
    }

    /**
     * Tries to move all items from the inventory invSrc into invDst within the provided slot range.
     */
    fun tryMoveAllItemsWithinSlotRange(
        invSrc: IItemHandler,
        invDst: IItemHandler,
        slotsSrc: SlotRange,
        slotsDst: SlotRange,
    ): InvResult {
        var movedAll = true
        var movedSome = false
        val lastSlot = slotsSrc.lastInc.coerceAtMost(invSrc.slots - 1)
        for (slot in slotsSrc.first..lastSlot) {
            var stack: ItemStack
            var limit: Int = SLOT_ITER_LIMIT
            while (limit-- > 0) {
                stack = invSrc.extractItem(slot, 64, false)
                if (stack.isEmpty) {
                    break
                }
                val origSize = stack.count
                stack = tryInsertItemStackToInventoryWithinSlotRange(invDst, stack, slotsDst)
                if (stack.isEmpty || stack.count != origSize) {
                    movedSome = true
                }

                // Can't insert anymore items
                if (!stack.isEmpty) {
                    // Put the rest of the items back to the source inventory
                    invSrc.insertItem(slot, stack, false)
                    movedAll = false
                    break
                }
            }
        }
        return if (movedAll) InvResult.MOVED_ALL else if (movedSome) InvResult.MOVED_SOME else InvResult.MOVED_NOTHING
    }

    /**
     * This will return the next item in the
     */
    fun extractNextPass(src: IItemHandler, count: Int, filters: List<String>): ItemStack {
        for (slot in 0 until src.slots) {
            val stack = src.extractItem(slot, count, false)
            val name = stack.displayName.string.toLowerCase()
            for (filter in filters)
                if (name.contains(filter.toLowerCase()))
                    return stack
            src.insertItem(slot, stack, false) //If we don't pass the filter, we put it
        }
        return EMPTY
    }

    /**
     * This will return the next item in the
     */
    fun extractNextFail(src: IItemHandler, count: Int, filters: List<String>): ItemStack {
        for (slot in 0 until src.slots) {
            val stack = src.getStackInSlot(slot)
            val name = stack.displayName.string.toLowerCase()
            var has = false
            for (filter in filters)
                if (name.contains(filter.toLowerCase())) {
                    has = true
                }
            if (!has) {
                if (!stack.isEmpty && count <= stack.count) {
                    return src.extractItem(0, count, false)
                }
            }
        }
        return EMPTY
    }

    /**
     * This will return the next item in the
     */
    fun nextItem(src: IItemHandler): ItemStack {
        for (slot in 0 until src.slots) {
            val stack = src.getStackInSlot(slot)
            if (!stack.isEmpty) return stack
        }
        return EMPTY
    }

    /**
     * This will call the [callback] for each unique item with the given count
     */
    fun forEachItem(src: IItemHandler, callback: (index: Int, total: Int, item: Item, count: Int) -> Unit) {
        countsBuffer.clear()
        for (i in 0 until src.slots) {
            val stack = src.getStackInSlot(i)
            if (!countsBuffer.containsKey(stack.item))
                countsBuffer[stack.item] = 0
            countsBuffer[stack.item] = countsBuffer[stack.item]!! + stack.count
        }
        var i = 0
        countsBuffer.forEach {
            callback(i++, countsBuffer.size, it.key, it.value)
        }
    }



    /**
     * This will try to insert the item stack into the next available slot
     */
    fun canAccept(itemStack: ItemStack, dest: IItemHandler): Boolean {
        return ItemHandlerHelper.insertItemStacked(dest, itemStack, true).isEmpty
    }

    /**
     * This will try to insert the item stack into the next available slot
     */
    fun tryInsert(itemStack: ItemStack, dest: IItemHandler): ItemStack {
        var stack = itemStack
        for (slot in 0 until dest.slots) {
            val result = dest.insertItem(slot, stack, false)
            if (result.isEmpty) return EMPTY //We inserted into the slot
            stack = result
        }
        return stack
    }

    /**
     * Tries to move all items from the inventory invSrc into invDst within the provided slot range.
     */
    fun tryMoveItemWithAmount(
        invSrc: IItemHandler,
        invDst: IItemHandler,
        slotsSrc: SlotRange,
        slotsDst: SlotRange,
        amount: Int
    ): InvResult {
        var movedAll = true
        var movedSome = false
        val lastSlot = slotsSrc.lastInc.coerceAtMost(invSrc.slots - 1)
        for (slot in slotsSrc.first..lastSlot) {
            var stack: ItemStack = invSrc.extractItem(slot, amount, false)
            if (stack.isEmpty) {
                break
            }
            val origSize = stack.count
            stack = tryInsertItemStackToInventoryWithinSlotRange(invDst, stack, slotsDst)
            if (stack.isEmpty || stack.count != origSize) {
                movedSome = true
            }
            // Can't insert anymore items
            if (!stack.isEmpty) {
                // Put the rest of the items back to the source inventory
                invSrc.insertItem(slot, stack, false)
                movedAll = false
                break
            }
        }
        return if (movedAll) InvResult.MOVED_ALL else if (movedSome) InvResult.MOVED_SOME else InvResult.MOVED_NOTHING
    }

    /**
     * Tries to move matching/existing items from the inventory invSrc into invDst within the provided slot range.
     */
    fun tryMoveMatchingItemsWithinSlotRange(
        invSrc: IItemHandler,
        invDst: IItemHandler,
        slotsSrc: SlotRange,
        slotsDst: SlotRange
    ): InvResult {
        var movedAll = true
        var movedSome = false
        var result: InvResult = InvResult.MOVED_NOTHING
        val lastSlot = Math.min(slotsSrc.lastInc, invSrc.slots - 1)
        for (slot in slotsSrc.first..lastSlot) {
            val stack = invSrc.getStackInSlot(slot)
            if (stack.isEmpty == false) {
                if (getSlotOfFirstMatchingItemStackWithinSlotRange(invDst, stack, slotsDst) !== -1) {
                    result = tryMoveAllItemsWithinSlotRange(invSrc, invDst, SlotRange(slot, 1), slotsDst)
                }
                if (result !== InvResult.MOVED_NOTHING) {
                    movedSome = true
                } else {
                    movedAll = false
                }
            }
        }
        return if (movedAll) InvResult.MOVED_ALL else if (movedSome) InvResult.MOVED_SOME else InvResult.MOVED_NOTHING
    }

    /**
     * Get the slot number of the first slot containing a matching ItemStack (including NBT, ignoring stackSize).
     * Note: stackIn can be empty.
     * @return The slot number of the first slot with a matching ItemStack, or -1 if there were no matches.
     */
    fun getSlotOfFirstMatchingItemStack(inv: IItemHandler, stackIn: ItemStack): Int {
        return getSlotOfFirstMatchingItemStackWithinSlotRange(inv, stackIn, SlotRange(inv))
    }

    /**
     * Get the slot number of the first slot containing a matching ItemStack (including NBT, ignoring stackSize) within the given slot range.
     * Note: stackIn can be empty.
     * @return The slot number of the first slot with a matching ItemStack, or -1 if there were no matches.
     */
    fun getSlotOfFirstMatchingItemStackWithinSlotRange(
        inv: IItemHandler,
        stackIn: ItemStack,
        slotRange: SlotRange
    ): Int {
        val lastSlot = (inv.slots - 1).coerceAtMost(slotRange.lastInc)
        for (slot in slotRange.first..lastSlot) {
            val stack = inv.getStackInSlot(slot)
            if (areItemStacksEqual(stack, stackIn)) {
                return slot
            }
        }
        return -1
    }
}