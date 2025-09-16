package cx.ctt.skm.score.commands

import cx.ctt.skm.score.Score
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CapCommand(private val plugin: Score): CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val cb = Bukkit.getPluginManager().getPlugin("CheatBreakerAPI")
        if (cb == null){
            sender.sendMessage("CheatBreakerAPI plugin not found, is it in /pl?")
            return true
        }
        val isCBUser = cb.javaClass.getMethod("isRunningCheatBreaker", Player::class.java)
        val ret = isCBUser.invoke(cb, sender as Player) as Boolean
        if (!ret){
            sender.sendMessage("Warning: you were not detected using CheatBreaker, cap will do nothing")
            sender.sendMessage("")
        }
        if (args.size != 3){
            sender.sendMessage("CheatBreaker.net CPS cap")
            sender.sendMessage("")
            sender.sendMessage("See #cps-cap in the discord for syntax information")
            sender.sendMessage("Disable all caps: /cap -1 -1 -1")
            sender.sendMessage("")
            return true
        }
        val capMethod = cb.javaClass.getMethod("setSwingInhibitor", Player::class.java, Int::class.java, Int::class.java, Double::class.java, Int::class.java)
        capMethod.invoke(cb, sender, 0, args[0].toIntOrNull(), args[1].toDoubleOrNull(), args[2].toIntOrNull())
        capMethod.invoke(cb, sender, 1, args[0].toIntOrNull(), args[1].toDoubleOrNull(), args[2].toIntOrNull())
        return true;
    }
}