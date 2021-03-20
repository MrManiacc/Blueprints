package me.jraynor.api.serverdata

import me.jraynor.BlueprintMod
import me.jraynor.api.Graph
import net.minecraft.nbt.CompoundNBT
import net.minecraft.world.storage.WorldSavedData
import java.util.*
import kotlin.collections.HashMap

/**
 * This is the actual internal storage mechanism for the node world data. It wil keep track of all of the nodes and be
 * able to be saved and loaded etc.
 */
class NodeWorldData : WorldSavedData(DATA_NAME) {
    private val graphs = HashMap<UUID, Graph>()

    /**
     * This will write out the groups data
     */
    override fun write(tag: CompoundNBT): CompoundNBT {
        tag.putInt("size", graphs.size)
        for ((i, graph) in graphs.values.withIndex())
            tag.put("graph_${i}", graph.serializeNBT())
        return tag
    }

    /**
     * reads in data from the NBTTagCompound into this MapDataBase
     */
    override fun read(tag: CompoundNBT) {
        graphs.clear()
        val size = tag.getInt("size")
        for (i in 0 until size) {
            val graphTag = tag.getCompound("graph_$i")
            val graph = Graph(parent = this)
            graph.deserializeNBT(graphTag)
            graphs[graph.uuid] = graph
        }
    }

    /**
     * This will add the given graph
     */
    fun add(graph: Graph): Graph {
        graph.parent = this
        graphs[graph.uuid] = graph
        markDirty()
        return graph
    }

    /**
     * This will get a graph by the given uuid
     */
    operator fun get(uuid: UUID): Graph? {
        return graphs[uuid]
    }

    /**
     * Gets or creates a new group with the given uuid
     */
    fun getOrCreate(uuid: UUID): Graph {
        if (graphs.containsKey(uuid)) return graphs[uuid]!!
        return add(Graph(uuid = uuid))
    }

    /**
     * This stores our name for the node data
     */
    companion object {
        const val DATA_NAME: String = "${BlueprintMod.ID}_NodeWorldData"
    }
}