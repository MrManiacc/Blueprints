package me.jraynor.objects.block

import imgui.extension.nodeditor.NodeEditor
import me.jraynor.api.extensions.SelectableBlockExt
import me.jraynor.api.network.Network
import me.jraynor.api.nodes.GrinderNode
import me.jraynor.api.nodes.HopperNode
import me.jraynor.api.packets.PacketGraphRequest
import me.jraynor.api.select.PlayerHooks
import me.jraynor.api.serverdata.WorldData
import me.jraynor.internal.Registry
import me.jraynor.objects.screens.ScreenBlueprint
import me.jraynor.objects.tile.SingularityTile
import net.minecraft.block.*
import net.minecraft.client.Minecraft
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item.BlockItemUseContext
import net.minecraft.item.ItemStack
import net.minecraft.loot.LootContext
import net.minecraft.nbt.CompoundNBT
import net.minecraft.state.StateContainer
import net.minecraft.state.properties.BlockStateProperties
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ActionResultType
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.RayTraceResult
import net.minecraft.world.IBlockReader
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld

/**
 * This is the main block for the bpm mod.
 */
class SingularityBlock : Block(Properties.from(Blocks.COBBLESTONE)) {
    private var lastScreen: ScreenBlueprint? = null

    /**
     * Called throughout the code as a replacement for block instanceof BlockContainer
     * Moving this to the Block base class allows for mods that wish to extend vanilla
     * blocks, and also want to have a tile entity on that block, may.
     *
     *
     * Return true from this function to specify this block has a tile entity.
     *
     * @param state State of the current block
     * @return True if block has a tile entity, false otherwise
     */
    override fun hasTileEntity(state: BlockState?): Boolean {
        return true
    }

    /**
     * Called throughout the code as a replacement for ITileEntityProvider.createNewTileEntity
     * Return the same thing you would from that function.
     * This will fall back to ITileEntityProvider.createNewTileEntity(World) if this block is a ITileEntityProvider
     *
     * @param state The state of the current block
     * @param world The world to create the TE in
     * @return A instance of a class extending TileEntity
     */
    override fun createTileEntity(state: BlockState?, world: IBlockReader?): TileEntity? {
        return Registry.Tiles.SINGULARITY_TILE.create()
    }

    override fun getPickBlock(
        state: BlockState?,
        target: RayTraceResult?,
        world: IBlockReader?,
        pos: BlockPos?,
        player: PlayerEntity?
    ): ItemStack {

        return super.getPickBlock(state, target, world, pos, player)
    }

    /**
     * This is called when the player breaks the block. We want to unload the tile.
     */
    override fun onBlockHarvested(worldIn: World, pos: BlockPos, state: BlockState, player: PlayerEntity) {
        super.onBlockHarvested(worldIn, pos, state, player)
        val tile = worldIn.getTileEntity(pos) ?: return
        if (!worldIn.isRemote) {
            if (tile is SingularityTile) {
                Network.serverRegisteredBlocks.remove(pos)
                if (tile.uuid != null)
                    WorldData[worldIn as ServerWorld].remove(tile.uuid!!)
                Network.removeListeners(tile)
            }
        } else {
            Network.clientRegisteredBlocks.remove(pos)
            Network.removeListeners(tile)
        }
    }

    /**
     * This is called when our block is right clicked on the client and server.
     */
    override fun onBlockActivated(
        state: BlockState,
        worldIn: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        rayTraceResult: BlockRayTraceResult?
    ): ActionResultType {
        if (!worldIn.isRemote) return ActionResultType.SUCCESS // on client side, don't do anything
        val tile = worldIn.getTileEntity(pos)
        if (tile is SingularityTile) {
            if (!player.isSneaking) {
                lastScreen = ScreenBlueprint(tile)
                Minecraft.getInstance().displayGuiScreen(lastScreen)
            } else {
                if (lastScreen != null)
                    if (lastScreen!!.graph != null) {
                        lastScreen!!.graph?.nodes?.forEach {
                            if (it is SelectableBlockExt) {
                                it.shown.set(false)
                                it.pushClientUpdate()
                            } else if (it is GrinderNode) {
                                it.shown.set(false)
                                it.pushClientUpdate()
                            } else if (it is HopperNode) {
                                it.shown.set(false)
                                it.pushClientUpdate()
                            }
                        }
                    }
                PlayerHooks.showFaces.clear()
            }
        }
        return ActionResultType.SUCCESS
    }

    override fun getStateForPlacement(context: BlockItemUseContext): BlockState? {
        return defaultState.with(BlockStateProperties.FACING, context.placementHorizontalFacing.opposite)
    }

    override fun fillStateContainer(builder: StateContainer.Builder<Block?, BlockState?>) {
        builder.add(BlockStateProperties.FACING, BlockStateProperties.POWERED)
    }

    override fun getLightValue(state: BlockState, world: IBlockReader?, pos: BlockPos?): Int {
        return if (state.get(BlockStateProperties.POWERED)) super.getLightValue(state, world, pos) else 0
    }

}