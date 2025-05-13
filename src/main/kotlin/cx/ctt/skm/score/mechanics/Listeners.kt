package cx.ctt.skm.score.mechanics

import cx.ctt.skm.score.Score
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.*
import org.bukkit.block.sign.Side
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
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
    fun onLeavesDecay(event: LeavesDecayEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onBreakCrop(e: PlayerInteractEvent) {
        if (e.action != Action.PHYSICAL || e.clickedBlock?.type != Material.FARMLAND) {
            return
        }
        e.isCancelled = true
    }
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent){
        val player = event.player
        val sponge: Block = event.to?.block!!.getRelative(BlockFace.DOWN)
        if (sponge.type != Material.SPONGE) {
            return
        }
        val signBlock: Block = sponge.getRelative(BlockFace.DOWN)
        val state: BlockState = signBlock.state as? Sign ?: return

        val sign: Sign = state as Sign
        val fl: String = sign.getSide(Side.FRONT).getLine(0).takeIf { it.isNotEmpty() } ?: sign.getSide(Side.BACK).getLine(0)
        val sl: String = sign.getSide(Side.FRONT).getLine(1).takeIf { it.isNotEmpty() } ?: sign.getSide(Side.BACK).getLine(1)
        val tl: String = sign.getSide(Side.FRONT).getLine(2).takeIf { it.isNotEmpty() } ?: sign.getSide(Side.BACK).getLine(2)
        try {
            val x = fl.toDouble()
            val y = sl.toDouble()
            val z = tl.toDouble()
            val vec = Vector(x, y, z)
            player.velocity = vec
            player.playSound(player.location, Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.0f)
        } catch (_: NumberFormatException) {}
    }
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

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action === Action.RIGHT_CLICK_BLOCK) {
            val player = event.player
            val state = event.clickedBlock!!.state
            if (state is Skull) {
                val skull: Skull = state as Skull
                if (skull.hasOwner()){
                    if (skull.owningPlayer!!.name == null){
                       val offlineplayer = Bukkit.getOfflinePlayer(skull.owningPlayer!!.uniqueId)
                        if (offlineplayer.name == null){
                            player.sendMessage("Could not determine which player this head belongs to")
                        } else {
                            player.sendMessage("This head belongs to " + offlineplayer.name)
                        }
                    } else {

                        player.sendMessage("This head belongs to " + skull.owningPlayer!!.name)
                    }
                } else if(skull.skullType.name.isNotEmpty()){
                    player.sendMessage("This head belongs to " + skull.skullType.name)
                } else {
                    player.sendMessage("Could not determine which player this head belongs to")
                }
                event.isCancelled = true
                return
            }
        }
        if (!(event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK)) {
            return
        }
        val inventory = event.player.inventory
        if (inventory.itemInMainHand.type != Material.MUSHROOM_STEW) {
            return
        }
        val heal = 7.0
        val feed = 7.0

        if (event.player.health < 20.0 && event.player.health > 0.0) {
            if (event.player.health < (20 - heal + 1)) {
                inventory.setItem(inventory.heldItemSlot, ItemStack(Material.BOWL))
                event.player.health = minOf(20.0, event.player.health + heal)
            } else if (event.player.health < 20.0 && event.player.health > (20 - heal)) {
                event.player.health = 20.0
                inventory.setItem(inventory.heldItemSlot, ItemStack(Material.BOWL))
            }
        } else if (event.player.health == 20.0 && event.player.foodLevel < 20) {
            if (event.player.foodLevel < 20 - feed + 1) {
                inventory.setItem(inventory.heldItemSlot, ItemStack(Material.BOWL))
                event.player.foodLevel = (event.player.foodLevel + feed).toInt()
            } else if (event.player.foodLevel < 20 && event.player.foodLevel > 20 - feed) {
                event.player.foodLevel = 20
                inventory.setItem(inventory.heldItemSlot, ItemStack(Material.BOWL))

            }
        }
    }

}