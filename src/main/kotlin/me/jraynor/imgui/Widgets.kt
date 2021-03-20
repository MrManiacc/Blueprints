package me.jraynor.imgui

import imgui.*
import imgui.flag.ImDrawCornerFlags
import imgui.internal.ImRect
import me.jraynor.api.Node
import me.jraynor.api.Pin
import me.jraynor.api.enums.IconType
import me.jraynor.util.IconRenderers
import me.jraynor.util.addRect
import me.jraynor.util.addRectFilled
import kotlin.math.ceil
import kotlin.math.floor

/***
 * This object provides icon drawing utilities.
 */
object Widgets {

    private val bufferSize: ImVec2 = ImVec2()

    /**This will add all of our renderers for the different icon types**/
    private val iconRenderers: MutableMap<IconType, IconRenderers> = hashMapOf(
        Pair(IconType.GRID, this::gridIcon),
        Pair(IconType.SQUARE, this::squareIcon),
        Pair(IconType.ROUND_SQUARE, this::roundedSquareIcon)
    )

    /**
     * This will draw a tree node with the given type
     */
    fun treeNode(container: Node, text: String) {

    }

    /**
     * This will draw the given icon type with the given width and height and colors
     */
    fun icon(
        pin: Pin,
        widthIn: Float,
        height: Float,
        type: IconType,
        filled: Boolean,
        color: Int,
        innerColor: Int,
        text: String,
        textAfter: Boolean,
        computeText: Boolean
    ) {
        if (computeText) {
            ImGui.calcTextSize(bufferSize, text)
            ImGui.indent(bufferSize.x)
        }
        pin.beginPin()
        if (ImGui.isRectVisible(widthIn, height)) {
            val startPos = ImGui.getCursorScreenPos()
            val stopPos = ImVec2(startPos.x + widthIn, startPos.y + height)
            val drawList = ImGui.getWindowDrawList()
            drawIcon(drawList, startPos, stopPos, type, filled, color, innerColor, text, textAfter)
        }
        ImGui.dummy(widthIn, height)
        pin.endPin()
        if (computeText)
            ImGui.unindent(bufferSize.x)
    }

    /**
     * This will draw the icon
     */
    private fun drawIcon(
        drawList: ImDrawList,
        min: ImVec2,
        max: ImVec2,
        type: IconType,
        filled: Boolean,
        color: Int,
        innerColor: Int,
        text: String? = null,
        textAfter: Boolean = true
    ) {
        val rect = ImRect(min, max)
        val rectX = rect.min.x
        val rectY = rect.min.y
        val rectW = rect.max.x - rect.min.x
        val rectH = rect.max.y - rect.min.y
        val rectCenterX = (rect.min.x + rect.max.x) * 0.5f
        val rectCenterY = (rect.min.y + rect.max.y) * 0.5f
        val outlineScale = rectW / 24.0f
        val extraSegments: Int = (2 * outlineScale).toInt() //TODO: maybe check for round to int
        if (text != null) {
            if (textAfter) {
                /**This will render the icon**/
                iconRenderers[type]?.let {
                    it(
                        drawList,
                        rectX,
                        rectY,
                        rectW,
                        rectH,
                        rectCenterX,
                        rectCenterY,
                        outlineScale,
                        extraSegments,
                        filled,
                        color,
                        innerColor
                    )
                }
                drawList.addText(rectX + rectW, rectY + 5, innerColor, text)
            } else {
                val size = ImVec2()
                ImGui.calcTextSize(size, text)
                drawList.addText(rectX - size.x, rectY + 5, innerColor, text)
                /**This will render the icon**/
                iconRenderers[type]?.let {
                    it(
                        drawList,
                        rectX + size.x,
                        rectY,
                        rectW,
                        rectH,
                        rectCenterX,
                        rectCenterY,
                        outlineScale,
                        extraSegments,
                        filled,
                        color,
                        innerColor
                    )
                }
            }
        } else {
            /**This will render the icon**/
            iconRenderers[type]?.let {
                it(
                    drawList,
                    rectX,
                    rectY,
                    rectW,
                    rectH,
                    rectCenterX,
                    rectCenterY,
                    outlineScale,
                    extraSegments,
                    filled,
                    color,
                    innerColor
                )
            }
        }
    }

    /**
     * This will render the square icon
     */
    private fun gridIcon(
        drawList: ImDrawList,
        rectX: Float,
        rectY: Float,
        rectW: Float,
        rectH: Float,
        rectCenterX: Float,
        rectCenterY: Float,
        outlineScale: Float,
        extractSegments: Int,
        filled: Boolean,
        color: Int,
        innerColor: Int
    ) {
        val r = 0.5f * rectW / 2.0f
        val w = ceil(r / 3.0f)

        val baseTl = ImVec2(floor(rectCenterX - w * 2.5f), floor(rectCenterY - w * 2.5f))
        val baseBr = ImVec2(floor(baseTl.x + w), floor(baseTl.y + w))

        val tl = ImVec2(baseTl)
        val br = ImVec2(baseBr)
        for (i in 0 until 3) {
            tl.x = baseTl.x
            br.x = baseBr.x
            drawList.addRectFilled(tl, br, color)
            tl.x += w * 2
            br.x += w * 2
            if (i != 1 || filled)
                drawList.addRectFilled(tl, br, color)
            tl.x += w * 2
            br.x += w * 2
            drawList.addRectFilled(tl, br, color)

            tl.y += w * 2
            br.y += w * 2
        }
    }

    /**
     * This will render the square icon
     */
    private fun squareIcon(
        drawList: ImDrawList,
        rectX: Float,
        rectY: Float,
        rectW: Float,
        rectH: Float,
        rectCenterX: Float,
        rectCenterY: Float,
        outlineScale: Float,
        extractSegments: Int,
        filled: Boolean,
        color: Int,
        innerColor: Int
    ) {
        if (filled) {
            val r = 0.5f * rectW / 2.0f
            val p0 = ImVec2(rectCenterX - r, rectCenterY - r)
            val p1 = ImVec2(rectCenterX + r, rectCenterY + r)
            drawList.addRectFilled(p0, p1, color, 0f, flags = ImDrawCornerFlags.All + extractSegments)
        } else {
            val r = 0.5f * rectW / 2.0f - 0.5f
            val p0 = ImVec2(rectCenterX - r, rectCenterY - r)
            val p1 = ImVec2(rectCenterX + r, rectCenterY + r)
            drawList.addRectFilled(p0, p1, innerColor, 0f, flags = ImDrawCornerFlags.All + extractSegments)
            drawList.addRect(p0, p1, color, 0f, 2.0f * outlineScale)
        }
    }

    /**
     * This will render the square icon
     */
    private fun roundedSquareIcon(
        drawList: ImDrawList,
        rectX: Float,
        rectY: Float,
        rectW: Float,
        rectH: Float,
        rectCenterX: Float,
        rectCenterY: Float,
        outlineScale: Float,
        extractSegments: Int,
        filled: Boolean,
        color: Int,
        innerColor: Int
    ) {
        if (filled) {
            val r = 0.5f * rectW / 2.0f
            val cr = r * 0.5f
            val p0 = ImVec2(rectCenterX - r, rectCenterY - r)
            val p1 = ImVec2(rectCenterX + r, rectCenterY + r)
            drawList.addRectFilled(p0, p1, color, cr)
        } else {
            val r = 0.5f * rectW / 2.0f - 0.5f
            val cr = r * 0.5f
            val p0 = ImVec2(rectCenterX - r, rectCenterY - r)
            val p1 = ImVec2(rectCenterX + r, rectCenterY + r)
            drawList.addRectFilled(p0, p1, innerColor, cr)
            drawList.addRect(p0, p1, color, cr, 2.0f * outlineScale)
        }
    }
}
