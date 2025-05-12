package cx.ctt.skm.score.commands

import cx.ctt.skm.score.Score
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.ChatColor.*
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.function.Consumer


data class DuelInformation(
    val warp: String,
    val kit: String,
    val knockback: String,
    val initiator: Player,
    val invitees: MutableList<Player>,
    var started: Boolean
)

class DuelCommand (private val plugin: Score): CommandExecutor, TabCompleter, Listener {

    companion object {
        var duels: HashMap<UUID, DuelInformation> = HashMap()
    }

    private var guiCallbacks: HashMap<UUID, Consumer<InventoryClickEvent>> = HashMap()
    private var closeCallbacks: HashMap<UUID, Runnable> = HashMap()
    // args to do:
    // - player - if not provided make or find a GUI that can list AVAILABLE players (people not in a duel already or have duel requests disabled)
    // - map - if not provided make or find a GUI that can list maps that can pre-place players in opposite locations
    // - kit - if not provided make or find a GUI that can list
    // - mechanic - if not provided, list the TEMPORARY mechanic to use, may also default current if not provided, not sure

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (args.isEmpty()){
            sender.sendMessage("/duel <player> <kit> <map>")
            return true
        }
        val players = Bukkit.getOnlinePlayers()
        val igns = players.map { it.name.lowercase() }
        val playersToDuel = mutableListOf<Player>()
        var kit: String? = null
        var warp: String? = null
        var knockback: String? = null
        for (arg in args){
            when (arg.lowercase()) {
                sender.name.lowercase() -> continue

                in igns.map { it.lowercase() } -> {
                    val player = Bukkit.getPlayer(arg) ?: run {
                        sender.sendMessage("Failed getting online player $arg")
                        return false
                    }
                    playersToDuel.add(player)
                }
//                in plugin.getWarps().map { it.lowercase() } -> warp = arg
                in WarpCommand.getWarpNamesRecursively(plugin.config.getConfigurationSection("warps")!!) -> warp = arg

//                in listOf("soccer", "spawn").map { it.lowercase() } -> warp = arg
                in plugin.config.getConfigurationSection("old-player-knockback.custom.configs")?.getKeys(false)!! -> knockback = arg

//                in plugin.playerKitsAPI.getKitsManager().kits.map { it.name.lowercase() } -> kit = arg
                in plugin.config.getConfigurationSection("kits")?.getKeys(false)!! -> kit = arg

                else -> sender.sendMessage("Skipping unknown ign/kit/map/kb `$arg`")
            }
        }
//        }
        val toRet = mutableListOf<String>()
        if (kit == null){
            toRet.add("You did not provide a kit")
        }
        if (warp == null){
            toRet.add("You did not provide a warp")
        }
        if (knockback == null){
            toRet.add("You did not provide a knockback preset")
        }
        if (playersToDuel.isEmpty()){
            toRet.add("You did not provide any player(s) to duel")
        }
        toRet.add("")
        toRet.add("Example: /duel couleur soccer nd ikari")

        if (toRet.size > 2){
            for(ret in toRet){
                sender.sendMessage(ret)
            }
            return true
        }
        if (sender !is Player){
            return true
        }

        val gui = Bukkit.createInventory(null, 9, "Choose some player(s) to duel")

        var slot = 0
        for (potentialVictim in players){
            if (potentialVictim.name == sender.name){continue}
            slot++
            val item = ItemStack(Material.PLAYER_HEAD)
            val meta = item.itemMeta as org.bukkit.inventory.meta.SkullMeta
            meta.setOwningPlayer(potentialVictim)
            meta.setDisplayName(potentialVictim.name)
            item.setItemMeta(meta)

            gui.setItem(slot, item)
        }

//        sender.openInventory(gui)
        guiCallbacks[sender.uniqueId] = Consumer {
            sender.sendMessage("You clicked an item!")
            guiCallbacks.remove(sender.uniqueId)
            closeCallbacks.remove(sender.uniqueId)
        }
//        guiCallbacks.put(sender.getUniqueId(), Any { event ->
//            player.sendMessage("You clicked an item!")
//            // remove callback after use
//            guiCallbacks.remove(player.getUniqueId())
//            closeCallbacks.remove(player.getUniqueId())
//        })
//
//
//        // Set up close handler
//        closeCallbacks.put(player.getUniqueId(), Any {
//            player.sendMessage("You closed the GUI.")
//            guiCallbacks.remove(player.getUniqueId())
//            closeCallbacks.remove(player.getUniqueId())
//        })
        val duelID = UUID.randomUUID()
        duels[duelID] = DuelInformation(
            warp!!,
            kit!!,
            knockback!!,
            sender,
            playersToDuel,
            false
        )

        for (invitee in playersToDuel){
            invitee.sendMessage("")
            invitee.sendMessage(
                "$YELLOW${BOLD}Duel Request"
            )
            invitee.sendMessage("$YELLOW${BOLD} ● From: $GREEN${sender.name} ${GRAY}($GREEN${sender.ping} ms${GRAY})")
            if (playersToDuel.size > 1){
                var list: String = ""
                for (otherInvitees in playersToDuel.filter { it != invitee }){
                    list += "$GREEN${otherInvitees.name} ${GRAY}($GREEN${otherInvitees.ping} ms${GRAY}), "
                }
                list.removeSuffix(", ")
                invitee.sendMessage("$YELLOW${BOLD} ● Invitees: $list")
            }
            invitee.sendMessage("$YELLOW${BOLD} ● Kit: ${LIGHT_PURPLE}$kit")
            invitee.sendMessage("$YELLOW${BOLD} ● Map: ${LIGHT_PURPLE}$warp")
            invitee.sendMessage("$YELLOW${BOLD} ● Knockback: ${LIGHT_PURPLE}$knockback")
            val acceptMsg = TextComponent("$GREEN${BOLD}(CLICK TO ACCEPT)")
            acceptMsg.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accept $duelID")
            invitee.spigot().sendMessage(acceptMsg)
        }

        return true
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (ChatColor.stripColor(event.view.title) != "Choose some player(s) to duel") return

        event.isCancelled = true // prevent item pickup

        val player = event.whoClicked as Player
        val clicked = event.currentItem

        if (clicked == null || clicked.type == Material.AIR) return

        if (clicked.type == Material.PLAYER_HEAD) {
            val uuid = player.uniqueId

            if (guiCallbacks.containsKey(uuid)) {
                event.isCancelled = true
                val a = guiCallbacks[uuid]!!
                a.accept(event)
                player.closeInventory()
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        val onlineNames = Bukkit.getOnlinePlayers()
            .filter { !it.name.equals(sender.name, true) }
            .map { it.name }
//        val warps = listOf("soccer", "spawn")// plugin.getWarps()
        val warps = plugin.config.getConfigurationSection("warps")!!.getKeys(false)
        val knockbacks = plugin.config.getConfigurationSection("old-player-knockback.custom.configs")?.getKeys(false)!!
//        val kits = plugin.playerKitsAPI.getKitsManager().kits.map { it.name }
        val kits = plugin.config.getConfigurationSection("kits")?.getKeys(false)!!
//        val kits = listOf("nd", "boxing")

        val suggestions: MutableList<String> = ArrayList()

        if (args.isEmpty()) {
            return onlineNames
        }

        if (args.size == 1) {
            for (name in onlineNames) {
                if (name.lowercase(Locale.getDefault()).startsWith(args[0].lowercase())) {
                    suggestions.add(name)
                }
            }
            return suggestions
        }

        val usedKit: Boolean = kits.stream().anyMatch(args::contains)
        val usedWarp: Boolean = warps.stream().anyMatch(args::contains)
        val usedKB: Boolean = knockbacks.stream().anyMatch(args::contains)

        val lastArg = args[args.size - 1].lowercase()

        for (name in onlineNames) {
            if (!args.contains(name) && name.lowercase(Locale.getDefault()).startsWith(lastArg)) {
                suggestions.add(name)
            }
        }

        if (!usedWarp) {
            for (warp in warps) {
                if (!args.contains(warp) && warp.lowercase(Locale.getDefault()).startsWith(lastArg)) {
                    suggestions.add(warp)
                }
            }
        }

        if (!usedKit) {
            for (kit in kits) {
                if (!args.contains(kit) && kit.lowercase(Locale.getDefault()).startsWith(lastArg)) {
                    suggestions.add(kit)
                }
            }
        }

        if (!usedKB) {
            for (kb in knockbacks) {
                if (!args.contains(kb) && kb.lowercase(Locale.getDefault()).startsWith(lastArg)) {
                    suggestions.add(kb)
                }
            }
        }

        return suggestions
//        Bukkit.getLogger().info("Tab complete called with args: ${args.joinToString()}")
//
//        val onlineNames = Bukkit.getOnlinePlayers()
//            .filter { !it.name.equals(sender.name, true) }
//            .map { it.name }
//
//        val warpNames = plugin.essentials.warps.list
//
//        return when {
//            args.isEmpty() -> onlineNames
//
//            args.size == 1 -> onlineNames
//                .filter { it.startsWith(args[0], ignoreCase = true) }
//
//            args.size >= 2 -> onlineNames
//                .filter { it.startsWith(args.last(), ignoreCase = true) } +
//                    warpNames
//                .filter { it.startsWith(args.last(), ignoreCase = true) }
//
//            else -> emptyList()
//        }
    }
}