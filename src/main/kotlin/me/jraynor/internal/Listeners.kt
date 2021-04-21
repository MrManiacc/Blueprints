package me.jraynor.internal

import me.jraynor.BlueprintMod
import me.jraynor.api.network.Network
import me.jraynor.api.select.PlayerHooks
import me.jraynor.imgui.Gui
import me.jraynor.util.runOnRender
import me.jraynor.util.whenClient
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent
import thedarkcolour.kotlinforforge.eventbus.KotlinEventBus
import net.minecraftforge.event.world.WorldEvent

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
        modBus.addListener(Common::onLoadComplete)
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
        internal fun onLoadComplete(event: FMLLoadCompleteEvent) {
            whenClient(logical = false) {
                runOnRender {
//                    with(BlueprintMod.natives) {
//                        init(Minecraft.getInstance().gameDir.toPath())
//                        extractNatives(true)
//                    }
                    Gui.init()
                }
            }
        }

        /**
         * This will destory the current imgui instance
         */
        internal fun onWorldUnload(event: WorldEvent.Unload) {
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