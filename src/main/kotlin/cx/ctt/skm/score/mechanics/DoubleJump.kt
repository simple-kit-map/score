package cx.ctt.skm.score.mechanics

import cx.ctt.skm.score.Score
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

/* This is based from an old plugin, class path started with net.thegamingcraft,
   I couldn't find the original spigotmc.org resource pag
*/
class DoubleJump(private val plugin: Score) : CommandExecutor, Listener {
    private var cooldown: HashMap<Player, Boolean> = HashMap()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            return true
        }
        val uuid = sender.uniqueId

        if (args.isNotEmpty()) {
            if (args[0] == "disable") {
                if (sender.gameMode in listOf(GameMode.SURVIVAL, GameMode.ADVENTURE)) {
                    sender.allowFlight = false
                }
            }
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
            if (plugin.config.getString("doublejump.preferences.$uuid").equals("disable", true)) {
                return
            }
//            if (p.hasPermission("DJP.doubleJump") || p.hasPermission("DJP.groundPound")) {
            e.isCancelled = true
//            }
//            if (!p.hasPermission("DJP.*") && !p.hasPermission("DJP.groundPound")) {
//                return
//            }
            if (p.isSneaking) {
                if (!plugin.config.getString("doublejump.preferences.$uuid").equals("disable particles", true)) {
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
                        for (player in Bukkit.getOnlinePlayers()) {
                            if (plugin.config.getString("doublejump.preferences.${player.uniqueId}")?.startsWith("disable") == true) return
                            player.playEffect(b.location, Effect.STEP_SOUND, b.type)
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
        if (plugin.config.getString("doublejump.preferences.$uuid").equals("disable", true)) {
            return
        }
        if (p.gameMode == GameMode.CREATIVE || p.gameMode == GameMode.SPECTATOR) {
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

        if (!plugin.config.getString("doublejump.preferences.$uuid").equals("disable particles", true)) {
            if (cooldown[p] != null && !cooldown[p]!!) {
                for (player in Bukkit.getOnlinePlayers()) {
                    if (plugin.config.getString("doublejump.preferences.${player.uniqueId}")?.startsWith("disable") == true) return
                    player.playEffect(p.location, Effect.SMOKE, 2004)
                }
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
        if (plugin.config.getString("doublejump.preferences.$uuid").equals("disable", true)) {
            return
        }
        if (cooldown[p]!!) {
            e.isCancelled = true
            cooldown[p] = false
            p.velocity = p.location.direction.multiply(1.6).setY(1.0)

            if (!plugin.config.getString("doublejump.preferences.$uuid").equals("disable particles", true)) {
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
        val uuid = p.uniqueId
        if (plugin.config.getString("doublejump.preferences.$uuid").equals("disable", true)) {
            return
        }
        if (!p.isOnGround && cooldown[p] != null && !cooldown[p]!!) {
            p.velocity = Vector(0, -5, 0)
        }
    }
}