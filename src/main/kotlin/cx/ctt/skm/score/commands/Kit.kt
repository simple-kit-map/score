package cx.ctt.skm.score.commands

import cx.ctt.skm.score.MainMenu
import cx.ctt.skm.score.Score
import net.md_5.bungee.api.ChatColor.*
import net.md_5.bungee.api.chat.ClickEvent
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
import java.util.*


class Kit(private val plugin: Score) : CommandExecutor, TabCompleter, Listener {

    companion object {
        fun listKits(plugin: Score, player: CommandSender, gui: Boolean): Boolean {
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
                        var authorName = (Bukkit.getPlayer(id) ?: Bukkit.getOfflinePlayer(id)).name
                        authorName = if (authorName == player.name) "${RED}you" else "${DARK_PURPLE}$authorName"
                        lore.addAll(
                            mutableListOf(
                                "${LIGHT_PURPLE}by $authorName",
                                "${LIGHT_PURPLE}to ${RED}delete ${LIGHT_PURPLE}it, press ${DARK_PURPLE}CTRL+{Drop key}"
                            )
                        )
                    }

                    lore.add("$DARK_GRAY/k ${kit.lowercase()}")
                    meta.lore = lore
                    icon.itemMeta = meta
                    inv.setItem(index, icon)
                }
                MainMenu.renderToolbar(player, inv)
            }
            return true
        }


        /** helo i give you player i fill his inv and yap abt it :) **/
        fun loadKit(plugin: Score, player: Player, name: String, label: String): Boolean {

            val kit = plugin.config.getConfigurationSection("kits.$name") ?: error("Failed getting kit $name")
            plugin.PlayerStatuses[player]?.kit = name
            kit["uses"] = kit.getInt("uses") + 1

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

//        player.inventory.setItemInOffHand(kit["offhand"] as ItemStack?)
            val itemStacks = readItemStackList(plugin, kit, "inventory").toTypedArray()
//        player.sendMessage("mes couilles: $itemStacks")
            player.inventory.contents = itemStacks


            player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
            kit.getList("effects")!!.forEach { player.addPotionEffect(it as PotionEffect) }

            if (label == "kit-s") {
                val msg = TextComponent("$DARK_GRAY${player.name} followed")
                msg.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kit-s $name")
                Bukkit.spigot().broadcast(msg)
            } else {
                val msg = TextComponent("$DARK_PURPLE* ${player.name} ${LIGHT_PURPLE}switched to ${DARK_PURPLE}kit ")
                val claimButton = TextComponent("$GRAY[$GREEN$name$GRAY]")
                claimButton.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kit-s $name")
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
            } else if (unsafeItems !is HashMap<*, *>){
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

            ret
            return ret.toList()
//        return when (items) {
//            is List<*> -> items.filterIsInstance<ItemStack>()
//            is Array<*> -> items.filterIsInstance<ItemStack>()
//            null -> throw NullPointerException("${kit.name}.$key is null")
//            else -> throw IllegalArgumentException("Unsupported ${kit.name}.$key input type: ${items::class}")
//        }
        }

        fun previewKit(plugin: Score, player: Player, name: String): Boolean {
            val kit = plugin.config.getConfigurationSection("kits.$name")!!
            val authorId = UUID.fromString(kit.getString("author"))
            val author = Bukkit.getPlayer(authorId) ?: Bukkit.getOfflinePlayer(authorId)

            val title = "${LIGHT_PURPLE}Kit $DARK_PURPLE$name ${LIGHT_PURPLE}by ${DARK_PURPLE}${author.name}"
            val inv = Bukkit.createInventory(null, 54, title)

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
                    val amplifier = if (effect.amplifier > 250) "MAX" else intToRoman(effect.amplifier)
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

            val cSlots = (45-9..53-9) + (18-9..44-9) // map player.inventory.contents
            cSlots.zip(itemStacks).forEach { (slot, item) ->
                if (item == null) inv.setItem(slot, ItemStack(Material.AIR))
                else inv.setItem(slot, item)
            }
            MainMenu.renderToolbar(player, inv)
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
            return listKits(plugin, sender, true)
        }
        val kits = plugin.config.getConfigurationSection("kits")!!.getKeys(false)
        when (val first = args[0]) {
            "delete", "del", "remove", "rm" -> {
                if (args.size < 2) {
                    sender.sendMessage(help)
                    return true
                }
                val second = args[1]
                kits.forEach {
                    if (it.equals(second, true)) {
                        plugin.config.set("kits.$second", null)
                        plugin.saveConfig()
                        return true
                    }
                }
                return kitNotFound(sender, second)
            }

            "seticon" -> return set(sender as Player, args[1], sender.inventory.itemInMainHand.clone(), KitPart.ICON)

            "seteffect", "seteffects" -> return set(sender as Player, args[1], sender.inventory.itemInMainHand.clone(), KitPart.POTIONEFFECTS)

            "list", "ls", "lists" -> return listKits(plugin, sender, false)
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
                val blacklist = mutableListOf("create", "new", "view", "preview", "pv")
                blacklist.addAll(Bukkit.getOnlinePlayers().map { it.name })
                blacklist.addAll(Bukkit.getOfflinePlayers().map { it.name!! })
                if (blacklist.any { second.equals(it, true) }) {
                    sender.sendMessage("${LIGHT_PURPLE}You can't use $DARK_PURPLE$second ${LIGHT_PURPLE}as a kit name")
                    return true
                }

                if ((sender as Player).inventory.itemInMainHand.type != Material.AIR) {
                    set(sender as Player, args[1], sender.inventory.itemInMainHand.clone())
                }
            }

            else -> {
                if (kits.any { first.equals(it, true) }) {
                    loadKit(plugin, sender as Player, first, label)
                } else {
                    return kitNotFound(sender, first)
                }
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
                val first = mutableListOf("create", "preview", "pv", "setaliases", "seticon", "setalias", "setinv", "setinventory", "setarmor", "seteffects")
                first.addAll(kits)
                return first.filter { it.startsWith(args[0], true) }
            }
            args[0] in listOf("preview", "pv", "setaliases", "seticon", "setalias", "setinv", "setinventory", "setarmor", "seteffects") -> {
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
            for ((index, item) in player.inventory.contents.withIndex()) {
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
            kit["author"] = player.uniqueId.toString()
            kit["uses"] = 0
            kit["created"] = System.currentTimeMillis()
            plugin.saveConfig()
        }

        val msg = TextComponent("$DARK_PURPLE* ${player.name} ${LIGHT_PURPLE}created kit ")
        val claimButton = TextComponent("$GRAY[$GREEN$name$GRAY]")
        claimButton.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kit preview $name")
//        claimButton.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kit open $name}")
        msg.addExtra(claimButton)
        Bukkit.spigot().broadcast(msg)
        return true
    }
}