package me.jraynor.api.impl

import me.jraynor.api.network.*
import me.jraynor.api.network.packets.*
import me.jraynor.api.structure.*
import me.jraynor.api.utilities.enums.*
import net.minecraft.tileentity.*
import kotlin.reflect.*

/**This is the base node. All nodes should in theory extend from this however, they do not need to. **/
open class BaseNode(
    override var graph: IGraph = IGraph.Empty,
    override var nodeId: Int = 0,
    override var title: String = "",
    override var graphX: Float = 0.0f,
    override var graphY: Float = 0.0f,
    override val pins: MutableList<IPin> = ArrayList(),
    override var titleColor: String = "#ffffff",
    override var headerColor: String = "#8c8c8c"
) : INode {
    /**Caches all of the pins by their id**/
    private val pinIdCache = HashMap<Int, IPin>()

    /**Caches pins by their label**/
    private val pinLabelCache = HashMap<String, IPin>()

    /**Caches the given pin types**/
    private val pinTypeCache = HashMap<KClass<out IPin>, IPin>()

    /**Adds the given pin**/
    protected fun addPin(pin: IPin): IPin {
        pin.nodeId = this.nodeId
        pins.add(pin)
        return pin
    }

    /**Sorts the pins so it's always one input, then output, then input etc.*/
    private fun sortPins(): List<IPin> {
        val array = ArrayList<IPin>()
        var last: IPin = IPin.Empty
        while (pins.isNotEmpty()) {
            val pin = pins.removeLastOrNull() ?: return array
            if (pin.inputOutput == InputOutput.Input && (last.isEmpty() || (last.inputOutput == InputOutput.Output || pins.isEmpty()))) {
                array.add(pin) //Add only if we're the first element, or if the last pin was output
                last = pin
            }
            if (pin.inputOutput == InputOutput.Output && (last.isEmpty() || (last.inputOutput == InputOutput.Input || pins.isEmpty()))) {
                array.add(pin)
                last = pin
            }
        }
        return array
    }

    /**True if the pin with the given name is present**/
    override fun hasPin(pinId: Int): Boolean {
        if (pinIdCache.containsKey(pinId)) return true
        for (pin in pins) {
            if (pin.pinId == pinId) {
                pinIdCache[pinId] = pin
                return true
            }
        }
        return false
    }

    /**True if the pin with the given name is present**/
    override fun hasPin(name: String): Boolean {
        if (pinLabelCache.containsKey(name)) return true
        for (pin in pins) {
            if (pin.label == name) {
                pinLabelCache[name] = pin
                return true
            }
        }
        return false
    }

    /**True if the pin with the given name is present**/
    override fun hasPin(type: KClass<out IPin>): Boolean {
        if (pinTypeCache.containsKey(type)) return true
        for (pin in pins) {
            if (type.isInstance(pin)) {
                pinTypeCache[type] = pin
                return true
            }
        }
        return false
    }

    /**Gets the pin with the given name**/
    override fun getPin(pinId: Int): IPin {
        if (pinIdCache.containsKey(pinId))
            return pinIdCache[pinId]!!
        for (pin in pins) {
            if (pin.pinId == pinId) {
                pinIdCache[pinId] = pin
                return pin
            }
        }
        return IPin.Empty
    }

    /**Gets the pin with the given name**/
    override fun getPin(name: String): IPin {
        if (pinLabelCache.containsKey(name)) return pinLabelCache[name]!!
        for (pin in pins) {
            pin.nodeId = nodeId
            if (pin.label == name) {
                pinLabelCache[name] = pin
                return pin
            }
        }
        return IPin.Empty
    }

    /**Gets the pin with the given name**/
    override fun <T : IPin> getPin(type: KClass<T>): T {
        if (pinTypeCache.containsKey(type)) return pinTypeCache[type]!! as T
        for (pin in pins) {
            pin.nodeId = nodeId
            if (type.isInstance(pin)) {
                pinTypeCache[type] = pin
                return pin as T
            }
        }
        return IPin.Empty as T
    }

    /**True if one of our pins has the given node type linked**/
    override fun <T : INode> hasLinkedNode(type: KClass<T>): Boolean {
        pins.filter { it.inputOutput == InputOutput.Output }.forEach { output ->
            output.links.forEach { link ->
                val node = graph.findNode(link.nodeId)
                if (!node.isEmpty() && type.isInstance(node))
                    return true
            }
        }
        return false
    }

    /**This should find and get the linked node or null**/
    override fun <T : INode> getLinkedNode(type: KClass<T>): T? {
        pins.filter { it.inputOutput == InputOutput.Output }.forEach { output ->
            output.links.forEach { link ->
                val node = graph.findNode(link.nodeId)
                if (!node.isEmpty() && type.isInstance(node))
                    return node as T
            }
        }
        return null
    }

    /**This will push an update for the specific node to the opposite graph. I.E if on client we will push an update
     * to the server for this node, and vice verses.**/
    override fun pushUpdate() {
        val parent = graph.parent
        if (parent is TileEntity)
            when (graph.side) {
                Side.Client -> Network.sendToServer(PacketSyncNode().apply {
                    this.node = this@BaseNode; this.position = parent.pos
                })
                Side.Server -> Network.sendToClientsWithBlockLoaded(PacketSyncNode().apply {
                    this.node = this@BaseNode; this.position = parent.pos
                }, parent.pos, parent.world!!)
                else -> error("Attempted to push update for node ${this.javaClass.simpleName}(title=$title, nodeId=$nodeId), but the graph's side is set to neither!")
            }
        else
            error("Tried to push update for unsupported graph parent. Currently only tile entities can hold graph capabilities")
    }

    override fun toString(): String =
        "${javaClass.simpleName}(nodeId=$nodeId, title='$title', graphX=$graphX, graphY=$graphY, pins=$pins)"


}