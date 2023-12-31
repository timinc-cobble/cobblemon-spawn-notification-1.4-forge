package us.timinc.mc.cobblemon.spawnnotification.config

import com.google.gson.GsonBuilder
import us.timinc.mc.cobblemon.spawnnotification.SpawnNotification
import java.io.File
import java.io.FileReader
import java.io.PrintWriter

class SpawnNotificationConfig {
    val broadcastShiny = true
    val broadcastCoords = true
    val broadcastBiome = false
    val playShinySound = true
    val playShinySoundPlayer = false
    val announceCrossDimensions = false
    val broadcastDespawns = false
    val labelsForBroadcast: MutableSet<String> = mutableSetOf("legendary")

    class Builder {
        companion object {
            fun load(): SpawnNotificationConfig {
                val gson = GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .create()

                var config = SpawnNotificationConfig()
                val configFile = File("config/${SpawnNotification.MOD_ID}.json")
                configFile.parentFile.mkdirs()

                if (configFile.exists()) {
                    try {
                        val fileReader = FileReader(configFile)
                        config = gson.fromJson(fileReader, SpawnNotificationConfig::class.java)
                        fileReader.close()
                    } catch (e: Exception) {
                        println("Error reading config file")
                    }
                }

                val pw = PrintWriter(configFile)
                gson.toJson(config, pw)
                pw.close()

                return config
            }
        }
    }
}