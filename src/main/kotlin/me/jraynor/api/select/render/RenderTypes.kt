@file:Suppress("INACCESSIBLE_TYPE")

package me.jraynor.api.select.render

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import me.jraynor.BlueprintMod
import net.minecraft.client.renderer.RenderState
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11
import java.util.*

/**
 * Stores various render types
 */
object RenderTypes {

    private val colorMask: RenderState.WriteMaskState = RenderState.WriteMaskState(true, false)

    private val translucentTransparency = RenderState.TransparencyState("translucent_transparency", {
        RenderSystem.enableBlend()
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        )
    }) {
        RenderSystem.disableBlend()
        RenderSystem.defaultBlendFunc()
    }

    private val viewOffsetZLayering = RenderState.LayerState("view_offset_z_layering", {
        RenderSystem.pushMatrix()
        RenderSystem.scalef(0.99975586f, 0.99975586f, 0.99975586f)
    }) { RenderSystem.popMatrix() }

    private val noDepth = RenderState.DepthTestState("always", GL11.GL_ALWAYS)

    private val glState = RenderType.State.getBuilder()
            .line(RenderState.LineState(OptionalDouble.of(3.0)))
            .layer(viewOffsetZLayering)
            .transparency(translucentTransparency)
            .writeMask(colorMask)
            .depthTest(noDepth)
            .build(false)

    val NO_DEPTH_LINE: RenderType = RenderType.makeType(
            "${BlueprintMod.ID}:line", DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 128, glState
    )


    val NO_DEPTH_QUAD: RenderType = RenderType.makeType(
        "${BlueprintMod.ID}:quad", DefaultVertexFormats.POSITION_COLOR, GL11.GL_QUADS, 256, glState
    )
}