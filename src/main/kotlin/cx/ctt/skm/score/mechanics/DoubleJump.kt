package cx.ctt.skm.score.mechanics

import org.bukkit.Bukkit
import org.bukkit.Effect
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.util.Vector
import cx.ctt.skm.score.Score

class DoubleJump (private val plugin: Score) : CommandExecutor, Listener {
    private var cooldown: HashMap<Player, Boolean> = HashMap()
    private var fixed: ArrayList<Player> = ArrayList()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            return true
        }
        var uuid = sender.uniqueId

        if (args.isNotEmpty()){
            plugin.config.set("doublejump.preferences.$uuid", args.joinToString(" "))
            plugin.saveConfig()
        } else {
            sender.sendMessage("You can manage double jumping with the following commands:")
            sender.sendMessage("")
            sender.sendMessage("/dj disable (disable double jumping)")
            sender.sendMessage("/dj disable particles (only disables particles)")
            sender.sendMessage("/dj enable (re-enables it)")
        }
        return true
    }

    @EventHandler
    fun onDamage(e: EntityDamageEvent) {
        if (e.cause == EntityDamageEvent.DamageCause.FALL && e.entity.type == EntityType.PLAYER) {
            val p = e.entity as Player
            val uuid = p.uniqueId
            if (plugin.config.getString("doublejump.preferences.$uuid").equals("disable")){
                return
            }
            if (p.hasPermission("DJP.doubleJump") || p.hasPermission("DJP.groundPound")) {
                e.isCancelled = true
            }
            if (!p.hasPermission("DJP.*") && !p.hasPermission("DJP.groundPound")) {
                return
            }
            if (p.isSneaking) {
                if (!plugin.config.getString("doublejump.preferences.$uuid").equals("disable particles")){
                    val blocks = ArrayList<Block>()
                    blocks.add(p.world.getBlockAt(p.location.subtract(0.0, 1.0, 0.0)))
                    blocks.add(p.world.getBlockAt(p.location.subtract(1.0, 1.0, 0.0)))
                    blocks.add(p.world.getBlockAt(p.location.subtract(0.0, 1.0, 1.0)))
                    blocks.add(p.world.getBlockAt(p.location.subtract(-1.0, 1.0, 0.0)))
                    blocks.add(p.world.getBlockAt(p.location.subtract(0.0, 1.0, -1.0)))
                    blocks.add(p.world.getBlockAt(p.location.subtract(1.0, 1.0, 1.0)))
                    blocks.add(p.world.getBlockAt(p.location.subtract(-1.0, 1.0, -1.0)))
                    blocks.add(p.world.getBlockAt(p.location.subtract(2.0, 1.0, 0.0)))
                    blocks.add(p.world.getBlockAt(p.location.subtract(-2.0, 1.0, 0.0)))
                    blocks.add(p.world.getBlockAt(p.location.subtract(0.0, 1.0, 2.0)))
                    blocks.add(p.world.getBlockAt(p.location.subtract(0.0, 1.0, -2.0)))
                    for (b in blocks) {
                        val playerArray = Bukkit.getOnlinePlayers().toTypedArray<Player>()
                        val n = playerArray.size
                        var n2 = 0
                        while (n2 < n) {
                            val pl = playerArray[n2]
                            pl.playEffect(b.location, Effect.STEP_SOUND, b.type)
                            ++n2
                        }
                    }
                }
                p.isSneaking = false
            }
        }
    }

    @EventHandler
    fun onMove(e: PlayerMoveEvent) {
        val p = e.player
        val uuid = p.uniqueId
        if (plugin.config.getString("doublejump.preferences.$uuid").equals("disable")){
            return
        }
        if (!p.hasPermission("DJP.doubleJump") && p.allowFlight && !fixed.contains(p)) {
            p.isFlying = false
            p.allowFlight = false
            fixed.add(p)
        }
        if (p.gameMode == GameMode.CREATIVE || p.gameMode == GameMode.SPECTATOR) {
            return
        }
        if (!p.hasPermission("DJP.doubleJump")) {
            return
        }
        if (cooldown[p] != null && cooldown[p]!!) {
            p.allowFlight = true
        } else {
            p.allowFlight = false
        }
        if (p.isOnGround) {
            cooldown[p] = true
        }

        if (cooldown[p] != null && !cooldown[p]!!) {
            for (player in Bukkit.getOnlinePlayers()) {
                player.playEffect(p.location, Effect.SMOKE, 2004)
            }
        }
    }

    @EventHandler
    fun onFly(e: PlayerToggleFlightEvent) {
        val p = e.player
        if (p.gameMode == GameMode.CREATIVE || p.gameMode == GameMode.SPECTATOR) {
            return
        }
        val uuid = p.uniqueId
        if (plugin.config.getString("doublejump.preferences.$uuid").equals("disable")){
            return
        }
        if (p.hasPermission("DJP.doubleJump") && cooldown[p]!!) {
            e.isCancelled = true
            cooldown[p] = false
            p.velocity = p.location.direction.multiply(1.6).setY(1.0)

            if (!plugin.config.getString("doublejump.preferences.$uuid").equals("disable particles")) {
                for (player in Bukkit.getOnlinePlayers()) {
                    player.playEffect(p.location, Effect.MOBSPAWNER_FLAMES, 2004)
                }
            }
            p.allowFlight = false
        }
    }

    @EventHandler
    fun onSneak(e: PlayerToggleSneakEvent) {
        val p = e.player
        if (p.gameMode == GameMode.CREATIVE || p.gameMode == GameMode.SPECTATOR) {
            return
        }
        if (!p.hasPermission("DJP.groundPound")) {
            return
        }
        val uuid = p.uniqueId
        if (plugin.config.getString("doublejump.preferences.$uuid").equals("disable")){
            return
        }
        if (!p.isOnGround && cooldown[p] != null && !cooldown[p]!!) {
            p.velocity = Vector(0, -5, 0)
        }
    }
}