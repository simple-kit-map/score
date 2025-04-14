/*
 * Frankensteined from https://github.com/kernitus/BukkitOldCombatMechanics/blob/master/src/main/kotlin/kernitus/plugin/OldCombatMechanics/module/ModulePlayerKnockback.kt
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package cx.ctt.skm.score.mechanics

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
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerVelocityEvent
import org.bukkit.util.Vector
import cx.ctt.skm.score.Score
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

        val kbName : String? = plugin.config.getString("old-player-knockback.custom.selection." + event.entity.uniqueId)

        val sender = event.entity

        val path = "old-player-knockback.custom.configs.$kbName"
        if (kbName.isNullOrEmpty() || !plugin.config.contains("$path.values")) {
            sender.sendMessage("Knockback preset $kbName does not exist, you will be taking vanilla knockback")
        }
        val knockbackFriction = plugin.config.getDouble("$path.values.friction")
        val knockbackHorizontal = plugin.config.getDouble("$path.values.horizontal")
        val knockbackVertical = plugin.config.getDouble("$path.values.vertical")
        val knockbackVerticalLimit = plugin.config.getDouble("$path.values.verticallimit")
        val knockbackExtraHorizontal = plugin.config.getDouble("$path.values.extrahorizontal")
        val knockbackExtraVertical = plugin.config.getDouble("$path.values.extravertical")

        val damager = event.damager as? LivingEntity ?: return
        val damagee = event.entity as? Player ?: return
        val netheriteKnockbackResistance = false
        if (event.cause != DamageCause.ENTITY_ATTACK) return
        if (event.getDamage(DamageModifier.BLOCKING) > 0) return

        // Figure out base knockback direction
        val attackerLocation = damager.location
        val victimLocation = damagee.location
        var d0 = attackerLocation.x - victimLocation.x
        var d1: Double

        d1 = attackerLocation.z - victimLocation.z
        while (d0 * d0 + d1 * d1 < 1.0E-4) {
            d0 = (Math.random() - Math.random()) * 0.01
            d1 = (Math.random() - Math.random()) * 0.01
        }

        val magnitude = sqrt(d0 * d0 + d1 * d1)

        // Get player knockback before any friction is applied
        val playerVelocity = damagee.velocity

        // Apply friction, then add base knockback
        playerVelocity.setX((playerVelocity.x / knockbackFriction) - (d0 / magnitude * knockbackHorizontal))
        playerVelocity.setY((playerVelocity.y / knockbackFriction) + knockbackVertical)
        playerVelocity.setZ((playerVelocity.z / knockbackFriction) - (d1 / magnitude * knockbackHorizontal))

        // Calculate bonus knockback for sprinting or knockback enchantment levels
        val equipment = damager.equipment
        if (equipment != null) {
            val heldItem =
                if (equipment.itemInMainHand.type == Material.AIR) equipment.itemInOffHand else equipment.itemInMainHand

            var bonusKnockback = heldItem.getEnchantmentLevel(Enchantment.KNOCKBACK)
            if (damager is Player && damager.isSprinting) ++bonusKnockback

            if (playerVelocity.y > knockbackVerticalLimit) playerVelocity.setY(knockbackVerticalLimit)

            if (bonusKnockback > 0) { // Apply bonus knockback
                playerVelocity.add(
                    Vector(
                        (-sin((damager.location.yaw * 3.1415927f / 180.0f).toDouble()) * bonusKnockback.toFloat() * knockbackExtraHorizontal),
                        knockbackExtraVertical,
                        cos((damager.location.yaw * 3.1415927f / 180.0f).toDouble()) * bonusKnockback.toFloat() * knockbackExtraHorizontal
                    )
                )
            }
        }

        if (netheriteKnockbackResistance) {
            // Allow netherite to affect the horizontal knockback. Each piece of armour yields 10% resistance
            val resistance = 1 - (damagee.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.value ?: 0.0)
            playerVelocity.multiply(Vector(resistance, 1.0, resistance))
        }

        val victimId = damagee.uniqueId

// some debug info, gotta add a way to opt into this someway
//        sender.sendMessage("dist: ${magnitude.toBigDecimal().setScale(3, RoundingMode.FLOOR).toDouble()} x: ${playerVelocity.x.toBigDecimal().setScale(3, RoundingMode.FLOOR).toDouble()} y: ${playerVelocity.y.toBigDecimal().setScale(3, RoundingMode.FLOOR).toDouble()} z: ${playerVelocity.z.toBigDecimal().setScale(3, RoundingMode.FLOOR).toDouble()}")

        // Knockback is sent immediately in 1.8+, there is no reason to send packets manually
        playerKnockbackHashMap[victimId] = playerVelocity

        // Sometimes PlayerVelocityEvent doesn't fire, remove data to not affect later events if that happens
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { playerKnockbackHashMap.remove(victimId) }, 1)
    }
}