package me.jraynor.api.logic

import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandler

/**
 * [V] is the handler value it can be []
 * This represents something that can be extract from one place to another.
 * V represents the return type
 */
@FunctionalInterface
fun interface IFilter<V : Any, H : Any> {
    /**
     * This method can be applied to a extraction method to allow either canceling the extraction or allowing it base on the regex
     * the parameter [test] is what you should test against
     */
    fun filter(test: V, source: H, target: H): Return<Boolean>

    companion object {
        /**
         * This is the handler for the item filter
         */
        val PASS_ITEM_FILTER: IFilter<ItemStack, IItemHandler> = IFilter { _, _, _ ->
            Return.of(true)
        }
    }
}