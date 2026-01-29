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
import org.bukkit.configuration.MemorySection
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*


class Kit(private val plugin: Score) : CommandExecutor, TabCompleter, Listener {

    companion object {
/*        fun listKits(plugin: Score, player: CommandSender, gui: Boolean): Boolean {
            val kits = plugin.config.getConfigurationSection("kits")!!.getKeys(false)
            if (!gui) {
                val msg = TextComponent("${DARK_PURPLE}* ${kits.size} ${LIGHT_PURPLE}kits are available:\n")
                var color = DARK_PURPLE
                kits.forEach {
                    color = if (color == DARK_PURPLE) LIGHT_PURPLE else DARK_PURPLE
                    val kitButton = TextComponent("${color}$it")
                    kitButton.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kit $it}")
                    msg.addExtra(kitButton)
                    if (it != kits.last()) msg.addExtra(", ")
                }
                player.spigot().sendMessage(msg)
            } else {
                if (player !is Player) return false
                val inv = Bukkit.createInventory(
                    null, 54, "${DARK_PURPLE}* ${kits.size} ${LIGHT_PURPLE}kits are available:"
                )
                inv.clear()
                var color = DARK_PURPLE
                kits.forEachIndexed { index, kit ->
                    color = if (color == DARK_PURPLE) LIGHT_PURPLE else DARK_PURPLE
                    val kitSection = plugin.config.getConfigurationSection("kits.$kit")!!
                    val icon = kitSection.getItemStack("icon") ?: ItemStack(Material.BOOK)
                    val meta = icon.itemMeta!!
                    meta.setDisplayName("$color$kit")

                    val lore = mutableListOf<String>()
                    kitSection.getString("author")?.let {
                        val id = UUID.fromString(it)
                        var authorName = (Bukkit.getPlayer(id) ?: Bukkit.getOfflinePlayer(id)).name ?: "Unknown?"
                        authorName = if (authorName == player.name) "${RED}you" else "${DARK_PURPLE}$authorName"
                        lore.add("${LIGHT_PURPLE}by $authorName")
                    }

                    lore.add("$DARK_GRAY/k ${kit.lowercase()}")
                    meta.lore = lore
                    icon.itemMeta = meta
                    inv.setItem(index, icon)
                }
                MainMenu.renderToolbar(plugin, player, inv)
            }
            return true
        }*/


        /** helo i give you player i fill his inv and yap abt it :) **/
        fun loadKit(plugin: Score, player: Player, name: String, label: String = "kit"): Boolean {


            val kitName = name.split(".")[0]
            val kit = plugin.config.getConfigurationSection("kits.$kitName") ?: error("Failed getting kit $name")
            plugin.config.set("status.${player.uniqueId}.kit", name)
            kit["uses"] = kit.getInt("uses") + 1
            MainMenu.updateHistory(plugin, "status.${player.uniqueId}.history.kit", kitName)

//        val armors = kit.get("armor")!!
//        val armorsDone = when (armors) {
//            is List<*> -> armors.filterIsInstance<ItemStack>()
//            is Array<*> -> armors.filterIsInstance<ItemStack>()
//            null -> throw NullPointerException("$name armor is null")
//            else -> throw IllegalArgumentException("Unsupported kit $name inventory input type: ${armors.javaClass.name}")
//        }
//        player.inventory.boots = armorsDone[0];
//        player.inventory.leggings = armorsDone[1];
//        player.inventory.chestplate = armorsDone[2];
//        player.inventory.helmet = armorsDone[3];

            val itemStacks = readItemStackList(plugin, kit, "inventory").toTypedArray()
            player.inventory.contents = itemStacks
            player.health = 20.0
            player.foodLevel = 20
            player.saturation = 20.0F


            player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
            kit.getList("effects")!!.forEach { player.addPotionEffect(it as PotionEffect) }

            if (label == "kit-f") {
                val msg = TextComponent("$DARK_GRAY${player.name.lowercase()}.$name")
                msg.hoverEvent =
                    HoverEvent(HoverEvent.Action.SHOW_TEXT, arrayOf(TextComponent("${DARK_GRAY}kit $name")))
                msg.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kit-f $name")
                Bukkit.spigot().broadcast(msg)
            } else if (label != "kit-s") {
                val msg = TextComponent("$DARK_PURPLE* ${player.name} ${LIGHT_PURPLE}equipped ${DARK_PURPLE}kit ")
                val claimButton = TextComponent("$GRAY[$GREEN$BOLD$name$GRAY]")
                claimButton.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kit-f $name")
                msg.addExtra(claimButton)
                Bukkit.spigot().broadcast(msg)
            }
            return true

        }


        private fun readItemStackList(plugin: Score, kit: ConfigurationSection, key: String): List<ItemStack?> {

//            val rawItems = kit.get(key)
//            val items = when (rawItems) {
//                is List<*> -> rawItems.filterIsInstance<ItemStack>()
//                is Array<*> -> rawItems.filterIsInstance<ItemStack>()
//                null -> throw NullPointerException("${kit.name}.$key is null")
//                else -> throw IllegalArgumentException("Unsupported ${kit.name}.$key input type: ${rawItems::class}")
//            }
            if (!kit.contains(key)) error("${kit.name} doesn't have $key")
            var unsafeItems = kit.get(key)
            if (unsafeItems is MemorySection) {
                unsafeItems = unsafeItems.getValues(true)
            } else if (unsafeItems !is HashMap<*, *>) {
                error("${kit.name} inventory is not a hashmap")
            }
            val items = unsafeItems as HashMap<String, ItemStack>
//            val items = kit.getConfigurationSection(key)

//            val HashItems = kit.get(key)
            if (items == null) {
                plugin.logger.info("${kit.name}'s $key is $items")
                error("items is null")
            } else {
//            plugin.logger.info("${kit.name}'s $key is ${items::class}")
//            items = items as HashMap<String, ItemStack>
            }
            val ret: Array<ItemStack?> = Array(41) { null }
            items.keys.forEach { key ->
                key.split(",").forEach {
                    ret[it.toInt()] = items[key] as ItemStack
                }

            }
//        plugin.logger.info("items: ${(items as HashMap<String, ItemStack>).size}")

            return ret.toList()
//        return when (items) {
//            is List<*> -> items.filterIsInstance<ItemStack>()
//            is Array<*> -> items.filterIsInstance<ItemStack>()
//            null -> throw NullPointerException("${kit.name}.$key is null")
//            else -> throw IllegalArgumentException("Unsupported ${kit.name}.$key input type: ${items::class}")
//        }
        }

        fun previewKit(plugin: Score, player: Player, name: String, duel: Boolean = false): Boolean {
            val kit = plugin.config.getConfigurationSection("kits.$name")!!
            val authorIdString = kit.getString("creator")
            val author = if (authorIdString == null) {
                "Unknown?"
            } else {
                val authorId = UUID.fromString(authorIdString)
                (Bukkit.getPlayer(authorId) ?: Bukkit.getOfflinePlayer(authorId)).name ?: "Unknown?"
            }
            val title = "${LIGHT_PURPLE}Kit $DARK_PURPLE$name ${LIGHT_PURPLE}by ${DARK_PURPLE}${author}"
            val mType = if(duel) MenuType.KitPreview else MenuType.KitPreviewDuel
            val inv = Bukkit.createInventory(mType, 54, title)

            inv.clear()
//        loadToolbar(inv, CurrentlyOpen.KIT_PREVIEW)
//            val backbutton = ItemStack(Material.ARROW)
//            val meta = backbutton.itemMeta!!
//            meta.setDisplayName("${GREEN}go back")
//            meta.lore = listOf("${DARK_GRAY}to kit list")
//            backbutton.itemMeta = meta
//            inv.setItem(50, backbutton)

            val itemStacks = readItemStackList(plugin, kit, "inventory")
//        player.sendMessage(kit.getKeys(false).joinToString(", "))
//        player.sendMessage(kit.contains("inventory").toString())
            /*
            //        // when the kit has been created and not saved to disk, it's still an Array, but old kits are ArrayList
            //        val itemsList = when (val items = kit.get("inventory")) {
            //            is List<*> -> items.filterIsInstance<ItemStack>()
            //            is Array<*> -> items.filterIsInstance<ItemStack>()
            //            null -> throw NullPointerException("items is null")
            //            else -> throw IllegalArgumentException("Unsupported input type: ${items::class}")
            //        }
            */


            val effects = kit["effects"]
            if (effects != null) {
                val text = mutableListOf<String>()
                val stand = ItemStack(Material.BREWING_STAND)
                val meta = stand.itemMeta!!
                meta.setDisplayName("${RESET}${LIGHT_PURPLE}Potion Effects:")
                for (effect in effects as Collection<PotionEffect>) {
                    val duration = if (effect.isInfinite || effect.duration == -1) "ထ" else effect.duration.toString()
                    val amplifier = if (effect.amplifier > 250) "MAX" else intToRoman(effect.amplifier + 1)
                    val hidden = if (effect.hasParticles()) "" else "$DARK_GRAY*"
                    text.add("$RESET$LIGHT_PURPLE${effect.type.name.lowercase()}$DARK_PURPLE$hidden $amplifier$LIGHT_PURPLE: $DARK_PURPLE$duration")

                }
                meta.lore = text
                stand.itemMeta = meta
                inv.setItem(4, stand)
            }
            //get armor keys of 36 37 38 39
            if (itemStacks[39] != null) inv.setItem(0, itemStacks[39])
            if (itemStacks[38] != null) inv.setItem(1, itemStacks[38])
            if (itemStacks[37] != null) inv.setItem(2, itemStacks[37])
            if (itemStacks[36] != null) inv.setItem(3, itemStacks[36])

            val cSlots = (45 - 9..53 - 9) + (18 - 9..44 - 9) // map player.inventory.contents
            cSlots.zip(itemStacks).forEach { (slot, item) ->
                if (item == null) inv.setItem(slot, ItemStack(Material.AIR))
                else inv.setItem(slot, item)
            }
            MainMenu.renderToolbar(plugin, player, inv)
            return true
        }


        fun intToRoman(num: Int): String {
            if (num <= 0 || num > 3999) throw IllegalArgumentException("Number must be in the range 1-3999")

            val romanNumerals = listOf(
                "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"
            )
            val values = listOf(
                1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1
            )

            var number = num
            val result = StringBuilder()

            for (i in values.indices) {
                while (number >= values[i]) {
                    number -= values[i]
                    result.append(romanNumerals[i])
                }
            }

            return result.toString()
        }
    }

    /*
    private fun listInChat(player: Player): Boolean {
        val kits = plugin.config.getConfigurationSection("kits")!!.getKeys(false)
        val msg = TextComponent("$DARK_PURPLE* Available kits: ")
        kits.forEach {
            val kitButton = TextComponent("${DARK_PURPLE}$it")
            kitButton.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kit $it")
            msg.addExtra(kitButton)
            if (it != kits.last()) msg.addExtra(", ")
        }
        player.spigot().sendMessage(msg)
        return true
    }
    */

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val help = "/kit <kitname>"
        if (args.isEmpty()) {
            if (sender !is Player){
                sender.sendMessage(help)
                return true
            }
            MainMenu.listSection(plugin, sender, mType = MenuType.Kit)
            return true
//            return listKits(plugin, sender, true)
        }
        val args = args.map { it.lowercase() }.toMutableList()

        // this makes kb set <name> behave like kb <name>
        if (args[0] in SET.entries.map { it.name }) {
            args.removeAt(0)
        }

        val first = args[0]
        val kits = plugin.config.getConfigurationSection("kits")!!.getKeys(false)
        if (args.size == 1) {
            when (first) {
                "clear" -> {
                    if (sender is Player) {
                        sender.inventory.clear()
                        sender.inventory.helmet = null
                        sender.inventory.chestplate = null
                        sender.inventory.leggings = null
                        sender.inventory.boots = null
                        sender.activePotionEffects.forEach { 
                            sender.removePotionEffect(it.type)
                        }
                    }
                }

                else -> {
                    val kitClean = first.split(".")
                    if (kitClean[0] !in kits) {
                        return kitNotFound(sender, kitClean[0])
                    }
                    loadKit(plugin, sender as Player, first, label)
                    if (kitClean.size == 1) {
                        return true
                    }
                    // else, apply the following effects :D
                    val modifiers = kitClean
                    var skipFirst = true
                    for (mod in modifiers) {
                        if (skipFirst) {
                            skipFirst = false
                            continue
                        }

                        val regex = Regex("([a-zA-Z]+)(\\d+)")
                        val matchResult = regex.matchEntire(mod)

                        val ret = matchResult?.destructured
                        if (ret == null) {
                            sender.sendMessage("Invalid format for kit modifier $mod")
                            sender.sendMessage("Example: /kit nodebuff.speed7.sat1.fres0")
                            sender.sendMessage("This gives you a kit with speed 7, saturation 1 and fire resistance removed")
                            continue
                        }
                        val (effect, amplifierParsed) = ret

                        val amplifier = amplifierParsed.toInt() - 1

                        val effectType = when (effect) {
                            "speed", "s" -> PotionEffectType.SPEED
                            "str", "strength", "strenght", "st", "strong" -> PotionEffectType.STRENGTH
                            "slowness", "slow" -> PotionEffectType.SLOWNESS
                            "nv", "nightvision" -> PotionEffectType.NIGHT_VISION
                            "jump", "jumpboost" -> PotionEffectType.JUMP_BOOST
                            "res", "resistance", "boxing", "invincible" -> PotionEffectType.RESISTANCE
                            "haste" -> PotionEffectType.HASTE
                            "invis", "invisibility" -> PotionEffectType.INVISIBILITY
                            "fres", "fireres", "fire", "fireresistance" -> PotionEffectType.FIRE_RESISTANCE
                            "saturation", "sat", "satur", "infinitefood", "infinitehunger" -> PotionEffectType.SATURATION
                            else -> {
                                val closest = PotionEffectType.values().find {
//                                    sender.sendMessage(it.name)
                                    it.name.equals(effect, true) }
                                if (closest == null){
                                    sender.sendMessage("Failed parsing effect $effect")
                                    continue
                                }
                                closest
                            }
                        }
                        sender.removePotionEffect(effectType)
                        if (amplifier != -1)
                            sender.addPotionEffect(
                                PotionEffect(
                                    effectType,
                                    -1,
                                    amplifier,
                                    true,
                                    true
                                )
                            )

                    }

                }
            }
            return true
        }



        when (first) {
            "setalias" -> {
                if (args.size < 3) {
                    sender.sendMessage("kit /setalias <kit> <alias1 alias2 ...>")
                }
                if (args[1] !in kits) {

                }
            }

            "delete", "del", "remove", "rm" -> {
                if (args.size < 2) {
                    sender.sendMessage(help)
                    return true
                }
                val second = args[1]
                val kitSect = plugin.config.getConfigurationSection("kits.$second")
                if (kitSect == null) {
                    return kitNotFound(sender, second)
                } else {
                    sender.sendMessage("${DARK_PURPLE}* Deleted ${LIGHT_PURPLE}kit $DARK_PURPLE$second")
                    plugin.config.set("kits.$second", null)
                    plugin.saveConfig()
                    return true
                }
            }

            "seticon" -> return set(sender as Player, args[1], sender.inventory.itemInMainHand.clone(), KitPart.ICON)

            "seteffect", "seteffects" -> return set(
                sender as Player,
                args[1],
                sender.inventory.itemInMainHand.clone(),
                KitPart.POTIONEFFECTS
            )
            "setinventory", "setinv" -> return set(
                sender as Player,
                args[1],
                sender.inventory.itemInMainHand.clone(),
                KitPart.INVENTORY
            )


//            "list", "ls", "lists" -> return listKits(plugin, sender, false)
            "list", "ls", "lists" -> {
                MainMenu.listSection(plugin, sender as Player, mType = MenuType.Kit)
                return true
            }
            "view", "preview", "pv" -> {
                if (args.size < 2) {
                    sender.sendMessage(help)
                    return true
                }
                val second = args[1]
                for (kit in kits) {
                    if (kit.equals(second, true)) {
                        return previewKit(plugin, sender as Player, second)
                    }
                }
                return kitNotFound(sender, first)

            }

            "create", "new" -> {
                if (args.size < 2) {
                    sender.sendMessage(help)
                    return true
                }
                val second = args[1]
                val blacklist = mutableListOf(
                    "create",
                    "new",
                    "view",
                    "preview",
                    "pv",
                    "seticon",
                    "seteffect",
                    "seteffects",
                    "list",
                    "ls",
                    "lists",
                    "delete",
                    "del",
                    "remove",
                    "rm"
                )
                blacklist.addAll(Bukkit.getOnlinePlayers().map { it.name })
                blacklist.addAll(Bukkit.getOfflinePlayers().map { it.name!! })
                if (blacklist.any { second.equals(it, true) }) {
                    sender.sendMessage("${LIGHT_PURPLE}You can't use $DARK_PURPLE$second ${LIGHT_PURPLE}as a kit name")
                    return true
                }

                if (sender !is Player){
                    sender.sendMessage("This command can only be used by players")
                    return true
                }

                var icon = sender.inventory.itemInMainHand
                if (icon == ItemStack(Material.AIR)) icon = ItemStack(Material.BOOK)
                set(sender, args[1], icon.clone())
            }


        }
        return true
    }

    private fun kitNotFound(sender: CommandSender, second: String): Boolean {
        sender.sendMessage("${LIGHT_PURPLE}Kit $DARK_PURPLE$second ${LIGHT_PURPLE}does not exist")
        return true
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command, label: String, args: Array<out String>
    ): List<String> {
        val kits = plugin.config.getConfigurationSection("kits")!!.getKeys(false).toList()
        when {
            args.isEmpty() -> return listOf("")
            args.size == 1 -> {
                val first = mutableListOf(
                    "create",
                    "preview",
                    "pv",
                    "setaliases",
                    "seticon",
                    "setalias",
                    "setinv",
                    "setinventory",
                    "setarmor",
                    "seteffects"
                )
                first.addAll(kits)
                return first.filter { it.startsWith(args[0], true) }
            }

            args[0] in listOf(
                "preview",
                "pv",
                "setaliases",
                "seticon",
                "setalias",
                "setinv",
                "setinventory",
                "setarmor",
                "seteffects"
            ) -> {
                return kits.filter { it.startsWith(args[1], true) }
            }
        }
        return listOf()
    }

    enum class KitPart {
        NEWKIT, INVENTORY, POTIONEFFECTS, ICON
    }

    // Edit a single slot while handling deduplication
    fun editKitSlot(map: HashMap<String, ItemStack>, slot: String, itemStack: ItemStack) {
        var found = false

        val iterator = map.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val key = entry.key
            val value = entry.value

            if (key == slot) {
                map[key] = itemStack
                found = true
                break
            } else if (key.contains(",")) {
                val indexSlots = key.split(",")
                if (slot in indexSlots) {
                    // Handle cases where the slot exists in a split key
                    val newKey = indexSlots.filter { singleSlot -> singleSlot != slot }.joinToString(",")
                    iterator.remove() // Remove the old key-value pair
                    if (newKey.isNotEmpty()) {
                        map[newKey] = value // Add the remaining slots back under the new key
                    }
                    map[slot] = itemStack // Update the slot with new itemStack
                    found = true
                    break
                }
            }
        }

        if (!found) {
            map[slot] = itemStack
        }
    }
    /*    // edit a single slot whilst handling deduplication
    //    fun editKitSlot(map: HashMap<String, ItemStack>, slot: String, itemStack: ItemStack): HashMap<String, ItemStack> {
    //        var found = false
    //        for ((key, value) in map) {
    //            if (key == slot) {
    //                map[key] = itemStack
    //                found = true
    //                break
    //            } else if (key.contains(",")) {
    //                val indexSlots = key.split(",")
    //                if (slot in indexSlots) {
    //                    val newKey = indexSlots.filter { singleSlot -> singleSlot != slot }.joinToString(",")
    //                    map[newKey] = value
    //                    map[slot] = itemStack
    //                    found = true
    //                    break
    //                }
    //            }
    //        }
    //        if (!found) {
    //            map[slot] = itemStack
    //        }
    //        return map
    //    }
    */

    //    fun editPart(player: Player, name: String, part: KitPart): Boolean {
//        val kit = plugin.config.getConfigurationSection("kits.$name")!!
//        when (part) {
//            KitPart.ARMOR -> {
//                player.inventory.armorContents[0]
//                return true
//            }
//
//            KitPart.INVENTORY -> TODO()
//            KitPart.POTIONEFFECTS -> TODO()
//            KitPart.OFFHAND -> TODO()
//            KitPart.NEWKIT -> TODO()
//        }
//    }
//
    fun set(player: Player, name: String, icon: ItemStack, part: KitPart = KitPart.NEWKIT): Boolean {
        if (plugin.config.contains("kits.$name") && part == KitPart.NEWKIT) {
            player.sendMessage("Kit $name already exists")
            return false
        }
        if (part != KitPart.NEWKIT && !plugin.config.contains("kits.$name")) {
            player.sendMessage("Kit $name doesn't exist")
            return false
        }

        val kit = if (plugin.config.contains("kits.$name")) plugin.config.getConfigurationSection("kits.$name")!!
        else plugin.config.createSection("kits.$name")

        if (part in listOf(KitPart.NEWKIT, KitPart.INVENTORY)) {
            val dedupMap: HashMap<ItemStack, MutableList<Int>> = HashMap()
//            player.sendMessage(player.inventory.contents.size.toString())
            for ((index, item) in player.inventory.contents.copyOf().withIndex()) {
                if (item == null) continue
                if (item.type == Material.AIR) continue
                if (dedupMap.contains(item)) {
                    dedupMap[item]!!.add(index)
                } else {
                    dedupMap[item] = mutableListOf(index)
                }
            }
            val invMap = LinkedHashMap<String, ItemStack>()
            for (item in dedupMap.keys) {
                val slot = dedupMap[item]!!
                val key = if (slot.size == 1) {
                    slot[0].toString()
                } else {
                    slot.joinToString(",")
                }
                invMap[key] = item
            }
            kit["inventory"] = invMap
//            val invMap: HashMap<Int, ItemStack> = HashMap()
//            for ((item, slot) in dedupMap.entries) {
//                if (item == null) continue
//                if (item.type == Material.AIR) continue
//                val key = if (slot.size == 1){
//                    slot.toString()
//                } else {
//                    slot.joinToString(",")
//                }
//                kit["inventory.$key"] = item
//            }

//            for ((index, item) in player.inventory.storageContents.withIndex()) {
//                if (item == null) continue
//                if (item.type == Material.AIR) continue
//                invMap[index] = item
//            }
//            kit["inventory"] = dedupMap
//            plugin.saveConfig()
//            return true
        }

//        if (part in listOf(KitPart.NEWKIT, KitPart.OFFHAND)) kit["offhand"] = player.inventory.itemInOffHand
        if (part in listOf(KitPart.NEWKIT, KitPart.POTIONEFFECTS)) kit["effects"] = player.activePotionEffects
//        if (part in listOf(KitPart.NEWKIT, KitPart.ARMOR)) kit["armor"] = player.inventory.armorContents
//      if (part in listOf(KitPart.NEWKIT, KitPart.INVENTORY)) kit["inventory"] = player.inventory.storageContents

        if (part == KitPart.ICON || part == KitPart.NEWKIT) {
            kit["icon"] = icon
        }

        if (part != KitPart.NEWKIT) {
            plugin.saveConfig()
            Bukkit.spigot()
                .broadcast(TextComponent("$DARK_PURPLE* ${player.name} ${LIGHT_PURPLE}modified $DARK_PURPLE${part.name.lowercase()} ${LIGHT_PURPLE}of kit $name"))
            return true
        } else {
            kit["creator"] = player.uniqueId.toString()
            kit["uses"] = 0
            kit["created"] = System.currentTimeMillis()
            plugin.saveConfig()
        }

        val msg = TextComponent("$DARK_PURPLE* ${player.name} ${LIGHT_PURPLE}created kit ")
        val claimButton = TextComponent("$GRAY[$GREEN$BOLD$name$GRAY]")
        claimButton.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kit preview $name")
//        claimButton.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kit open $name}")
        msg.addExtra(claimButton)
        Bukkit.spigot().broadcast(msg)
        return true
    }
}