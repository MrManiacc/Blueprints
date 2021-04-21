package me.jraynor

import com.google.common.io.Files
import me.jraynor.internal.Listeners
import me.jraynor.internal.Registry
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.LogManager
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import java.io.File
import java.nio.file.Path

/**
 * Main mod class. Should be an `object` declaration annotated with `@Mod`.
 * The modid should be declared in this object and should match the modId entry
 * in mods.toml.
 */
@Mod(BlueprintMod.ID)
object BlueprintMod {
    const val ID: String = "bpm"

    /**We should only create this once, not per call.**/
    internal val logger = LogManager.getLogger(ID)



    /**
     * This will setup everything first initializing out event callbacks,
     * then initializing our blocks and other objects.
     */
    init {
        Listeners.register(MOD_BUS, FORGE_BUS)
        Registry.register(MOD_BUS)
    }



}