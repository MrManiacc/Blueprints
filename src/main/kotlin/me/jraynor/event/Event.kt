package me.jraynor.event

import me.jraynor.api.structure.*

/**This is the core of the logic for this mod. All of the logic is sent via events between pins.**/
abstract class Event<D : Any, R : Any>(
    /**The pin that send the Event**/
    val sender: IPin,
    /**The passable data. This will be passed from one pin to another**/
    val data: D? = null,
    /**This is will be passed back to the original sender.**/
    var result: R? = null,
) {


    /**Simple check to see if we have data**/
    val hasData: Boolean
        get() = data != null

    /**Simple check to see if we have a result**/
    val hasResult: Boolean
        get() = result != null

}