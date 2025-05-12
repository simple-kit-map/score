package cx.ctt.skm.score.mechanics

import cx.ctt.skm.score.Score
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.potion.PotionEffect
import java.util.UUID
import kotlin.collections.HashMap
/*
* https://github.com/GavvyDizzle/PersistentEffects
* https://www.spigotmc.org/resources/persistenteffects.106587/
*/
class DeathHandler(private val plugin: Score) : Listener {
    private val playerEffects = HashMap<UUID, Collection<PotionEffect>>()

    @EventHandler
    fun onDeath(e: PlayerDeathEvent) {
        val effects = e.entity.activePotionEffects
        if (effects.isEmpty()) {
            return
        }
        playerEffects[e.entity.uniqueId] = e.entity.activePotionEffects
    }

    @EventHandler
    fun onRespawn(e: PlayerRespawnEvent) {
        if (playerEffects.containsKey(e.player.uniqueId)) {
            val player = e.player
            Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, {
                if (!player.isOnline) {
                    return@scheduleSyncDelayedTask
                }
                for (effect in playerEffects.remove(player.uniqueId)!!) {
                    player.addPotionEffect(effect)
                }
            }, 1L)
        } else {
            plugin.logger.info("Player " + e.player.name + "respawned without an effects key")
        }
    }
}