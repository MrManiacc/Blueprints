package me.jraynor.api.packets

import me.jraynor.api.Node
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockPos
import java.util.*

/**
 * Is sent to the server for each node. It is used to update the positions.
 */
class PacketNodeMove(
    pos: BlockPos? = null,
    uuid: UUID? = null,
    var nodeId: Int? = null,
    var x: Float? = null,
    var y: Float? = null
) : PacketBaseTile(pos, uuid) {
    /**
     * This will read the node from the memory buffer
     */
    override fun readBuffer(buf: PacketBuffer) {
        super.readBuffer(buf)
        if (buf.readBoolean())
            this.nodeId = buf.readVarInt()
        if (buf.readBoolean())
            this.x = buf.readFloat()
        if (buf.readBoolean())
            this.y = buf.readFloat()
    }

    /**
     * This will write our node to the packet buffer
     */
    override fun writeBuffer(buf: PacketBuffer) {
        super.writeBuffer(buf)
        buf.writeBoolean(nodeId != null)
        if (nodeId != null)
            buf.writeVarInt(nodeId!!)
        buf.writeBoolean(x != null)
        if (x != null)
            buf.writeFloat(x!!)
        buf.writeBoolean(y != null)
        if (y != null)
            buf.writeFloat(y!!)
    }

}