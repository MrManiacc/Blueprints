package me.jraynor.api.extensions

import imgui.ImGui
import me.jraynor.api.Node
import me.jraynor.api.extensions.INodeExtension
import me.jraynor.api.extensions.Callback

/**
 * This type of node is used for automation tasks that require a fake player.
 */
interface InputOutputExt : INodeExtension {

    /**
     * This will create a new use item renderer.
     */
    private fun renderProperties(node: Node) {
        ImGui.text("This is an input output node!")
    }
}