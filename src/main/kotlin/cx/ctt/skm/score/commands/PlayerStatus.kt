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

class PlayerStatus(private val plugin: Score): Listener, CommandExecutor, TabCompleter {
    private val conf = plugin.config
    data class PlayerStatus(
        var warp: String,
        var kit: String,
        var knockback: String
    )
    companion object {
        fun listPlayerStatus(plugin: Score, player: Player) {
            val inv = Bukkit.createInventory(null, 54, "${DARK_PURPLE}* ${Bukkit.getOnlinePlayers().size} ${LIGHT_PURPLE}players are connected:")

            var i = 0
            for ((playerToList, pstatus) in plugin.PlayerStatuses) {
                val statusItem = ItemStack(Material.PLAYER_HEAD)
                val meta = statusItem.itemMeta!! as org.bukkit.inventory.meta.SkullMeta
                meta.setDisplayName("$AQUA${playerToList.name}")
                meta.setOwningPlayer(playerToList)
                val lore = mutableListOf<String>()
                lore.add("${RESET}Warp: $AQUA${pstatus.warp}")
                lore.add("${RESET}Kit: $AQUA${pstatus.kit}")
                lore.add("${RESET}Knockback: $AQUA${pstatus.knockback}")
                meta.lore = lore
                statusItem.itemMeta = meta
                inv.setItem(i, statusItem)
                i++
            }
            MainMenu.renderToolbar(player, inv)
            player.openInventory(inv)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (plugin.PlayerStatuses.containsKey(event.player)) return
        val player = event.player
        var warp = conf.getString("status.${player.name}.warp")
        val kit = conf.getString("status.${player.name}.kit") ?: "None"
        var knockback = conf.getString("old-player-knockback.custom.selection.${player.uniqueId}")
        if (warp == null){
            WarpCommand.teleportToWarp(plugin, player, "spawn")
            warp = "spawn"
        }
//        if (kit == null){
//            player.inventory.clear()
//            player.inventory.helmet = null
//            player.inventory.chestplate = null
//            player.inventory.leggings = null
//            player.inventory.boots = null
//            kit = "None"
//        }
        if (knockback == null){
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                player.sendMessage("You have no knockback preset selected, defaulting to ${AQUA}ikari")
            }, 5L)
            knockback = "ikari"
            conf.set("old-player-knockback.custom.selection.${player.uniqueId}", knockback)
            plugin.saveConfig()
        }

        plugin.PlayerStatuses[player] = PlayerStatus(warp, kit, knockback)
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
                val theirLastWarp = plugin.PlayerStatuses[p]?.warp
                Bukkit.broadcastMessage("${player.name} have teleported to ${p.name}, which was at $theirLastWarp")
                if (theirLastWarp != null){
                    plugin.PlayerStatuses[player]?.warp = theirLastWarp
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
        player.sendMessage("${event.cause}")
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