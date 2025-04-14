package cx.ctt.skm.score

import cx.ctt.skm.score.mechanics.Knockback
import net.ess3.api.IEssentials
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import pk.ajneb97.PlayerKits2
import cx.ctt.skm.score.commands.DuelCommand
import cx.ctt.skm.score.commands.HideCommand
import cx.ctt.skm.score.commands.KnockbackCommand
import cx.ctt.skm.score.mechanics.DoubleJump
import cx.ctt.skm.score.mechanics.Listeners
import cx.ctt.skm.score.mechanics.Dash

class Score : JavaPlugin() {
    var essentials = Bukkit.getPluginManager().getPlugin("Essentials") as IEssentials
    var playerKitsAPI = Bukkit.getPluginManager().getPlugin("PlayerKits2") as PlayerKits2
    override fun onEnable() {

        getCommand("hide")?.setExecutor(HideCommand(this))
        getCommand("knockback")?.setExecutor(KnockbackCommand(this))

        getCommand("duel")?.setExecutor(DuelCommand(this))
        getCommand("duel")?.tabCompleter = DuelCommand(this)
        server.pluginManager.registerEvents(DuelCommand(this), this);

        getCommand("dash")?.setExecutor(Dash())
        server.pluginManager.registerEvents(Dash(), this);
        getCommand("doublejump")?.setExecutor(DoubleJump(this))
        server.pluginManager.registerEvents(Listeners(this), this);
        server.pluginManager.registerEvents(Knockback(this), this)
    }

    override fun onDisable() {}
}
