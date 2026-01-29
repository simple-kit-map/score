/*
 * Frankensteined from https://github.com/kernitus/BukkitOldCombatMechanics/blob/master/src/main/kotlin/kernitus/plugin/OldCombatMechanics/module/ModulePlayerKnockback.kt
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package cx.ctt.skm.score.mechanics

import cx.ctt.skm.score.Score
import cx.ctt.skm.score.commands.KnockbackCommand
import cx.ctt.skm.score.commands.KnockbackCommand.Companion.castValue
import cx.ctt.skm.score.commands.KnockbackCommand.Companion.mechMeta
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerVelocityEvent
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Knockback(val plugin: Score): Listener {

    private val playerKnockbackHashMap: MutableMap<UUID, Vector> = WeakHashMap()



    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        playerKnockbackHashMap.remove(e.player.uniqueId)
    }

    // Vanilla does its own knockback, so we need to set it again.
    // priority = lowest because we are ignoring the existing velocity, which could break other plugins
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerVelocityEvent(event: PlayerVelocityEvent) {
        val uuid = event.player.uniqueId
        event.velocity = playerKnockbackHashMap[uuid] ?: return
        playerKnockbackHashMap.remove(uuid)
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {

        // Disable netherite kb, the knockback resistance attribute makes the velocity event not be called
        val entity = event.entity
        if (entity !is Player) return

        val attribute = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE)
        attribute?.modifiers?.forEach { attribute.removeModifier(it) }
    }

    // Monitor priority because we don't modify anything here, but apply on velocity change event
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamageEntity(event: EntityDamageByEntityEvent) {

        if (event.entity !is Player) return
        val mechName = plugin.config.getString("status.${event.entity.uniqueId}.mechanic") ?: return
        val mechSect = plugin.config.getConfigurationSection("mechanics.$mechName.values")!!.getValues(false)

        val fixedArray = mutableListOf<Any>()
        for ((key, info) in mechMeta){
           var value = mechSect[key] ?: info.defaultValue
            if (value != info.defaultValue && value::class != info.defaultValue::class) {
//                plugin.logger.info ("$mechName.$key: casting $value from ${value::class} to ${info.defaultValue::class.simpleName}")
                value = castValue(mechSect[key].toString(), info.defaultValue, info.name, mechName)
            }
            fixedArray.add(value)
        }
        val constructor = KnockbackCommand.Companion.MechData::class.constructors.first()

        val typed = fixedArray.toTypedArray()
        val a = constructor.call(*typed)


        val damager = event.damager as? LivingEntity ?: return
        val damagee = event.entity as? Player ?: return
//        if (event.cause != DamageCause.ENTITY_ATTACK) return
//        if (event.getDamage(DamageModifier.BLOCKING) > 0) return

        // Figure out base knockback direction
        var distanceX: Double = damager.location.x - damagee.location.x
        var distanceZ: Double = damager.location.z - damagee.location.z

        while (distanceX * distanceX + distanceZ * distanceZ < 1.0E-4) {
            distanceX = (Math.random() - Math.random()) * 0.01
            distanceZ = (Math.random() - Math.random()) * 0.01
        }

        val magnitude = sqrt(distanceX * distanceX + distanceZ * distanceZ)
//        val magnitude = hypot(distanceX, distanceZ)

        // Get player knockback before any friction is applied
        val playerVelocity = damagee.velocity

        // Apply friction, then add base knockback
        playerVelocity.setX((playerVelocity.x / a.horizontalfriction) - (distanceX / magnitude * a.horizontal))
        playerVelocity.setY((playerVelocity.y / a.verticalfriction) + a.vertical)
        playerVelocity.setZ((playerVelocity.z / a.horizontalfriction) - (distanceZ / magnitude * a.horizontal))

        // Calculate bonus knockback for sprinting or knockback enchantment levels
        var bonusKnockback = 0.0

        val equipment = damager.equipment
        if (equipment != null) {
            val heldItem =
                if (equipment.itemInMainHand.type == Material.AIR) equipment.itemInOffHand else equipment.itemInMainHand

            bonusKnockback += heldItem.getEnchantmentLevel(Enchantment.KNOCKBACK).toDouble()
        }
        if (damager is Player && damager.isSprinting) bonusKnockback += 1

        if (playerVelocity.y > a.verticallimit) playerVelocity.setY(a.verticallimit)

        if (bonusKnockback > 0) { // Apply bonus knockback

            var extraHorizontal = a.horizontalextra

            if (a.rangereduction){
                var rangeReduction = Math.max((magnitude - a.startrange) * a.rangefactor, 0.0)

                if (rangeReduction > a.maximumrange) rangeReduction = a.maximumrange

                extraHorizontal -= rangeReduction
            }
            playerVelocity.add(
                Vector(
                    -sin((damager.location.yaw * 3.1415927f / 180.0f).toDouble()) * bonusKnockback.toFloat() * extraHorizontal,
                    a.verticalextra,
                    cos((damager.location.yaw * 3.1415927f / 180.0f).toDouble()) * bonusKnockback.toFloat() * extraHorizontal
                )
            )
        }


//        // Example reduction values per armor piece (modifiable as required)
//        val knockbackReductionPerPiece = netheriteKnockbackResistance // 10% knockback reduction for each piece of armor
//
//        // Calculate total knockback reduction from armor
//        val armorKnockbackReduction = damagee.inventory.armorContents.filterNotNull().size * knockbackReductionPerPiece
//
//        // Convert reduction to a factor (e.g., 10% -> 0.1)
//        val reductionFactor = 1 - (armorKnockbackReduction / 100.0)
//
//        // Get the player's current knockback resistance from attributes
//        val baseResistance = damagee.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.value ?: 0.0
//
//        // Multiply the velocity by the combined resistance factor
//        val combinedResistance = reductionFactor * (1 - baseResistance)
//
//        // Apply the adjusted knockback to the velocity
//        playerVelocity.multiply(Vector(combinedResistance, 1.0, combinedResistance))

        // Allow netherite to affect the horizontal knockback. Each piece of armour yields 10% resistance
//        if (a.netheritekbresistance > 0) {
//            val resistance = 1 - (damagee.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.value ?: 0.0)
//            Bukkit.broadcastMessage("resistance: $resistance")
//            playerVelocity.multiply(Vector(resistance, 1.0, resistance))
//        }

        val victimId = damagee.uniqueId

// some debug info, gotta add a way to opt into this someway
//        sender.sendMessage("dist: ${magnitude.toBigDecimal().setScale(3, RoundingMode.FLOOR).toDouble()} x: ${playerVelocity.x.toBigDecimal().setScale(3, RoundingMode.FLOOR).toDouble()} y: ${playerVelocity.y.toBigDecimal().setScale(3, RoundingMode.FLOOR).toDouble()} z: ${playerVelocity.z.toBigDecimal().setScale(3, RoundingMode.FLOOR).toDouble()}")

        // Knockback is sent immediately in 1.8+, there is no reason to send packets manually
        playerKnockbackHashMap[victimId] = playerVelocity

        // Sometimes PlayerVelocityEvent doesn't fire, remove data to not affect later events if that happens
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { playerKnockbackHashMap.remove(victimId) }, 1)
    }
}