package me.jraynor.api.packets

import me.jraynor.api.Node
import me.jraynor.api.network.IPacket
import me.jraynor.objects.tile.SingularityTile
import me.jraynor.util.getClass
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockPos
import java.util.*

/**
 * This is used as a base for any packets that have to do with tile synchronization.
 */
open class PacketBlockSelect(
    pos: BlockPos? = null,
    uuid: UUID? = null,
    var node: Node? = null
) : PacketBaseTile(pos, uuid) {

    /**
     * This will reads the uuid of the requested graph
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
    }

    /**
     * This will write the uuid of the requested graph
     */
    override fun writeBuffer(buf: PacketBuffer) {
        super.writeBuffer(buf)
        buf.writeBoolean(node != null)
        if (node != null)
            buf.writeCompoundTag(node!!.serializeNBT())
    }

}