package me.jraynor.api.network

import net.minecraftforge.fml.LogicalSide

/**
 * This is used to mark a method as a net event. The network will subscribe each method annotated with this type.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NetEvent(
    /**This will decided which listener callback to add this too**/
    val side: LogicalSide,
    /**If true the method will be checked against the current logical thread.***/
    val checkSide: Boolean = true,
)