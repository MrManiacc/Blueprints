package me.jraynor.render

import imgui.*
import imgui.extension.nodeditor.*
import imgui.extension.nodeditor.flag.*
import imgui.flag.*
import me.jraynor.api.structure.*
import me.jraynor.api.utilities.enums.*
import me.jraynor.nodes.*
import java.lang.Float.*

/**This will render our nodes**/
object NodeRenderer {
    /**Buffers data from ImGui**/
    private val buffer = ImVec2()

    /**The min header buffer**/
    private var headerMin = ImVec2()

    /**The max header buffer**/
    private var headerMax = ImVec2()

    /**This is used when for when a node has content**/
    private var contentMax = ImVec2()

    /**Gets the border width of the node editor**/
    private val width: Float
        get() = NodeEditor.getStyle().nodeBorderWidth

    /**This will render a node**/
    fun render(node: INode) {

        NodeEditor.pushStyleVar(NodeEditorStyleVar.NodePadding, 8f, 4f, 8f, 2f)
        NodeEditor.pushStyleColor(NodeEditorStyleColor.NodeBg, 0.2705f, 0.2705f, 0.2705f, 1f)
        NodeEditor.beginNode(node.nodeId.toLong())
        renderHeader(node)
        if (node !is ContentNode) renderPins(node)
        else {
            headerMax.x = node.renderContent(headerMax.x)
            contentMax = ImGui.getItemRectMax()
            if (node.pins.isNotEmpty())
                renderPins(node)
        }
        NodeEditor.endNode()
        NodeEditor.popStyleVar(1)
        NodeEditor.popStyleColor(1)
        renderNode(node)
    }

    /**Renders a node's header**/
    private fun renderHeader(node: INode) {
        ImGui.calcTextSize(buffer, node.title)
        var maxX = -1f
        if (node is ContentNode) {
            if (node.headerContentSpacing >= 0) {
                val pos = ImGui.getCursorScreenPos()
                ImGui.getWindowDrawList().addText(pos.x, pos.y + 2, ImColor.rgbToColor(node.titleColor), node.title)
                ImGui.dummy((buffer.x - ImGui.getStyle().cellPadding.x * 2) + node.headerContentSpacing, buffer.y)
                ImGui.sameLine()
                node.renderHeaderContent()
                maxX = ImGui.getItemRectMaxX()
            }
        } else {
            ImGui.textColored(ImColor.rgbToColor(node.titleColor), node.title)
        }
        ImGui.dummy(buffer.x, 2f) //Invisible sizing?
        headerMin = ImGui.getItemRectMin()
        headerMax = ImGui.getItemRectMax()
        if (maxX != -1f)
            headerMax.x = maxX
    }

    /**Renders the actual node**/
    private fun renderNode(node: INode) {
        val alpha = (ImGui.getStyle().alpha * 255).toInt()
        if (ImGui.isItemVisible()) {
            val drawList = NodeEditor.getNodeBackgroundDrawList(node.nodeId.toLong())
            val offset = 0.5f
            val x = NodeEditor.getNodePositionX(node.nodeId.toLong())
            val y = NodeEditor.getNodePositionY(node.nodeId.toLong())
            if ((headerMax.x > headerMin.x) && (headerMax.y > headerMin.y)) {
                drawList.addLine(
                    headerMin.x - (8 - width * 0.5f),
                    headerMin.y - 0.5f,
                    headerMax.x + (8 - (width)),
                    headerMin.y - 0.5f,
                    ImColor.intToColor(255, 255, 255, 93 * alpha / (3 * 255)),
                    1.0f
                )
                drawList.addRectFilled(
                    x + (width * 0.5f),
                    y + (width * 0.5f),
                    headerMax.x + (8 - (width * 0.5f)),
                    (headerMin.y - offset),
                    ImColor.rgbToColor(node.headerColor),
                    NodeEditor.getStyle().nodeRounding,
                    ImDrawFlags.RoundCornersTop
                )


            }

        }
    }

    /**Starts the area for where input will be rendered*/
    private fun renderPins(node: INode) {
        val inputX = ImGui.getCursorPosX()
        var outputX = 0f
        var hasOutput = false
        node.pins.forEachIndexed { i, it ->
            if (it.inputOutput == InputOutput.Input) {
                val text = it.label.substringBefore("##")
                ImGui.calcTextSize(buffer, text)
                val size = 24 + buffer.x  //The computed size.
                if (size > outputX) {
                    outputX = size
                    if (i < node.pins.size - 1) {
                        val next = node.pins[i + 1]
                        val nextText = next.label.substringBefore("##")
                        if (next.inputOutput == InputOutput.Output) {
                            ImGui.calcTextSize(buffer, nextText)
                            val nextSize = 24 + buffer.x //The computed size.
                            outputX += nextSize
                        }
                    }
                }
            } else if (it.inputOutput == InputOutput.Output) {
                hasOutput = true
            }
        }
        if (node is ContentNode) {
            val start = NodeEditor.getNodePositionX(node.nodeId.toLong())
            outputX = (max(this.headerMax.x, this.contentMax.x) - start) - 24
        }
        outputX += inputX
        var lastType = InputOutput.None

        node.pins.forEach {
            if (it.inputOutput == InputOutput.Input) {
                renderInput(inputX, it, node)
                lastType = InputOutput.Input
            } else if (it.inputOutput == InputOutput.Output) {
                renderOutput(outputX, lastType, it, node)
                lastType = InputOutput.Output
            }
        }
        if (!hasOutput) {
            ImGui.dummy(outputX - inputX, 1f)
            val max = ImGui.getItemRectMaxX()
            if (max > this.headerMax.x)
                this.headerMax.x = max
        }
    }

    /**This will render a given pin**/
    private fun renderInput(x: Float, pin: IPin, node: INode) {
        NodeEditor.beginPin(pin.pinId.toLong(), NodeEditorPinKind.Input)
        val pos = ImGui.getCursorScreenPos()
        val text = pin.label.substringBefore("##")
        if (text.isNotBlank())
            ImGui.getWindowDrawList().addText(pos.x + 22, pos.y + 2, pin.labelColor, text)
        Widgets.icon(
            24f,
            24f,
            pin.icon,
            node.graph.hasActiveLinks(pin.pinId),
            pin.baseColor,
            pin.innerColor,
            startX = x
        )
        NodeEditor.endPin()
    }

    /**This will render a given pin**/
    private fun renderOutput(x: Float, last: InputOutput, pin: IPin, node: INode) {
        if (last == InputOutput.Input)
            ImGui.sameLine()
        NodeEditor.beginPin(pin.pinId.toLong(), NodeEditorPinKind.Output)
        val text = pin.label.substringBefore("##")
        ImGui.calcTextSize(buffer, text)
        val textSize = buffer.x
        if (last == InputOutput.None) {
            val header = ((headerMax.x - headerMin.x)) - 4 - 24
            ImGui.dummy(header, 1f)
            val max = ImGui.getItemRectMaxX()
            if (max > this.headerMax.x)
                this.headerMax.x = max
            ImGui.sameLine()
            Widgets.icon(
                24f,
                24f,
                pin.icon,
                pin.links.isNotEmpty(),
                pin.baseColor,
                pin.innerColor
            )
        } else {
            Widgets.icon(
                24f,
                24f,
                pin.icon,
                pin.links.isNotEmpty(),
                pin.baseColor,
                pin.innerColor,
                startX = x
            )
        }
        val min = ImGui.getItemRectMin()
        val max = ImGui.getItemRectMax()
        NodeEditor.pinRect(min.x, min.y, max.x, max.y)
        if (text.isNotBlank())
            ImGui.getWindowDrawList().addText(min.x - textSize, min.y + 2, pin.labelColor, text)
        if (max.x > this.headerMax.x)
            this.headerMax.x = max.x
        NodeEditor.endPin()
    }


}