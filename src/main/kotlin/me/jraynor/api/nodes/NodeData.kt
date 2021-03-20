package me.jraynor.api.nodes

import me.jraynor.api.Graph
import me.jraynor.api.Node
import me.jraynor.api.Pin
import net.minecraft.world.World

/***
 * This is used to pass along input in the on tick method.
 */
data class NodeData(val world: World, val graph: Graph, val fromNode: Node, val data: Any) {

    /**
     * This will allow to call the [block] if the given data is of the given type
     */
    inline fun <reified T : Any> casted(): T? {
        if (data is T)
            return data
        return null
    }
}
