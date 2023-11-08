package us.timinc.mc.cobblemon.spawnnotification

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.entity.SpawnEvent
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.playSoundServer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.Level
import net.minecraftforge.event.entity.EntityLeaveLevelEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import us.timinc.mc.cobblemon.spawnnotification.config.SpawnNotificationConfig
import us.timinc.mc.cobblemon.spawnnotification.util.Broadcast

@Mod(SpawnNotification.MOD_ID)
object SpawnNotification {
    const val MOD_ID = "spawn_notification"
    private lateinit var config: SpawnNotificationConfig

    @JvmStatic
    var SHINY_SOUND_ID: ResourceLocation = ResourceLocation("$MOD_ID:pla_shiny")

    @JvmStatic
    var SHINY_SOUND_EVENT: SoundEvent = SoundEvent.createVariableRangeEvent(SHINY_SOUND_ID)

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    object Registration {
        @SubscribeEvent
        fun onInit(e: ServerStartedEvent) {
            config = SpawnNotificationConfig.Builder.load()

            CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe { evt ->
                val pokemon = evt.entity.pokemon
                if (pokemon.isPlayerOwned()) return@subscribe

                broadcastSpawn(evt)
                if (config.playShinySound && pokemon.shiny) {
                    playShinySound(evt.ctx.world, evt.ctx.position)
                }
            }
            CobblemonEvents.POKEMON_SENT_POST.subscribe { evt ->
                if (config.playShinySoundPlayer && evt.pokemon.shiny) {
                    playShinySound(evt.pokemonEntity.level(), evt.pokemonEntity.blockPosition())
                }
            }
            CobblemonEvents.POKEMON_CAPTURED.subscribe { evt ->
                broadcastDespawn(evt.pokemon, DespawnReason.CAPTURED)
            }
            CobblemonEvents.POKEMON_FAINTED.subscribe { evt ->
                broadcastDespawn(evt.pokemon, DespawnReason.FAINTED)
            }
        }

        @SubscribeEvent
        fun onEntityUnload(e: EntityLeaveLevelEvent) {
            if (e.entity !is PokemonEntity) return
            if (e.level.isClientSide) return

            broadcastDespawn((e.entity as PokemonEntity).pokemon, DespawnReason.DESPAWNED)
        }
    }

    private fun broadcastDespawn(
        pokemon: Pokemon,
        reason: DespawnReason
    ) {
        if (config.broadcastDespawns && (pokemon.shiny || pokemon.isLegendary())) {
            Broadcast.broadcastMessage(Component.translatable(
                "$MOD_ID.notification.${reason.translationKey}",
                pokemon.getDisplayName()
            ))
        }
    }

    private fun broadcastSpawn(
        evt: SpawnEvent<PokemonEntity>
    ) {
        val pokemon = evt.entity.pokemon
        val pokemonName = pokemon.getDisplayName()

        val message = when {
            config.broadcastLegendary && config.broadcastShiny && pokemon.isLegendary() && pokemon.shiny -> "$MOD_ID.notification.both"
            config.broadcastLegendary && pokemon.isLegendary() -> "$MOD_ID.notification.legendary"
            config.broadcastShiny && pokemon.shiny -> "$MOD_ID.notification.shiny"
            else -> return
        }

        var messageComponent = Component.translatable(message, pokemonName)
        val pos = evt.ctx.position
        if (config.broadcastCoords) {
            messageComponent = messageComponent.append(
                Component.translatable(
                    "$MOD_ID.notification.coords",
                    pos.x,
                    pos.y,
                    pos.z
                )
            )
        }
        val level = evt.ctx.world
        if (config.broadcastBiome) {
            messageComponent = messageComponent.append(
                Component.translatable(
                    "$MOD_ID.notification.biome",
                    Component.translatable("biome.${evt.ctx.biomeName.toLanguageKey()}")
                )
            )
        }

        if (config.announceCrossDimensions) {
            messageComponent = messageComponent.append(Component.translatable(
                "$MOD_ID.notification.dimension",
                Component.translatable("dimension.${level.dimension().location().toLanguageKey()}")
            ))

            Broadcast.broadcastMessage(messageComponent)
        } else {
            Broadcast.broadcastMessage(level, messageComponent)
        }
    }

    private fun playShinySound(
        level: Level,
        pos: BlockPos
    ) {
        level.playSoundServer(pos.center, SHINY_SOUND_EVENT, SoundSource.NEUTRAL, 10f, 1f)
    }
}