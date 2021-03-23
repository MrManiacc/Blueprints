package me.jraynor.internal

import me.jraynor.api.network.Network
import me.jraynor.api.select.PlayerHooks
import me.jraynor.imgui.Gui
import me.jraynor.objects.tile.SingularityTile
import me.jraynor.util.runOnClient
import me.jraynor.util.runOnRender
import me.jraynor.util.whenClient
import me.jraynor.util.whenServer
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent
import thedarkcolour.kotlinforforge.eventbus.KotlinEventBus

import net.minecraftforge.event.world.BlockEvent.BreakEvent
import net.minecraftforge.event.world.WorldEvent

import net.minecraftforge.fml.LogicalSide

/**
 * This will listener to the client/server specific events.
 */
internal object Listeners {

    /**
     * This will register the listeners
     */
    fun register(modBus: KotlinEventBus, forgeBus: KotlinEventBus) {
        modBus.addListener(Common::onCommonSetup)
//        forgeBus.addListener(Common::onBlockBreak)
        modBus.addListener(Common::onLoadCComplete)
        forgeBus.addListener(Common::onWorldUnload)
        PlayerHooks.register(modBus, forgeBus)
    }

    /**
     * This will register the client eventsl
     */
    internal object Client {

    }

    /**
     * This will register the client events
     */
    internal object Common {

        /**
         * This will create the imgui instance
         */
        internal fun onLoadCComplete(event: FMLLoadCompleteEvent) {
            whenClient(logical = false) {
                runOnRender {
                    Gui.init()
                }
            }
        }

        /**
         * This will destory the current imgui instance
         */
        internal fun onWorldUnload(event: WorldEvent.Unload) {
            SingularityTile.forEach(LogicalSide.SERVER) {
                it.onUnload()
                true
            }
            SingularityTile.forEach(LogicalSide.CLIENT) {
                it.onUnload()
                true //We want to remove the tile
            }
        }

        /**
         * This is for initializing anything that's shared acorss the client
         * and server.
         */
        internal fun onCommonSetup(event: FMLCommonSetupEvent) {
            Network.initializeNetwork()
        }
    }

}