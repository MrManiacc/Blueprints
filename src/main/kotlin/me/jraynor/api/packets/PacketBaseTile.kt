package me.jraynor.api.packets

import me.jraynor.api.network.IPacket
import me.jraynor.objects.tile.SingularityTile
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockPos
import java.util.*

/**
 * This is used as a base for any packets that have to do with tile synchronization.
 */
open class PacketBaseTile(var pos: BlockPos? = null, var uuid: UUID? = null) : IPacket {

    /**
     * This will reads the uuid of the requested graph
     */
    override fun readBuffer(buf: PacketBuffer) {
        val hasPos = buf.readBoolean()
        val hasUUID = buf.readBoolean()
        if (hasPos)
            this.pos = buf.readBlockPos()
        if (hasUUID)
            this.uuid = buf.readUniqueId()
    }

    /**
     * This will write the uuid of the requested graph
     */
    override fun writeBuffer(buf: PacketBuffer) {
        buf.writeBoolean(pos != null)
        buf.writeBoolean(uuid != null)
        if (pos != null)
            buf.writeBlockPos(pos!!)
        if (uuid != null)
            buf.writeUniqueId(uuid!!)
    }


    /**
     * This will check to see if the packet is valid for the given object type
     */
    override fun validate(receiver: Any?): Boolean {
        if (receiver !is SingularityTile) return false
        if (this.uuid != null && receiver.uuid == this.uuid) return true //The uuid takes precedence over the pos, so we check it first.
        if (this.pos != null && this.pos == receiver.pos) return true
        return false
    }
}