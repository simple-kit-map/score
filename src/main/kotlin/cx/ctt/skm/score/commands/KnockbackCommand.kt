package cx.ctt.skm.score.commands
import cx.ctt.skm.score.Score
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.*

enum class KB_NEW {
    new, create, add
}
enum class KB_DEL {
    remove, rm, delete, del
}
enum class KB_LIST {
    list, l, ls
}
enum class KB_SET {
    set, select, choose, pick, switch
}
class KnockbackCommand (private val plugin: Score) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        var args = args.map { it.lowercase() }.toMutableList()

        val uuid = if (sender !is Player){
            Bukkit.getOfflinePlayer(args.removeAt(1)).uniqueId
        } else {
            sender.uniqueId
        }

        val presets = plugin.config.getConfigurationSection("old-player-knockback.custom.configs")?.getKeys(false)
        if (!presets.isNullOrEmpty() && args.size == 1 && args[0] in presets){
            args = listOf("set", args[0]).toMutableList()
        }
        if (args.size == 2 && args[0] in KB_DEL.entries.map { it.name }){
            if (sender.hasPermission("deleteotherskb")){
                val path = "old-player-knockback.custom.configs." + args[1]
                sender.sendMessage("deleted $path")
                plugin.config.set(path, null)
                plugin.saveConfig()
            }
            return true
        }
        if (args.size > 0 && args[0] in KB_NEW.entries.map { it.name }){
            args.removeAt(0)
        }


        if (args.size == 8 || args.size == 7 || args.size == 9){

            val path = "old-player-knockback.custom.configs.${args[0]}"
            plugin.config.set("$path.values.friction",         args[1].toDouble())
            plugin.config.set("$path.values.horizontal",       args[2].toDouble())
            plugin.config.set("$path.values.vertical",         args[3].toDouble())
            plugin.config.set("$path.values.verticallimit",    args[4].toDouble())
            plugin.config.set("$path.values.extrahorizontal",  args[5].toDouble())
            plugin.config.set("$path.values.extravertical",    args[6].toDouble())
            if (args.size == 8 && args[7].isNotEmpty() && args[7] == "true"){
                plugin.config.set("$path.values.netheriteKnockbackResistance",  args[7].toDouble())
            }

            if (args.size == 9 && args[8].isNotEmpty()){
                plugin.config.set("$path.values.hitdelay", args[8].toInt())
            }
            val valueDisplay = args.slice(1..args.lastIndex).joinToString(" ")
            plugin.config.set("$path.valuesDisplay", valueDisplay)

            val keys = plugin.config.getConfigurationSection(path)?.getKeys(false)

            if (!keys?.contains("created")!!){
                plugin.config.set("$path.created" , System.currentTimeMillis())
            }
            if (!keys.contains("creatorUUID")){
                plugin.config.set("$path.creatorUUID" , uuid.toString())
            }
            args = listOf("select", args[0]).toMutableList()
        }
        if (args.size == 2 && args[0] == "show"){
            sender.sendMessage("${args[1]} is using preset ${plugin.config.getString("old-player-knockback.custom.selection.${args[1]}")}")
        }

        if (args.size == 2 && args[0] == "get"){
            val targetkb = plugin.config.getString("old-player-knockback.custom.selection.${args[1]}")

            plugin.config.set("old-player-knockback.custom.selection.$uuid", targetkb)
            sender.sendMessage("now using ${args[1]}'s preset $targetkb")
        }

        if (args.size == 2 && args[0] in KB_SET.entries.map { it.name }) {

            val knockback = args[1]

            val selectedKb = "old-player-knockback.custom.configs.$knockback"

            val hitDelay = plugin.config.getInt("$selectedKb.values.hitdelay", 20)

            sender.sendMessage("Setting hit tick delay to $hitDelay")
            val p = sender as Player
            p.noDamageTicks = hitDelay
            p.maximumNoDamageTicks = hitDelay

            if (!plugin.config.contains(selectedKb)){
                sender.sendMessage("Knockback preset $knockback does not exist")
                return false
            }
            val valuesDisplay = plugin.config.getString("$selectedKb.valuesDisplay")

            val selectionPath = "old-player-knockback.custom.selection.$uuid"

            val previousPreset = plugin.config.getString(selectionPath)

            val previousString = if (previousPreset.isNullOrEmpty()){
                "Applied ${args[1]}, NOTE: this preset only applies to the knockback you'll be taking"
            } else {
                if (previousPreset.equals(args[1], true)){
                    "Already using preset ${args[1]}"
                } else {
                    "Switched from $previousPreset to $knockback"
                }
            }
            val message = TextComponent(previousString)
            message.clickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/kb create ${sender.name}s$knockback $valuesDisplay")
            sender.spigot().sendMessage(message)


            val claim = TextComponent("${GRAY}[${GREEN}Click to switch${GRAY}]")
            var hover = "$knockback\n"
            for(key in plugin.config.getConfigurationSection("${selectedKb}.values")?.getKeys(false)!!
            ){
                hover += "$key: ${plugin.config.get("${selectedKb}.values.$key")}\n"
            }
            hover += "Click to switch"
            claim.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kb $knockback")
            claim.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, arrayOf(TextComponent(hover)))

            for (player in Bukkit.getOnlinePlayers()) {

                player.sendMessage("${DARK_PURPLE}* ${sender.name} ${LIGHT_PURPLE}switched to kb preset ${DARK_PURPLE}$knockback")
                player.spigot().sendMessage(claim)
            }

            plugin.config.set(selectionPath, args[1])

            plugin.PlayerStatuses[sender]?.knockback = args[1]
            plugin.saveConfig()
            return true
        }

        if (args.size == 0 || args[0] !in KB_LIST.entries.map { it.name }) {
            sender.sendMessage("")
            sender.sendMessage("Customize the knockback you will take")
            sender.sendMessage("")
            sender.sendMessage("to create a kb:")
            sender.sendMessage("/kb create <name> <friction> <horizontal> <vertical> <verticallimit> <extrahorizontal> <extravertical> <netherkbresistance (true)> <tickdelay(20)>")
            sender.sendMessage("/kb <name> and /kb set <name> both work")
            sender.sendMessage("Current preset: " + plugin.config.getString("old-player-knockback.custom.selection.$uuid"))
            args = listOf("list").toMutableList()
        }

        if (args.size == 1 && args[0] in KB_LIST.entries.map { it.name }){

            val section = plugin.config.getConfigurationSection("old-player-knockback.custom.configs")
            if (section == null) {
                sender.sendMessage("custom config section is null")
                return false
            }
            if (section.getKeys(false).size == 0) {
                sender.sendMessage("custom config section is empty")
                return false
            }

            sender.sendMessage("")
            sender.sendMessage("Available knockbacks (click to set)")
            sender.sendMessage("")
            for (knockback in section.getKeys(false)){
                val config = plugin.config.getConfigurationSection("old-player-knockback.custom.configs.$knockback")

                if (config == null || config.getKeys(false).size == 0){
                    continue
                }

//                var creatorUUID = UUID.fromString(config.getString("creatorUUID"))

                val valuesClean = config.getString("valuesDisplay")

                if (valuesClean.isNullOrEmpty()) {
                    continue
                }

                val message = TextComponent("$knockback: $valuesClean");
                message.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kb set $knockback");

                var hover = "$knockback\n"
                for(key in config.getConfigurationSection("values")?.getKeys(false)!!
                ){
                    hover += "$key: ${config.get("values.$key")}\n"
                }
                hover += "Click to switch"

                message.hoverEvent = HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    arrayOf(TextComponent(hover))
                )

                sender.spigot().sendMessage(message)
            }
            return true
        }
        return true

    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {

        sender.sendMessage(args.toString())

        when (args.size) {
            1 -> {

                val configs = mutableListOf("list", "set", "add")

                configs.addAll(
                    (plugin.config.getConfigurationSection("old-player-knockback.custom.configs")
                        ?.getKeys(false)!!))

                return configs.filter { it.startsWith(args[0]) }
            }
            2 -> {
                return listOf("two!!")
            }
            3 -> {
                return listOf("three!!")
            }
            else -> return Collections.emptyList()
        }
    }
}