package cx.ctt.skm.score

import cx.ctt.skm.score.commands.Kit
import cx.ctt.skm.score.commands.KnockbackCommand
import cx.ctt.skm.score.commands.PlayerStatus
import cx.ctt.skm.score.commands.WarpCommand
import net.md_5.bungee.api.ChatColor.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal


class MainMenu(private val plugin: Score) : Listener {
    companion object {
        fun listSection(section: ConfigurationSection, player: Player, page: Int = 1) {

            val elements = (if (section.name == "warps") {
                WarpCommand.getWarpNamesRecursively(section)
            } else {
                section.getKeys(false)
            }).sorted()

            if (elements.isEmpty()) {
                player.sendMessage("No ${section.name} found"); return
            }
            val invTitle = when (section.name) {
                "kits" -> "$DARK_PURPLE* ${elements.size} ${LIGHT_PURPLE}kits are available:"
                "warps" -> "$DARK_PURPLE* ${elements.size} ${LIGHT_PURPLE}warps are available:"
                "status" -> "$DARK_PURPLE* ${elements.size} ${LIGHT_PURPLE}players are connected:"
                "mechanics" -> "$DARK_PURPLE* ${elements.size} ${LIGHT_PURPLE}mechanics are available:"
                else -> error("MainMenu#listSection: Unknown section name: ${section.name}, please implement a dedicated inventory title")
            }
            val inv = Bukkit.createInventory(null, 54, invTitle)

            val itemsPerPage = 45

            renderToolbar(
                player, inv,
                currentPage = page,
                previous = page > 1,
                next = page * itemsPerPage < elements.size,
            )

            val startIndex = (itemsPerPage * (page - 1))
            var slot = 0
            for (index in startIndex..elements.size) {
                if (index == elements.size || slot == itemsPerPage) break
                val el = elements[index]

                when (section.name) {
                    "mechanics" -> {
                        val mechIcon = section.getItemStack("$el.icon") ?: ItemStack(Material.FILLED_MAP)
//                        mechIcon.editMeta(MapMeta::class.java) { meta -> meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP) }
                        val meta = mechIcon.itemMeta!!
                        meta.lore = emptyList()
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                        val lore: MutableList<String> = mutableListOf()

                        KnockbackCommand.mechMeta.forEach { mechMeta ->
                            val mechVal = section.get("values.${mechMeta.name.lowercase().replace(" ", "")}")
                            if (mechVal != null) {
                                lore.add("${mechMeta.name}: $mechVal")
                            }
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
                }
                slot++
            }
            player.openInventory(inv)
        }

        fun renderToolbar(
            p: Player, inv: Inventory, currentPage: Int = 1, previous: Boolean = false, next: Boolean = false
        ): Inventory {
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
                meta.setOwningPlayer(p)
                pplInfo.itemMeta = meta
                inv.setItem(49, pplInfo)
            }
            run {
                val pplInfo = ItemStack(Material.PAPER)
                val meta = pplInfo.itemMeta!!
                meta.setDisplayName("${AQUA}warps")
                pplInfo.itemMeta = meta
                inv.setItem(48, pplInfo)
            }
            run {
                val pplInfo = ItemStack(Material.ENCHANTED_BOOK)
                val meta = pplInfo.itemMeta!!
                meta.setDisplayName("${AQUA}kits")
                pplInfo.itemMeta = meta
                inv.setItem(47, pplInfo)
            }
            run {
                val pplInfo = ItemStack(Material.FILLED_MAP)
                val meta = pplInfo.itemMeta!!
                meta.setDisplayName("${AQUA}mechanics")
                pplInfo.itemMeta = meta
                inv.setItem(50, pplInfo)
            }
            p.openInventory(inv)
            return inv
        }
    }

    enum class MenuType {
        Kit, KitPreview, Warp, PlayerStatus, Mechanic
    }

    @EventHandler
    fun onInventoryClickEvent(e: InventoryClickEvent) {
//        e.whoClicked.sendMessage("clicked ${e.slot}")
//        if (e.slot == -999) e.view.close() // close inv when clicking outside of inventory
        val invTitle = e.view.title
        if (invTitle == "Crafting") return
        val slotInToolbar = e.slot in 45..53
        if (e.currentItem == null) return
        if (e.currentItem!!.type == Material.AIR) return



        fun handleMenuBarClicks(type: MenuType) {
            when (e.slot) {
                45, 53 -> {
                    if (e.currentItem != null && e.currentItem!!.type == Material.ARROW) {
                        val targetPage = e.currentItem!!.itemMeta!!.displayName.replace("$RESET", "").toInt()
                        when (type) {
                            MenuType.Warp -> {
                                MainMenu.listSection(
                                    plugin.config.getConfigurationSection("warps")!!,
                                    e.whoClicked as Player,
                                    page = targetPage
                                )
                            }

                            else -> {
                                error("couleur gotta implement pagination for ${type.name}")
                            }
                        }
                    }
                }

                47 -> Kit.listKits(plugin, e.whoClicked as Player, true)
                48 -> MainMenu.listSection(plugin.config.getConfigurationSection("warps")!!, e.whoClicked as Player)
                49 -> PlayerStatus.listPlayerStatus(plugin, e.whoClicked as Player)
                50 -> MainMenu.listSection(plugin.config.getConfigurationSection("mechanics")!!, e.whoClicked as Player)
            }
        }
        when {
            invTitle.matches(".*players are connected:.*".toRegex()) -> {
                e.isCancelled = true
                handleMenuBarClicks(MenuType.PlayerStatus)
                if (slotInToolbar) return
                if (e.currentItem!!.type != Material.PLAYER_HEAD) {
                    plugin.logger.info("how tf did ${e.whoClicked} click slot ${e.slot}?")
                    return
                }
                val lore = e.currentItem!!.itemMeta!!.lore
                if (lore == null) {
                    plugin.logger.warning("${e.whoClicked} clicked a player head without lore..??")
                    return
                }
                when (e.click) {
                    ClickType.LEFT -> {
                        val warp = lore[0].replace("Warp: §b", "")
                        WarpCommand.teleportToWarp(plugin, e.whoClicked as Player, warp)
                    }

                    ClickType.SHIFT_LEFT -> {
                        val warp = lore[1].replace("Kit: §b", "")
                        Kit.loadKit(plugin, e.whoClicked as Player, warp)
                    }

                    ClickType.RIGHT -> {
                        val warp = lore[2].replace("Knockback: §b", "")
                        KnockbackCommand.setMechanic(e.whoClicked as Player, warp, plugin)
                    }

                    ClickType.MIDDLE -> TODO()
                    else -> {}
                }
            }

            invTitle.matches(".*warps are available:.*".toRegex()) -> {
                handleMenuBarClicks(MenuType.Warp)
                if (slotInToolbar) return
                e.isCancelled = true
                if (e.click == ClickType.LEFT) {
                    val warp = e.currentItem?.itemMeta?.lore?.last()?.replace("§8/w ", "")!!
                    WarpCommand.teleportToWarp(plugin, e.whoClicked as Player, warp)
                }
            }

            invTitle.matches(".*Kit .*by .*".toRegex()) -> {
                if (e.slot == -999) Kit.listKits(plugin, e.whoClicked as Player, true)
                if (e.click != ClickType.MIDDLE) e.isCancelled = true
                handleMenuBarClicks(MenuType.Kit)
                if (slotInToolbar) return
            }


            invTitle.matches(".*mechanic by .*".toRegex()) -> {
                val mechName = stripColor(invTitle).split(" ")[1]

                e.isCancelled = true
                handleMenuBarClicks(MenuType.Kit)
                if (slotInToolbar) return
                val keyval = stripColor(e.currentItem?.itemMeta?.displayName)
                    ?: error("${e.whoClicked} claimed non existent mechanic: ${e.currentItem}")

                val (key, value) = keyval.split(":", limit = 2).map { it.trim() }
                val internalKey = key.lowercase().replace(" ", "")

                val type = (KnockbackCommand.mechMeta.find { it.name == key }!!).type
                val (diff, newVal) = when (type) {
                    KnockbackCommand.Companion.mechType.FLOAT, KnockbackCommand.Companion.mechType.DOUBLE ->
                        when (e.click) {
                            ClickType.LEFT -> Pair("${RED}-0.1", (BigDecimal(-0.1) + value.toBigDecimal()).toFloat())
                            ClickType.SHIFT_LEFT -> Pair(
                                "${RED}-0.5",
                                (BigDecimal(-0.5) + value.toBigDecimal()).toFloat()
                            )

                            ClickType.RIGHT -> Pair("${GREEN}+0.1", (BigDecimal(0.1) + value.toBigDecimal()).toFloat())
                            ClickType.SHIFT_RIGHT -> Pair(
                                "${GREEN}+0.5",
                                (BigDecimal(0.5) + value.toBigDecimal()).toFloat()
                            )

                            else -> return
                        }

                    KnockbackCommand.Companion.mechType.INT ->
                        when (e.click) {
                            ClickType.LEFT -> Pair("${RED}-1", -1 + value.toInt())
                            ClickType.SHIFT_LEFT -> Pair("${RED}-3", -3 + value.toInt())
                            ClickType.RIGHT -> Pair("${GREEN}+1", 1 + value.toInt())
                            ClickType.SHIFT_RIGHT -> Pair("${GREEN}+3", 3 + value.toInt())
                            else -> return
                        }

                    KnockbackCommand.Companion.mechType.BOOLEAN ->
                        when (e.click) {
                            ClickType.LEFT -> Pair("${RED}off", false)
                            ClickType.RIGHT -> Pair("${GREEN}on", true)
                            ClickType.MIDDLE -> Pair("${GOLD}toggled", !(value.toBooleanStrictOrNull() ?: false))
                            else -> return
                        }
                }
                Bukkit.broadcastMessage("${e.whoClicked.name} $key: $diff")
                val path = "mechanics.$mechName.values.$internalKey"
                if (!plugin.config.contains(path)) error("Tried adding unknown value $internalKey")
                if (plugin.config.getString(path) != value) error("Config value and parsed are different")
                Bukkit.broadcastMessage("${GRAY}mechanics.$mechName.values.$internalKey = $newVal (${newVal.javaClass.simpleName})")
//                plugin.config.set("mechanics.$mechName.values.$internalKey", newVal)
            }

            invTitle.matches(".*mechanics are available:.*".toRegex()) -> {
                e.isCancelled = true
                handleMenuBarClicks(MenuType.Kit)
                if (slotInToolbar) return
                val mechName = stripColor(e.currentItem?.itemMeta?.displayName)
                    ?: error("${e.whoClicked} claimed non existent mechanic: ${e.currentItem}")
                when (e.click) {
                    ClickType.LEFT -> KnockbackCommand.setMechanic(e.whoClicked as Player, mechName, plugin)
                    ClickType.RIGHT -> KnockbackCommand.previewMechanic(plugin, mechName, e.whoClicked as Player)
                    else -> {}
                }
            }

            invTitle.matches(".*kits are available:.*".toRegex()) -> {
                handleMenuBarClicks(MenuType.Kit)
                if (slotInToolbar) return
                when (e.click) {
                    ClickType.LEFT -> {
                        val kitName = e.currentItem?.itemMeta?.lore?.last()?.replace("§8/k ", "")
                        if (kitName != null) Kit.loadKit(plugin, e.whoClicked as Player, kitName)
                        else plugin.logger.warning("${e.whoClicked} claimed non existent kit: ${e.currentItem}")
                        e.isCancelled = true
                    }

                    ClickType.RIGHT -> {
                        val kitName = e.currentItem?.itemMeta?.lore?.last()?.replace("§8/k ", "")!!
                        Kit.previewKit(plugin, e.whoClicked as Player, kitName)
                        e.isCancelled = true
                    }

                    else -> {}
                }
            }
        }
    }
}