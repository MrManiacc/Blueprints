package me.jraynor.objects.tile

import me.jraynor.api.impl.*
import me.jraynor.api.network.*
import me.jraynor.api.network.packets.*
import me.jraynor.api.utilities.enums.*
import me.jraynor.internal.Registry
import me.jraynor.nodes.*
import net.minecraftforge.fml.*
import net.minecraftforge.fml.network.*

/**
 * TODO: write description
 */
@Suppress("UNREACHABLE_CODE") class SingularityTile : AbstractGraphTile(Registry.Tiles.SINGULARITY_TILE) {
    /**This will be called any time we are syncing from client to server. **/
    @NetEvent(LogicalSide.SERVER)
    fun onClientRequestSync(packet: PacketSyncRequest, context: NetworkEvent.Context): Boolean {
        println("Received sync request packet from client: $packet")
        pushUpdate(Side.Client)
        return true
    }

    /**This will be called any time we are syncing from client to server. **/
    @NetEvent(LogicalSide.CLIENT)
    fun onServerRequestSync(packet: PacketSyncRequest, context: NetworkEvent.Context): Boolean {
        println("Received sync request packet from server: $packet")
        pushUpdate(Side.Server)
        return true
    }

    /**This will be called any time we are syncing from client to server. **/
    @NetEvent(LogicalSide.SERVER)
    fun onServerGraphSync(packet: PacketSyncGraph, context: NetworkEvent.Context): Boolean {
        println("Received sync packet from client: $packet")
        val graph = this.nodeGraph.resolve().get()
        graph.deserializeNBT(packet.graph.serializeNBT())
        graph.side = Side.Server
        graph.parent = this
        markDirty()
        return true
    }

    /**This will be called any time we are syncing from client to server. **/
    @NetEvent(LogicalSide.CLIENT)
    fun onClientGraphSync(packet: PacketSyncGraph, context: NetworkEvent.Context): Boolean {
        println("Received sync packet from server: $packet")
        val graph = this.nodeGraph.resolve().get()
        graph.deserializeNBT(packet.graph.serializeNBT())
        graph.side = Side.Client
        graph.parent = this
        return true
    }

    /**This will be called any time we are syncing from client to server. **/
    @NetEvent(LogicalSide.CLIENT)
    fun onServerNodeSync(packet: PacketSyncNode, context: NetworkEvent.Context): Boolean {
        val graph = this.nodeGraph.resolve().get()
        if (!graph.hasNode(packet.node.nodeId)) {
            graph.add(packet.node)
            println("Added new node on client: ${packet.node}")
        } else {
            //This will essentially merge the nodes
            graph.findNode(packet.node.nodeId).deserializeNBT(packet.node.serializeNBT())
            println("Merged nodes on client: ${packet.node}")
        }
        return true
    }

    /**This will be called any time we are syncing from client to server. **/
    @NetEvent(LogicalSide.SERVER)
    fun onClientNodeSync(packet: PacketSyncNode, context: NetworkEvent.Context): Boolean {
        val graph = this.nodeGraph.resolve().get()
        if (!graph.hasNode(packet.node.nodeId)) {
            graph.add(packet.node)
            println("Added new node on server: ${packet.node}")
        } else {
            //This will essentially merge the nodes
            graph.findNode(packet.node.nodeId).deserializeNBT(packet.node.serializeNBT())
            println("Merged nodes on server: ${packet.node}")
        }
        return true
    }

    /**Handles all of the logic for the tile entity**/
    override fun tick() {
        if (!intiailzied) {
            installListeners()
            if (world?.isRemote == true) requestUpdate(Side.Server) //We are asking the server to update our graph capability
            intiailzied = true
        }
        processNodes()
    }

    /**updates/ processes all of the nodes**/
    private fun processNodes() {
        nodeGraph.ifPresent { graph ->
            graph.parent = this
            if (world!!.isRemote) graph.side = Side.Client
            else graph.side = Side.Server
            graph.nodes.forEach {
                if (it.graph.isEmpty())
                    it.graph = graph
                if (!world!!.isRemote) {
                    if (it is TickNode)
                        it.tick()
                }
            }
        }
    }
}