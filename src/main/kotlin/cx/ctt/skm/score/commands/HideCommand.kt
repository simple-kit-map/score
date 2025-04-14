package cx.ctt.skm.score.commands


import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import cx.ctt.skm.score.Score

class HideCommand (private val plugin: Score): CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player){
            return false
        }

        Bukkit.getPlayer(args[0])?.let { sender.hideEntity(plugin, it) }
        return true
    }
}