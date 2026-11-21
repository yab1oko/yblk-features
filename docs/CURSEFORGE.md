# CurseForge page kit — Features 2.0.0

Do not paste this whole file. Paste the description under **Description**, then the changelog under the file upload.

Images to upload first (then replace the two `src` values with the forgecdn URLs CurseForge gives you):

| File | Use |
|---|---|
| `docs/curseforge/menu.png` | Menu |
| `docs/curseforge/border-feature.png` | Barrier wall next to Border Danger settings |
| `docs/curseforge/border-wall.png` | Barrier only, if you want a second world shot |
| `docs/curseforge/border-settings.png` | Settings only |

Discord banner already on the page: `https://media.forgecdn.net/attachments/description/1733109/description_21a9c9c7-989f-4444-ba7f-2e16083187b8.png`

## Project settings

| Field | Value |
|---|---|
| **Name** | Features |
| **Slug** | `yblk-features` |
| **Summary** | Toggleable gameplay modules for adventure maps and servers. |
| **Categories** | Adventure and RPG; Utility & QoL; Server Utility |
| **License** | All Rights Reserved |
| **Source** | https://github.com/yab1oko/yblk-features |

### Relations

| Mod | Relation |
|---|---|
| Cloth Config API | **Required** (client) |
| Curios API | Optional |
| Timeless and Classics Zero | Optional |
| Player Animator | Optional |
| Better Combat | Optional |

### File upload (2.0.0)

| Field | Value |
|---|---|
| **File** | `build/libs/Features_Forge_2.0.0.jar` |
| **Game versions** | 1.20.1 |
| **Mod loaders** | Forge |
| **Environment** | Client & Server |
| **Release type** | Release |

## Changelog for the file

```
2.0.0 — everything since 1.2.1

Menu
- Categories, search, favorites, and presets

True Night
- Admin lock for the module, darkness, moon phases, and fog

Item Remover
- Remove a banned item from inventories, recipes (and JEI / REI / EMI when those mods are installed), or loot and villager trades separately
- Trades come back when the item is allowed again

Restrictions
- Command Blocker, Interaction Blocker, Tool Restrictions

World
- Portal Lock and Elytra / Flight Rules
- Border Danger still warns, harms, and can draw the vanilla barrier

Mobs
- Mob Drops and Villager Trades

Player
- Item Weight

Server
- Server Tweaks: MOTD, icon, tab, kits, roles, moderation, inventory view
- Each anti-cheat check chooses log, notify operators, or kick. Checks stay off until enabled

Settings
- Tooltips, gold dots, and Reset on settings screens
- Scrollbars on the right of a list can be dragged
- Command Blocker lists blocked commands above the suggestions
- Tooltips keep their text when a mob preview is on the same screen
```

## Description

After you upload the images, paste this into the description editor's **Source code** view (the `...` menu). The visual editor rewrites the header: it drops centering, merges the four chips into one plate, and prints the GitHub link as literal `[GitHub](url)`. The two `<img>` lines still point at the repo until you swap in forgecdn URLs.

````markdown
<h1 align="center">Features</h1>
<p align="center"><b>Toggleable gameplay modules for adventure maps and servers.</b></p>

<p align="center">
<code>Minecraft 1.20.1</code> · <code>Forge</code> · <code>Client &amp; Server</code> · <a href="https://github.com/yab1oko/yblk-features"><code>GitHub</code></a>
</p>

---

## ✨ Overview

Features is a menu of modules you turn on only for the map or server you are building. Each module has its own settings. On a dedicated server the rules live on the server, and operator modules stay locked to operators.

<p align="center">
<img src="UPLOAD docs/curseforge/menu.png" alt="Features menu" width="720">
</p>

## 🧩 Features

The menu groups modules into Player, Interface, World, Mobs, Restrictions, and Server. Search, favorites, and presets sit on the same screen.

**World.** True Night can be locked by an operator. Border Danger Zone warns, harms, and draws the world border, including the vanilla barrier texture. Portal Lock allows, blocks, or opens dimensions on an in-game day or a real date. Elytra and flight rules are set per dimension.

<p align="center">
<img src="UPLOAD docs/curseforge/border-feature.png" alt="Border danger beside its settings" width="720">
</p>

**Mobs.** Mob Drops adds, removes, or replaces loot per mob, with chance, count, and looting. Villager Trades blocks professions or the wandering trader, removes trades, and can freeze prices.

**Restrictions.** Command Blocker hides commands from tab-completion and denies them by role. Interaction Blocker stops opening, using, or breaking chosen blocks and entities. Tool Restrictions makes tagged blocks break only with the matching tool.

**Player.** Item Weight shows a load in the inventory and on tooltips, then slows you, stops sprinting, and burns hunger as the load climbs. Inventory Revamp, local chat, wetness, HUD hide, F3 hide, hide names, and real first person are still here.

**Server.** Server Tweaks covers a colored MOTD, a server-list icon, join and leave lines, rules, the tab list, kits, roles, mute, kick, ban, freeze, vanish, and a look at a player's inventory. Each anti-cheat check stays off until you enable it, and each one can log, tell operators, or kick.

Settings rows have a tooltip, a gold dot when a value left its default, and a Reset button for that screen's toggles and numbers. Lists on the right can be dragged.

## 🎮 How to use

Open the pause menu and click Features. Enable a module and open its settings. On a dedicated server, admin modules need operator permission.

<p align="center">
<a href="https://discord.gg/tXMBWwSuRp"><img src="https://media.forgecdn.net/attachments/description/1733109/description_21a9c9c7-989f-4444-ba7f-2e16083187b8.png" alt="Join Discord" width="560"></a>
</p>

## More mods by yab1oko

- [Cold Weapons](https://www.curseforge.com/minecraft/mc-mods/cold-weapons)
- [Custom Loot System (CTS)](https://www.curseforge.com/minecraft/mc-mods/custom-loot-system)
- [YBLK Fashion](https://www.curseforge.com/minecraft/mc-mods/yblk-fashion)
- [Roleplay Materials](https://www.curseforge.com/minecraft/mc-mods/roleplay-materials)

## Feedback

Bugs, balance notes, and ideas: join the Discord — [yab1oko mods](https://discord.gg/tXMBWwSuRp).
````
