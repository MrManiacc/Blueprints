package me.jraynor.api.extensions

import com.mojang.authlib.GameProfile
import imgui.ImColor
import imgui.ImGui
import imgui.type.ImBoolean
import me.jraynor.api.Graph
import me.jraynor.api.Node
import me.jraynor.api.Pin
import me.jraynor.api.data.Buffers
import me.jraynor.api.enums.IO
import me.jraynor.api.enums.IconType
import me.jraynor.api.select.PlayerHooks
import me.jraynor.api.serverdata.WorldData
import me.jraynor.util.clear
import net.minecraft.nbt.CompoundNBT
import net.minecraft.network.IPacket
import net.minecraft.network.NetworkManager
import net.minecraft.network.PacketDirection
import net.minecraft.network.play.ServerPlayNetHandler
import net.minecraft.util.Hand
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler
import java.util.*
import kotlin.collections.HashMap

/**
 * This type of node is used for automation tasks that require a fake player.
 */
interface FakePlayerExt : INodeExtension {
    /**This is the fake player's uuid. It should be generated on the server and sent to the client**/
    var playerId: UUID

    /**
     * This will get the fake player. It will only work on the server
     */
    val player: FakePlayer?
        get() {
            WorldData.world ?: return null
            return FakePlayerExt[WorldData.world!!, playerId]
        }

    /**
     * Whether or not we should set the input
     */
    val useInputForPlacement: Boolean

    /***
     * this is the current item in the hand. It is set from the player and synchronize to the client
     */
    val inventory: Buffers.ItemHandlerBuffer

    /***When true, show the inventory**/
    val showInventory: ImBoolean

    /***
     * This will add our input pin
     */
    fun addInputPins(node: Node) {
        if (useInputForPlacement)
            node.add(
                Pin(
                    io = IO.INPUT,
                    label = "Insert",
                    textAfter = true,
                    computeText = false,
                    sameLine = false,
                    icon = IconType.GRID,
                    innerColor = ImColor.rgbToColor("#fafafa")
                )
            )
    }

    /**
     * This will read the player info for the given ndoe
     */
    fun readFakePlayer(tag: CompoundNBT, node: Node) {
        if (tag.contains("fake_player_id"))
            this.playerId = tag.getUniqueId("fake_player_id")
        else
            this.playerId = UUID.randomUUID()
        if (tag.contains("show_inventory"))
            this.showInventory.set(tag.getBoolean("show_inventory"))
        if (tag.contains("item_in_hand"))
            inventory.deserializeNBT(tag.getCompound("item_in_hand"))
        val held = inventory.getStackInSlot(0)
        val fakePlayer = player ?: return
        fakePlayer.setHeldItem(Hand.MAIN_HAND, held)
    }

    /**
     * This will read the player info for the given ndoe
     */
    fun writeFakePlayer(tag: CompoundNBT, node: Node) {
        tag.putUniqueId("fake_player_id", playerId)
        tag.put("item_in_hand", inventory.serializeNBT())
        tag.putBoolean("show_inventory", this.showInventory.get())
    }

    /**
     * This will render the debug info for the fake player extension
     */
    fun renderDebug(node: Node) {
        ImGui.text("Fake Player:")
        ImGui.separator()
        ImGui.indent()
        if (ImGui.button("clear items")) {
            inventory.clear()
            node.pushClientUpdate()
        }
        if (ImGui.button("set held item"))
            PlayerHooks.selectItem(this)
        ImGui.text("held item: ${inventory.getStackInSlot(0).item.name.string}")
        if (ImGui.checkbox("show inventory", showInventory))
            node.pushClientUpdate()
        ImGui.spacing()
        if (showInventory.get())
            inventory.render("")
        ImGui.unindent()
    }

    /**
     * This hooks our methods needed for a fake player .
     */
    override fun hook(
        nodeRenders: MutableList<Callback>,
        propertyRenders: MutableList<Callback>,
        pinAdds: MutableList<Callback>,
        tickCalls: MutableList<TickCallback>,
        readCalls: MutableList<NBTCallback>,
        writeCalls: MutableList<NBTCallback>
    ) {
        readCalls.add(this::readFakePlayer)
        writeCalls.add(this::writeFakePlayer)
        propertyRenders.add(this::renderDebug)
        pinAdds.add(this::addInputPins)
    }

    /**
     * This should only be used from the server
     */
    companion object {
        private val fakePlayers: MutableMap<UUID, FakePlayer> = HashMap()

        /**
         * This will get or create a new fake player with the given uuid.
         * It will store the player if it's not present. This should only be called from the server
         */
        operator fun get(world: ServerWorld, uuid: UUID): FakePlayer {
            val worldPlayer = world.getPlayerByUuid(uuid)
            if (worldPlayer != null && worldPlayer is FakePlayer) {
                if (!this.fakePlayers.containsKey(uuid))
                    this.fakePlayers[uuid] = worldPlayer
                return worldPlayer
            }
            if (fakePlayers.containsKey(uuid)) return fakePlayers[uuid]!!
            val player = FakePlayer(world, GameProfile(uuid, "bpm_player_$uuid"))
            player.isOnGround = true
            player.connection =
                ServerPlayNetHandler(world.server, object : NetworkManager(PacketDirection.SERVERBOUND) {
                    override fun sendPacket(packetIn: IPacket<*>) {}
                }, player)
            player.isSilent = true
            player.setNoGravity(true)
            fakePlayers[uuid] = player
            return player
        }
    }
}