package me.jraynor.api.network.packets

import me.jraynor.api.impl.*
import me.jraynor.api.network.IPacket
import net.minecraft.network.*
import net.minecraft.util.math.*

/**This is used to sync nodes from client to server and server to client**/
class PacketSyncRequest : IPacket {
    /**The position of the block attempting to sync**/
    lateinit var position: BlockPos

    /**
     * This will construct the packet from the buf.
     *
     * @param buf the buf to construct the packet from
     */
    override fun readBuffer(buf: PacketBuffer) {
        this.position = buf.readBlockPos()
    }

    /**
     * This will convert the current packet into a packet buffer.
     *
     * @param buf the buffer to convert
     */
    override fun writeBuffer(buf: PacketBuffer) {
        buf.writeBlockPos(position)

    }

    /**This allows us to check if the recieve is valid**/
    override fun validate(receiver: Any?): Boolean {
        if (receiver !is AbstractGraphTile) return false
        return receiver.pos == this.position
    }

    override fun toString(): String {
        return "PacketSyncRequest(position=$position)"
    }


}