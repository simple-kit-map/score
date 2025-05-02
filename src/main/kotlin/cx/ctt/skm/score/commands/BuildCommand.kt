package cx.ctt.skm.score.commands

import cx.ctt.skm.score.Score
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.GlowItemFrame
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Painting
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.player.PlayerInteractEntityEvent


class BuildCommand (private val plugin: Score): Listener, CommandExecutor {
    private var activated: MutableSet<Player> = mutableSetOf();
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player || args.isNotEmpty()){
            Bukkit.getPlayer(args[0])?.let { activated.add(it) }
            return true
        }
        togglePlayer(sender)
        return true
    }
    private fun togglePlayer(player: Player){
        if (player !in activated){
            activated.add(player)
            player.sendMessage("Build mode ON, you can now build")
        } else {
            activated.remove(player)
            player.sendMessage("Build mode OFF, you can no longer build")
        }
    }

    @EventHandler
    fun onBlockPlaceEvent (event: BlockPlaceEvent){
        if (event.player !in activated){
            event.player.sendMessage("/build to place blocks")
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockBreakEvent(event: BlockBreakEvent){
        if (event.player !in activated){
            event.player.sendMessage("/build to break blocks")
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockFromTo(event: BlockFromToEvent){
        if (event.block.type == Material.DRAGON_EGG){
            event.isCancelled = true
            plugin.logger.warning("cancelling dragon egg tp")
        }
    }
    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (!event.isCancelled && event.player !in activated && event.rightClicked !is Player){
            event.player.sendMessage("/build to modify entities")
            event.isCancelled = true
        }
    }
    @EventHandler
    fun onHangingBreakByEntity(event: HangingBreakByEntityEvent)
    {
        val player = event.remover as Player
        if (player !in activated){
            player.sendMessage("/build to break hanging entities")
            event.isCancelled = true
        }
    }
    @EventHandler
    fun onPlayerInteractEntityEvent(e: PlayerInteractEntityEvent) {
        if (e.player !in activated && e.rightClicked !is Player) {
            e.player.sendMessage("/build to interact with entities")
            e.isCancelled = true
        }
    }
    @EventHandler
    fun onHangingPlace(event: HangingPlaceEvent){
        if (event.player !in activated && listOf(Painting::class, ItemFrame::class, GlowItemFrame::class).any { it.isInstance(event.entity)}){
            event.player?.sendMessage("/build to place hanging entities")
            event.isCancelled = true
        }
    }
}