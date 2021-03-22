package me.jraynor.api.extensions

import jdk.nashorn.internal.codegen.CompilerConstants
import me.jraynor.api.Graph
import me.jraynor.api.Node
import net.minecraft.nbt.CompoundNBT
import net.minecraft.world.server.ServerWorld

/***
 * This is used to define a render call type
 */
typealias Callback = (node: Node) -> Unit
/**This is called upon ticking.**/
typealias TickCallback = (world: ServerWorld, graph: Graph, node: Node) -> Unit
/**This will be used for when trying to save and load**/
typealias NBTCallback = (tag: CompoundNBT, node: Node) -> Unit

/**
 * this allows us to add things to nodes and render things
 */
interface INodeExtension {
    /**By default the name of the extenion with just be the class name**/
    val name: String
        get() = javaClass.simpleName

    /**
     * This allows us to add render functions
     */
    fun hook(
        nodeRenders: MutableList<Callback>,
        propertyRenders: MutableList<Callback>,
        pinAdds: MutableList<Callback>,
        tickCalls: MutableList<TickCallback>,
        readCalls: MutableList<NBTCallback>,
        writeCalls: MutableList<NBTCallback>
    ){}
}