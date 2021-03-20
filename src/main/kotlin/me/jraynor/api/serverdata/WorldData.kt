package me.jraynor.api.serverdata

import net.minecraft.world.server.ServerWorld

/**
 * This keeps track of the world data. It it only on the server side, if the client wants to interact with the world data,
 * they must do so through krequests and callbacks.
 */
object WorldData {
    /**This allows us to statically track the server world on the server.**/
    var world: ServerWorld? = null
    /**
     * This will get the node data for the given world. This is server sided only
     */
    operator fun get(world: ServerWorld): NodeWorldData {
        this.world = world
        return world.savedData.getOrCreate({ NodeWorldData() }, NodeWorldData.DATA_NAME)
    }


}