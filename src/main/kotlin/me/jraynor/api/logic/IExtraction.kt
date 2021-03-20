package me.jraynor.api.logic

import me.jraynor.util.InventoryUtils
import net.minecraft.item.ItemStack
import net.minecraftforge.energy.IEnergyStorage
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.templates.FluidTank
import net.minecraftforge.items.IItemHandler

/**
 * [H] is the handler value it can be []
 * This represents something that can be extract from one place to another.
 * R represents the return type
 */
@FunctionalInterface
fun interface IExtraction<H : Any, R : Any, V : Any> {

    /**
     * [rate] is decided by the gui. Its the rate at which we should extract the elements per tick.
     * This will extract the the
     * [source] is the input capability. We should be able extract this value
     */
    fun extract(rate: Int, source: H, target: H, iFilter: IFilter<*, *>): Return<R>

    /**
     * This stores our different kind of default extractions.
     */
    companion object {
        /**This will keep track of the items **/
        @Suppress("UNCHECKED_CAST")
        val ITEM = IExtraction<IItemHandler, Boolean, ItemStack> { rate, source, target, filter ->
            Return.of(
                InventoryUtils.tryMoveNextItem(
                    source,
                    target,
                    rate,
                    filter as IFilter<ItemStack, IItemHandler>
                ).isEmpty
            )
        }

        /**This will keep track of the energy **/
        val ENERGY = IExtraction<IEnergyStorage, Boolean, Int> { rate, source, target, _ ->
            if (source.energyStored >= rate && source.canExtract() && target.energyStored + rate <= target.maxEnergyStored) {
                val extracted = source.extractEnergy(rate, false)
                if (target.energyStored + extracted <= target.maxEnergyStored && target.canReceive()) {
                    target.receiveEnergy(extracted, false)
                }
            }
            Return.of(false)
        }

        /**This will keep track of the energy **/
        val FLUID = IExtraction<IFluidHandler, Boolean, FluidTank> { rate, source, target, _ ->
            val simDrained = source.drain(rate, IFluidHandler.FluidAction.SIMULATE)
            if (target.fill(simDrained, IFluidHandler.FluidAction.SIMULATE) > 0) {
                val drained = source.drain(rate, IFluidHandler.FluidAction.EXECUTE)
                val filled = target.fill(drained, IFluidHandler.FluidAction.EXECUTE)
                if (filled == 0)
                    Return.of(false)
            }

            Return.of(true)
        }
    }
}