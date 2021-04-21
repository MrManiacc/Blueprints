package me.jraynor.api.packets

import me.jraynor.api.network.IPacket
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockPos

/**
 * This will request a uuid
 */
class PacketUUIDRequest(pos: BlockPos? = null) : PacketBaseTile(pos)