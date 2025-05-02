package cx.ctt.skm.score

import cx.ctt.skm.score.mechanics.Knockback
import net.ess3.api.IEssentials
import cx.ctt.skm.score.commands.*
import cx.ctt.skm.score.mechanics.*
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import pk.ajneb97.PlayerKits2
import java.io.File

class Score : JavaPlugin() {
    var essentials = Bukkit.getPluginManager().getPlugin("Essentials") as IEssentials
    var playerKitsAPI = Bukkit.getPluginManager().getPlugin("PlayerKits2") as PlayerKits2
    override fun saveConfig() {
        this.config.save(File(this.dataFolder, "config.yml"))
    }

    override fun onEnable() {
        for (mainSection in listOf("old-player-knockback", "warps", "kits")) {
            if (!config.isConfigurationSection(mainSection)) {
                config.createSection(mainSection);
            }
        }

        getCommand("hide")?.setExecutor(HideCommand(this))
        getCommand("ping")?.setExecutor(PingCommand(this))

        server.pluginManager.registerEvents(Listeners(this), this);
        server.pluginManager.registerEvents(DeathHandler(this), this)

        val buildCommand = BuildCommand(this)
        getCommand("build")?.setExecutor(buildCommand)
        server.pluginManager.registerEvents(buildCommand, this);

        getCommand("duel")?.setExecutor(DuelCommand(this))
        getCommand("duel")?.tabCompleter = DuelCommand(this)
        server.pluginManager.registerEvents(DuelCommand(this), this);

        getCommand("dash")?.setExecutor(Dash())
        server.pluginManager.registerEvents(Dash(), this);
        getCommand("doublejump")?.setExecutor(DoubleJump(this))
        server.pluginManager.registerEvents(DoubleJump(this), this)


        getCommand("knockback")?.setExecutor(KnockbackCommand(this))
        server.pluginManager.registerEvents(Knockback(this), this)
    }

    override fun onDisable() {}
}
