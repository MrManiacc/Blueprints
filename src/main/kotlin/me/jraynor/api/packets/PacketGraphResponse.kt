package me.jraynor.api.packets

import me.jraynor.api.Graph
import me.jraynor.api.network.IPacket
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockPos

/**
 * This sends a request to the server for a graph of uuid.
 */
class PacketGraphResponse(pos: BlockPos? = null, var graph: Graph? = null) : PacketBaseTile(pos) {
    /**
     * This will write the uuid of the requested graph
     */
    override fun writeBuffer(buf: PacketBuffer) {
        buf.writeBoolean(graph != null)
        graph?.let {
            buf.writeUniqueId(it.uuid)
            val graphTag = it.serializeNBT()
            buf.writeCompoundTag(graphTag)
        }
        super.writeBuffer(buf)
    }

    /**
     * This will reads the uuid of the requested graph
     */
    override fun readBuffer(buf: PacketBuffer) {
        if (buf.readBoolean()) { //this is a null safety check
            val uuid = buf.readUniqueId()
            val graphTag = buf.readCompoundTag()
            if (graph == null && graphTag != null) {
                graph = Graph(uuid = uuid)
                graph!!.deserializeNBT(graphTag)
            }
        }
        super.readBuffer(buf)
    }
}