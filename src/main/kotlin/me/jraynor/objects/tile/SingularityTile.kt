package me.jraynor.objects.tile

import me.jraynor.api.Graph
import me.jraynor.api.network.NetEvent
import me.jraynor.api.network.Network
import me.jraynor.api.nodes.TickingNode
import me.jraynor.api.packets.*
import me.jraynor.api.select.PlayerHooks
import me.jraynor.api.serverdata.NodeWorldData
import me.jraynor.api.serverdata.WorldData
import me.jraynor.internal.Registry
import me.jraynor.util.*
import net.minecraft.block.BlockState
import net.minecraft.nbt.CompoundNBT
import net.minecraft.network.NetworkManager
import net.minecraft.tileentity.ITickableTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.fml.LogicalSide
import net.minecraftforge.fml.network.NetworkEvent
import java.util.*
import net.minecraft.network.play.server.SUpdateTileEntityPacket

/**
 * TODO: write description
 */
@Suppress("UNREACHABLE_CODE") class SingularityTile : TileEntity(Registry.Tiles.SINGULARITY_TILE), ITickableTileEntity {
    /**This is a pointer to the graph of this tile entity**/
    var uuid: UUID? = null

    /**This should only ever be used when on the server**/
    private val worldData: NodeWorldData
        get() {
            return WorldData[world!! as ServerWorld]
        }
    var serverGraph: Graph? = null

    /**
     * This is called 20 times per second
     */
    override fun tick() {
        if (!world?.isRemote!!) {
            if (!Network.serverRegisteredBlocks.contains(this.pos)) {
                Network.addListeners(this)
                Network.serverRegisteredBlocks.add(this.pos)
            }
            if (serverGraph == null && uuid != null)
                serverGraph = getGraphFor(uuid!!)
            if (serverGraph != null) {
                serverGraph!!.blockPos = this.pos
                tickNodes(serverGraph!!)
            }
        }
    }

    /**
     * This will tick all of the nodes.
     */
    private fun tickNodes(graph: Graph) {
        for (node in graph.nodes) {
            node.parent = graph
            if (node is TickingNode) {
                if (node.active.get())
                    node.doTick(world!!, graph)
            }
        }
    }

    /**
     * This will get a node graph for the given uuid
     */
    private fun getGraphFor(uuid: UUID): Graph {
        assert(logicalServer)
        return worldData.getOrCreate(uuid) //Get's or creates the server graph data.
    }

    /***
     * This will be fired on the server when the client sends an update packet
     */
    @NetEvent(LogicalSide.SERVER)
    fun onRemoveLink(packet: PacketNodeLinkRemove, context: NetworkEvent.Context): Boolean {
        if (uuid != null)
            with(serverGraph!!) {
                packet.linkId ?: return false
                val count = removeLinkById(packet.linkId!!)
                println("Unlinked link '${packet.linkId}' from $count pins!")
                return count > 0
            }
        return false
    }

    /***
     * This will be fired on the server when the client sends an update packet
     */
    @NetEvent(LogicalSide.SERVER)
    fun onBlockSelect(packet: PacketSelectStart, context: NetworkEvent.Context): Boolean {
        PlayerHooks.selectingPlayers.add(packet.uuid!!)
        println("Got start selection ${packet.uuid}")
        return true
    }

    /***
     * This will be fired on the server when the client sends an update packet
     */
    @NetEvent(LogicalSide.SERVER)
    fun onUpdateNodeServer(packet: PacketUpdateNode, context: NetworkEvent.Context): Boolean {
        if (uuid != null)
            with(serverGraph!!) {
                val node = packet.node ?: return false
                node.parent = this
                if (tryNodeMerge(node)) {
                    debug("Updated node on server with id ${node.id}")
                    worldData.markDirty()//We update the node data so we mark dirty to save.
                    return true
                }
            }
        return false
    }

    /***
     * This will be fired on the server when the client requests the uuid
     */
    @NetEvent(LogicalSide.SERVER)
    fun onAddNode(packet: PacketNodeAdd, context: NetworkEvent.Context): Boolean {
        if (uuid != null)
            with(serverGraph!!) {
                if (nodes.add(packet.node!!)) {
                    packet.nextNodeId?.let { nextNodeId = it }
                    packet.nextPinId?.let { nextPinId = it }
                    worldData.markDirty()//We update the node data so we mark dirty to save.
                    return true
                }
            }
        return true
    }

    /***
     * This will be fired on the server when the client requests the uuid
     */
    @NetEvent(LogicalSide.SERVER)
    fun onNodeMoved(packet: PacketNodeMove, context: NetworkEvent.Context): Boolean {
        packet.nodeId ?: return false
        packet.x ?: return false
        packet.y ?: return false
        if (uuid != null)
            with(serverGraph!!) {
                val node = findById(packet.nodeId!!) ?: return true
                node.pos = Pair(packet.x!!, packet.y!!)
                worldData.markDirty()
                println("Moved node with id: '${packet.nodeId}'")
                return true
            }
        return true
    }

    /***
     * This will be fired on the server when the client requests the uuid
     */
    @NetEvent(LogicalSide.SERVER)
    fun onLinkNode(packet: PacketNodeLink, context: NetworkEvent.Context): Boolean {
        packet.source ?: return false
        packet.target ?: return false
        if (uuid != null)
            with(serverGraph!!) {
                val from = findByOutput(packet.source!!) ?: return true
                val to = findByInput(packet.target!!) ?: return true
                from.addLink(to)
                worldData.markDirty()
            }
        return true
    }

    /***
     * This will be fired on the server when the client requests the uuid
     */
    @NetEvent(LogicalSide.SERVER)
    fun onRemoveNode(packet: PacketNodeRemove, context: NetworkEvent.Context): Boolean {
        packet.nodeId ?: return false
        if (uuid != null)
            with(serverGraph!!) {
                this.remove(packet.nodeId!!)
                worldData.markDirty()
                println("Removed node with id: '${packet.nodeId}'")
            }
        return true
    }

    /**
     * This will be called upon the request uuid
     */
    @NetEvent(LogicalSide.SERVER)
    fun onGraphRequest(packet: PacketGraphRequest, context: NetworkEvent.Context): Boolean {
        val sender = context.sender ?: return false
        val graph = serverGraph ?: return false
        Network.sendToClient(PacketGraphResponse(this@SingularityTile.pos, graph), sender)
        return true
    }

    /**
     * This will read the uuid
     */
    override fun read(state: BlockState, tag: CompoundNBT) {
        super.read(state, tag)
        if (tag.hasUniqueId("graph_uuid"))
            this.uuid = tag.getUniqueId("graph_uuid")
    }

    /**
     * This will write the uuid
     */
    override fun write(compound: CompoundNBT): CompoundNBT {
        val tag = super.write(compound)
        if (uuid == null && logicalServer)
            uuid = UUID.randomUUID()
        if (uuid != null)
            tag.putUniqueId("graph_uuid", uuid!!)
        return tag
    }

    /**
     * This is called on the client when the chunk of the tile entity is loaded. It should hopefully read the uuid from the server
     */
    override fun onDataPacket(net: NetworkManager?, packet: SUpdateTileEntityPacket) {
        read(this.blockState, packet.nbtCompound)
        debug("Received data tag: ${packet.nbtCompound}")
        super.onDataPacket(net, packet)
    }

    /**
     * This will will send our uuid to the client upon the chunk loading it. At t
     */
    override fun getUpdateTag(): CompoundNBT {
        val syncData = CompoundNBT()
        write(syncData) //this calls writeInternal
        return syncData
    }

    /**
     * THis is used to wire out data tag
     */
    override fun getUpdatePacket(): SUpdateTileEntityPacket {
        return SUpdateTileEntityPacket(pos, 1, updateTag)
    }

    /**
     * Checks the locations and uuid's against each other
     */
    override fun equals(other: Any?): Boolean {
        if (other is SingularityTile)
            return (other.pos == this.pos && other.uuid == this.uuid)
        return super.equals(other)
    }


}