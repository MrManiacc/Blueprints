package me.jraynor.api.packets

import net.minecraft.util.math.BlockPos
import java.util.*

/**
 * This will request a uuid
 */
class PacketUUIDResponse(pos: BlockPos? = null, uuid: UUID? = null) : PacketBaseTile(pos, uuid)