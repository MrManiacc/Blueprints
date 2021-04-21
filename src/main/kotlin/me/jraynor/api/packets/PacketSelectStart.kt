package me.jraynor.api.packets

import me.jraynor.api.network.IPacket
import net.minecraft.network.PacketBuffer
import java.util.*

/**
 * This will read and write the player's uuid
 */
class PacketSelectStart(var uuid: UUID? = null) : IPacket {

    /**
     * This will construct the packet from the buf.
     *
     * @param buf the buf to construct the packet from
     */
    override fun readBuffer(buf: PacketBuffer) {
        if (buf.readBoolean())
            this.uuid = buf.readUniqueId()
    }

    /**
     * This will convert the current packet into a packet buffer.
     *
     * @param buf the buffer to convert
     */
    override fun writeBuffer(buf: PacketBuffer) {
        buf.writeBoolean(uuid != null)
        if (uuid != null)
            buf.writeUniqueId(uuid!!)
    }
}