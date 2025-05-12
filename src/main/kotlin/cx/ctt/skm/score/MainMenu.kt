package cx.ctt.skm.score

import cx.ctt.skm.score.commands.Kit
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
import org.bukkit.inventory.ItemStack

class MainMenu(private val plugin: Score) : Listener {
    companion object {
        fun listSection(section: ConfigurationSection, player: Player, page: Int = 1) {

            val elements = if (section.name == "warps") {
                WarpCommand.getWarpNamesRecursively(section)
            } else {
                section.getKeys(false) as List<String>
            }

            if (elements.isEmpty()) {
                player.sendMessage("No ${section.name} found"); return
            }
            val invTitle = when (section.name) {
                "kits" -> "$DARK_PURPLE* ${elements.size} ${LIGHT_PURPLE}kits are available:"
                "warps" -> "$DARK_PURPLE* ${elements.size} ${LIGHT_PURPLE}warps are available:"
                "status" -> "$DARK_PURPLE* ${elements.size} ${LIGHT_PURPLE}players are connected:"
                else -> error("MainMenu#listSection: Unknown section name: ${section.name}, please implement a dedicated inventory title")
            }
            val inv = Bukkit.createInventory(null, 54, invTitle)

            val itemsPerPage = 45

            renderToolbar(
                player,
                inv,
                currentPage = page,
                previous = page > 1,
                next = page * itemsPerPage < elements.size,
            )

            val startIndex = (itemsPerPage * (page - 1))
            var slot = 0
            for (index in startIndex..elements.size) {
                if (slot == 45) break
                val warp = elements[index]

                val warpIcon = section.getItemStack("$warp.icon") ?: ItemStack(Material.GUNPOWDER)
                val meta = warpIcon.itemMeta!!
                if (!meta.hasDisplayName()) meta.setDisplayName("${RESET}$warp")
                val author = section["author"]
                val lore = if (author != null) mutableListOf("added by $author") else mutableListOf()
                lore.add("$DARK_GRAY/w $warp")
                meta.lore = lore

                warpIcon.itemMeta = meta
                inv.setItem(slot, warpIcon)
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
            p.openInventory(inv)
            return inv
        }
    }

    enum class MenuType {
        Kit, KitPreview, Warp, PlayerStatus
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
                                WarpCommand.listWarps(
                                    plugin.config.getConfigurationSection("warps")!!,
                                    e.whoClicked as Player,
                                    page = targetPage
                                )
                            }

                            else -> {}
                        }
                    }
                }

                47 -> Kit.listKits(plugin, e.whoClicked as Player, true)
                48 -> WarpCommand.listWarps(plugin.config.getConfigurationSection("warps")!!, e.whoClicked as Player)
                49 -> PlayerStatus.listPlayerStatus(plugin, e.whoClicked as Player)
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
            }

            invTitle.matches(".*warps are available:.*".toRegex()) -> {
                handleMenuBarClicks(MenuType.Warp)
                if (slotInToolbar) return
                if (e.click == ClickType.LEFT) {
                    val warp = e.currentItem?.itemMeta?.lore?.last()?.replace("§8/w ", "")!!
                    WarpCommand.teleportToWarp(plugin, e.whoClicked as Player, warp, silent = false)
                    e.isCancelled = true
                }
            }

            invTitle.matches(".*Kit .*by .*".toRegex()) -> {
                if (e.slot == -999) Kit.listKits(plugin, e.whoClicked as Player, true)
                if (e.click != ClickType.MIDDLE) e.isCancelled = true
                handleMenuBarClicks(MenuType.Kit)
                if (slotInToolbar) return
            }

            invTitle.matches(".*kits are available:.*".toRegex()) -> {
                handleMenuBarClicks(MenuType.Kit)
                if (slotInToolbar) return
                when (e.click) {
                    ClickType.LEFT -> {

                        val kitName = e.currentItem?.itemMeta?.lore?.last()?.replace("§8/k ", "")
                        if (kitName != null) Kit.loadKit(plugin, e.whoClicked as Player, kitName, "kit")
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