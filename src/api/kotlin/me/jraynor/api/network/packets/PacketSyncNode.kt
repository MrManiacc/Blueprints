package me.jraynor.api.network.packets

import me.jraynor.api.impl.*
import me.jraynor.api.network.IPacket
import me.jraynor.api.structure.*
import me.jraynor.api.utilities.*
import net.minecraft.network.*
import net.minecraft.util.math.*

/**This is used to sync nodes from client to server and server to client**/
class PacketSyncNode : IPacket {
    /**The position of the block attempting to sync**/
    lateinit var position: BlockPos

    /**The node to send to the client/server**/
    lateinit var node: INode

    /**
     * This will construct the packet from the buf.
     *
     * @param buf the buf to construct the packet from
     */
    override fun readBuffer(buf: PacketBuffer) {
        this.position = buf.readBlockPos()
        val cls = buf.readClass() ?: error("Attempted to read invalid class for PacketSyncNode!")
        val node = cls.newInstance()
        if (node is INode) {
            this.node = node
            buf.readCompoundTag()?.let { this.node.deserializeNBT(it) }
        }
    }

    /**
     * This will convert the current packet into a packet buffer.
     *
     * @param buf the buffer to convert
     */
    override fun writeBuffer(buf: PacketBuffer) {
        buf.writeBlockPos(position)
        buf.writeClass(node::class.java)
        buf.writeCompoundTag(node.serializeNBT())
    }

    /**This allows us to check if the recieve is valid**/
    override fun validate(receiver: Any?): Boolean {
        if (receiver !is AbstractGraphTile) return false
        return receiver.pos == this.position
    }

    override fun toString(): String {
        return "PacketSyncNode(position=$position, node=$node)"
    }


}