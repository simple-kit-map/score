package cx.ctt.skm.score.commands

//import pk.ajneb97.model.internal.GiveKitInstructions
import cx.ctt.skm.score.Score
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AcceptCommand(private val plugin: Score): CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(sender !is Player){
            return false
        }
        if (args.isEmpty()){
            sender.sendMessage("NO ID: When players receive a duel, they get a clickable message that contains a duel UUID, get a duel request to accept one")
            return true
        }
        val initiator = Bukkit.getPlayer(args[0])
        if(initiator == null || !DuelCommand.duels.containsKey(initiator)){
            sender.sendMessage("Invalid duel ID, did the duel already start?")
            return true
        }
        val dih = DuelCommand.duels[initiator] ?: run {
            sender.sendMessage("Failed getting duel information helper (dih), if u see this couleur is a shit dev")
            return true
        }


//        val instructions : GiveKitInstructions=GiveKitInstructions (
//            false,
//            true,
//            true,
//            true
//        )

        if (dih.kit == null || dih.mechanic == null || dih.warp == null){
            sender.sendMessage("The duel you tried to accept is not complete..???????? wat")
            return true
        }

        if (!dih.started){
            WarpCommand.teleportToWarp(plugin, initiator, dih.warp!!, label = "warp-s")
            Kit.loadKit(plugin, initiator, dih.kit!!)
//            dih.initiator.teleport(plugin.getWarp(dih.warp)!!.block.location)
//            plugin.playerKitsAPI.getKitsManager().giveKit(dih.initiator, dih.kit, instructions)
            plugin.config.set("status.${initiator.uniqueId}.mechanic", dih.mechanic)
            plugin.logger.info ("${initiator.name} getting ${dih.mechanic}")
            dih.started = true
        }
//        sender.teleport(plugin.essentials.warps.getWarp(dih.warp).block.location)
//        plugin.playerKitsAPI.getKitsManager().giveKit(sender, dih.kit, instructions)
        WarpCommand.teleportToWarp(plugin, sender, dih.warp!!, label = "warp-s")
        Kit.loadKit(plugin, sender, dih.kit!!, label = "kit-s")

        plugin.logger.info ("${sender.name} getting ${dih.mechanic}")
        plugin.config.set("status.${sender.uniqueId}.mechanic", dih.mechanic)
        dih.gang.remove(sender)
        dih.opps.remove(sender)
        if (dih.gang.isEmpty() && dih.opps.isEmpty()){
            DuelCommand.duels.remove(initiator)
        }
        return true
    }
}