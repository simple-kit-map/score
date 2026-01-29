package cx.ctt.skm.score.commands

import cx.ctt.skm.score.Score
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class ReloadConfigCommand(private val plugin: Score): CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission("score.reload"))
        {
            sender.sendMessage("no permission")
            return true
        }
        if (args.size == 1)
        {
            if (args[0].equals("save", true))
            {
                plugin.saveConfig();
            }
        } else {
            plugin.reloadConfig();
        }
        return true;
    };
}