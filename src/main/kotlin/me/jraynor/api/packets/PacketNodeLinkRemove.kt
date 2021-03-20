package me.jraynor.api.packets

import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockPos
import java.util.*

/**
 * This will be used to send the server a message saying we added a node.
 */
class PacketNodeLinkRemove(pos: BlockPos? = null, uuid: UUID? = null, var linkId: Int? = null) :
    PacketBaseTile(pos, uuid) {
    /**
     * This will read the node from the memory buffer
     */
    override fun readBuffer(buf: PacketBuffer) {
        super.readBuffer(buf)
        if (buf.readBoolean())
            this.linkId = buf.readVarInt()
    }

    /**
     * This will write our node to the packet buffer
     */
    override fun writeBuffer(buf: PacketBuffer) {
        super.writeBuffer(buf)
        buf.writeBoolean(linkId != null)
        if (linkId != null)
            buf.writeVarInt(linkId!!)
    }

}