package cx.ctt.skm.score.commands

import cx.ctt.skm.score.MainMenu
import cx.ctt.skm.score.MenuType
import cx.ctt.skm.score.Score
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor.*
import org.bukkit.EntityEffect
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import java.util.*
import java.util.function.Consumer


data class DuelInformation(
    var warp: String? = null,
    var kit: String? = null,
    var mechanic: String? = null,
    val gang: MutableSet<Player> = mutableSetOf(),
    val opps: MutableSet<Player> = mutableSetOf(),
    var started: Boolean = false
)

class DuelCommand (private val plugin: Score): CommandExecutor, TabCompleter, Listener {

    companion object {
        var duels: HashMap<Player, DuelInformation> = HashMap()
        fun sendDuel(sender: Player) {
            sender.closeInventory()
            val di = duels[sender] ?: error("Attempted to start duel with unknown duel ID ${sender.name}")
            val playersToDuel = di.gang + di.opps

            sender.sendMessage("")
            sender.sendMessage("$YELLOW${BOLD}Duel Sent")
            if (di.gang.isEmpty())
                sender.sendMessage("$YELLOW ● To: $LIGHT_PURPLE${(di.opps.joinToString { "${it.name}${GRAY}, $LIGHT_PURPLE"}).removeSuffix(", $LIGHT_PURPLE")}")
            else {
                sender.sendMessage("$YELLOW ● With: $LIGHT_PURPLE${di.gang.joinToString("${GRAY}, $GREEN")}")
                sender.sendMessage("$YELLOW ● Against: $LIGHT_PURPLE${di.opps.joinToString("${GRAY}, $GREEN")}")
            }
            sender.sendMessage("$YELLOW ● Kit: $LIGHT_PURPLE${di.kit}")
            sender.sendMessage("$YELLOW ● Warp: $LIGHT_PURPLE${di.warp}")
            sender.sendMessage("$YELLOW ● Mechanic: $LIGHT_PURPLE${di.mechanic}")
            sender.sendMessage("")

            for (invitee in playersToDuel) {

                invitee.sendMessage("")
                invitee.sendMessage("$YELLOW${BOLD}Duel Request")
                invitee.sendMessage("$YELLOW ● From: $GREEN${sender.name} ${GRAY}($GREEN${sender.ping} ms${GRAY})")
                if (playersToDuel.size > 1) {
                    var list = ""
                    for (otherInvitee in playersToDuel.filter { it != invitee }) {
                        val color = if (otherInvitee in di.gang) GREEN else if (otherInvitee in di.opps) RED else GRAY
                        val ping = otherInvitee.ping
                        list += "$color${otherInvitee.name} ${GRAY}(${PingCommand.formatMs(ping)}${ping} ms${GRAY}), "
                    }
                    list.removeSuffix(", ")
                    invitee.sendMessage("$YELLOW ● Invitees: $list")
                }
                invitee.sendMessage("$YELLOW ● Kit: ${LIGHT_PURPLE}${di.kit}")
                invitee.sendMessage("$YELLOW ● Map: ${LIGHT_PURPLE}${di.warp}")
                invitee.sendMessage("$YELLOW ● Knockback: ${LIGHT_PURPLE}${di.mechanic}")
                val acceptMsg = TextComponent("$GREEN${BOLD}(CLICK TO ACCEPT)")
                acceptMsg.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, arrayOf(TextComponent("${YELLOW}Click to accept!")))
                acceptMsg.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accept ${sender.name}")
                invitee.spigot().sendMessage(acceptMsg)
            }
        }
    }

    private var guiCallbacks: HashMap<UUID, Consumer<InventoryClickEvent>> = HashMap()
    private var closeCallbacks: HashMap<UUID, Runnable> = HashMap()
    // args to do:
    // - player - if not provided make or find a GUI that can list AVAILABLE players (people not in a duel already or have duel requests disabled)
    // - map - if not provided make or find a GUI that can list maps that can pre-place players in opposite locations
    // - kit - if not provided make or find a GUI that can list
    // - mechanic - if not provided, list the TEMPORARY mechanic to use, may also default current if not provided, not sure


    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player) {
            sender.sendMessage("only players can initiate duels")
            return true
        }
        if (args.isEmpty()) {
            if (duels[sender] == null)
                duels[sender] = DuelInformation()

            MainMenu.listSection(
                plugin,
                sender,
                mType = MenuType.StatusDuel
            )
//            sender.sendMessage("/duel <player> <kit> <map>")
            return true
        }
        if (args[0] == "play"){
            sender.playEffect(EntityEffect.DEATH)
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                sender.teleport(sender.location)
//                sender.closeInventory()
            }, 40L)
            return true
        }
        if (args[0] == "close"){
            sender.closeInventory()
            return true
        }
        if (args[0] == "dump") {
            sender.sendMessage("")
            duels[sender].toString().split(", ").forEach { sender.sendMessage(it) }
            return true
        }
        if (args[0] == "clear"){
            duels.remove(sender)
            return true
        }
        val players = Bukkit.getOnlinePlayers()
        val igns = players.map { it.name.lowercase() }
        val gang = mutableSetOf<Player>()
        val opps = mutableSetOf<Player>()
        var kit: String? = null
        var warp: String? = null
        var mechanic: String? = null
        for (arg in args){
            val isGang = arg.startsWith("+")
            val arg = if (arg.startsWith("+")) arg.removePrefix("+") else arg

            when (arg.lowercase()) {

                sender.name.lowercase() -> continue

                in igns.map { it.lowercase() } -> {
                    val player = Bukkit.getPlayer(arg)
                    if (player == null) {
                        sender.sendMessage("Failed getting online player $arg..??")
                        continue
                    }
                   if (isGang) gang.add(player)
                   else opps.add(player) // then both teams are empty
                }
                in plugin.config.getConfigurationSection("warps")!! -> warp = arg
                in plugin.config.getConfigurationSection("mechanics")?.getKeys(false)!! -> mechanic = arg
                in plugin.config.getConfigurationSection("kits")?.getKeys(false)!! -> kit = arg

                else -> sender.sendMessage("Skipping unknown ign/kit/map/kb `$arg`")
            }
        }
//        }

        duels[sender] = DuelInformation(
            warp,
            kit,
            mechanic,
            gang,
            opps
        )
        val di = duels[sender]!!
        if (di.opps.isEmpty()) return MainMenu.listSection(plugin, sender, mType = MenuType.StatusDuel)
        if (di.kit == null) return MainMenu.listSection(plugin, sender, mType = MenuType.KitDuel)
        if (di.mechanic == null) return MainMenu.listSection(plugin, sender, mType = MenuType.MechanicDuel)
        if (di.warp == null) return MainMenu.listSection(plugin, sender, mType = MenuType.WarpDuel)
        sendDuel(sender)
        return true
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
        val warps = plugin.config.getConfigurationSection("warps")!!.getKeys(false)
        val knockbacks = plugin.config.getConfigurationSection("mechanics")!!.getKeys(false)
        val kits = plugin.config.getConfigurationSection("kits")?.getKeys(false)!!

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
    }
}