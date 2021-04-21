@file:Suppress("NON_EXHAUSTIVE_WHEN")

package me.jraynor.api.network

import net.minecraft.network.PacketBuffer
import net.minecraftforge.fml.network.NetworkDirection
import net.minecraftforge.fml.network.NetworkEvent
import java.util.function.Supplier

interface IPacket {
    /**
     * This will construct the packet from the buf.
     *
     * @param buf the buf to construct the packet from
     */
    fun readBuffer(buf: PacketBuffer) {}

    /**
     * This will convert the current packet into a packet buffer.
     *
     * @param buf the buffer to convert
     */
    fun writeBuffer(buf: PacketBuffer) {}

    /**
     * This will reroute the event back to the network so there can be subscribers for when this packet is handles.
     *
     * @param ctx the current network context
     * @return the handle state, true if successful
     */
    fun handle(ctx: Supplier<NetworkEvent.Context>) {
        var serverHandled = false
        var clientHandled = false
        ctx.get().enqueueWork {
            when (ctx.get().direction) {
                NetworkDirection.PLAY_TO_CLIENT -> {
                    if (Network.clientCallbacks.containsKey(javaClass)) {
                        for (it in Network.clientCallbacks[javaClass]!!)
                            if (validate(it.second) && !clientHandled)
                                if (it.first.invoke(it.second, this, ctx.get()) as Boolean) {
                                    clientHandled = true
                                    break
                                }
                        if (clientHandled)
                            ctx.get().packetHandled = true
                    }
                }
                NetworkDirection.PLAY_TO_SERVER -> {
                    if (Network.serverCallbacks.containsKey(javaClass)) {
                        for (it in Network.serverCallbacks[javaClass]!!)

                            if (validate(it.second) && !serverHandled)
                                if (it.first.invoke(it.second, this, ctx.get()) as Boolean) {
                                    serverHandled = true
                                    break
                                }
                        if (serverHandled)
                            ctx.get().packetHandled = true
                    }
                }
            }
            ctx.get().packetHandled = true
        }
    }

    /**
     * This will check to see if the packet is valid for the given object type
     */
    fun validate(receiver: Any?): Boolean {
        return true
    }

}