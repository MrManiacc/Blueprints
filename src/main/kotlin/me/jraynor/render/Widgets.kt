package me.jraynor.render

import imgui.*
import imgui.ImGui
import imgui.flag.*
import imgui.internal.*
import me.jraynor.api.utilities.*
import me.jraynor.api.utilities.enums.*
import kotlin.math.*

/***
 * This object provides icon drawing utilities.
 */
object Widgets {

    /**This will add all of our renderers for the different icon types**/
    private val iconRenderers: MutableMap<IconType, IconRenderers> = hashMapOf(
        Pair(IconType.GRID, this::gridIcon),
        Pair(IconType.SQUARE, this::squareIcon),
        Pair(IconType.ROUND_SQUARE, this::roundedSquareIcon),
        Pair(IconType.FLOW, this::flowIcon)
    )

    /**
     * This will draw the given icon type with the given width and height and colors
     */
    fun icon(
        widthIn: Float,
        height: Float,
        type: IconType,
        filled: Boolean,
        color: Int,
        innerColor: Int,
        startX: Float = Float.MIN_VALUE,
        startY: Float = Float.MIN_VALUE
    ) {
        if (ImGui.isRectVisible(widthIn, height)) {
            if (startX != Float.MIN_VALUE)
                ImGui.setCursorPosX(startX)
            if (startY != Float.MIN_VALUE)
                ImGui.setCursorPosY(startY)
            val startPos = ImGui.getCursorScreenPos()
            startPos.x -= 2
            startPos.y -= 2
            val stopPos = ImVec2(startPos.x + widthIn, startPos.y + height)
            val drawList = ImGui.getWindowDrawList()
            drawIcon(drawList, startPos, stopPos, type, filled, color, innerColor)
            ImGui.dummy(widthIn - 4f, height - 4f)
        } else ImGui.dummy(1f, 1f)
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
        innerColor: Int
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

    /**
     * This will render the square icon
     */
    private fun flowIcon(
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
        val originScale = rectW / 24.0f
        val offsetX = 1.0f * originScale
        val offsetY = 0.0f * originScale
        val margin = (if (filled) 2.0 else 2.0) * originScale
        val rounding = 0.1f * originScale
        val tipRound = 0.7f
        val canvas = ImRect(
            ImVec2((rectX + margin + offsetX).toFloat(), (rectY + margin + offsetY).toFloat()),
            ImVec2(((rectX + rectW) - margin + offsetX).toFloat(), ((rectY + rectH) - margin + offsetY).toFloat())
        )
        val canvasX = canvas.min.x
        val canvasY = canvas.min.y
        val canvasW = canvas.max.x - canvas.min.x
        val canvasH = canvas.max.y - canvas.min.y

        val left = canvasX + canvasW * 0.5f * 0.3f
        val right = canvasX + canvasW - canvasW * 0.5f * 0.3f
        val top = canvasY + canvasH * 0.5f * 0.2f
        val bottom = canvasY + canvasH - canvasH * 0.5f * 0.2f
        val centerY = (top + bottom) * 0.5f

        val tipTop = ImVec2(canvasX + canvasW * 0.5f, top)
        val tipRight = ImVec2(right, centerY)
        val tipBottom = ImVec2(canvasX + canvasW * 0.5f, bottom)



        drawList.pathLineTo(left, top + rounding)
        drawList.pathBezierCubicCurveTo(left, top, left, top, left + rounding, top)
        drawList.pathLineTo(tipTop.x, tipTop.y)
        drawList.pathLineTo(tipTop.x + (tipRight.x - tipTop.x) * tipRound, tipTop.y + (tipRight.y - tipTop.y) * tipRound)
        drawList.pathBezierCubicCurveTo(
            tipRight.x, tipRight.y, tipRight.x, tipRight.y,
            tipBottom.x + (tipRight.x - tipBottom.x) * tipRound, tipBottom.y + (tipRight.y - tipBottom.y) * tipRound
        )
        drawList.pathLineTo(tipBottom.x, tipBottom.y)
        drawList.pathLineTo(left + rounding, bottom)
        drawList.pathBezierCubicCurveTo(left, bottom, left, bottom, left, bottom - rounding)
        if (!filled) {
            drawList.pathStroke(color, ImDrawFlags.None, 2.0f * outlineScale)
            drawList.addLine(left - (outlineScale / 2.0f), top - outlineScale, left - outlineScale / 2.0f, bottom, color, 2.0f * outlineScale)
        } else
            drawList.pathFillConvex(color)
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
            drawList.addRectFilled(p0, p1, color, 0f, flags = 0xF + extractSegments)
        } else {
            val r = 0.5f * rectW / 2.0f - 0.5f
            val p0 = ImVec2(rectCenterX - r, rectCenterY - r)
            val p1 = ImVec2(rectCenterX + r, rectCenterY + r)
            drawList.addRectFilled(p0, p1, innerColor, 0f, flags = 0xF + extractSegments)
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
