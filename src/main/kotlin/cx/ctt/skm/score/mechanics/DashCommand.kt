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
            dash(sender)
        }
        return true
    }
    private fun dash(player: Player, multiplier: Double = 2.0) {
        val playerDirection: Vector = player.location.direction
        playerDirection.multiply(multiplier)
        when {
            player.velocity.x > 0 -> playerDirection.x += player.velocity.x
            player.velocity.y > 0 -> playerDirection.x += player.velocity.y
            player.velocity.x > 0 -> playerDirection.x += player.velocity.x
        }
        player.velocity = playerDirection
    }
}