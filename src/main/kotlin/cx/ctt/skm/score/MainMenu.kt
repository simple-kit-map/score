package cx.ctt.skm.score

import cx.ctt.skm.score.commands.DuelCommand
import cx.ctt.skm.score.commands.DuelCommand.Companion.sendDuel
import cx.ctt.skm.score.commands.Kit
import cx.ctt.skm.score.commands.KnockbackCommand
import cx.ctt.skm.score.commands.WarpCommand
import net.md_5.bungee.api.ChatColor.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.util.*


public enum class MenuType : InventoryHolder {
    Kit { override fun getInventory(): Inventory = TODO() },
    KitPreview { override fun getInventory(): Inventory = TODO() },
    KitDuel { override fun getInventory(): Inventory = TODO() },
    KitPreviewDuel { override fun getInventory(): Inventory = TODO() },

    Mechanic { override fun getInventory(): Inventory = TODO() },
    MechanicPreview { override fun getInventory(): Inventory = TODO() },
    MechanicDuel { override fun getInventory(): Inventory = TODO() },
    MechanicPreviewDuel { override fun getInventory(): Inventory = TODO() },
    MechanicSetValue { override fun getInventory(): Inventory = TODO() },

    Warp { override fun getInventory(): Inventory = TODO() },
    WarpDuel{ override fun getInventory(): Inventory = TODO() },
    Status { override fun getInventory(): Inventory = TODO() },
    StatusDuel { override fun getInventory(): Inventory = TODO() },
}

class MainMenu(private val plugin: Score) : Listener {
    companion object {
        fun listSection(plugin: Score, player: Player, page: Int = 1, mType: MenuType): Boolean {

            var sectionName = mType.name.lowercase().replace("duel", "")
            if (sectionName.last() != 's') sectionName += "s"
            val section = plugin.config.getConfigurationSection(sectionName)!!

            val elements = when (section.name){
//                "warps" -> WarpCommand.getWarpNamesRecursively(section)
                "status" -> Bukkit.getOnlinePlayers().map { it.uniqueId.toString() }
                else -> section.getKeys(false)
            }.toList()

            if (elements.isEmpty()) {
                player.sendMessage("No ${section.name} found")
                return false
            }
            val invTitle = when (section.name) {
                "kits" -> "$DARK_PURPLE* ${elements.size} ${LIGHT_PURPLE}kits are available:"
                "warps" -> "$DARK_PURPLE* ${elements.size} ${LIGHT_PURPLE}warps are available:"
                "status" -> "$DARK_PURPLE* ${elements.size} ${LIGHT_PURPLE}players are connected:"
                "mechanics" -> "$DARK_PURPLE* ${elements.size} ${LIGHT_PURPLE}mechanics are available:"
                else -> error("MainMenu#listSection: Unknown section name: ${section.name}, please implement a dedicated inventory title")
            }

            val inv = Bukkit.createInventory(mType, 54, invTitle)

            val itemsPerPage = 45


            val startIndex = (itemsPerPage * (page - 1))
            var slot = 0
            for (index in startIndex..elements.size) {
                if (index == elements.size || slot == itemsPerPage) break
                val el = elements[index]

                when (section.name) {
                    "mechanics" -> {
                        val mechIcon = section.getItemStack("$el.icon") ?: ItemStack(Material.FILLED_MAP)
                        val meta = mechIcon.itemMeta!!
                        meta.lore = emptyList()
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                        val lore: MutableList<String> = mutableListOf()
                        val creatorId = UUID.fromString(section.getString("$el.creator")!!)
                        val username = Bukkit.getPlayer(creatorId)?.name ?: Bukkit.getOfflinePlayer(creatorId).name ?: "Unknown?"
                        lore.add("${LIGHT_PURPLE}by $DARK_PURPLE$username")

                        if (username == player.name)
                            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

                        KnockbackCommand.mechMeta.forEach { (internalKey, mechMeta) ->
                            val mechVal = section.get("$el.values.$internalKey") ?: mechMeta.defaultValue
                            val valColor = if (mechVal == mechMeta.defaultValue) GRAY else AQUA
                            lore.add("${mechMeta.name}$DARK_GRAY: $valColor$mechVal")
                        }
                        meta.lore = lore
                        meta.setDisplayName("$RESET$el")
                        mechIcon.itemMeta = meta
                        inv.setItem(slot, mechIcon)
                    }

                    "warp", "warps" -> {
                        val warpIcon = section.getItemStack("$el.icon") ?: ItemStack(Material.GUNPOWDER)
                        val meta = warpIcon.itemMeta!!
                        if (!meta.hasDisplayName()) meta.setDisplayName("${RESET}$el")
                        val author = section["creator"]
                        val lore = if (author != null) mutableListOf("added by $author") else mutableListOf()
                        lore.add("$DARK_GRAY/w $el")
                        meta.lore = lore

                        warpIcon.itemMeta = meta
                        inv.setItem(slot, warpIcon)
                    }
                    "kit", "kits" -> {

                        val color = DARK_PURPLE
                        val kitSection = section.getConfigurationSection(el)!!
                        val icon = kitSection.getItemStack("icon") ?: ItemStack(Material.BOOK)
                        val meta = icon.itemMeta!!
                        meta.setDisplayName("$color$el")

                        val lore = mutableListOf<String>()
                        kitSection.getString("author")?.let {
                            val id = UUID.fromString(it)
                            var authorName = (Bukkit.getPlayer(id) ?: Bukkit.getOfflinePlayer(id)).name ?: "Unknown???"
                            authorName = if (authorName == player.name) "${RED}you" else "${DARK_PURPLE}$authorName"
                            lore.add("${LIGHT_PURPLE}by $authorName")
                        }

                        lore.add("$DARK_GRAY/k ${el.lowercase()}")
                        meta.lore = lore
                        icon.itemMeta = meta
                        inv.setItem(slot, icon)
                    }
                    "status" -> {
                        val pstatus = plugin.config.getConfigurationSection("status.$el")!!
                        val warp = pstatus.getString("warp")!!
                        val kit = pstatus.getString("kit")!!
                        val knockback = pstatus.getString("mechanic")!!

                        val statusItem = ItemStack(Material.PLAYER_HEAD)
                        val meta = statusItem.itemMeta!! as org.bukkit.inventory.meta.SkullMeta
                        val curPlayer = Bukkit.getPlayer(UUID.fromString(el)) ?: continue

                        val color  = DuelCommand.Companion.duels[player]?.let {
                            if (curPlayer in it.opps) RED
                            else if (curPlayer in it.gang) GREEN
                            else GREEN
                        } ?: GREEN

                        meta.setDisplayName("$color${curPlayer.name}")
                        meta.owningPlayer = curPlayer
                        meta.lore = mutableListOf(
                            "",
                            "${YELLOW}Warp: $LIGHT_PURPLE${warp}",
                            "${YELLOW}Kit: $LIGHT_PURPLE${kit}",
                            "${YELLOW}Knockback: $LIGHT_PURPLE${knockback}"
                        )
                        statusItem.itemMeta = meta
                        inv.setItem(slot, statusItem)
                    }

                    else -> {
                        player.sendMessage("No implementation for ${section.name}!")
                        plugin.logger.severe("No implementation for ${section.name}!")
                        return true
                    }
                }
                slot++
            }

            renderToolbar(
                plugin,
                player, inv,
                currentPage = page,
                previous = page > 1,
                next = page * itemsPerPage < elements.size,
            )
            player.openInventory(inv)
            return true
        }

        fun renderToolbar(
            plugin: Score, p: Player, inv: Inventory, currentPage: Int = 1, previous: Boolean = false, next: Boolean = false, duel: Boolean = false): Inventory {
            val pStatus = plugin.config.getConfigurationSection("status.${p.uniqueId}")!!
            if (previous) {
                val previousItem = ItemStack(Material.ARROW)
                val itemMeta = previousItem.itemMeta!!
                itemMeta.setDisplayName("${RESET}${currentPage - 1}")
                previousItem.itemMeta = itemMeta
                inv.setItem(45, previousItem)
            }
            if (next) {
                val nextItem = ItemStack(Material.ARROW)
                val itemMeta = nextItem.itemMeta!!
                itemMeta.setDisplayName("${RESET}${currentPage + 1}")
                nextItem.itemMeta = itemMeta
                inv.setItem(53, nextItem)
            }
            run {
                val pplInfo = ItemStack(Material.PLAYER_HEAD)
                val meta = pplInfo.itemMeta as org.bukkit.inventory.meta.SkullMeta
                meta.setDisplayName("${AQUA}status")
                meta.lore = listOf("$GRAY$ITALIC${Bukkit.getOnlinePlayers().size} online")
                meta.owningPlayer = p
                pplInfo.itemMeta = meta
                inv.setItem(49, pplInfo)
            }
            run {
                val pplInfo = ItemStack(Material.PAPER)
                val meta = pplInfo.itemMeta!!
                meta.setDisplayName("${AQUA}warps")
                val curWarp = pStatus.getString("warp")
                if (curWarp != null) meta.lore = listOf("${GRAY}${ITALIC}$curWarp")
                pplInfo.itemMeta = meta
                inv.setItem(48, pplInfo)
            }
            run {
                val pplInfo = ItemStack(Material.ENCHANTED_BOOK)
                val meta = pplInfo.itemMeta!!
                meta.setDisplayName("${AQUA}kits")
                val curKit = pStatus.getString("kit")
                if (curKit != null) meta.lore = listOf("${GRAY}${ITALIC}$curKit")
                pplInfo.itemMeta = meta
                inv.setItem(47, pplInfo)
            }
            run {
                val pplInfo = ItemStack(Material.FILLED_MAP)
                val meta = pplInfo.itemMeta!!
                meta.setDisplayName("${AQUA}mechanics")
                val curMech = pStatus.getString("mechanic")
                if (curMech != null) meta.lore = listOf("${GRAY}${ITALIC}$curMech")
                pplInfo.itemMeta = meta
                inv.setItem(50, pplInfo)
            }
            p.openInventory(inv)
            return inv
        }
    }


    @EventHandler
    fun onAnvilPrepare (e: PrepareAnvilEvent) {
        null
    }

    @EventHandler
    fun onInventoryClickEvent(e: InventoryClickEvent) {
        if (e.inventory.holder !is MenuType) return
        if (e.slot == -999) return // clicking outside the inventory
        if (e.currentItem == null) return
        if (e.currentItem!!.type == Material.AIR) return
        e.isCancelled = true

        val p = e.whoClicked as Player
        val invTitle = e.view.title
        val type = MenuType.entries.first { it == e.inventory.holder }
        val isDuel = type.name.lowercase().contains("duel")

        var clickedToolbar = true
        when (e.slot) {
            45, 53 -> if (e.currentItem!!.type == Material.ARROW)
                  listSection(plugin,p, page = stripColor(e.currentItem!!.itemMeta!!.displayName).toInt(), type)
            47 -> listSection(plugin, p, mType = (if (isDuel) MenuType.Kit else MenuType.KitDuel))
            48 -> listSection(plugin, p, mType = (if (isDuel) MenuType.Warp else MenuType.WarpDuel))
            49 -> listSection(plugin, p, mType = (if(isDuel) MenuType.Status else MenuType.StatusDuel))
            50 -> listSection(plugin, p, mType = (if(isDuel) MenuType.Mechanic else MenuType.MechanicDuel))
            else -> clickedToolbar = false
        }
        if (clickedToolbar) return

        // this is a very cursed way to check if the inventory is a duel
        // this makes it more convenient to check if it's a duel without needing unnecessary hashmap!!.key indexing
        val duelState = if (isDuel) DuelCommand.duels[e.whoClicked] else null

        when (type){
            MenuType.Kit, MenuType.KitDuel -> {

                val kitName = stripColor(e.currentItem?.itemMeta?.lore?.last())?.replace("/k ", "")
                if (kitName == null){
                    e.whoClicked.sendMessage("you clicked non existent kit: ${e.currentItem.toString()}")
                    plugin.logger.warning("${e.whoClicked} claimed non existent kit: ${e.currentItem}")
                    return
                }
                when (e.click) {
                    ClickType.LEFT -> {
                        if (duelState == null)
                            Kit.loadKit(plugin, p, kitName)
                        else {
                            duelState.kit = kitName
                            if (duelState.opps.isEmpty())
                                listSection(plugin, p, mType = MenuType.StatusDuel)
                            else sendDuel(p)
                        }
                    }

                    ClickType.RIGHT, ClickType.SHIFT_RIGHT -> {
                        if (duelState == null){
                            Kit.previewKit(plugin, p, kitName)
                        } else {
                            duelState.kit = kitName
                            if (duelState.opps.isEmpty()) listSection(plugin, p, mType = MenuType.StatusDuel)
                            else if (duelState.warp == null) listSection(plugin,p, mType = MenuType.WarpDuel)
                            else if (duelState.mechanic == null) listSection(plugin,p, mType = MenuType.MechanicDuel)
                            else sendDuel(p)
                        }
                    }

                    else -> {}
                }
            }

            MenuType.KitPreview, MenuType.KitPreviewDuel -> if (e.click == ClickType.MIDDLE) e.isCancelled = false

            MenuType.Warp, MenuType.WarpDuel -> {
                val warp = stripColor(e.currentItem?.itemMeta?.lore?.last())?.replace("/w ", "")!!
                if (duelState == null){
                    if (e.click == ClickType.LEFT) {
                        WarpCommand.teleportToWarp(plugin, p, warp)
                    }
                } else {
                    duelState.warp = warp
                    if (e.click == ClickType.LEFT) {
                        if (duelState.opps.isEmpty()) listSection(
                            plugin,
                            p,
                            mType = MenuType.StatusDuel
                        )
                        else sendDuel(p)
                    } else if (e.click == ClickType.RIGHT) {
                        if (duelState.opps.isEmpty()) listSection(
                            plugin,
                            p,
                            mType = MenuType.StatusDuel
                        )
                        else if (duelState.kit == null) listSection(
                            plugin,
                            p,
                            mType = MenuType.KitDuel
                        )
                        else if (duelState.mechanic == null) listSection(
                            plugin,
                            p,
                            mType = MenuType.MechanicDuel
                        )
                        else sendDuel(p)
                    }
                }
            }

            MenuType.Status, MenuType.StatusDuel -> {
                if (e.currentItem!!.type != Material.PLAYER_HEAD) {
                    plugin.logger.info("how tf did ${e.whoClicked} click slot ${e.slot}?")
                    return
                }
                val lore = e.currentItem?.itemMeta?.lore
                if (lore == null) {
                    plugin.logger.warning("${e.whoClicked} clicked a player head without lore..??")
                    return
                }

                if (duelState == null)
                    when (e.click) {
                        ClickType.LEFT -> {
                            val warp = lore[0].replace("Warp: §b", "")
                            WarpCommand.teleportToWarp(plugin, p, warp)
                        }

                        ClickType.SHIFT_LEFT -> {
                            val warp = lore[1].replace("Kit: §b", "")
                            Kit.loadKit(plugin, p, warp)
                        }

                        ClickType.RIGHT -> {
                            val warp = lore[2].replace("Knockback: §b", "")
                            KnockbackCommand.setMechanic(p, warp, plugin)
                        }
                        else -> return
                    }
                else {
                    val headName = stripColor(e.currentItem?.itemMeta?.displayName)
                    if (headName == null) {
                        e.whoClicked.sendMessage("you tried clicking a non existent player, report that pls :P")
                        return
                    }
                    val whoGotClicked = Bukkit.getPlayer(headName)
                    if (whoGotClicked == null) {
                        e.whoClicked.sendMessage("player $headName does not exist...?? report that pls :P")
                        return
                    }
                    if (whoGotClicked == e.whoClicked){
                        e.whoClicked.sendMessage("${RED}you can't duel yourself")
                        return
                    }

                    when (e.click) {
                        ClickType.DROP -> {
                            if (whoGotClicked in duelState.opps) {
                                e.whoClicked.sendMessage("Removed $DARK_PURPLE${whoGotClicked.name} ${RESET}from the enemy team!")
                                duelState.opps.remove(whoGotClicked)
                            }
                            if (whoGotClicked in duelState.gang) {
                                e.whoClicked.sendMessage("Removed $DARK_PURPLE${whoGotClicked.name} ${RESET}from your team!")
                                duelState.gang.remove(whoGotClicked)
                            }
                        }
                        ClickType.LEFT -> {
                            duelState.opps.add(whoGotClicked)
                            if (whoGotClicked in duelState.gang) duelState.gang.remove(whoGotClicked)
                            if (duelState.kit == null)
                                listSection(
                                    plugin,
                                    p,
                                    mType = MenuType.KitDuel
                                )
                            else sendDuel(p)
                        }
                        ClickType.RIGHT -> {
                            e.whoClicked.sendMessage("Left-click to duel a single player")
                            e.whoClicked.sendMessage("SHIFT+LEFT/RIGHT click to add to your/the enemy team")
                            e.whoClicked.sendMessage("Then confirm anywhere MIDDLE mouse button")
                        }
                        ClickType.MIDDLE -> {
                            if (duelState.gang.isEmpty()) {
                                e.whoClicked.sendMessage("The opponent team is empty")
                                return
                            }
                            sendDuel(p)
                        }

                        ClickType.SHIFT_LEFT -> {
                            if (whoGotClicked in duelState.gang) {
                                p.sendMessage("${GREEN}Removed $LIGHT_PURPLE${whoGotClicked.name} ${GREEN}from your team")
                                duelState.gang.remove(whoGotClicked)
                            }
                            else {
                                p.sendMessage("${GREEN}Added $LIGHT_PURPLE${whoGotClicked.name} ${GREEN}from your team")
                                duelState.gang.add(whoGotClicked)
                            }
                            if (whoGotClicked in duelState.opps) duelState.opps.remove(whoGotClicked)

                        }

                        ClickType.SHIFT_RIGHT -> {
                            if (whoGotClicked in duelState.opps) {
                                p.sendMessage("${RED}Removed $LIGHT_PURPLE${whoGotClicked.name} ${RED}from enemy team")
                                duelState.opps.remove(whoGotClicked)
                            }
                            else {
                                p.sendMessage("${RED}Added $LIGHT_PURPLE${whoGotClicked.name} ${RED}from enemy team")
                                duelState.opps.add(whoGotClicked)
                            }
                            if (whoGotClicked in duelState.gang) duelState.gang.remove(whoGotClicked)
                        }
                        else -> {

                        }
                    }
                }
            }

            MenuType.Mechanic, MenuType.MechanicDuel -> {
                val mechName = stripColor(e.currentItem?.itemMeta?.displayName)
                    ?: error("${e.whoClicked} claimed non existent mechanic: ${e.currentItem}")
                when (e.click) {
                    ClickType.LEFT -> {
                        if (duelState == null)
                            KnockbackCommand.setMechanic(p, mechName, plugin)
                        else {
                            duelState.mechanic = mechName
                            if (duelState.opps.isEmpty()) listSection(
                                plugin,
                                p,
                                mType = MenuType.StatusDuel
                            )
                            else sendDuel(p)
                        }
                    }
                    ClickType.RIGHT -> {
                        if (duelState == null)
                        KnockbackCommand.previewMechanic(plugin, mechName, p)
                        else {
                            duelState.mechanic = mechName
                            if (duelState.opps.isEmpty()) listSection(
                                plugin,
                                p,
                                mType = MenuType.StatusDuel
                            )
                            else if (duelState.warp == null) listSection(
                                plugin,
                                p,
                                mType = MenuType.WarpDuel
                            )
                            else if (duelState.kit == null) listSection(
                                plugin,
                                p,
                                mType = MenuType.KitDuel
                            )
                            else sendDuel(p)
                        }
                    }
                    else -> {}
                }
            }

            MenuType.MechanicPreview, MenuType.MechanicPreviewDuel -> {
//            invTitle.matches(".*mechanic by .*".toRegex()) -> {

                if (e.slot == 0) return
                val mechName = stripColor(invTitle).split(" ")[1]

//                e.isCancelled = true
//                handleToolBarClicks(MenuType.Mechanic)
//                if (slotInToolbar) return
                val keyval = stripColor(e.currentItem?.itemMeta?.displayName)
                    ?: error("${e.whoClicked} claimed non existent mechanic: ${e.currentItem}")

                val (key, value) = keyval.split(":", limit = 2).map { it.trim() }
                val internalKey = key.lowercase().replace(" ", "")

                val type = (KnockbackCommand.mechMeta.values.find { it.name == key }!!).defaultValue
                val (diff, newVal: Any) = when (type) {
                    is Double ->
                        when (e.click) {
                            ClickType.LEFT -> Pair("${RED}-0.1", value.toBigDecimal().subtract(0.1.toBigDecimal()).toString().toDouble())
                            ClickType.SHIFT_LEFT -> Pair(
                                "${RED}-0.5",
                                value.toBigDecimal().subtract(0.5.toBigDecimal()).toString().toDouble()
                            )

                            ClickType.RIGHT -> Pair("${GREEN}+0.1", value.toBigDecimal().add(0.1.toBigDecimal()).toString().toDouble())
                            ClickType.SHIFT_RIGHT -> Pair(
                                "${GREEN}+0.5",
                                value.toBigDecimal().add(0.5.toBigDecimal()).toString().toDouble()
                            )

                            else -> return
                        }

                    is Int ->
                        when (e.click) {
                            ClickType.LEFT -> Pair("${RED}-1", -1 + value.toInt())
                            ClickType.SHIFT_LEFT -> Pair("${RED}-3", -3 + value.toInt())
                            ClickType.RIGHT -> Pair("${GREEN}+1", 1 + value.toInt())
                            ClickType.SHIFT_RIGHT -> Pair("${GREEN}+3", 3 + value.toInt())
                            else -> return
                        }

                    is Boolean -> {
                        val bool = !(value.toBooleanStrictOrNull() ?: error("mech $mechName $key is not a boolean: $value"))
                        if (bool) Pair("${GREEN}on", true) else Pair("${RED}off", false)
                    }

                    else -> {
                        error("mech $mechName $key has unsupported type: $type")
                    }
                }
                Bukkit.broadcastMessage("${e.whoClicked.name} $key: $diff")
                val path = "mechanics.$mechName.values.$internalKey"
//                if (!plugin.config.contains(path)) error("Tried adding unknown value $internalKey")
//                if (plugin.config.getString(path) != value) error("Config value and parsed are different")
                Bukkit.broadcastMessage("${GRAY}mechanics.$mechName.values.$internalKey = $newVal (${newVal.javaClass.simpleName})")
                plugin.logger.info("newVal is $newVal of type ${newVal.javaClass.simpleName}")
                plugin.config.set("mechanics.$mechName.values.$internalKey", newVal)
                plugin.saveConfig()
                KnockbackCommand.previewMechanic(plugin, mechName, p)
            }

            else -> plugin.logger.severe("Unknown inventory click!")
        }
    }
}