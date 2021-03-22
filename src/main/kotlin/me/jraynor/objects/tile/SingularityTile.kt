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
import net.minecraft.tileentity.ITickableTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.fml.LogicalSide
import net.minecraftforge.fml.network.NetworkEvent
import java.util.*
import kotlin.collections.HashMap

/**
 * TODO: write description
 */
@Suppress("UNREACHABLE_CODE") class SingularityTile : TileEntity(Registry.Tiles.SINGULARITY_TILE), ITickableTileEntity {
    /**This is a pointer to the graph of this tile entity**/
    var uuid: UUID? = null

    /**This simply routes the boolean to whether or not this tile is loaded**/
    val loaded: Boolean get() = isLoaded(this)

    /**This is the client equivalent of a graph**/
    var clientGraph: Graph? = null

    /**This should only ever be used when on the server**/
    private val worldData: NodeWorldData
        get() {
            assert(logicalServer)//This will throw an exception when trying to access the worldData from the client
            return WorldData[world!! as ServerWorld]
        }

    /**This will get graph for the server if present**/
    val serverGraph: Graph
        get() = worldData.getOrCreate(uuid!!) //Get's or creates the server graph data.

    /**
     * This is called 20 times per second
     */
    override fun tick() {
        /**This is called from the logical server. It should create the world data**/
        whenClient {
            if (uuid == null)
                Network.sendToServer(PacketUUIDRequest(this.pos))
            if (clientGraph != null && clientGraph!!.blockPos == null)
                clientGraph!!.blockPos = this.pos

        }
        whenServer {
            if (serverGraph.blockPos == null)
                serverGraph.blockPos = this.pos
            tickNodes()
        }
    }

    /**
     * This will tick all of the nodes.
     */
    private fun tickNodes() {
        with(serverGraph) {
            for (node in nodes) {
                if (node is TickingNode) {
                    node.doTick(world!!, this)
                }
            }
        }
    }

    /**
     * This should initialize the tile entity.
     */
    override fun onLoad() {
        super.onLoad()
        if (!loaded) {
            Network.addListeners(this)
            whenServer {
                if (uuid == null) uuid = UUID.randomUUID()
            }
            whenClient {
                Network.sendToServer(PacketUUIDRequest(this.pos))
            }
            if (load(this))
                println("Loaded singularity tile at $pos")
        }
    }

    /***
     * This will be fired on the server when the client sends an update packet
     */
    @NetEvent(LogicalSide.SERVER)
    fun onRemoveLink(packet: PacketNodeLinkRemove, context: NetworkEvent.Context): Boolean {
        with(serverGraph) {
            packet.linkId ?: return false
            val count = removeLinkById(packet.linkId!!)
            println("Unlinked link '${packet.linkId}' from $count pins!")
            return count > 0
        }
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
        with(serverGraph) {
            val node = packet.node ?: return false
            if (tryNodeMerge(node)) {
                println("Update node on server with id ${node.id}")
                worldData.markDirty()//We update the node data so we mark dirty to save.
                return true
            }
            return false
        }
    }

    /***
     * This will be fired on the server when the client requests the uuid
     */
    @NetEvent(LogicalSide.SERVER)
    fun onAddNode(packet: PacketNodeAdd, context: NetworkEvent.Context): Boolean {
        with(serverGraph) {
            if (nodes.add(packet.node!!)) {
                packet.nextNodeId?.let { nextNodeId = it }
                packet.nextPinId?.let { nextPinId = it }
                worldData.markDirty()//We update the node data so we mark dirty to save.
                return true
            }
            return false
        }
    }

    /***
     * This will be fired on the server when the client requests the uuid
     */
    @NetEvent(LogicalSide.SERVER)
    fun onNodeMoved(packet: PacketNodeMove, context: NetworkEvent.Context): Boolean {
        packet.nodeId ?: return false
        packet.x ?: return false
        packet.y ?: return false
        with(serverGraph) {
            val node = findById(packet.nodeId!!) ?: return false
            node.pos = Pair(packet.x!!, packet.y!!)
            worldData.markDirty()
            println("Moved node with id: '${packet.nodeId}'")
            return true
        }
    }

    /***
     * This will be fired on the server when the client requests the uuid
     */
    @NetEvent(LogicalSide.SERVER)
    fun onLinkNode(packet: PacketNodeLink, context: NetworkEvent.Context): Boolean {
        packet.source ?: return false
        packet.target ?: return false
        with(serverGraph) {
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
        with(serverGraph) {
            if (remove(packet.nodeId!!)) {
                worldData.markDirty()
                println("Removed node with id: '${packet.nodeId}'")
            }
        }
        return true
    }

    /***
     * This will be fired on the server when the client requests the uuid
     */
    @NetEvent(LogicalSide.SERVER)
    fun onUuidRequest(packet: PacketUUIDRequest, context: NetworkEvent.Context): Boolean {
        Network.sendToClient(PacketUUIDResponse(this.pos, this.uuid), context.sender!!)
        println("Received uuid request, and sent uuid response to client: $uuid, at ${packet.pos?.coords}")
        return true
    }

    /***
     * This will be fired on the server when the client sends an update packet
     */
    @NetEvent(LogicalSide.CLIENT)
    fun onUpdateNodeClient(packet: PacketUpdateNode, context: NetworkEvent.Context): Boolean {
        clientGraph ?: return false
        with(clientGraph!!) {
            val node = packet.node ?: return false
            if (tryNodeMerge(node))
                return true
            return false
        }
    }

    /**
     * this is called on the client when there is a uuid
     */
    @NetEvent(LogicalSide.CLIENT)
    fun onUuidResponse(packet: PacketUUIDResponse, context: NetworkEvent.Context): Boolean {
        this.uuid = packet.uuid
        Network.sendToServer(PacketGraphRequest(this.pos, this.uuid))
        println("Got uuid $uuid for $pos and sent graph request")
        return true
    }

    /**
     * This will be fired on the client when the response packet is sent.
     */
    @NetEvent(LogicalSide.CLIENT)
    fun onGraphRequest(packet: PacketGraphResponse, context: NetworkEvent.Context): Boolean {
        println("Received graph from server: ${packet.graph} at ${this.pos.coords}")
        this.clientGraph = packet.graph
        return true
    }

    /**
     * This will be called upon the request uuid
     */
    @NetEvent(LogicalSide.SERVER)
    fun onGraphResponse(packet: PacketGraphRequest, context: NetworkEvent.Context): Boolean {
        val sender = context.sender ?: return false
        with(worldData) {
            Network.sendToClient(PacketGraphResponse(this@SingularityTile.pos, getOrCreate(packet.uuid!!)), sender)
            println("Received graph sync request from client: ${packet.uuid}, at: ${packet.pos?.coords}")
        }
        return true
    }

    /**
     * This will read the uuid
     */
    override fun read(state: BlockState, tag: CompoundNBT) {
        super.read(state, tag)
        if (tag.hasUniqueId("graph_uuid"))
            this.uuid = tag.getUniqueId("graph_uuid")
        whenServer {
            if (uuid == null) uuid = UUID.randomUUID()
        }
    }

    /**
     * This will write the uuid
     */
    override fun write(compound: CompoundNBT): CompoundNBT {
        val tag = super.write(compound)
        if (uuid != null)
            tag.putUniqueId("graph_uuid", uuid!!)
        return tag
    }

    /**
     * We call this method our self when the tile is either destoryed, or when the world is unloaded.
     */
    fun onUnload() {
        if (loaded)
            Network.removeListeners(this)
    }

    /**
     * This class should object be accessed from the server
     */
    companion object {
        private val serverLoaded = HashMap<BlockPos, SingularityTile>()
        private val clientLoaded = HashMap<BlockPos, SingularityTile>()

        /**
         * This add the tile to the laoded list
         */
        private fun load(tile: SingularityTile): Boolean {
            if (isLoaded(tile) || tile.pos == null || tile.world == null) return false
            if (!tile.world!!.isRemote)
                serverLoaded[tile.pos] = tile
            else
                clientLoaded[tile.pos] = tile
            return true

        }

        /**
         * Checks to see if the given tile is loaded or not
         */
        internal fun isLoaded(tile: SingularityTile): Boolean {
            tile.pos ?: return false
            tile.world ?: return false
            if (!tile.world!!.isRemote)
                return serverLoaded.containsKey(tile.pos)
            return clientLoaded.containsKey(tile.pos)
        }

        /**
         * This will unload the given tile if successful it will return true
         */
        fun unload(tile: SingularityTile): Boolean {
            if (!isLoaded(tile) || tile.pos == null || tile.world == null) return false
            if (!tile.world!!.isRemote)
                return serverLoaded.remove(tile.pos!!) != null
            return clientLoaded.remove(tile.pos!!) != null
        }

        /**
         * This will iterate each of the loaded tiles.
         * If the returned value is true, then it will be removed.
         */
        fun forEach(side: LogicalSide, removeIf: (SingularityTile) -> Boolean) {
            when (side) {
                LogicalSide.SERVER -> {
                    val iter = serverLoaded.iterator()
                    while (iter.hasNext()) {
                        val tile = iter.next().value
                        if (removeIf(tile))
                            iter.remove()
                    }
                }
                LogicalSide.CLIENT -> {
                    val iter = clientLoaded.iterator()
                    while (iter.hasNext()) {
                        val tile = iter.next().value
                        if (removeIf(tile))
                            iter.remove()
                    }
                }
            }
        }
    }

}