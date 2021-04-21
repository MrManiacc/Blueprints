package me.jraynor.nodes

import imgui.*
import me.jraynor.api.impl.*
import me.jraynor.api.structure.*
import me.jraynor.api.utilities.enums.*

/**This node is the base node for all tickable nodes.**/
abstract class ContentNode(
    override var title: String = "Content Node",
    override var headerColor: String = "#e36334"
) : BaseNode() {
    /**IF header content spacing is less than 0, we don't render the header content*/
    open val headerContentSpacing: Float = -1f

    /**When true we render a seperator between the content and the pins**/
    open val contentSeparator: Boolean = false

    /**This should render anything that will be put inside the header**/
    open fun renderHeaderContent() {
        ImGui.dummy(1f, 1f)
    }

    /**This will render the contents of the node. This should return the new content width, if it hasn't been updated we simply
     * return the input [contentWidth]**/
    abstract fun renderContent(contentWidth: Float): Float

}