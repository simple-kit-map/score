package cx.ctt.skm.score.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player

class setUnbreakable() : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            return true
        }
        run {
            val hand = sender.inventory.itemInHand
            val meta = hand.itemMeta!!
            if (args.isNotEmpty() && args.contains("unbreaking")) meta.removeEnchant(Enchantment.UNBREAKING)
            meta.isUnbreakable = meta.isUnbreakable != true
        }

        if (args.isNotEmpty() && args.contains("armor")) {
            sender.inventory.armorContents.forEach {
                val meta = it?.itemMeta
                if (meta != null) {
                    meta.isUnbreakable = meta.isUnbreakable != true

                    if (args.isNotEmpty() && args.contains("unbreaking")) meta.removeEnchant(Enchantment.UNBREAKING)
                    it.itemMeta = meta
                }
            }
        }
        return true
    }
}