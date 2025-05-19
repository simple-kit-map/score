package cx.ctt.skm.score.mechanics

import cx.ctt.skm.score.MainMenu
import cx.ctt.skm.score.Score
import cx.ctt.skm.score.commands.Kit
import cx.ctt.skm.score.commands.WarpCommand
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatColor.*
import org.bukkit.Bukkit
import org.bukkit.Location
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
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin


class Listeners (private val plugin: Score): Listener {
    private var lastSneak: MutableMap<UUID, Long> = HashMap()
    private var currentWarp: MutableMap<UUID, Int> = HashMap()

// for later :>
@EventHandler
fun onProjectileLaunch(event: ProjectileLaunchEvent) {
    val shooter = event.entity.shooter
    if (shooter !is Player) return

    val location: Location = shooter.eyeLocation.clone()
    location.subtract(
        cos((location.yaw / 180.0 * 3.1415927)) * 0.16f,
        0.10000000149011612,
        sin((location.yaw / 180.0 * 3.1415927)) * 0.16f
    )

    val projectile = event.entity
    val velocity = projectile.velocity
    location.setDirection(projectile.location.direction)
    projectile.teleport(location)
    plugin.logger.info("shoo:${shooter.location.toString()}")
    plugin.logger.info("proj:${location.toString()}")
    projectile.velocity = velocity

    if (event.entityType != EntityType.ARROW){
        val direction = shooter.location.direction.normalize()
        val projDirection = projectile.velocity
        val originalMagnitude = projDirection.length()
        projDirection.normalize()
        direction.multiply(min(originalMagnitude, 0.502))
        projectile.velocity = direction
    }
}
    @EventHandler
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        val player = event.player

        if (event.isSneaking) {
            val now = System.currentTimeMillis()
            val uuid = player.uniqueId

            val lastTime = lastSneak.getOrDefault(uuid, 0L)
            val delta = now - lastTime

            if (delta in 1..150) {
                MainMenu.listSection(plugin.config.getConfigurationSection("status")!!, player)
                lastSneak.remove(uuid)
            } else {
                // Update the timestamp
                lastSneak[uuid] = now
            }
        }
    }

    @EventHandler
    fun onLeavesDecay(e: LeavesDecayEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun onEggThrow(event: PlayerEggThrowEvent) {
        event.isHatching = false
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
        if (event.item?.type == Material.EMERALD){
            if (!currentWarp.containsKey(event.player.uniqueId)){
                currentWarp[event.player.uniqueId] = 0
            }
            val warps = WarpCommand.getWarpNamesRecursively(plugin.config.getConfigurationSection("warps")!!).sorted()

            currentWarp[event.player.uniqueId] = if (event.player.isSneaking){
                Random().nextInt(warps.size)
            }  else {
                (when (event.action) {
                    in listOf(
                        Action.LEFT_CLICK_AIR,
                        Action.LEFT_CLICK_BLOCK
                    ) -> currentWarp[event.player.uniqueId]!! - 1

                    in listOf(
                        Action.RIGHT_CLICK_AIR,
                        Action.RIGHT_CLICK_BLOCK
                    ) -> currentWarp[event.player.uniqueId]!! + 1

                    else -> return
                }).coerceAtLeast(0).coerceAtMost(warps.size - 1)
            }
            val currentIndex = currentWarp[event.player.uniqueId]!!
            val targetWarp = warps[currentIndex]
            event.player.sendMessage("${LIGHT_PURPLE}Warping to $DARK_PURPLE$targetWarp $DARK_GRAY($LIGHT_PURPLE$currentIndex$DARK_GRAY/$DARK_PURPLE${warps.size-1}$DARK_GRAY)")
//            if (event.player.gameMode != GameMode.SPECTATOR) event.player.gameMode = GameMode.SPECTATOR
            if (!event.player.allowFlight) event.player.allowFlight = true
            if (!event.player.isFlying) event.player.isFlying = true
            WarpCommand.teleportToWarp(plugin, event.player, targetWarp, label = "warp-s")
        }
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

    private fun countItems(inv: Inventory, mat: Material): Int{
        var ret=0
        for (slot in inv){
            if (slot == null) continue
            if (slot.type == mat){
                ret++
            }
        }
        return ret
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (event.entity.killer !is Player) return

        val deathCause = event.entity.lastDamageCause ?: run {
            plugin.logger.info("no last damage cause")
            return
        }
        if (deathCause.entity !is Player) run {
            plugin.logger.info("entity no playa")
            return
        }
        if (deathCause.cause !in listOf(DamageCause.ENTITY_ATTACK, DamageCause.FALL, DamageCause.LAVA, DamageCause.FIRE_TICK, DamageCause.FIRE)) run {

            plugin.logger.info("cause")
            return
        }
        plugin.logger.info(deathCause.cause.toString())

        val killed = event.entity
        val killer = event.entity.killer as Player

        val mat: Material
        val term: String
        var color: ChatColor
        if (killer.inventory.contents.any { if (it !=null) it.type == Material.MUSHROOM_STEW else false }){
            mat = Material.MUSHROOM_STEW
            term = "souped"
            color = GOLD
        } else if(killer.inventory.contents.any { if (it !=null) it.type == Material.SPLASH_POTION else false }){
            mat = Material.SPLASH_POTION
            term = "potted"
            color = RED
        } else {
            return
        }

        val killerMats = countItems(killer.inventory, mat)
        val killedMats = countItems(killed.inventory, mat)

        val killerHealth = "${DARK_GRAY}(${RED}❤ " + round(killer.health / 2.0) + "$DARK_GRAY)"
        if (killedMats == 0) {
            event.deathMessage = "${killed.name} was ${color}${killerMats - killedMats} $term$RESET by ${killer.name} $killerHealth"
        } else {

            event.deathMessage = "${killed.name} got quickdropped by $DARK_GRAY(${color}${killerMats - killedMats} $term$DARK_GRAY)$RESET against ${killer.name} $killerHealth"
        }
    }
    @EventHandler
    fun onPlayerRespawn (event: PlayerRespawnEvent){
        val ps = plugin.config.getConfigurationSection("status.${event.player.uniqueId}")

        if (ps == null){
            plugin.logger.severe("no ps for ${event.player.name}")
            return
        }
        val hdPath = "old-player-knockback.custom.configs.${ps.getString("knockback")!!}.values.hitdelay"
        val ht = plugin.config.getInt(hdPath, 20)
        event.player.noDamageTicks = ht

        val kit = ps.getString("kit")
        if (kit !=null && kit != "None")
            Kit.loadKit(plugin, event.player, kit, label = "kit-s")

        val warpSect = plugin.config.getConfigurationSection("warps")!!
        var warpName = ps.getString("warp")!!
        val warps = WarpCommand.getWarpNamesRecursively(warpSect)

        val hasSubWarps = warps.any { "$it.".startsWith(warpName) }

        if (hasSubWarps || warpName.last().isDigit() && warpName.contains('.')){
            var randomWarp = warpName
            for (char in randomWarp.toCharArray().reversed()){
                randomWarp = randomWarp.removeSuffix(char.toString())
                if (char == '.'){
                    break
                }
            }
            val subWarps = warps.filter {
                it.contains('.') &&
                it.startsWith(randomWarp) && it != warpName
            }
            event.player.sendMessage("$warpName shouldnt be in ${subWarps.joinToString(", ")}")

            warpName = if (subWarps.isNotEmpty()){
                subWarps.random()
            } else {
                randomWarp
            }
            event.player.sendMessage("new warp: $warpName")
        }
        val warpLoc = warpSect.getLocation("$warpName.coords")!!
        ps.set("warp", warpName)
        event.respawnLocation = warpLoc
    }
}