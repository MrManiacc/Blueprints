package me.jraynor.api.select

import me.jraynor.api.Node
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.*

/**
 * This simply stores the current selection data
 */
data class SelectionContext(
    var graphId: UUID? = null,
    var node: Node? = null,
    var callback: ((Selection) -> Unit)? = null,
    var owner: BlockPos? = null,
    var currentBlock: BlockPos? = null,
    var currentFace: Direction? = null
) {
    /**This is used to update the ray cast if valid.**/
    val valid: Boolean get() = graphId != null && node != null && callback != null

    /**This is true if the selection is read to be used**/
    val ready: Boolean get() = currentBlock != null && currentFace != null

    /**The local player, which is used for the block selection**/
    val player: PlayerEntity get() = Minecraft.getInstance().player!!

    /**This will get the local world instance**/
    val world: World get() = Minecraft.getInstance().world!!

    /**
     * This will set the selection data, making it valid.
     */
    fun new(
        graphId: UUID,
        node: Node,
        callback: ((Selection) -> Unit),
        pos: BlockPos
    ) {
        this.graphId = graphId
        this.node = node
        this.callback = callback
        this.owner = pos
    }

    /**
     * This will finish the selection data/invalidate it.
     */
    fun invalidate() {
        graphId = null
        node = null
        callback = null
        owner = null
        currentBlock = null
        currentFace = null
    }
}
