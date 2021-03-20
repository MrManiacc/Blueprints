package me.jraynor.api.packets

import me.jraynor.api.Node
import me.jraynor.api.network.IPacket
import me.jraynor.util.getClass
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockPos
import java.util.*

/**
 * This will be used to send the server a message saying we added a node.
 */
class PacketNodeAdd(
    pos: BlockPos? = null,
    uuid: UUID? = null, var node: Node? = null,
    var nextNodeId: Int? = null,
    var nextPinId: Int? = null,
) : PacketBaseTile(pos, uuid) {
    /**
     * This will read the node from the memory buffer
     */
    override fun readBuffer(buf: PacketBuffer) {
        super.readBuffer(buf)
        if (buf.readBoolean()) {
            val tag = buf.readCompoundTag()!!
            val type = tag.getClass("node_type")
            if (Node::class.java.isAssignableFrom(type)) {
                val node = type.newInstance()
                if (node != null && node is Node) {
                    node.deserializeNBT(tag)
                    this.node = node
                }
            }
        }
        if (buf.readBoolean())
            this.nextNodeId = buf.readVarInt()

        if (buf.readBoolean())
            this.nextPinId = buf.readVarInt()

    }

    /**
     * This will write our node to the packet buffer
     */
    override fun writeBuffer(buf: PacketBuffer) {
        super.writeBuffer(buf)
        buf.writeBoolean(node != null)
        if (node != null)
            buf.writeCompoundTag(node!!.serializeNBT())
        buf.writeBoolean(nextNodeId != null)
        if (nextNodeId != null)
            buf.writeVarInt(nextNodeId!!)
        buf.writeBoolean(nextPinId != null)
        if (nextPinId != null)
            buf.writeVarInt(nextPinId!!)
    }

}