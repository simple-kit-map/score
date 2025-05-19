package cx.ctt.skm.score.commands

import cx.ctt.skm.score.MainMenu
import cx.ctt.skm.score.Score
import net.md_5.bungee.api.ChatColor.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import java.util.*

class PlayerStatus(private val plugin: Score): Listener, CommandExecutor, TabCompleter {
    private val conf = plugin.config
    companion object {
        fun listPlayerStatus(plugin: Score, player: Player) {
            val inv = Bukkit.createInventory(null, 54, "${DARK_PURPLE}* ${Bukkit.getOnlinePlayers().size} ${LIGHT_PURPLE}players are connected:")

            val a = plugin.config
            val players = plugin.config.getConfigurationSection("status")!!.getKeys(false).filter { Bukkit.getPlayer(UUID.fromString(it)) != null }
            for ((i, playerName) in  players.withIndex()) {
                val pstatus = plugin.config.getConfigurationSection("status.$playerName")!!
                val warp = pstatus.getString("warp")!!
                val kit = pstatus.getString("kit")!!
                val knockback = pstatus.getString("knockback")!!

                val statusItem = ItemStack(Material.PLAYER_HEAD)
                val meta = statusItem.itemMeta!! as org.bukkit.inventory.meta.SkullMeta
                val playerPlayer = Bukkit.getPlayer(UUID.fromString(playerName)) ?: continue
                meta.setDisplayName("$AQUA${playerPlayer.name}")
                meta.setOwningPlayer(playerPlayer)
                val lore = mutableListOf<String>()
                lore.add("${RESET}Warp: $AQUA${warp}")
                lore.add("${RESET}Kit: $AQUA${kit}")
                lore.add("${RESET}Knockback: $AQUA${knockback}")
                meta.lore = lore
                statusItem.itemMeta = meta
                inv.setItem(i, statusItem)
            }
            MainMenu.renderToolbar(player, inv)
            player.openInventory(inv)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {

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
        }

        val hdPath = "old-player-knockback.custom.configs.${knockback}.values.hitdelay"
        val ht = plugin.config.getInt(hdPath, 20)
        event.player.maximumNoDamageTicks = ht

        plugin.config.set("status.${player.uniqueId}.warp", warp)
        plugin.config.set("status.${player.uniqueId}.kit", kit)
        plugin.config.set("status.${player.uniqueId}.knockback", knockback)
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
        if (event.to == null) {
            event.player.sendMessage("You have teleported to an unknown location, how did that happen?")
            return
        }
//        event.to!!.world!!.getNearbyEntities(event.to!!, event.to!!.x, event.to!!.y, event.to!!.z)
        for (p in Bukkit.getOnlinePlayers()){
            if (p.location == event.to){
                val theirLastWarp = plugin.config.getString("status.${p.uniqueId}.warp")
                Bukkit.broadcastMessage("${player.name} have teleported to ${p.name}, which is at $theirLastWarp")
                if (theirLastWarp != null){
                    plugin.config.set("status.${player.uniqueId}.warp", theirLastWarp)
                    return
                } else {
                    event.player.sendMessage("")
                }
                break
            }
//            else {
//                Bukkit.broadcastMessage("othe:${p.location}")
//                Bukkit.broadcastMessage("to..:${event.to!!}")
//                Bukkit.broadcastMessage("tper:${player.location}")
//            }
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true
//        val inv = Bukkit.createInventory(null, 54, "Player Status")
        listPlayerStatus(plugin, sender)
//        sender.openInventory(inv)
        return true
    }

    override fun onTabComplete(
        p0: CommandSender,
        p1: Command,
        p2: String,
        p3: Array<out String>
    ): MutableList<String>? {
        TODO("Not yet implemented")
    }
}