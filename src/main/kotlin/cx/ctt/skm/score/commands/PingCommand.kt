package cx.ctt.skm.score.commands

import cx.ctt.skm.score.Score
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatColor.*
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PingCommand(private val plugin: Score): CommandExecutor {
companion object {
    public fun formatMs(ping: Int): ChatColor {
        return when {
            ping > 199 -> DARK_RED
            ping > 149 -> RED
            ping > 99 -> GOLD
            ping > 49 -> YELLOW
            ping == 0 -> AQUA
            else -> GREEN
        }
    }
}
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player){
//            if ((System.currentTimeMillis() - plugin.essentials.getUser(sender).lastLogin) < 120000){
//                sender.sendMessage("You logged in recently, please wait a few minutes for it to stabilize")
//            }
            sender.sendMessage("${DARK_PURPLE}Your ${LIGHT_PURPLE}latency is: ${formatMs(sender.ping)}${sender.ping} ${LIGHT_PURPLE}ms")
        }
        if (args.isNotEmpty()){
            for (arg in args){
                Bukkit.getPlayer(arg).let {
                    if (it != null) {
                        var diff = if (sender is Player){(sender.ping - it.ping).toString()} else {"0"}
                        if (!diff.startsWith("-")){diff = "+$diff"}
                        sender.sendMessage("$DARK_PURPLE${it.name} ${LIGHT_PURPLE}has ${formatMs(it.ping)}${it.ping} ${LIGHT_PURPLE}ms $RESET(${diff} difference)")
                    }
                }
            }
        }
        val txt = TextComponent("skm is hosted in New York, USA on ${AQUA}sear.host")
        txt.clickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, "https://sear.host")
        sender.spigot().sendMessage(txt)

        return true
    }
}