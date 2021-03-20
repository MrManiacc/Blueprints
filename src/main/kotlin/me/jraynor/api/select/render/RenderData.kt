package me.jraynor.api.select.render

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.player.ClientPlayerEntity
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.world.ClientWorld
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.vector.Matrix4f
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.math.vector.Vector3f
import net.minecraft.util.math.vector.Vector4f
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RenderWorldLastEvent
import org.lwjgl.opengl.GL11

/**
 * This simply stores all of the data needed to render
 */
data class RenderData(
    var stack: MatrixStack? = null,
    var partialTicks: Float? = null,
    var projectionMatrix: Matrix4f? = null
) {
    /**
     * Sets the render data/validates it
     */
    fun start(stack: MatrixStack, ticks: Float, matrix: Matrix4f? = null) {
        this.stack = stack
        this.partialTicks = ticks
        if (matrix != null)
            this.projectionMatrix = matrix
    }

    /**
     * This is a simple helper
     */
    fun start(event: RenderWorldLastEvent) {
        start(event.matrixStack, event.partialTicks, event.projectionMatrix)
    }

    /**
     * STops the render data/ invalidates.
     */
    fun stop() {
        stack = null
        partialTicks = null
        projectionMatrix = null
    }

    /**
     * Checks to see if everything is present, and if this is valid to render
     */
    val valid: Boolean
        get() = stack != null && partialTicks != null


    /**
     * Gets the world from the minecraft instance, this will cause problems if ran from the server
     */
    val world: ClientWorld
        get() = Minecraft.getInstance().world!!

    /**
     * Gets the local player from the minecraft instance, will cause problems if ran from the server
     */
    val player: ClientPlayerEntity
        get() = Minecraft.getInstance().player!!

    /**
     * Gets the projected view from the minecraft instance, will cause problems if ran from server
     */
    val projectedView: Vector3d
        get() = Minecraft.getInstance().gameRenderer.activeRenderInfo.projectedView


    /**
     * Gets the buffers source from the minecraft instance, will cause problems if ran from server
     */
    val buffers: IRenderTypeBuffer
        get() = Minecraft.getInstance().renderTypeBuffers.bufferSource

    /**
     * Gets the player's position
     */
    val pos: BlockPos
        get() = player.position

    /**
     * Draws a line given the start and end point, color, and transform
     */
    fun drawLine(
        start: Array<Float> = arrayOf(0f, 0f, 0f),
        stop: Array<Float> = arrayOf(0f, 1f, 0f),
        translation: Array<Double> = arrayOf(0.0, 0.0, 0.0),
        color: Array<Float> = arrayOf(1f, 0f, 0f, 1f),
        renderType: RenderType = RenderTypes.NO_DEPTH_LINE,
    ) {
        if (!valid) return
        val lineBuilder = buffers.getBuffer(renderType)
        if (start.size != 3 || stop.size != 3 || translation.size != 3 || color.size != 4) return
        stack!!.push()
        stack!!.translate(-projectedView.x, -projectedView.y, -projectedView.z)
        val matrix = stack!!.last.matrix
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        lineBuilder.pos(matrix, start[0], start[1], start[2])
            .color(color[0], color[1], color[2], 0.5f)
            .endVertex();
        lineBuilder.pos(matrix, stop[0], stop[1], stop[2])
            .color(color[0], color[1], color[2], 0.5f)
            .endVertex();
        stack!!.pop();
    }

    /**
     * Draws an overlay onto a face at a given block pos
     */
    fun drawFaceOverlay(
        posIn: BlockPos,
        direction: Direction,
        color: Vector4f = Vector4f(1.0f, 0.0f, 0.0f, 0.65f),
        renderType: RenderType = RenderTypes.NO_DEPTH_QUAD
    ) {
        if (!valid) return
        val builder = buffers.getBuffer(renderType)
        stack!!.push()
        stack!!.translate(-projectedView.x, -projectedView.y, -projectedView.z)
        val matrix = stack!!.last.matrix
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        val pos = Vector3f(posIn.x.toFloat(), posIn.y.toFloat(), posIn.z.toFloat())
        val min = Vector3f(pos.x + 0.0f, pos.y + 0.0f, pos.z + 0.0f)
        val max = Vector3f(pos.x + 1.0f, pos.y + 1.0f, pos.z + 1.0f)
        when (direction) {
            Direction.NORTH -> {
                builder.pos(matrix, min.x, max.y, min.z).color(color.x, color.y, color.z, color.w).endVertex()
                builder.pos(matrix, max.x, max.y, min.z).color(color.x, color.y, color.z, color.w).endVertex()
                builder.pos(matrix, max.x, min.y, min.z).color(color.x, color.y, color.z, color.w).endVertex()
                builder.pos(matrix, min.x, min.y, min.z).color(color.x, color.y, color.z, color.w).endVertex()
            }
            Direction.SOUTH -> {
                builder.pos(matrix, max.x, max.y, max.z).color(color.x, color.y, color.z, color.w).endVertex()
                builder.pos(matrix, min.x, max.y, max.z).color(color.x, color.y, color.z, color.w).endVertex()
                builder.pos(matrix, min.x, min.y, max.z).color(color.x, color.y, color.z, color.w).endVertex()
                builder.pos(matrix, max.x, min.y, max.z).color(color.x, color.y, color.z, color.w).endVertex()
            }
            Direction.EAST -> {
                builder.pos(matrix, max.x, max.y, min.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //top left
                builder.pos(matrix, max.x, max.y, max.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //top right
                builder.pos(matrix, max.x, min.y, max.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //bot right
                builder.pos(matrix, max.x, min.y, min.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //bot left
            }
            Direction.WEST -> {
                builder.pos(matrix, min.x, max.y, max.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //top left
                builder.pos(matrix, min.x, max.y, min.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //top right
                builder.pos(matrix, min.x, min.y, min.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //bot right
                builder.pos(matrix, min.x, min.y, max.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //bot left
            }
            Direction.UP -> {
                builder.pos(matrix, max.x, max.y, min.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //top left
                builder.pos(matrix, min.x, max.y, min.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //top right
                builder.pos(matrix, min.x, max.y, max.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //bot right
                builder.pos(matrix, max.x, max.y, max.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //bot left
            }
            Direction.DOWN -> {
                builder.pos(matrix, min.x, min.y, min.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //top left
                builder.pos(matrix, max.x, min.y, min.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //top right
                builder.pos(matrix, max.x, min.y, max.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //bot right
                builder.pos(matrix, min.x, min.y, max.z)
                    .color(color.x, color.y, color.z, color.w)
                    .endVertex() //bot left
            }
        }
        stack!!.pop();
    }


}