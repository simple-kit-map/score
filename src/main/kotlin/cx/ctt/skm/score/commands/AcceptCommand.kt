package cx.ctt.skm.score.commands

//import pk.ajneb97.model.internal.GiveKitInstructions
import cx.ctt.skm.score.Score
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class AcceptCommand(private val plugin: Score): CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(sender !is Player){
            return false
        }
        if (args.isEmpty()){
            sender.sendMessage("NO ID: When players receive a duel, they get a clickable message that contains a duel UUID, get a duel request to accept one")
            return true
        }
        val duelId = UUID.fromString(args[0])
        if(!DuelCommand.duels.containsKey(duelId)){
            sender.sendMessage("Invalid duel ID, did the duel already start?")
            return true
        }
        val dih = DuelCommand.duels[duelId] ?: run {
            sender.sendMessage("Failed getting duel information helper (dih), if u see this couleur is a shit dev")
            return true
        }


//        val instructions : GiveKitInstructions=GiveKitInstructions (
//            false,
//            true,
//            true,
//            true
//        )
        if (!dih.started){
            WarpCommand.teleportToWarp(plugin, dih.initiator, dih.warp, label = "warp-s")
            Kit.loadKit(plugin, dih.initiator, dih.kit)
//            dih.initiator.teleport(plugin.getWarp(dih.warp)!!.block.location)
//            plugin.playerKitsAPI.getKitsManager().giveKit(dih.initiator, dih.kit, instructions)
            plugin.config.set("status.${dih.initiator.uniqueId}.mechanic", dih.knockback)
            dih.started = true
        }
//        sender.teleport(plugin.essentials.warps.getWarp(dih.warp).block.location)
//        plugin.playerKitsAPI.getKitsManager().giveKit(sender, dih.kit, instructions)
        WarpCommand.teleportToWarp(plugin, sender, dih.warp, label = "warp-s")
        Kit.loadKit(plugin, sender, dih.kit, label = "kit-s")

        plugin.config.set("status.${sender.uniqueId}.mechanic", dih.knockback)
        dih.invitees.remove(sender)
        if (dih.invitees.isEmpty()){
            DuelCommand.duels.remove(duelId)
        }
        return true
    }
}