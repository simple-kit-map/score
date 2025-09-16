package cx.ctt.skm.score.commands

import cx.ctt.skm.score.MainMenu.Companion.listSection
import cx.ctt.skm.score.MenuType
import cx.ctt.skm.score.Score
import net.md_5.bungee.api.ChatColor.AQUA
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.*

class PlayerStatus(private val plugin: Score): Listener, CommandExecutor, TabCompleter {
    private val conf = plugin.config
    companion object {
//        fun listPlayerStatus(plugin: Score, player: Player, duel: Boolean = false) {
//            val mType = if (duel) MenuType.PlayerStatusDuel else MenuType.PlayerStatus
//            val inv = Bukkit.createInventory(mType, 54, "${DARK_PURPLE}* ${Bukkit.getOnlinePlayers().size} ${LIGHT_PURPLE}players are connected:")
//
//            val a = plugin.config
//            val players = plugin.config.getConfigurationSection("status")!!.getKeys(false).filter { Bukkit.getPlayer(UUID.fromString(it)) != null }
//            for ((i, playerName) in  players.withIndex()) {
//                val pstatus = plugin.config.getConfigurationSection("status.$playerName")!!
//                val warp = pstatus.getString("warp")!!
//                val kit = pstatus.getString("kit")!!
//                val knockback = pstatus.getString("knockback")!!
//
//                val statusItem = ItemStack(Material.PLAYER_HEAD)
//                val meta = statusItem.itemMeta!! as org.bukkit.inventory.meta.SkullMeta
//                val playerPlayer = Bukkit.getPlayer(UUID.fromString(playerName)) ?: continue
//                meta.setDisplayName("$AQUA${playerPlayer.name}")
//                meta.setOwningPlayer(playerPlayer)
//                val lore = mutableListOf<String>()
//                lore.add("${RESET}Warp: $AQUA${warp}")
//                lore.add("${RESET}Kit: $AQUA${kit}")
//                lore.add("${RESET}Knockback: $AQUA${knockback}")
//                meta.lore = lore
//                statusItem.itemMeta = meta
//                inv.setItem(i, statusItem)
//            }
//            MainMenu.renderToolbar(plugin, player, inv)
//            player.openInventory(inv)
//        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {

        val cb = Bukkit.getPluginManager().getPlugin("CheatBreakerAPI")
        if (cb != null)
        {
            val method = cb.javaClass.getMethod("giveAllStaffModules", Player::class.java)
            method.invoke(cb, event.player)
        }

        val player = event.player
        var warp = conf.getString("status.${player.uniqueId}.warp")
        val kit = conf.getString("status.${player.uniqueId}.kit") ?: "None"
        var knockback = conf.getString("status.${player.uniqueId}.mechanic")
        if (warp == null){
            WarpCommand.teleportToWarp(plugin, player, "spawn")
            warp = "spawn"
        }
        if (knockback == null){
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                player.sendMessage("You have no knockback preset selected, defaulting to ${AQUA}ikari")
            }, 5L)
            knockback = "ikari"
            conf.set("status.${player.uniqueId}.mechanic", knockback)
        } else {
            val hdPath = "mechanics.${knockback}.values.hitdelay"
            val ht = plugin.config.getInt(hdPath, 20)
            event.player.maximumNoDamageTicks = ht
            event.player.noDamageTicks = ht
        }


        plugin.config.set("status.${player.uniqueId}.warp", warp)
        plugin.config.set("status.${player.uniqueId}.kit", kit)
        plugin.saveConfig()
    }
/*    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        plugin.PlayerStatuses.remove(player)
    }*/
    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        if (event.cause !in listOf(COMMAND, PLUGIN, SPECTATE)) return
        if (event.to == null) {
            event.player.sendMessage("You have teleported to an unknown location, how did that happen?")
            return
        }
        val tpedTo = Bukkit.getOnlinePlayers().filter { it != player && it.location == event.to }
        if (tpedTo.isNotEmpty()){
            val p = tpedTo.first()
            val theirLastWarp = plugin.config.getString("status.${p.uniqueId}.warp")
            plugin.logger.info("${player.name} have teleported to ${p.name}, which is at $theirLastWarp")
            if (theirLastWarp != null){
                plugin.config.set("status.${player.uniqueId}.warp", theirLastWarp)
                return
            } else {
                player.sendMessage("You have teleported to ${p.name}, but they have no last warp set.")
            }
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val target: Player
        if (args.isNotEmpty() && args[0] == "info") {
            target = if (args.size == 1 && sender is Player) {
                sender
            } else {
                Bukkit.getPlayer(args[1]) ?: run {
                    sender.sendMessage("${args[1]} not exists/online")
                    return false
                }
            }
            sender.sendMessage("Health: ${target.health}")
            sender.sendMessage("Damage tick: ${target.maximumNoDamageTicks}")
            sender.sendMessage("Damage tick: ${target.noDamageTicks}")
            sender.sendMessage("Ping: ${target.ping}")
            sender.sendMessage("Warp: " + conf.getString("status.${target.uniqueId}.warp"))
            sender.sendMessage("Kit: " + conf.getString("status.${target.uniqueId}.kit"))
            sender.sendMessage("Mech: " + conf.getString("status.${target.uniqueId}.mechanic"))
            sender.sendMessage("")
            return true
        }
        if (sender !is Player) return true
        return listSection(plugin, sender, mType = MenuType.Status)
    }

    override fun onTabComplete(
        p0: CommandSender,
        p1: Command,
        p2: String,
        p3: Array<String>
    ): MutableList<String>? {return mutableListOf()}
}