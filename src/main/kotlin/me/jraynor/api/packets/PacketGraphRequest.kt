package me.jraynor.api.packets

import me.jraynor.objects.tile.SingularityTile
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockPos
import java.util.*

/**
 * This sends a request to the server for a graph of uuid.
 */
class PacketGraphRequest(pos: BlockPos? = null, uuid: UUID? = null) : PacketBaseTile(pos, uuid)