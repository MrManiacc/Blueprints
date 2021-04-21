package me.jraynor.api.network

import net.minecraft.util.math.BlockPos

/**
 * Provides a blockPos position, this allows it to be registered inside the network with the correct position
 */
interface IBlockPosNetProvider {
    /**
     * This is used to register the methods with the position as a key so when they're unloaded, only the correct
     */
    val blockPos: BlockPos?
}