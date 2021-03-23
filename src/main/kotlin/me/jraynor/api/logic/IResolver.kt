package me.jraynor.api.logic

import me.jraynor.api.Node
import me.jraynor.api.data.Buffers
import me.jraynor.api.enums.Mode
import me.jraynor.api.extensions.FakePlayerExt
import me.jraynor.api.extensions.SelectableBlockExt
import me.jraynor.api.nodes.BufferNode
import me.jraynor.api.nodes.HopperNode
import me.jraynor.api.nodes.LinkNode
import net.minecraft.world.World
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.energy.IEnergyStorage
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.IItemHandler
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * This will resolve the capability for the given node
 */
@FunctionalInterface
fun interface IResolver<H : Any> {

    /**
     * This should  resolve the capability for the given
     */
    fun resolve(node: Node, world: World): Return<LazyOptional<H>>

    @Suppress("UNCHECKED_CAST")
    companion object {
        /**This keeps track of each of our resolvers**/
        val resolvers: MutableMap<Class<*>, MutableList<IResolver<*>>> = HashMap()

        init {
            add(IItemHandler::class.java) { node, world ->
                if (node is FakePlayerExt)
                    return@add Return.of(LazyOptional.of { node.inventory }) //This just makes it so we can always insert.
                else if (node is SelectableBlockExt) {
                    if (node.selectedBlock != null) {
                        val tile =
                            world.getTileEntity(node.selectedBlock!!) ?: return@add Return.of(LazyOptional.empty())
                        var cap = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
                        if (node.selectedFace != null)
                            cap = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, node.selectedFace!!)
                        if (cap.isPresent) return@add Return.of(cap)
                    }
                } else if (node is BufferNode)
                    return@add Return.of(node.buffers[IItemHandler::class.java])
                else if (node is HopperNode)
                    return@add Return.of(node.buffers[IItemHandler::class.java])
                Return.of(LazyOptional.empty())
            }
            add(IEnergyStorage::class.java) { node, world ->
                if (node is SelectableBlockExt) {
                    if (node.selectedBlock != null) {
                        val tile =
                            world.getTileEntity(node.selectedBlock!!) ?: return@add Return.of(LazyOptional.empty())
                        var cap = tile.getCapability(CapabilityEnergy.ENERGY)
                        if (node.selectedFace != null)
                            cap = tile.getCapability(CapabilityEnergy.ENERGY, node.selectedFace!!)
                        if (cap.isPresent) return@add Return.of(cap)
                    }
                } else if (node is BufferNode)
                    return@add Return.of(node.buffers[IEnergyStorage::class.java])
                Return.of(LazyOptional.empty())
            }
            add(IFluidHandler::class.java) { node, world ->
                if (node is LinkNode) {
                    if (node.selectedBlock != null) {
                        val tile =
                            world.getTileEntity(node.selectedBlock!!) ?: return@add Return.of(LazyOptional.empty())
                        var cap = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
                        if (node.selectedFace != null)
                            cap =
                                tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, node.selectedFace!!)
                        if (cap.isPresent) return@add Return.of(cap)
                    }
                }
                Return.of(LazyOptional.empty())
            }
        }

        /**
         * this will add a resolver
         */
        private fun <T : Any> add(nodeType: Class<T>, resolver: IResolver<T>) {
            if (!resolvers.containsKey(nodeType))
                resolvers[nodeType] = ArrayList()
            resolvers[nodeType]!!.add(resolver)
        }

        /**
         * This will get the correct resolver method
         */
        fun <T : Any> resolve(clazz: Class<T>, node: Node, world: World): LazyOptional<T> {
            if (!resolvers.containsKey(clazz)) return LazyOptional.empty()
            for (resolver in resolvers[clazz]!!) {
                val result = resolver.resolve(node, world)
                if (result.value.isPresent)
                    return result.value as LazyOptional<T>
            }
            return LazyOptional.empty()
        }

        /**
         * This will get the correct resolver method
         */
        inline fun <reified T : Any> resolve(node: Node, world: World): LazyOptional<T> {
            return resolve(T::class.java, node, world)
        }
    }
}