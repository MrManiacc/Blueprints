package me.jraynor.api

import me.jraynor.api.enums.IO
import me.jraynor.api.serverdata.NodeWorldData
import me.jraynor.util.getClass
import me.jraynor.util.getUUID
import me.jraynor.util.putClass
import me.jraynor.util.putUUID
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.util.INBTSerializable
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashSet

/**
 * This represents a group of nodes. It has a unique uuid and can also have a display name.
 */
data class Graph(
    /**This is used to idenfity the group uniqueuly. If not specified it will be randomly generated**/
    var uuid: UUID = UUID.randomUUID(),
    /**Used for sending the packets**/
    var blockPos: BlockPos? = null,
    /**This will always be present**/
    var parent: NodeWorldData? = null
) : INBTSerializable<CompoundNBT> {
    /**These nodes can be used on the client and server for rendering**/
    val nodes = ArrayList<Node>()

    /**Stores the next id for the node**/
    var nextNodeId: Int = 1

    /**Stores the next id for the pin**/
    var nextPinId = 20_000

    /**
     * This will write the group
     */
    override fun serializeNBT(): CompoundNBT {
        val tag = CompoundNBT()
        tag.putUUID("uuid", uuid)
        tag.putInt("size", nodes.size)
        tag.putInt("next_node_id", nextNodeId)
        tag.putInt("next_pin_id", nextPinId)
        for (i in 0 until nodes.size) {
            tag.putClass("node_class_$i", nodes[i].javaClass)
            tag.put("node_$i", nodes[i].serializeNBT())
        }
        return tag
    }

    /**
     * This will read the group from the tag
     */
    override fun deserializeNBT(tag: CompoundNBT) {
        this.uuid = tag.getUUID("uuid") ?: return
        this.nextNodeId = tag.getInt("next_node_id")
        this.nextPinId = tag.getInt("next_pin_id")
        this.nodes.clear()
        val size = tag.getInt("size")
        for (i in 0 until size) {
            val cls = tag.getClass("node_class_$i")
            if (Node::class.java.isAssignableFrom(cls)) {
                val node = cls.newInstance() as Node
                node.deserializeNBT(tag.getCompound("node_$i"))
                add(node)
            }
        }
    }

    /**
     * This will create a new node of the given type and with set the appropriate node id and pin ids
     */
    inline fun <reified T : Node> createNode(
        noinline updateCallback: ((Node) -> Unit)? = null,
        noinline blockCallback: ((Node) -> Unit)? = null
    ): T {
        val cls = T::class.java
        val node = cls.newInstance()
        if (node is Node) {
            node.parent = this
            if (updateCallback != null)
                node.updateCallback = updateCallback
            if (blockCallback != null)
                node.blockCallback = blockCallback
            node.addPins()
        }
        return node
    }

    /**
     * This should remove all links with the given id
     */
    fun removeLinkById(linkId: Int): Int {
        var removeCount = 0
        for (node in nodes)
            for (pin in node.pins)
                if (pin.id == linkId)
                    if (pin.links.isNotEmpty()) {
                        pin.links.clear()
                        removeCount++
                    }

        return removeCount
    }

    /**
     * Finds a node via it's output
     */
    fun findById(id: Int): Node? {
        for (node in nodes)
            if (node.id == id)
                return node
        return null
    }

    /**
     * returns true if we have a node by the given id
     */
    fun hasNode(nodeId: Int): Boolean {
        return findById(nodeId) != null
    }

    /**
     * Finds a node via it's output
     */
    fun findByLinkId(linkId: Int): Pin? {
        for (node in nodes)
            for (port in node.pins)
                if (port.linkIds.containsKey(linkId))
                    return port
        return null
    }

    /**
     * Finds a node via it's output
     */
    fun findPinById(pinId: Int): Pin? {
        for (node in nodes)
            for (port in node.pins)
                if (port.id == pinId)
                    return port
        return null
    }

    /**
     * Finds a node via it's output
     */
    fun findNodeByPinId(pinId: Int): Node? {
        for (node in nodes)
            for (port in node.pins)
                if (port.id == pinId)
                    return node
        return null
    }

    /**
     * Finds a node via it's output
     */
    fun findInputByLinkId(linkId: Int): Pin? {
        for (node in nodes)
            for (port in node.pins)
                if (port.io == IO.INPUT) {
                    if (port.linkIds.contains(linkId))
                        return port
                }
        return null
    }

    /**
     * Finds a node via it's output
     */
    fun findByOutput(outputPinId: Int): Pin? {
        for (node in nodes)
            for (port in node.pins)
                if (port.io == IO.OUTPUT && port.id == outputPinId)
                    return port
        return null
    }

    /**
     * returns true if we have a pin by the given id with the output type
     */
    fun hasPinOut(pinId: Int): Boolean {
        return findByOutput(pinId) != null
    }

    /**
     * Finds a node via it's output
     */
    fun findByInput(inputPinId: Int): Pin? {
        for (node in nodes)
            for (port in node.pins)
                if (port.io == IO.INPUT && port.id == inputPinId)
                    return port
        return null
    }

    /**
     * returns true if we have a pin by the given id with the output type
     */
    fun hasPinInput(pinId: Int): Boolean {
        return findByInput(pinId) != null
    }

    /**
     * This will find  the pin from the input pin id
     */
    private fun findPinsConnectedTo(inputPinID: Int): MutableSet<Pin> {
        val output = HashSet<Pin>()
        for (node in nodes) {
            for (port in node.pins) {
                if (output.contains(port)) continue
                for (link in port.links) {
                    if (link == inputPinID) {
                        output.add(port)
                        continue
                    }
                }
            }
        }
        return output
    }

    /**
     * This will merge the data from the nodeIn with node of the given id here.
     */
    fun tryNodeMerge(nodeIn: Node): Boolean {
        val nodeId = nodeIn.id ?: return false
        val node = findById(nodeId) ?: return false
        node.deserializeNBT(nodeIn.serializeNBT()) //This overrides the data  inside the node
        node.onAdd(this)
        return true
    }

    /**
     * This will add the given node.
     */
    fun add(node: Node) {
        if (node.id == null)
            node.id = nextNodeId++
        for (pin in node.pins)
            if (pin.id == null)
                pin.id = nextPinId++
        nodes.add(node)
        node.onAdd(this)
        parent?.markDirty()
    }

    /**
     * This will remove a node
     */
    fun remove(nodeId: Int): Boolean {
        val node = findById(nodeId) ?: return false
        remove(node)
        return true
    }

    /**
     * This will remove a node
     */
    fun remove(toRemove: Node) {
        toRemove.pins.forEach { pin ->
            pin.id ?: return
            val connected = findPinsConnectedTo(pin.id!!)
            connected.forEach {
                it.links.remove(pin.id)
            }
        }
        nodes.remove(toRemove)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("${this.nodes.size}")
        this.nodes.forEach {
            sb.append("$it, ")
        }
        return "UUID: $uuid, nodes: $sb"
    }
}