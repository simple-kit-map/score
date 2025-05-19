package cx.ctt.skm.score.commands

import cx.ctt.skm.score.Score
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerListPingEvent
import java.io.File

class MotdCommand(private val plugin: Score): CommandExecutor, Listener {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("score.motd")){
            sender.sendMessage("no op, no motd!!")
            return true
        }
        val args = args.map { it.replace("&", "§").replace("§§", "&").replace("\\n", "\n") }
        val motd = if (args[0].equals("header", true)){
            plugin.config.set("misc.motdHeader", args.drop(1).joinToString(" "))
            plugin.config.getString("misc.motd")
        } else {
            plugin.config.set("misc.motd", args.joinToString(" "))
            args.joinToString(" ")
        }
        val header = plugin.config.getString("misc.motdHeader")
        plugin.saveConfig()
        Bukkit.setMotd(header + motd)
        return true
    }
    @EventHandler
    fun onPluginEnable(e: org.bukkit.event.server.PluginEnableEvent){
        Bukkit.setMotd(
            plugin.config.getString("misc.motdHeader")
            + plugin.config.getString("misc.motd")
        )
    }
}