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
        forgeBus.addListener(Common::onBlockBreak)
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
            whenClient(logical = false) {
                runOnRender {
                    Gui.destroy()
                    SingularityTile.forEach(LogicalSide.CLIENT) {
                        it.onUnload()
                        true
                    }
                }
            }
            whenServer { //If we're on the server, we want to unload all of it
                SingularityTile.forEach(LogicalSide.SERVER) {
                    it.onUnload()
                    true //We want to remove the tile
                }
            }
        }

        /**
         * This is called when a block is broken on both the client and server
         */
        internal fun onBlockBreak(event: BreakEvent) {
            val tile = event.world.getTileEntity(event.pos)
            if (tile != null && tile is SingularityTile) {
                if (tile.loaded) {
                    tile.onUnload()
                    SingularityTile.unload(tile)
                }
            }
        }

        /**
         * This will load the imgui context
         */
        internal fun onServerStopping(event: FMLServerStoppingEvent) {
            whenClient(false) { //When we're on the client instance, we will do the tasks on the client thread.
                runOnClient { //Here we make sure to run this unloading code on the client
                    Gui.destroy()
                    SingularityTile.forEach(LogicalSide.CLIENT) {
                        it.onUnload()
                        true
                    }
                }
            }
            whenServer { //If we're on the server, we want to unload all of it
                SingularityTile.forEach(LogicalSide.SERVER) {
                    it.onUnload()
                    true //We want to remove the tile
                }
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