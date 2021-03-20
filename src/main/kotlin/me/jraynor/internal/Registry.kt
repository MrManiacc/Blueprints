package me.jraynor.internal

import me.jraynor.BlueprintMod
import me.jraynor.objects.block.SingularityBlock
import me.jraynor.objects.tile.SingularityTile
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.tileentity.TileEntityType
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.eventbus.KotlinEventBus
import thedarkcolour.kotlinforforge.forge.KDeferredRegister

/**
 * This is the container for each of the registry types.
 */
object Registry {
    /**
     * This will register everything.
     */
    fun register(bus: KotlinEventBus) {
        Blocks.REGISTRY.register(bus);
        Items.REGISTRY.register(bus);
        Tiles.REGISTRY.register(bus);
    }

    /**
     * Stores references to all of the blocks
     */
    object Blocks {
        // use of the new KDeferredRegister
        val REGISTRY = KDeferredRegister(ForgeRegistries.BLOCKS, BlueprintMod.ID)

        // the returned ObjectHolderDelegate can be used as a property delegate
        // this is automatically registered by the deferred registry at the correct times
        val SINGULARITY_BLOCK by REGISTRY.registerObject("singularity") {
            SingularityBlock()
        }
    }


    /**
     * Stores all of our items for kratos mod.
     */
    object Items {
        val REGISTRY = KDeferredRegister(ForgeRegistries.ITEMS, BlueprintMod.ID)

        val SINGULARITY_ITEM by REGISTRY.registerObject("singularity") {
            BlockItem(Blocks.SINGULARITY_BLOCK.block, Item.Properties().group(ItemGroup.MISC))
        }

    }

    /**
     * Stores all of our items for kratos mod.
     */
    object Tiles {
        val REGISTRY = KDeferredRegister(ForgeRegistries.TILE_ENTITIES, BlueprintMod.ID)

        val SINGULARITY_TILE: TileEntityType<SingularityTile> by REGISTRY.register("singularity") {
            TileEntityType.Builder.create(
                { SingularityTile() }, Blocks.SINGULARITY_BLOCK.block
            ).build(null)
        }
    }
}