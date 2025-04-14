package cx.ctt.skm.score.mechanics

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.scheduler.BukkitRunnable
import cx.ctt.skm.score.Score
import java.util.*

class Listeners (private val plugin: Score): Listener {
    private var lastSneak: MutableMap<UUID, Long> = HashMap()

// for later :>

//    @EventHandler
//    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
//        val player = event.player
//
//        if (event.isSneaking) {
//            val now = System.currentTimeMillis()
//            val uuid = player.uniqueId
//
//            val lastTime = lastSneak.getOrDefault(uuid, 0L)
//            val delta = now - lastTime
//
//            if (delta in 1..350) {
//                player.sendMessage("Double sneak detected!")
//                lastSneak.remove(uuid)
//            } else {
//                // Update the timestamp
//                lastSneak[uuid] = now
//            }
//        }
//    }
    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val droppedItem = event.itemDrop
        val material = droppedItem.itemStack.type

        if (listOf(Material.GLASS_BOTTLE, Material.BOWL).contains(material)) {
            object : BukkitRunnable() {
                override fun run() {
                    droppedItem.remove()
                }
            }.runTaskLater(plugin, 1L)

        }
        if (material in listOf(
                Material.NETHERITE_AXE, Material.NETHERITE_SWORD,
                Material.DIAMOND_AXE, Material.DIAMOND_SWORD,
                Material.STONE_AXE, Material.STONE_SWORD,
                Material.IRON_SWORD, Material.IRON_AXE,
                Material.WOODEN_SWORD,
                Material.WOODEN_AXE, Material.MUSHROOM_STEW
            )
        ) {
            if (!event.player.isSneaking) {
                event.player.sendMessage("Sneak to confirm dropping your item")
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onProjectileHit(ev: ProjectileHitEvent) {
        val entity = ev.entity
        if (entity.type == EntityType.ARROW && (entity.isOnGround)) {
            entity.remove()
        }
    }
}