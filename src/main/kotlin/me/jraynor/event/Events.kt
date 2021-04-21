package me.jraynor.event

import me.jraynor.api.structure.*
import me.jraynor.nodes.*
import net.minecraft.util.*
import net.minecraft.util.math.*

/**Stores all of our node events**/
object Events {
    /**Used for when we are ticking**/
    class TickEvent(sender: IPin) : Event<Void, Void>(sender)

    /**Used to filter nodes upon extraction**/
    class FilterEvent(sender: IPin, data: FilterData) : Event<FilterEvent.FilterData, Boolean>(sender, data = data) {
        /**The data that will be sent from the [ExtractNode] to the [FilterNode]**/
        data class FilterData(val position: BlockPos, val face: Direction, val type: ExtractNode.ExtractType)
    }
}
