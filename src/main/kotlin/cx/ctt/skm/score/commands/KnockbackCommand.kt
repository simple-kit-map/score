package cx.ctt.skm.score.commands

import cx.ctt.skm.score.MainMenu
import cx.ctt.skm.score.MenuType
import cx.ctt.skm.score.Score
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor.*
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import java.util.*


enum class NEW {
    new, create, add
}

enum class DEL {
    remove, rm, delete, del
}

enum class LIST {
    list, l, ls
}

enum class SET {
    set, select, choose, pick, switch
}

class KnockbackCommand(private val plugin: Score) : CommandExecutor, TabCompleter {
    private val conf
        get() = plugin.config

    companion object {

        data class MechInfo(
            val slot: Int,
//            val type: mechType,
            val name: String,
            val defaultValue: Any,
            val lore: List<String>,
            val icon: Material = Material.PAPER
        )

        /*
        horizontal
        vertical
        horizontalextra
        verticalextra
        horizontallimit
        verticallimit
        verticalfriction
        horizontalfriction
        startrange
        rangefactor
        maximumrange
        hitdelay
        netheritearmorknockbackresistance
        */

        data class MechData (
            val horizontal: Double,
            val vertical: Double,

            val horizontalextra: Double,
            val verticalextra: Double,

            val horizontallimit: Double,
            val verticallimit: Double,

            val verticalfriction: Double,
            val horizontalfriction: Double,

            val rangereduction: Boolean,
            val startrange: Double,
            val rangefactor: Double,
            val maximumrange: Double,

            val hitdelay: Int,
            val netheritekbresistance: Int,
        )

        public val mechMeta: LinkedHashMap<String, MechInfo> = linkedMapOf(
            "horizontal" to MechInfo(10, "Horizontal", 0.4, listOf("")),
            "vertical" to MechInfo(11, "Vertical", 0.4, listOf("")),
            "horizontalextra" to MechInfo(19, "Horizontal extra", 0.5, listOf("Added to horizontal when sprinting")),
            "verticalextra" to MechInfo(20, "Vertical extra", 0.1, listOf("Added to vertical when sprinting")),
            "horizontallimit" to MechInfo(28, "Horizontal limit", 0.8, listOf("Maximum horizontal knockback one can take")),
            "verticallimit" to MechInfo(29, "Vertical limit", 0.4, listOf("Maximum vertical knockback one can take")),
            "verticalfriction" to MechInfo(37, "Vertical friction", 2.0, listOf("")),
            "horizontalfriction" to MechInfo(38, "Horizontal friction", 2.0, listOf("")),

            "rangereduction" to MechInfo(4, "Range reduction", false, listOf("")),
            "startrange" to MechInfo(13, "Start range", -1.0, listOf("")),
            "rangefactor" to MechInfo(22, "Range factor", 1.0, listOf("")),
            "maximumrange" to MechInfo(31, "Maximum range", 0.4, listOf("")),

            "hitdelay" to MechInfo(40, "Hit Delay", 20, listOf("Delay in ticks (20=1s) between hits"), Material.PUFFERFISH),

            "netheritekbresistance" to MechInfo(49, "Netherite KB resistance", 10, listOf("Percentage of armor reduction per netherite armor slot")),
        )
        fun castValue(value: String, defaultValue: Any, valueName: String, mechName: String): Any {
            val value = when (defaultValue) {
                is Boolean -> value.toBooleanStrictOrNull()
                is Double -> value.toDoubleOrNull()
                is Float -> value.toFloatOrNull()
                is Int -> value.toIntOrNull()
                else -> error("Unsupported type ${defaultValue::class.simpleName}")
            } ?: error("${mechName}: invalid default value $value for $valueName, which defaults to $defaultValue of type ${defaultValue::class.simpleName}")
            return value
        }

        fun previewMechanic(plugin: Score, mechName: String, player: Player, duel: Boolean = false) {

            val sect = plugin.config.getConfigurationSection("mechanics.$mechName")
            if (sect == null) {
                player.sendMessage("mechanic $mechName not found")
                return
            }

            val authorId = UUID.fromString(sect.getString("creator"))
            val author = Bukkit.getPlayer(authorId)?.name ?: Bukkit.getOfflinePlayer(authorId).name ?: "Unknown?"

            val inv = Bukkit.createInventory(
                if (duel)
                MenuType.MechanicPreviewDuel else MenuType.MechanicPreview, 54, "${DARK_PURPLE}* $mechName ${LIGHT_PURPLE}mechanic by $author"
            )
            MainMenu.renderToolbar(plugin, player, inv)

            val helper = ItemStack(Material.NAME_TAG)
            val hMeta = helper.itemMeta!!
            hMeta.setDisplayName("${RESET}tweak tips:")
            hMeta.lore = listOf(
                "${WHITE}LMB/RMB: $RED-0.1${GRAY}/${GREEN}+0.1${WHITE}", "${WHITE}w/ SHIFT: ${RED}-0.5${GRAY}/${GREEN}+0.5"
                )
    helper.itemMeta = hMeta
    inv.setItem(0, helper)

            val rangeReductionEnabled = sect.getBoolean("values.rangereduction")
            for ((internalName, mechanic) in mechMeta) {

                val mechValue = sect.get("values.$internalName") ?: mechanic.defaultValue
                if (!rangeReductionEnabled && internalName in listOf("rangefactor", "maximumrange", "startrange")) continue
                val mechItem = ItemStack(mechanic.icon)
                val meta = mechItem.itemMeta!!
                meta.setDisplayName("$RESET${mechanic.name}${GRAY}: ${AQUA}$mechValue")

                val lore = mutableListOf<String>() // mutableListOf("${WHITE}LMB/RMB: $RED-0.1${GRAY}/${GREEN}+0.1${WHITE}, w/ SHIFT: ${RED}-0.5${GRAY}/${GREEN}+0.5")
                if (mechanic.lore.isNotEmpty()) {
                    lore.addAll(mechanic.lore)
                }
                meta.lore = lore
                mechItem.itemMeta = meta
                inv.setItem(mechanic.slot, mechItem)
            }
            player.openInventory(inv)

        }
        fun modifyKnockback(plugin: Score, mechName: String, key: String, value: String, player: Player) {
            val sect = plugin.config.getConfigurationSection("mechanics.$mechName.values")!!
            val key = key.lowercase().replace(" ", "")
            val parsedValue: Any = when {
                value.startsWith("-") || value.startsWith("+") -> {
                    val originalvalue = sect.getDouble(key)
                    if (value.startsWith("+")) value.toFloat() + originalvalue else value.toFloat() - originalvalue
                }
                value.toBooleanStrictOrNull() != null -> value.toBooleanStrictOrNull()!!
                value.toDoubleOrNull() != null -> value.toDoubleOrNull()!!
                else -> {value}
            }
            sect.set(key, parsedValue)
        }
        // apply a mechanic to a player
        fun setMechanic(sender: Player, mech: String, plugin: Score, label: String = "mech"): Boolean {

            if (label.endsWith("-f")){
                Bukkit.broadcastMessage("${DARK_GRAY}${sender.name.lowercase()}=$mech")
            } else if(!label.endsWith("-s")){

                val msg = TextComponent("${DARK_PURPLE}* ${sender.name} ${LIGHT_PURPLE}switched to ${DARK_PURPLE}mech ")

                val claimButton = TextComponent("$GRAY[$GREEN$mech$GRAY]")
                claimButton.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, arrayOf(TextComponent("${DARK_GRAY}mech $mech")))
                claimButton.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mech-f $mech")
                var desc = mech
                for ((_, meta) in mechMeta){
                    desc += "\n${meta.name}${GRAY}: ${AQUA}${plugin.config.get("mechanics.$mech.values.${meta.name.lowercase().replace(" ", "")}")}${RESET}"
                }
                claimButton.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, arrayOf(TextComponent(desc)))
                msg.addExtra(claimButton)

                val hd = plugin.config.getInt("mechanics.$mech.values.hitdelay", 20)
                sender.maximumNoDamageTicks = hd
                sender.noDamageTicks = hd

                Bukkit.spigot().broadcast(msg)
            }

            plugin.config.set("status.${sender.uniqueId}.mechanic", mech)
            return true
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        plugin.logger.info (plugin.config.getString("status.${(sender as Player).uniqueId}.mechanic"))
        val args = args.map { it.lowercase() }.toMutableList()
        val sender = if (sender !is Player) {
            val pl = Bukkit.getPlayer(args.removeAt(0))
            if (pl != null) {
                pl
            }
            else {
                sender.sendMessage("player not found")
                return true
            }
        } else {
            sender
        }

        val kitBlacklist: MutableSet<String> = mutableSetOf()

        listOf(NEW.entries, DEL.entries, LIST.entries, SET.entries).forEach { subcommand ->
            kitBlacklist.addAll(subcommand.map { it.name })
        }
        if (args.isEmpty()){
            MainMenu.listSection(plugin, sender, mType = MenuType.Mechanic)
            return true
        }

        val helpArgs = listOf("help", "?", "/?", "-h", "--help", "-help", "/help", "about")
        kitBlacklist.addAll(helpArgs)

        val mechs = plugin.config.getConfigurationSection("mechanics")?.getKeys(false)!!

        if (args.size == 1 && args[0] in conf.getConfigurationSection("mechanics")!!.getKeys(false)){
            return setMechanic(sender, args[0], plugin, label)
        }

        when (val first = args[0]) {
            "setvalue", "setval" -> {
                if (args.size == 1){
                    sender.sendMessage("missing $label name and value")
                    return true
                }
                if (args.size == 2){
                    sender.sendMessage("missing $label value")
                    return true
                }
                val mech = args[1]
                val mechValue = args[2]
                if (mech !in mechs){
                    sender.sendMessage("$label $mech does not exist")
                    return true
                }
                if (mechValue !in mechMeta.keys){
                    sender.sendMessage("$label do not support any values named ${RED}$mechValue${RESET}, select amongst:")
                    for (valid in mechMeta.keys) sender.sendMessage("${GRAY}- ${GREEN}$valid")
                    return true
                }

                if (args.size == 3){
                    val anvil = Bukkit.createInventory(MenuType.MechanicSetValue, InventoryType.ANVIL, "${GRAY}${args[1]}.${args[2]}")
                    val itemToRename = ItemStack(Material.PAPER)
                    val meta = itemToRename.itemMeta!!
                    meta.setDisplayName("${args[2]}: ")
                    itemToRename.itemMeta = meta
                    anvil.setItem(0, itemToRename)
                    sender.level = 1
                    sender.openInventory(anvil)
                    return true
                }
                if (args.size != 4) {
                    sender.sendMessage("missing $label name, key, and value to set")
                    return true
                }
                val mechName = args[1].lowercase()
                val key = args[2].lowercase()
                val value = args[3]
                val sect = conf.getConfigurationSection("mechanics.$mechName.values")
                if (sect == null) {
                    sender.sendMessage("$label $mechName does not exist")
                    return true
                }
                if (key !in mechMeta.keys) {
                    sender.sendMessage("$label do not support any values named $key")
                    return true
                }
                //! TODO: check type castable
                sect.set(key, value)
                sender.sendMessage("$label set $mechName.$key to $value")

            }
            // mech preview
            in listOf("preview", "pv", "edit") -> {
                if (args.size != 2) {
                    args.add(conf.getString("status.${sender.uniqueId}.mechanic") ?: run {
                        sender.sendMessage("missing $label name to preview")
                        return true
                    })
                }
                if (args[1] !in mechs) {
                    sender.sendMessage("$label ${args[1]} does not exist")
                    return true
                }
                previewMechanic(plugin, args[1], sender)
                return true
            }
            // mech claim
            in SET.entries.map { it.name } -> {
                if (args.size != 2) {
                    sender.sendMessage("Don't even bother doing /kb set <name> just do /kb <name> bro")
                    return true
                }
                return setMechanic(sender, args[1], plugin, label)
            }
            in helpArgs -> {

                val label = label.lowercase()
                sender.sendMessage("")
                sender.sendMessage("Customize the knockback you will take")
                sender.sendMessage("")
                sender.sendMessage("to create a $label:")
//            sender.sendMessage("/kb create <name> <friction> <horizontal> <vertical> <verticallimit> <extrahorizontal> <extravertical> <netherkbresistance (true)> <tickdelay(20)>")
                sender.sendMessage("/$label create <knockback to use as start values> (use vanilla to get started")
                sender.sendMessage("To select a preset, doing /$label <name> and /$label set <name> both work")
                val currentPreset = plugin.config.getString("satus.${sender.uniqueId}.mechanic")
                sender.sendMessage("\n" + if (currentPreset == null) {
                    "You have no preset selected"
                } else {
                    "Current preset: $currentPreset"
                })
            }

            // creation of a mechanic
            in NEW.entries.map { it.name } -> {
                if (args.size < 2) {
                    sender.sendMessage("missing knockback name and knockback values")
                    return true
                }
                val mechName = args[1].lowercase()
                if (mechName in kitBlacklist){
                    sender.sendMessage("Cannot use $mechName as a knockback name")
                    return true
                }
                if (conf.contains("mechanics.$mechName")) {

                    if (conf.getString("mechanics.$mechName.creator") != sender.uniqueId.toString()){
                        sender.sendMessage("There already exists a mech named $mechName")
                        return true
                    } else {
                        sender.sendMessage("Squashing your existing mechanic $mechName")
                    }
                }
                args.removeFirst() // KB_NEW arg
                args.removeFirst() // kb name
                // we're left with just the values
                if (args.size != mechMeta.size) {
                    sender.sendMessage("you provided ${args.size} knockback values, but ${mechMeta.size} are needed")
                    var index = 0
                    mechMeta.forEach { (_, mechInfo) ->
                        val displayValue = if (index < args.size){
                            "${GREEN}${args[index]}"
                        } else {
                            "${RED}?"
                        }
                        sender.sendMessage("#${index+1}, ${mechInfo.name.replace(" ", "")}${DARK_GRAY}: $displayValue")
                        index++
                    }
                    return true
                }
//                conf.createSection("mechanics.$mechName")
//                conf.createSection("mechanics.$mechName.values")
//                val vals = conf.getConfigurationSection("mechanics.$mechName.values")!!

//                val typedArgs = mutableListOf<Any>()
                var index = 0
                for ((_, meta) in mechMeta){
//                mechMeta2.foreach {(_, meta) ->

                    val value = when (meta.defaultValue) {
                        is Boolean -> args[index].toBooleanStrictOrNull()
                        is Double -> args[index].toDoubleOrNull()
                        is Float -> args[index].toFloatOrNull()
                        is Int -> args[index].toIntOrNull()
                        else ->
                            error("invalid default value for ${meta.name}, ${meta.defaultValue} of type ${meta.defaultValue::class.simpleName}")
                    }
                    if (value == null){
                        sender.sendMessage("${LIGHT_PURPLE}Invalid input value for ${DARK_PURPLE}${meta.name}${LIGHT_PURPLE}, ${RED}${args[index]} ${LIGHT_PURPLE}is not a valid ${DARK_PURPLE}${meta.defaultValue::class.simpleName}")
                        conf.set("mechanics.$mechName", null)
                        return true
                    }
                    conf.set("mechanics.$mechName.values.${meta.name.lowercase().replace(" ", "")}", value)
//                    typedArgs.add(value)
                    index++
                }
//                val argsToMech = ArgsToMech<MechData>(typedArgs)
//                sender.sendMessage("$argsToMech")
//                sender.sendMessage("${argsToMech.toString()}")
//                conf.set("mechanics.$mechName.values", argsToMech.toString())

                conf.set("mechanics.$mechName.creator", sender.uniqueId.toString())
                conf.set("mechanics.$mechName.created", System.currentTimeMillis())

                conf.set("status.${sender.uniqueId}.mechanic", mechName)
                plugin.saveConfig()
                Bukkit.spigot().broadcast(TextComponent("${DARK_PURPLE}* ${sender.name} ${LIGHT_PURPLE}created new mechanic ${DARK_PURPLE}$mechName"))
                return true
            }
            // deletion of a mechanic
            in DEL.entries.map { it.name } -> {
                if (args.size != 2) {
                    sender.sendMessage("missing knockback name, use /$label del <name>")
                    return true
                }
                val mechName = args[1]
                val mechSect = conf.getConfigurationSection("mechanics.$mechName")
                if (mechSect == null) {
                    sender.sendMessage("$label $mechName does not exist")
                    return true
                }
                val creator = mechSect.getString("creator")
                if (!sender.hasPermission("score.mechanics.delete.others") || (creator != null && creator != sender.uniqueId.toString())) {
                    sender.sendMessage("You don't have permission to delete this $label")
                    return true
                }
                sender.sendMessage("${DARK_PURPLE}* Deleted ${LIGHT_PURPLE}$label ${DARK_PURPLE}$mechName")
                plugin.config.set("mechanics.$mechName", null)
            }
            in LIST.entries.map { it.name } -> {
                val mechs = conf.getConfigurationSection("mechanics")?.getKeys(false)!!
                sender.sendMessage(mechs.joinToString(", "))
            }
            else -> {
                sender.sendMessage("${LIGHT_PURPLE}$label ${DARK_PURPLE}${args.joinToString(", ")} ${LIGHT_PURPLE}does not exist")
                plugin.logger.warning("unrecognized args `${args.joinToString(", ")}`")
            }
        }
        return true
    }

/*fun onCommandold(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        var args = args.map { it.lowercase() }.toMutableList()

        val uuid = if (sender !is Player) {
            val pl = Bukkit.getPlayer(args.removeAt(0))
            if (pl != null) {
                pl.uniqueId
            }
            else {
                sender.sendMessage("player not found")
                return true
            }
        } else {
            sender.uniqueId
        }

        val presets = plugin.config.getConfigurationSection("old-player-knockback.custom.configs")?.getKeys(false)
        if (!presets.isNullOrEmpty() && args.size == 1 && args[0] in presets) {
            args = listOf("set", args[0]).toMutableList()
        }
        if (args.size == 2 && args[0] in KB_DEL.entries.map { it.name }) {
            val author = plugin.config.getString("old-player-knockback.custom.configs.${args[1]}.creator")
            val isCreator = author != null && author.equals(uuid.toString(), true)
            if (!isCreator || !sender.hasPermission("score.knockback.delete.others")) {
                val path = "old-player-knockback.custom.configs." + args[1]
                sender.sendMessage("deleted knockback ${args[1]} ($path)")
                plugin.config.set(path, null)
                plugin.saveConfig()
            } else {
                sender.sendMessage("you do not have permission to delete knockback presets")
            }
            return true
        }
        if (args.size > 0 && args[0] in KB_NEW.entries.map { it.name }) {
            args.removeAt(0)
        }



        if (args.size == 8 || args.size == 7 || args.size == 9) {

            val path = "old-player-knockback.custom.configs.${args[0]}"
            val vals = "$path.values"
            plugin.config.set("$vals.friction",          args[1].toDouble())
            plugin.config.set("$vals.horizontal",        args[2].toDouble())
            plugin.config.set("$vals.vertical",          args[3].toDouble())
            plugin.config.set("$vals.verticallimit",     args[4].toDouble())
            plugin.config.set("$vals.extrahorizontal",   args[5].toDouble())
            plugin.config.set("$vals.extravertical",     args[6].toDouble())
            if (args.size == 8 && args[7].isNotEmpty() && args[7] == "true") {
                plugin.config.set("$vals.netheriteKnockbackResistance", args[7].toDouble())
            }

            if (args.size == 9 && args[8].isNotEmpty()) {
                plugin.config.set("$vals.hitdelay", args[8].toInt())
            }
            val valueDisplay = args.slice(1..args.lastIndex).joinToString(" ")
            plugin.config.set("$path.valuesDisplay", valueDisplay)

            val keys = plugin.config.getConfigurationSection(path)?.getKeys(false)

            if (!keys?.contains("created")!!) {
                plugin.config.set("$path.created", System.currentTimeMillis())
            }
            if (!keys.contains("creatorUUID")) {
                plugin.config.set("$path.creatorUUID", uuid.toString())
            }
            args = listOf("select", args[0]).toMutableList()
        }
        if (args.size == 2 && args[0] == "show") {
            sender.sendMessage("${args[1]} is using preset ${plugin.config.getString("status.${args[1]}.mechanic")!!}")
        }

        if (args.size == 2 && args[0] == "get") {
            val targetkb = plugin.config.getString("status.${args[1]}.mechanic")!!

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

            if (!plugin.config.contains(selectedKb)) {
                sender.sendMessage("Knockback preset $knockback does not exist")
                return false
            }
            val valuesDisplay = plugin.config.getString("$selectedKb.valuesDisplay")

            val selectionPath = "old-player-knockback.custom.selection.$uuid"

            val previousPreset = plugin.config.getString(selectionPath)

            val previousString = if (previousPreset.isNullOrEmpty()) {
                "Applied ${args[1]}, NOTE: this preset only applies to the knockback you'll be taking"
            } else {
                if (previousPreset.equals(args[1], true)) {
                    "Already using preset ${args[1]}"
                } else {
                    "Switched from $previousPreset to $knockback"
                }
            }
            val message = TextComponent(previousString)
            message.clickEvent =
                ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/kb create ${sender.name}s$knockback $valuesDisplay")
            sender.spigot().sendMessage(message)


            val claim = TextComponent("${GRAY}[${GREEN}Click to switch${GRAY}]")
            var hover = "$knockback\n"
            for (key in plugin.config.getConfigurationSection("${selectedKb}.values")?.getKeys(false)!!) {
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

            plugin.config.set("status.${sender.uniqueId}.knockback", args[1])
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

        if (args.size == 1 && args[0] in KB_LIST.entries.map { it.name }) {

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
            for (knockback in section.getKeys(false)) {
                val config = plugin.config.getConfigurationSection("old-player-knockback.custom.configs.$knockback")

                if (config == null || config.getKeys(false).size == 0) {
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
                for (key in config.getConfigurationSection("values")?.getKeys(false)!!) {
                    hover += "$key: ${config.get("values.$key")}\n"
                }
                hover += "Click to switch"

                message.hoverEvent = HoverEvent(
                    HoverEvent.Action.SHOW_TEXT, arrayOf(TextComponent(hover))
                )

                sender.spigot().sendMessage(message)
            }
        }
        return true

    }
*/

    override fun onTabComplete(
        sender: CommandSender, command: Command, label: String, args: Array<out String>
    ): List<String> {

        val mechs = plugin.config.getConfigurationSection("mechanics")?.getKeys(false)!!
        when (args.size) {
            1 -> {
                val configs = mutableListOf("list", "add", "del")
                configs.addAll(mechs)
                return configs.filter { it.startsWith(args[0]) }
            }

            2 -> {
                when (args[0]){
                    in DEL.entries.map { it.name } -> {
                        return if (sender is Player) {
                            mechs.filter {
                                plugin.config.getString("mechanics.${it}.creator")!! == sender.uniqueId.toString()
                            }
                        } else {
                            mechs.toList()
                        }
                    }
                }
            }
        }
        return Collections.emptyList()
    }
}