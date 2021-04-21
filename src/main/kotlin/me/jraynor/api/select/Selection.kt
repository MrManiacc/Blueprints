package me.jraynor.api.select

import me.jraynor.api.Node
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos

/**
 * This stores the current selection of node. It's used for the callback to update the node when the selection is
 * completed
 */
data class Selection(val blockPos: BlockPos, val face: Direction, val forNode: Node)