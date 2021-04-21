package me.jraynor.internal

import me.jraynor.*
import me.jraynor.api.capability.*
import me.jraynor.api.network.*
import me.jraynor.api.render.*
import me.jraynor.api.utilities.*
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent
import thedarkcolour.kotlinforforge.eventbus.KotlinEventBus

/**
 * This will listener to the client/server specific events.
 */
internal object Listeners {

    /**
     * This will register the listeners
     */
    fun register(modBus: KotlinEventBus, forgeBus: KotlinEventBus) {
        modBus.addListener(::onCommonSetup)
        modBus.addListener(::onLoadComplete)
    }

    /**
     * This will create the imgui instance
     */
    private fun onLoadComplete(event: FMLLoadCompleteEvent) {
        whenClient(logical = false) {
            runOnRender {
                UserInterface.init()
            }
        }
    }

    /**
     * This is for initializing anything that's shared acorss the client
     * and server.
     */
    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        Network.initialize(Bpm.ID)
        CapabilityGraphHandler.register()
    }

}