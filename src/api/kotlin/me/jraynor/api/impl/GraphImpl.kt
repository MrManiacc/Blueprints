package me.jraynor.api.impl

import me.jraynor.api.structure.*
import me.jraynor.api.utilities.enums.*
import net.minecraft.entity.*
import net.minecraftforge.common.capabilities.*

/***
 * The internal implementation for a graph
 */
class GraphImpl() :
    IGraph {
    /**Keep track of the current side we're on.**/
    override var side: Side = Side.Neither

    /**The parent of this graph, normally it will be a tile entity**/
    override var parent: CapabilityProvider<*>? = null

    /**Our internal list of nodes. **/
    override val nodes: MutableList<INode> = ArrayList()

    /**Allows us to keep track of the last used id**/
    private var nextId: Int = 0

    /**Caches node to their node id**/
    private val nodeIdCache = HashMap<Int, INode>()

    /**Caches pins with their pin id**/
    private val pinIdCache = HashMap<Int, IPin>()

    /**Caches all of the nodes based upon the pin id. **/
    private val pinIdToNodeCache = HashMap<Int, INode>()

    /**Caches all ids.**/
    private val activeIdCache = HashSet<Int>()

    /**Caches all of the linked pins**/
    private val pinLinkCache = HashMap<Int, MutableSet<IPin>>()

    /**This will find the given node by the given id.**/
    override fun findNode(nodeId: Int): INode {
        if (nodeIdCache.containsKey(nodeId)) return nodeIdCache[nodeId]!!
        for (node in nodes) {
            if (node.nodeId == nodeId) {
                nodeIdCache[node.nodeId] = node
                return node
            }
        }
        return INode.Empty
    }

    /**This will return true if we have a node by teh given id**/
    override fun hasNode(nodeId: Int): Boolean {
        if (nodeIdCache.containsKey(nodeId)) return true
        for (node in nodes) {
            if (node.nodeId == nodeId) {
                return if (!node.isEmpty()) {
                    nodeIdCache[node.nodeId] = node
                    true
                } else false
            }
        }
        return false
    }

    /**This should find the pin with the given [pinId] or return [IPin.Empty] **/
    override fun findPin(pinId: Int): IPin {
        if (pinIdCache.containsKey(pinId)) return pinIdCache[pinId]!!
        for (node in nodes) {
            val pin = node.getPin(pinId)
            pin.nodeId = node.nodeId
            if (!pin.isEmpty()) {
                pinIdCache[pinId] = pin
                return pin
            }
        }
        return IPin.Empty
    }

    /**This will return true if we have a pin by the given id**/
    override fun hasPin(pinId: Int): Boolean {
        if (pinIdCache.containsKey(pinId)) return true
        for (node in nodes) {
            val pin = node.getPin(pinId)
            if (!pin.isEmpty()) {
                pinIdCache[pinId] = pin
                return true
            }
        }
        return false
    }

    /**This will attempt to find the node that has a pin with the given id or returns [INode.Empty].
     * It will use the io to mach the given type. If none is the [forIo] we won't check the InputOutput state
     * TODO: cache this**/
    override fun findNodeByPin(pinId: Int, forIo: InputOutput): INode {
        if (pinIdToNodeCache.containsKey(pinId)) return pinIdToNodeCache[pinId]!!
        for (node in nodes) {
            val pin = node.getPin(pinId)
            if (!pin.isEmpty() && pin.inputOutput == forIo) {
                this.pinIdToNodeCache[pinId] = node
                return node
            }
        }
        return INode.Empty
    }

    /**True if any pins are linked to the given pin**/
    override fun hasActiveLinks(pinId: Int): Boolean {
        if (pinLinkCache.containsKey(pinId)) return pinLinkCache[pinId]!!.isNotEmpty()
        val searchedNode = findNodeByPin(pinId)
        val searchedPin = findPin(pinId)
        for (node in nodes) {
            if (node.nodeId == searchedNode.nodeId) continue
            for (p in node.pins) {
                if (p.pinId != pinId)
                    if (p.hasLink(searchedPin)) {
                        pinLinkCache.getOrPut(pinId) { HashSet() }.add(p)
                    }
            }

        }
        return pinLinkCache.containsKey(pinId)
    }

    /**True if any pins are linked to the given pin**/
    override fun removeActiveLinks(pinId: Int) {
        val pin = findPin(pinId)
        pin.links.forEach {
            if (hasActiveLinks(it.pinId)) {
                val cache = pinLinkCache[it.pinId]!!
                println("Found link cache for $it: $cache")
                cache.remove(pin)
                if (cache.isEmpty())
                    pinLinkCache.remove(it.pinId)
            }
        }
    }

    /**This will check to see if the given id is in use or not. This can become quite an expensive**/
    private fun isIdUsed(id: Int): Boolean {
        if (this.activeIdCache.contains(id)) return true
        if (this.pinIdCache.containsKey(id) || nodeIdCache.containsKey(id) || pinIdToNodeCache.containsKey(id)) {
            activeIdCache.add(id)
            return true
        }
        for (node in nodes) {
            nodeIdCache[node.nodeId] = node
            if (node.nodeId == id) {
                activeIdCache.add(id)
                return true
            }
            for (pin in node.pins) {
                pin.nodeId = node.nodeId
                pinIdCache[pin.pinId] = pin
                if (pin.pinId == id) {
                    activeIdCache.add(id)
                    return true
                }
            }
        }
        return false
    }

    /**This will return the next available id. **/
    override fun nextId(): Int {
        while (isIdUsed(nextId)) {
            nextId++
        }
        return nextId
    }

    /**This will add the given node. The node shouldn't have an id when it's passed to this add method. This nodeId will be
     * added dynamically by this [IGraph]. The pins for the node too will have their id's setl.
     * @return true if removed successfully**/
    override fun add(node: INode): Boolean {
        if (node.isEmpty()) return false
        if (this.nodes.add(node)) {
            node.graph = this
            node.nodeId = nextId()
            for (pin in node.pins) {
                pin.pinId = nextId()
                pin.nodeId = node.nodeId
            }
            return true
        }
        return false
    }

    /**This will remove the given node. It will also remove the pins. It should also update the caches.
     * @return true if removed successfully**/
    override fun remove(node: INode): Boolean {
        for (pin in node.pins) {
            pinIdCache.remove(pin.pinId)
            activeIdCache.remove(pin.pinId)
            pinIdToNodeCache.remove(pin.pinId)
            this.nodes.forEach { n ->
                n.pins.forEach {
                    it.removeLink(pin)
                }
            }
            removeActiveLinks(pin.pinId)
        }
        activeIdCache.remove(node.nodeId)
        nodeIdCache.remove(node.nodeId)
        return nodes.remove(node)
    }

    /**The the to string**/
    override fun toString(): String = "Graph(nodes=$nodes)"


}