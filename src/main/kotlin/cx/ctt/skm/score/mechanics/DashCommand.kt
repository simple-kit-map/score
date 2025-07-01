package cx.ctt.skm.score.mechanics

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.util.Vector


class Dash : CommandExecutor, Listener{
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            val mult = if (args.isNotEmpty()) args[0].toDoubleOrNull() ?: 2.0 else 2.0
            if (mult > 50){
                sender.sendMessage("nah thats too much bro, 50 at most should be sufficient")
                return true
            }
            dash(sender, mult)
        }
        return true
    }
    private fun dash(player: Player, multiplier: Double = 2.0) {
        val playerDirection: Vector = player.location.direction
        playerDirection.multiply(multiplier)
        playerDirection.x += player.velocity.x
        playerDirection.y += player.velocity.y + 0.5
        playerDirection.z += player.velocity.z
        player.velocity = playerDirection
    }
}