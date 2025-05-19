package cx.ctt.skm.score

//import net.ess3.api.IEssentials
//import pk.ajneb97.PlayerKits2
import cx.ctt.skm.score.commands.*
import cx.ctt.skm.score.mechanics.*
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File


class Score : JavaPlugin() {
    //    var essentials = Bukkit.getPluginManager().getPlugin("Essentials") as IEssentials
//    var playerKitsAPI = Bukkit.getPluginManager().getPlugin("PlayerKits2") as PlayerKits2
    var isModern = Bukkit.getVersion().startsWith("1.2")
    //    fun getWarps(): Collection<String> { return if (isModern) essentials.warps.list else listOf("soccer", "spawn")}
//    fun getWarp(name: String): Location? { return if (isModern) essentials.warps.getWarp(name) else Location(Bukkit.getWorld("world"),0.0, 70.0, 0.0)}
//    fun getKits(): List<String> { return if (isModern) (playerKitsAPI.kitsManager.kits.map{ it.name.lowercase()} ) else listOf("nd", "ndf", "soup") }

    fun setMetadata(sect: ConfigurationSection, player: Player) {
        if (!sect.contains("created")) sect.set("created", System.currentTimeMillis())
        if (!sect.contains("creator")) sect.set("creator", player.uniqueId.toString())
    }

    fun incrementUse(sect: ConfigurationSection) {
        if (!sect.contains("uses")) sect.set("uses", 1)
        else sect.set("uses", sect.getInt("uses") + 1)
    }

    override fun saveConfig() {
        this.config.save(File(this.dataFolder, "config.yml"))
    }

    override fun onEnable() {
//        val ess = Bukkit.getPluginManager().getPlugin("Essentials")
//        if (ess!!.isEnabled) {
//            this.logger.info((ess::class.members.first { member -> member.name == "getWarp" }).call("soccer") as String)
//        }
        for (mainSection in listOf("old-player-knockback", "warps", "kits", "status", "mechanics")) {
            if (!config.isConfigurationSection(mainSection)) {
                config.createSection(mainSection);
            }
        }
        val ps = PlayerStatus(this)
        getCommand("playerstatus")?.setExecutor(ps)
        getCommand("playerstatus")?.tabCompleter = ps
        server.pluginManager.registerEvents(ps, this)


        val kitCommand = Kit(this)
        getCommand("kit")?.setExecutor(kitCommand)
        getCommand("kit")?.tabCompleter = kitCommand
        server.pluginManager.registerEvents(kitCommand, this)
        server.pluginManager.registerEvents(MainMenu(this), this)

        val warpCommand = WarpCommand(this)
        getCommand("warp")?.setExecutor(warpCommand)
        server.pluginManager.registerEvents(warpCommand, this)

        getCommand("motd")?.setExecutor(MotdCommand(this))
        server.pluginManager.registerEvents(MotdCommand(this), this)

        getCommand("hide")?.setExecutor(HideCommand(this))
        getCommand("ping")?.setExecutor(PingCommand(this))
        getCommand("accept")?.setExecutor(AcceptCommand(this))

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

    override fun onDisable() {
        this.saveConfig()
    }
}
