package me.jraynor.event

import imgui.*
import me.jraynor.api.impl.*
import me.jraynor.api.structure.*
import me.jraynor.api.utilities.enums.*

/**This is used for setting up call backs for pinsl**/
class EventPin(
    eventName: String = "event",
    override var icon: IconType = IconType.SQUARE,
    override var baseColor: Int = ImColor.intToColor(255, 255, 255)
) :
    Pin(label = eventName, inputOutput = InputOutput.Input) {

    /**This fires an event and returns the expected type**/
    inline fun <reified D : Any, reified R : Any> fireEvent(graph: IGraph, event: Event<D, R>): R? {
        val parent = graph.findNode(this.nodeId)
        if (parent.isEmpty()) return null
        if (parent is IEventReceiver) {
            parent.onEvent(event)
            return event.result
        }
        return null
    }


}