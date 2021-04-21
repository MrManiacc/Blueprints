package me.jraynor.api.structure

import imgui.*
import me.jraynor.api.utilities.*
import net.minecraft.nbt.*
import net.minecraftforge.common.util.*
import kotlin.reflect.*

/**
 * This is the base node. Every single type of node will extend off of this. This will store critical methods for things
 * such as saving and loading, which allows for serialization from the server to the client. We also much have [ImGui]
 * rendering calls, but only for the client.
 */
interface INode : INBTSerializable<CompoundNBT> {
    /**A node shouldn't be created outside of [IGraph], so we should always have a reference**/
    var graph: IGraph

    /**This can be used as way to check if the node is properly serialized/deserialized. This should be unique and always present.**/
    var nodeId: Int

    /**The label of the node.**/
    var title: String

    /**The color of the title text**/
    var titleColor: String

    /**The color, in hex string of the current title color.**/
    var headerColor: String

    /**This is used to keep track of the x position on screen of the node.**/
    var graphX: Float

    /**This is used to keep track of the y position on screen of the node.**/
    var graphY: Float

    /**This stores the pins. Pins acts as an intermediate between nodes.
     * TODO: in the implementation, add caching for searching of pins so we don't have to iterate n times**/
    val pins: MutableList<IPin>

    /**True if the pin with the given name is present**/
    fun hasPin(pinId: Int): Boolean

    /**Gets the pin with the given name**/
    fun getPin(pinId: Int): IPin

    /**True if the pin with the given name is present**/
    fun hasPin(name: String): Boolean

    /**Gets the pin with the given name**/
    fun getPin(name: String): IPin

    /**True if the pin with the given name is present**/
    fun hasPin(type: KClass<out IPin>): Boolean

    /**Gets the pin with the given name**/
    fun <T : IPin> getPin(type: KClass<T>): T

    /**True if the INode is empty**/
    fun isEmpty(): Boolean = false

    /**True if one of our pins has the given node type linked**/
    fun <T : INode> hasLinkedNode(type: KClass<T>): Boolean

    /**This should find and get the linked node or null**/
    fun <T : INode> getLinkedNode(type: KClass<T>): T?

    /**This will push an update for the specific node to the opposite graph. I.E if on client we will push an update
     * to the server for this node, and vice verses.**/
    fun pushUpdate()

    /**This is used as a way to add code to the suspend block in a node**/
    fun postProcess() {}

    /**This will write all of our base data.**/
    override fun serializeNBT(): CompoundNBT = with(CompoundNBT()) {
        putInt("nodeId", nodeId)
        putDeepList("pins", pins)
        putFloat("graphX", graphX)
        putFloat("graphY", graphY)
        putString("title", title)
        return this
    }

    /**Reads our data like the nodeId, and graphX/graphY and pins.**/
    override fun deserializeNBT(tag: CompoundNBT) {
        this.nodeId = tag.getInt("nodeId")
        with(this.pins) {
            clear()
            addAll(tag.getDeepList("pins"))
        }
        this.graphX = tag.getFloat("graphX")
        this.graphY = tag.getFloat("graphY")
        this.title = tag.getString("title")
    }

    companion object {
        /**Allows for null safety. We can always check if the [INode] is empty using this**/
        val Empty: INode = object : INode {
            override var graph: IGraph = IGraph.Empty
            override var nodeId: Int = -1
            override var title: String = ""
            override var titleColor: String = "#ffffff"
            override var headerColor: String = "#ffffff"
            override var graphX: Float = -1f
            override var graphY: Float = -1f
            override val pins: MutableList<IPin> = arrayListOf()
            override fun hasPin(pinId: Int): Boolean = false
            override fun hasPin(name: String): Boolean = false
            override fun hasPin(type: KClass<out IPin>): Boolean = false
            override fun getPin(pinId: Int): IPin = IPin.Empty
            override fun getPin(name: String): IPin = IPin.Empty
            override fun <T : IPin> getPin(type: KClass<T>): T = IPin.Empty as T
            override fun isEmpty(): Boolean = true
            override fun <T : INode> hasLinkedNode(type: KClass<T>): Boolean = false
            override fun <T : INode> getLinkedNode(type: KClass<T>): T? = null
            override fun pushUpdate() {}
        }
    }
}