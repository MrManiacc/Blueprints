package me.jraynor.api.capability

import me.jraynor.api.impl.*
import me.jraynor.api.structure.*
import net.minecraft.nbt.*
import net.minecraft.util.*
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.items.*
import java.util.concurrent.Callable

/***
 * This allows us to attach graphs to pretty much anything. We can attach a graph to an entity, so you can have a local
 * version of a graph. We can attach a graph to an item, allowing for some interesting stuff, but most importantly, we can attach
 * a graph to a block
 */
class CapabilityGraphHandler : Capability.IStorage<IGraph> {

    /**
     * Serialize the capability instance to a NBTTag.
     * This allows for a central implementation of saving the data.
     *
     * It is important to note that it is up to the API defining
     * the capability what requirements the 'instance' value must have.
     *
     * Due to the possibility of manipulating internal data, some
     * implementations MAY require that the 'instance' be an instance
     * of the 'default' implementation.
     *
     * Review the API docs for more info.
     *
     * @param capability The Capability being stored.
     * @param instance An instance of that capabilities interface.
     * @param side The side of the object the instance is associated with.
     * @return a NBT holding the data. Null if no data needs to be stored.
     */
    override fun writeNBT(capability: Capability<IGraph>, instance: IGraph, side: Direction): INBT {
        return instance.serializeNBT()
    }

    /**
     * Read the capability instance from a NBT tag.
     *
     * This allows for a central implementation of saving the data.
     *
     * It is important to note that it is up to the API defining
     * the capability what requirements the 'instance' value must have.
     *
     * Due to the possibility of manipulating internal data, some
     * implementations MAY require that the 'instance' be an instance
     * of the 'default' implementation.
     *
     * Review the API docs for more info.         *
     *
     * @param capability The Capability being stored.
     * @param instance An instance of that capabilities interface.
     * @param side The side of the object the instance is associated with.
     * @param tag A NBT holding the data. Must not be null, as doesn't make sense to call this function with nothing to read...
     */
    override fun readNBT(capability: Capability<IGraph>, instance: IGraph, side: Direction, tag: INBT) {
        if (tag is CompoundNBT) {
            instance.deserializeNBT(tag)
        } else error("Attempted to read from non CompoundTag for graph capability")
    }

    companion object {
        @CapabilityInject(IGraph::class) @JvmStatic lateinit var GRAPH_CAPABILITY: Capability<IGraph>

        /**registers the capability**/
        fun register() = CapabilityManager.INSTANCE.register(IGraph::class.java, CapabilityGraphHandler()) { GraphImpl() }

    }
}