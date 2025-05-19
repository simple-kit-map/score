# **s**imple **core**

The core plugin for simple kit map, the versatile minecraft server for pvp, building and client development/debugging

## limitations, design choices

* commands are not designed to be troll-proof (teleporting around, spamming kit/warp creations...)
  * if you wish to use this for a public server feel free to PR options to add limits, cooldowns and permissions
* sticking to org.bukkit for backwards compatibility, ideally [bukkit 1.7.10](https://jd.bukkit.org/)
  * so that score can eventually be compatible down to [1.7](https://github.com/sathonay/nPaper)-[1.21.x](https://papermc.io/software/paper)

## features
* practice-style duel command
* FFA-style warps, with recursion options (e.g. to place duel-oriented `arena.teamA` and `arena.teamB`)
* kits with preview, can contain effects, armor, off-hand
* playerstatus displays an overview of player activity
  * k, w, ps all support both inventory interface and command+tab completion usage
  * their inventory interfaces are interconnected via a bottom toolbar
* customize knockback per-player using /kb
* dash, double jumping movement mechanics
* the typical qol practice core event listeners

## building

`mvn compile && mvn package` or `gradle build`, pick your poison