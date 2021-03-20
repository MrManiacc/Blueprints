@file:Suppress("UNNECESSARY_SAFE_CALL", "UNCHECKED_CAST", "NON_EXHAUSTIVE_WHEN")

package me.jraynor.api.network

import me.jraynor.BlueprintMod
import me.jraynor.api.packets.*
import me.jraynor.util.logicalClient
import me.jraynor.util.logicalServer
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.fml.LogicalSide
import net.minecraftforge.fml.network.NetworkDirection
import net.minecraftforge.fml.network.NetworkRegistry
import net.minecraftforge.fml.network.PacketDistributor
import net.minecraftforge.fml.network.PacketDistributor.TargetPoint
import net.minecraftforge.fml.network.simple.SimpleChannel
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.function.BiConsumer

/**
 * This is responsible for registering listener and automatically parsing packet
 */
object Network {
    //    val callbacks: HashMap<Class<*>, ArrayList<(Any, NetworkEvent.Context) -> Unit>> = HashMap()
    val serverCallbacks: HashMap<Class<*>, MutableList<Pair<Method, Any?>>> = HashMap()
    val clientCallbacks: HashMap<Class<*>, MutableList<Pair<Method, Any?>>> = HashMap()
    private var INSTANCE: SimpleChannel? = null
    private var ID = 0

    /**
     * This will register lal of the packets create server instance
     */
    fun initializeNetwork() {
        INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation(BlueprintMod.ID, "bpm_network"),
            { "1.0" },
            { true }) { true }
        registerPacket(PacketGraphRequest::class.java)
        registerPacket(PacketGraphResponse::class.java)
        registerPacket(PacketUUIDRequest::class.java)
        registerPacket(PacketUUIDResponse::class.java)
        registerPacket(PacketNodeMove::class.java)
        registerPacket(PacketNodeAdd::class.java)
        registerPacket(PacketNodeLink::class.java)
        registerPacket(PacketNodeRemove::class.java)
        registerPacket(PacketUpdateNode::class.java)
        registerPacket(PacketBlockSelect::class.java)
        registerPacket(PacketSelectStart::class.java)
        registerPacket(PacketNodeLinkRemove::class.java)
    }

    /**
     * This will register a packet of a give class
     */
    private fun <T : IPacket?> registerPacket(cls: Class<T>) {
        INSTANCE?.messageBuilder(cls, ID++)
            ?.encoder { packet, buffer -> packet?.writeBuffer(buffer) }
            ?.decoder { createPacket(cls, it) }
            ?.consumer(BiConsumer { packet, ctx ->
                packet?.handle(ctx)
            })
            ?.add()
    }

    /**
     * This will generate a packet based upon the class type
     *
     * @param cls    the packet class
     * @param buffer the buffer to help generate the class with
     * @param <T>    the generic packet type
     * @return returns a packet of the given type
    </T> */
    private fun <T : IPacket?> createPacket(cls: Class<T>, buffer: PacketBuffer): T? {
        try {
            try {
                val noArgs = cls.getConstructor()
                val packet = noArgs.newInstance()
                packet?.readBuffer(buffer)
                return packet
            } catch (e: NoSuchMethodException) {
                val constructors = cls.constructors
                for (constructor in constructors) {
                    val packet = constructor.newInstance(arrayOfNulls<Any>(constructor.parameterCount)) as T
                    packet?.readBuffer(buffer)
                    return packet
                }
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        return null
    }

    fun addListener(method: Method, any: Any?) {
        if (method.isAnnotationPresent(NetEvent::class.java)) {
            method.isAccessible = true
            val netEvent = method.getAnnotation(NetEvent::class.java)
            if (!Boolean::class.java.isAssignableFrom(method.returnType)) {
                println("Tried to register listener for method '${method.name}' but failed because it's return type is invalid")
                return
            }
            if (netEvent.checkSide) {
                if (netEvent.side.isServer && !logicalServer) {
                    println("Tried to add server listener on logical client!")
                    return
                } else if (netEvent.side.isClient && !logicalClient) {
                    println("Tried to add client listener on logical server!")
                    return
                }
            }
            val type = method.parameterTypes[0]
            if (IPacket::class.java.isAssignableFrom(type)) {
                when (netEvent.side) {
                    LogicalSide.SERVER -> {
                        if (!serverCallbacks.containsKey(type))
                            serverCallbacks[type] = ArrayList()
                        if (serverCallbacks[type]?.add(Pair(method, any)) == true)
                            println("successfully registered method callback ${method.name} on the server")
                    }
                    LogicalSide.CLIENT -> {
                        if (!clientCallbacks.containsKey(type))
                            clientCallbacks[type] = ArrayList()
                        if (clientCallbacks[type]?.add(Pair(method, any)) == true)
                            println("successfully registered method callback ${method.name} on the client")
                    }
                }
            }
        }
    }

    /**
     * This will find all of the functions annotated with [NetEvent] and automatically subscribe them
     */
    fun addListeners(any: Any) {
        val cls = any::class.java
        val methods = cls.declaredMethods //Used to get the current fields
        for (method in methods) {
            addListener(method, any)
        }
    }

    /**
     * This will remove all listeners from the given instance
     */
    fun removeListeners(any: Any) {
        val cls = any::class.java
        val methods = cls.declaredMethods //Used to get the current fields
        methods.forEach {
            if (it.isAnnotationPresent(NetEvent::class.java)) {
                val netEvent = it.getAnnotation(NetEvent::class.java)
                val type = it.parameterTypes[0]
                if (IPacket::class.java.isAssignableFrom(type)) {
                    when (netEvent.side) {
                        LogicalSide.SERVER -> {
                            if (serverCallbacks.containsKey(type)) {
                                val methodList = serverCallbacks[type]!!
                                for (method in methodList)
                                    if (method.first.name.equals(it.name) && method.second == any) {
                                        methodList.remove(method)
                                        println("successfully removed method callback ${it.name} on the server")
                                        break
                                    }
                            }
                        }
                        LogicalSide.CLIENT -> {
                            if (clientCallbacks.containsKey(type)) {
                                val methodList = clientCallbacks[type]!!
                                for (method in methodList)
                                    if (method.first.name.equals(it.name) && method.second == any) {
                                        methodList.remove(method)
                                        println("successfully removed method callback ${it.name} on the client")
                                        break
                                    }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This will remove the current object from the list of registered subscribers
     *
     * @param object the object to unsubscribe
     */

    /**
     * This will send the packet directly to the given player
     *
     * @param packet the packet to send to the client
     * @param player the player to recieve the packet
     */
    fun sendToClient(packet: IPacket?, player: ServerPlayerEntity) {
        INSTANCE?.sendTo<Any>(
            packet,
            player.connection.netManager,
            NetworkDirection.PLAY_TO_CLIENT
        )
    }

    /**
     * This will broadcast to all clients.
     *
     * @param packet the packet to broadcast
     */
    fun sendToAllClients(
        packet: IPacket?
    ) {
        INSTANCE?.send<Any>(PacketDistributor.ALL.noArg(), packet)
    }

    /**
     * This will broadcast to all the clients with the specified chunk.
     *
     * @param packet the packet to send
     * @param chunk  the chunk to use
     */
    fun sendToClientsWithChunk(packet: IPacket?, chunk: Chunk?) {
        INSTANCE?.send<Any>(PacketDistributor.TRACKING_CHUNK.with { chunk }, packet)
    }

    /**
     * This will broadcast to all the clients with the specified chunk.
     *
     * @param packet the packet to send
     * @param near   The target point to use as reference for what is near
     */
    fun sendToClientsWithBlockLoaded(
        packet: IPacket,
        blockPos: BlockPos,
        world: World
    ) {
        INSTANCE?.send<Any>(PacketDistributor.TRACKING_CHUNK.with { world.getChunkAt(blockPos) }, packet)
    }

    /**
     * This will send the packet directly to the server
     *
     * @param packet the packet to be sent
     */
    fun <T : IPacket> sendToServer(packet: T) {
        INSTANCE?.sendToServer(packet)
    }
}