package cx.ctt.skm.score.mechanics

import cx.ctt.skm.score.Score
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

class ItemPerks(private val plugin: Score): Listener, CommandExecutor, TabCompleter {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
    }
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        TODO("Not yet implemented")
    }
}