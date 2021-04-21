package me.jraynor.api.nodes
//
//import me.jraynor.api.Node
//
//import net.minecraft.world.server.ServerWorld
//
//import net.minecraftforge.common.util.FakePlayer
//
//import java.lang.ref.WeakReference
//
//import net.minecraft.util.math.BlockPos
//
//import net.minecraft.network.PacketDirection
//
//import net.minecraft.network.NetworkManager
//
//import net.minecraft.network.play.ServerPlayNetHandler
//
//import net.minecraftforge.common.util.FakePlayerFactory
//
//import com.mojang.authlib.GameProfile
//import imgui.ImGui
//import me.jraynor.BlueprintMod
//import me.jraynor.api.Graph
//import me.jraynor.api.data.Buffers
//import me.jraynor.api.data.Modes
//import me.jraynor.api.enums.Mode
//import me.jraynor.api.extensions.Callback
//import me.jraynor.api.extensions.NBTCallback
//import me.jraynor.api.extensions.SelectableBlockExt
//import me.jraynor.api.extensions.TickCallback
//import me.jraynor.api.select.PlayerHooks
//import me.jraynor.util.*
//import me.jraynor.util.debug
//import me.jraynor.util.info
//import me.jraynor.util.warn
//import net.minecraft.block.Block
//import net.minecraft.entity.Entity
//import net.minecraft.network.IPacket
//import java.util.*
//import net.minecraft.util.ActionResultType
//import net.minecraft.util.Direction
//
//import net.minecraft.util.math.BlockRayTraceResult
//
//import net.minecraft.util.Hand
//
//import net.minecraft.world.World
//import net.minecraftforge.items.IItemHandler
//import net.minecraft.item.ItemStack
//import net.minecraft.nbt.CompoundNBT
//
//import net.minecraftforge.common.util.LazyOptional
//import net.minecraftforge.items.CapabilityItemHandler
//import net.minecraftforge.items.ItemStackHandler
//import net.minecraft.entity.item.ItemEntity
//import net.minecraft.inventory.ItemStackHelper
//import net.minecraftforge.items.ItemHandlerHelper
//
///**
// * This is the base for any type of node that wants to interact with the world.
// */
//abstract class PlayerNode(
//    var playerId: UUID? = null,
//    protected var player: WeakReference<FakePlayer>? = null,
//    /**This allows us to have the inventory of the player exposed to the client */
//    var clientInventory: ItemStackHandler = Buffers.ItemHandlerBuffer(36)
//) : Node(), SelectableBlockExt {
//
//    /**This will retrieve the current inventory item handler for the player if possible or else it will return an empty optional**/
//    val inventory: LazyOptional<IItemHandler>
//        get() {
//            return if (parent?.world is ServerWorld)
//                player?.get()?.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) ?: LazyOptional.empty()
//            else LazyOptional.of {
//                clientInventory
//            }
//        }
//
//    /**
//     * This will write our inventory
//     */
//    override fun serializeNBT(): CompoundNBT {
//        val tag = super.serializeNBT()
//        val p = parent ?: return tag
//        if (p.world is ServerWorld) {
//            inventory.ifPresent {
//                for (i in 0 until it.slots - 1) {
//                    val stack = it.getStackInSlot(i)
//                    if (!stack.isEmpty)
//                        ItemHandlerHelper.insertItem(clientInventory, stack, false)
//                }
//            }
//        }
//        tag.put("inventory_from_client", clientInventory.serializeNBT())
//        return tag
//    }
//
//    /**
//     * This will read the clientInventory
//     */
//    override fun deserializeNBT(tag: CompoundNBT) {
//        super.deserializeNBT(tag)
//        this.clientInventory.deserializeNBT(tag.getCompound("inventory_from_client"))
//        parent ?: return
//    }
//
//    /**
//     * This should extract all from the client inventory buffer into the actual player's inventory
//     */
//    override fun doTick(world: World, graph: Graph) {
//        super.doTick(world, graph)
//        parent ?: return
//        val blockPos = parent!!.blockPos ?: return
//        //This should extract the client inventory in the player's inventory
//        if (Mode.ITEM.extract(Mode.ITEM.type, 1, LazyOptional.of { clientInventory }, inventory).value) {
//            pushServerUpdates(world, blockPos, graph)
//        }
//    }
//
//    /**
//     * This here we'll initialize our player
//     */
//    override fun onAdd(graph: Graph, world: World) {
//        super.onAdd(graph, world)
//        if (!world.isRemote) {
//            if (this.playerId == null)
//                this.playerId = UUID.randomUUID()
//            player = newPlayer(
//                world as ServerWorld,
//                GameProfile(playerId, "${BlueprintMod.ID}.fake_player.${javaClass.simpleName}#${id}")
//            )
//            if (player != null && player?.get() != null)
//                info("successfully created WeakRef to player ${player?.get()?.name?.string}")
//            else
//                warn("Failed to initialize the WeakRef of the player!")
//        }
//    }
//
//    /**
//     * This will render our player information
//     */
//    override fun renderEx() {
//        ImGui.text("Fake Player:")
//        ImGui.separator()
//        ImGui.indent()
//        if (ImGui.button("set held item"))
//            PlayerHooks.selectItem(this)
//        clientInventory.renderGui("main hand##$id", IntRange(0, 1))
//        ImGui.separator()
//        clientInventory.renderGui("inventory##$id", IntRange(1, clientInventory.slots - 1))
//        ImGui.unindent()
//        super.renderEx()
//    }
//
//    override fun hook(
//        nodeRenders: MutableList<Callback>,
//        propertyRenders: MutableList<Callback>,
//        pinAdds: MutableList<Callback>,
//        tickCalls: MutableList<TickCallback>,
//        readCalls: MutableList<NBTCallback>,
//        writeCalls: MutableList<NBTCallback>
//    ) {
//        super<SelectableBlockExt>.hook(nodeRenders, propertyRenders, pinAdds, tickCalls, readCalls, writeCalls)
//    }
//
//    companion object {
//        /**
//         * This will create a new player
//         */
//        fun newPlayer(world: ServerWorld, profile: GameProfile): WeakReference<FakePlayer>? {
//            val playerIn: WeakReference<FakePlayer>? = try {
//                WeakReference(FakePlayerFactory.get(world, profile))
//            } catch (e: Exception) {
//                debug("Exception thrown trying to create fake player : ${e.message}")
//                return null
//            }
//            val playerOut = playerIn ?: return null
//            val player = playerOut.get() ?: return null
//            player.isOnGround = true
//            player.connection = object :
//                ServerPlayNetHandler(world.server, NetworkManager(PacketDirection.SERVERBOUND), player) {
//                override fun sendPacket(packetIn: IPacket<*>?) {}
//            }
//            player.isSilent = true
//            return playerOut
//        }
//
//    }
//}
//
///**This will return true if the entity is a fake player **/
//val Entity.isFakePlayer: Boolean get() = this is FakePlayer
//
///**
// * This will try to harvest at the given block postion and return the result
// */
//fun WeakReference<FakePlayer>?.tryHarvest(targetPos: BlockPos): Boolean {
//    return if (this == null) {
//        false
//    } else {
//        val result = get()!!.interactionManager.tryHarvestBlock(targetPos)
//        result
//    }
//}
//
///**
// * https://github.com/Lothrazar/Cyclic/blob/64a243d2afd2f4eef71c88e8ff39b3d5d1ae044e/src/main/java/com/lothrazar/cyclic/base/TileEntityBase.java#L128
// */
//fun WeakReference<FakePlayer>?.rightClickBlock(
//    world: World,
//    targetPos: BlockPos,
//    hand: Hand,
//    placement: Direction
//): ActionResultType {
//    if (this == null) {
//        debug("Tried to right click with null player reference at ${targetPos.coords} ")
//        return ActionResultType.FAIL
//    }
//    val player = if (this.get() == null) {
//        warn("Tried to reference null player for weak reference $this")
//        return ActionResultType.FAIL
//    } else get()!!
//
//    val trace = BlockRayTraceResult(
//        player.lookVec, placement,
//        targetPos, true
//    )
//    return player.interactionManager.func_219441_a(
//        player, world,
//        player.getHeldItem(hand), hand, trace
//    )
//}
//
///**This will get the main inventory wrapped as an itemstack handler**/
//val WeakReference<FakePlayer>?.mutableInventoryHandler: ItemStackHandler?
//    get() {
//        this ?: return null
//        this.get() ?: return null
//        return ItemStackHandler(this.get()?.inventory?.mainInventory)
//    }
//
///**
// * https://github.com/Lothrazar/Cyclic/blob/64a243d2afd2f4eef71c88e8ff39b3d5d1ae044e/src/main/java/com/lothrazar/cyclic/base/TileEntityBase.java#L111
// */
//fun WeakReference<FakePlayer>?.tryEquipItem(i: LazyOptional<IItemHandler>, hand: Hand, slot: Int): Boolean {
//    if (this == null)
//        return false
//    val player = if (this.get() != null) this.get()!! else return false
//    i.ifPresent { inv: IItemHandler ->
//        var maybeTool = inv.getStackInSlot(slot)
//        if (!maybeTool.isEmpty) {
//            if (maybeTool.count <= 0) {
//                maybeTool = ItemStack.EMPTY
//            }
//        }
//        if (maybeTool != player.getHeldItem(hand))
//            player.setHeldItem(hand, maybeTool)
//    }
//    return i.isPresent
//}
//
///**
// * https://github.com/Lothrazar/Cyclic/blob/64a243d2afd2f4eef71c88e8ff39b3d5d1ae044e/src/main/java/com/lothrazar/cyclic/base/TileEntityBase.java#L101
// */
//fun WeakReference<FakePlayer>?.syncEquippedItem(i: LazyOptional<IItemHandler>, hand: Hand, slot: Int) {
//    val player = if (this != null && this.get() != null) this.get()!! else return
//    i.ifPresent { inv: IItemHandler ->
//        inv.extractItem(slot, 64, false) //delete and overwrite
//        inv.insertItem(slot, player.getHeldItem(hand), false)
//    }
//}
//
///**
// * This adds a drop method to the world class and will only execute on the server
// */
//fun World.drop(pos: BlockPos, drop: Block) {
//    if (!isRemote) {
//        addEntity(
//            ItemEntity(
//                this, pos.x.toDouble(), pos.y.toDouble(),
//                pos.z.toDouble(), ItemStack(drop.asItem())
//            )
//        )
//    }
//}
//
///**
// * This adds a drop method to the world class and will only execute on the server
// */
//fun World.drop(pos: BlockPos, drop: ItemStack) {
//    if (!isRemote) {
//        addEntity(ItemEntity(this, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), drop))
//    }
//}
//
