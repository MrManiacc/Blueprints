package me.jraynor.objects.block

import me.jraynor.internal.Registry
import me.jraynor.objects.screens.ScreenBlueprint
import me.jraynor.objects.tile.SingularityTile
import net.minecraft.block.*
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item.BlockItemUseContext
import net.minecraft.state.StateContainer
import net.minecraft.state.properties.BlockStateProperties
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ActionResultType
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

/**
 * This is the main block for the bpm mod.
 */
class SingularityBlock : Block(Properties.from(Blocks.COBBLESTONE)) {
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
            Minecraft.getInstance().displayGuiScreen(ScreenBlueprint(tile)) //TODO create the bpm screen
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