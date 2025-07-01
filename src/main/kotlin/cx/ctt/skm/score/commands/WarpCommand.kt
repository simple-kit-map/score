package cx.ctt.skm.score.commands

import cx.ctt.skm.score.MainMenu
import cx.ctt.skm.score.MenuType
import cx.ctt.skm.score.Score
import net.md_5.bungee.api.ChatColor.*
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import java.util.*


class WarpCommand(private val plugin: Score) : CommandExecutor, TabCompleter, Listener {
    companion object {

/*
        fun listWarps(warpSect: ConfigurationSection, player: Player, page: Int = 1) {
            val warps = getWarpNamesRecursively(warpSect).sorted()
            if (warps.isEmpty()) {
                player.sendMessage("No warps found"); return
            }
            val inv =
                Bukkit.createInventory(null, 54, "${DARK_PURPLE}* ${warps.size} ${LIGHT_PURPLE}warps are available:")

            val itemsPerPage = 45

            MainMenu.renderToolbar(
                player,
                inv,
                currentPage = page,
                previous = page > 1,
                next = page * itemsPerPage < warps.size,
            )

            val startIndex = (itemsPerPage * (page - 1))
            var slot = 0
            for (index in startIndex..warps.size){
                if (index == warps.size) break
                if (slot == 45) break
                val warp = warps[index]

                val warpIcon = warpSect.getItemStack("$warp.icon") ?: ItemStack(Material.GUNPOWDER)
                val meta = warpIcon.itemMeta!!
                if (!meta.hasDisplayName()) meta.setDisplayName("${RESET}$warp")
                val author = warpSect["author"]
                val lore = if (author != null) mutableListOf("added by $author") else mutableListOf()
                lore.add("$DARK_GRAY/w $warp")
                meta.lore = lore

                warpIcon.itemMeta = meta
                inv.setItem(slot, warpIcon)
                slot++
            }
            player.openInventory(inv)

        }
*/

        fun teleportToWarp(plugin: Score, player: Player, name: String, label: String = "warp"): Boolean {
            player.teleport(
                plugin.config.getLocation("warps.$name.coords") ?: run {
                    if (name.lowercase() in Bukkit.getOnlinePlayers().map { it.name.lowercase() })
                        player.teleport(Bukkit.getPlayer(name)!!.location)
                    else
                        player.sendMessage("Failed getting warp $name, are you sure it exists?")

                    return false
                })

            plugin.config.set("status.${player.uniqueId}.warp", name)

            MainMenu.updateHistory(plugin, "status.${player.uniqueId}.history.warp", name)
            if (label == "warp-f") {
                val followMsg = TextComponent("$DARK_GRAY${player.name.lowercase()}->$name")
                followMsg.hoverEvent =
                    HoverEvent(HoverEvent.Action.SHOW_TEXT, arrayOf(TextComponent("${DARK_GRAY}warped to $name")))
                followMsg.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp-f $name")
                Bukkit.spigot().broadcast(followMsg)
            } else if (!label.endsWith("-s")){
                val msg =
                    TextComponent("${DARK_PURPLE}* ${player.name} ${LIGHT_PURPLE}tped to ${DARK_PURPLE}warp $GRAY[$GREEN$BOLD$name$GRAY]")
                msg.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, arrayOf(TextComponent("${LIGHT_PURPLE}Click me to warp to $DARK_PURPLE$name!")))
                msg.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp-f $name")
                Bukkit.spigot().broadcast(msg)
            }
            return true
        }

        fun getWarpNamesRecursively(section: ConfigurationSection): List<String> {
            val warps: MutableList<String> = ArrayList()

            fun scanSection(section: ConfigurationSection, currentPath: String) {
                for (key in section.getKeys(false)) {

                    // Skip reserved keys
                    if (blacklistedKeys.contains(key.lowercase(Locale.getDefault()))) {
                        continue
                    }

                    // Build the full path (adds a dot if we're deeper than root)
                    val fullPath = if (currentPath.isEmpty()) key else "$currentPath.$key"
                    val subSection = section.getConfigurationSection(key) ?: continue

                    // Check if this section contains "coords" (making it a valid warp)
                    if (subSection.contains("coords", false)) {
                        warps.add(fullPath)
                    }

                    // Recursively scan deeper subsections
                    scanSection(subSection, fullPath)
                }
            }

            scanSection(section, "")
            return warps
        }

        private val blacklistedKeys = arrayOf("coords, x, y, z, pitch, yaw, icon, displayname")
    }

    private val help = "/warp list | <warp> [<player1>...]"

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(help)
            return true
        }
        if (args.isEmpty()) {
//            listWarps(plugin.config.getConfigurationSection("warps")!!, sender)
            MainMenu.listSection(plugin, sender, mType = MenuType.Warp)
            return true
        }


        if (label.endsWith("setwarp") && args.size == 1) {
            if (plugin.config.contains("warps.${args[0]}")) {
                sender.sendMessage("Warp ${args[0]} already exists")
                return true
            }
            val msg = TextComponent("$DARK_PURPLE* ${sender.name} ${LIGHT_PURPLE}created warp ")
            val name = TextComponent("$GRAY[$GREEN$BOLD${args[0]}${GRAY}]")
            name.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, arrayOf(TextComponent("${GREEN}Click me to warp to ${args[0]}!")))
            name.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp-f ${args[0]}")
            msg.addExtra(name)
            Bukkit.spigot().broadcast(msg)
            plugin.config.set("warps.${args[0]}.coords", sender.location)
            return true
        }

        val warps = getWarpNamesRecursively(plugin.config.getConfigurationSection("warps")!!)
        when {
            label.endsWith("warps") || args.size == 1 && args[0] in listOf("list", "lists") -> {
                if (warps.isEmpty()) {
                    sender.sendMessage("There are no warps")
                    return true
                }
                val list =
                    TextComponent("$DARK_PURPLE* $DARK_PURPLE${warps.size} ${LIGHT_PURPLE}warps are available:\n\n")
                for (warp in warps) {

                    // ONLY show subwarps when doing listS... with an S
                    if (args[0] != "lists" && warp.contains(".")) continue

                    val warpButton = TextComponent(warp)
                    warpButton.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp $warp")
                    list.addExtra(warpButton)

                    if (warp != warps.last()) list.addExtra(", ")
                }
                sender.spigot().sendMessage(list)
            }
            args[0] in listOf("del", "delete", "remove", "rm") -> {
                if (args.size == 1) {
                    sender.sendMessage("${RED}usage: /warp del <warpname>")
                    return true
                }
                if (args[1] !in warps){
                    sender.sendMessage("${RED}Warp ${args[1]} does not exist.")
                    return true
                }
                if (plugin.config.getString("warps.${args[1]}.author") != sender.uniqueId.toString() && !sender.hasPermission("score.warps.delete.others")) {
                    sender.sendMessage("${LIGHT_PURPLE}You don't have permission to delete $DARK_PURPLE${args[1]}")
                    return true
                }
                plugin.config.set("warps.${args[1]}", null)
                plugin.saveConfig()
                sender.sendMessage("${GREEN}Warp ${args[1]} deleted")
                return true

            }

            args[0] == "seticon" -> {
                if (args.size == 1) {
                    sender.sendMessage("${RED}usage: /warp seticon <warpname> (it uses item that you're holding")
                    return true
                }
                if (args[1] !in warps){
                    sender.sendMessage("${RED}Warp ${args[1]} does not exist")
                    return true
                }
                if (sender.inventory.itemInMainHand.type == Material.AIR) {
                    sender.sendMessage("${RED}You must be holding an item in your main hand to use this command")
                    return true
                }
                plugin.config.set("warps.${args[1]}.icon", ItemStack(sender.inventory.itemInMainHand))
                plugin.saveConfig()
                sender.sendMessage("${GREEN}warp icon set for ${args[1]}")
                return true
            }
            args.size == 1 -> return teleportToWarp(plugin, sender, args[0], label)

            else -> {
                args.drop(1).map { playerToWarp ->
                    Bukkit.getPlayer(playerToWarp).let {
                        if (it == null) {
                            sender.sendMessage("Could not warp unknown user $playerToWarp")
                        } else teleportToWarp(plugin, it, args[0])
                    }
                }
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command, label: String, args: Array<out String>
    ): List<String> {
        if (args.isEmpty()) return listOf()
        val warps = getWarpNamesRecursively(plugin.config.getConfigurationSection("warps")!!)
        val last = args.last()
        return when (args.size) {
            // only show subwarps when a dot is typed
            1 -> warps.filter {
                it.contains('.') && !last.contains('.')
                if (it.contains('.') && !args[0].contains('.')) {
                    false
                } else {
                    it.lowercase().startsWith(last.lowercase())
                }
            }

            else -> Bukkit.getOnlinePlayers().filter {
                it.name != sender.name && it.name.lowercase().startsWith(last.lowercase())
            }.map { it.name }

        }
    }
}