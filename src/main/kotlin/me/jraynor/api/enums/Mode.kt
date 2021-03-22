package me.jraynor.api.enums

import me.jraynor.api.logic.Return
import me.jraynor.api.logic.Return.Companion.of
import me.jraynor.api.logic.IExtraction
import me.jraynor.api.logic.IFilter
import me.jraynor.api.logic.IFilter.Companion.PASS_ITEM_FILTER
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.IEnergyStorage
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.items.IItemHandler

/**
 * This is the mode of the
 */
enum class Mode(
    val min: Int,
    val initial: Int,
    val max: Int,
    val step: Int,
    val speedStep: Int,
    val label: String,
    val methodIn: IExtraction<*, Boolean, *>,
    val type: Class<*>
) {
    ITEM(0, 1, 64, 1, 10, "items/t", IExtraction.ITEM, IItemHandler::class.java),
    ENERGY(0, 2000, Int.MAX_VALUE - 1, 50, 500, "rf/t", IExtraction.ENERGY, IEnergyStorage::class.java),
    FLUID(0, 500, Int.MAX_VALUE - 1, 50, 500, "mb/t", IExtraction.FLUID, IFluidHandler::class.java);

    /**
     * This will extract the given data from into the given type
     */
    @Suppress("UNCHECKED_CAST") fun <H : Any> extract(
        type: Class<H>,
        /**This is the speed that we extract at**/
        rate: Int,
        /**This the the starting extraction position**/
        sourceIn: LazyOptional<*>,
        /**This the the starting extraction position**/
        targetIn: LazyOptional<*>,
    ): Return<Boolean> {
        val method = this.methodIn as IExtraction<H, Boolean, *>
        if (!sourceIn.isPresent || !targetIn.isPresent) return of(false)
        if (!sourceIn.resolve().isPresent || !targetIn.resolve().isPresent) return of(false)
        val source = sourceIn.resolve().get()
        val target = targetIn.resolve().get()
        return method.extract(rate, source as H, target as H, PASS_ITEM_FILTER)
    }

    /**
     * This will extract the given data from into the given type
     */
    @Suppress("UNCHECKED_CAST") fun <H : Any, V : Any> extractFiltered(
        /**The type of h, it's used to infer type**/
        type: Class<H>,
        /**This is the filter type**/
        filterType: Class<V>,
        /**This is the speed that we extract at**/
        rate: Int,
        /**This the the starting extraction position**/
        sourceIn: LazyOptional<*>,
        /**This the the starting extraction position**/
        targetIn: LazyOptional<*>,
        /**This is the filter, if it's not null we'll use it.**/
        filter: IFilter<*, *>
    ): Return<Boolean> {
        val method = this.methodIn as IExtraction<H, Boolean, V>
        if (!sourceIn.isPresent || !targetIn.isPresent) return of(false)
        if (!sourceIn.resolve().isPresent || !targetIn.resolve().isPresent) return of(false)
        val source = sourceIn.resolve().get()
        val target = targetIn.resolve().get()
        return method.extract(rate, source as H, target as H, filter as IFilter<V, H>)
    }
}