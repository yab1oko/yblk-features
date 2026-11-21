# Changelog

## 2.0.0 — Minecraft 1.20.1 Forge

Everything since 1.2.1.

### Menu
- Categories, search, favorites, and presets
- Quieter cards, a star for favorites, a clearer collapse mark, and more space before the Admin / Optional / Server badges

### True Night
- Admin lock: an operator can lock the module so only operators can toggle it or change darkness, moon phases, and fog

### Item Remover
- Each banned item can be removed from inventories, recipes (and JEI / REI / EMI when those mods are installed), or loot and villager trades separately
- Villager trades come back when the item is allowed again

### Restrictions
- **Command Blocker** — hide commands from tab-completion and deny them by role (player, moderator, operator), with a custom message
- **Interaction Blocker** — block opening, using, right-clicking, or breaking chosen blocks and entities. Operators can bypass
- **Tool Restrictions** — blocks in the mineable tool tags break only with that tool. Exceptions and a hand whitelist are configurable

### World
- **Portal Lock** — allow, block, or open nether, end, and modded dimensions on an in-game day or a real date. Optional block on portal creation. `/features portal unlock <dimension>`
- **Elytra / Flight Rules** — per dimension: elytra, firework boost, max glide time, and creative-style or mod flight

### Mobs
- **Mob Drops** — per mob, tag, or mod: add, remove, or replace drops, with chance, count, looting, player-only kills, and an experience multiplier. Optional clear of the vanilla table
- **Villager Trades** — block every villager, chosen professions, or the wandering trader; remove trades by result item; freeze prices so curing and reputation do not discount them

### Player
- **Item Weight** — items have weight (defaults by material, editable per item or tag). The total is shown in the inventory and on item tooltips. Past a threshold the player slows, then cannot sprint, then burns hunger. A heavy load slows to a crawl. Strength raises capacity. Worn armor counts, wet armor weighs more while Wetness is on, and extra Inventory Revamp slots raise capacity

### Server
- **Server Tweaks** — two-line colored MOTD with a preview, a server-list icon picked from a PNG, join and leave lines, welcome title, rules that must be accepted, tab header and footer, optional hidden ping, role sort and colored prefixes, kits built from the item picker (or the inventory you are holding), roles with player heads, mute, kick, ban with a duration, freeze, vanish, a visual inventory view, and basic checks that stay off until enabled. Each check chooses its own action: write a log, tell operators, or kick. An older single action is kept for every check until you change one

### Settings
- Item pickers open a visual catalog. Weight values that differ from the default are marked, and closing with unsaved numbers asks to save or discard. Settings rows show a tooltip, a gold dot when the value left its default, and a Reset button that restores that screen's toggles and numbers. HUD Hide, True Night, and Border Danger still apply as you change them
- Scrollbars on the right of a list can be dragged. Real First Person offsets, True Night darkness, and the color picker follow the mouse while held
- Command Blocker lists the blocked commands above the suggestions
- Mob drop rows no longer pop a tooltip
- Setting tooltips show their text. A mob preview on the same screen no longer wipes the letters and leaves an empty plate

## 1.2.1-HOTFIX
### Inventory Revamp
- **Curios:** equip/unequip (`CurioChangeEvent`) refreshes unlocked slots immediately (same as armor)
- Soft Curios dependency marked `BOTH` (server + client) so slot bonuses work dedicated-side
- Fashion clothing NBT mods (e.g. pouch `FashionMods.pouch`) add +1 slot via stack bonus hook

## 1.2
### Inventory Revamp
- Curios-worn items (Fashion bags, outerwear, etc.) grant slot bonuses again — fixed Curios `LazyOptional` hook
- Side-wing slots and locked-slot chrome work in the **Curios** inventory screen (not only vanilla inventory)
- Side slots reinstall after Curios rebuilds its menu (scroll / reset)

### Hide Names
- **Masks** mode: bandanas and balaclavas hide nametags when worn in Curios `mask` (and matching item ids)

### Item Remover / Mob Spawn Block
- Search supports `#namespace:tag` (item/entity tags) and `@mod:id` (registry id / namespace)

### Real First Person
- Pose tuning no longer launches real elytra flight from the settings menu
- Player is frozen in place while the RFP settings screen is open (all pose chips)

### Admin / permissions
- **Inventory Revamp**, **Mob Spawn Block**, **Border Danger Zone**, **Local Chat**, **Wetness**, **Item Remover** require operator (permission level 2+) to toggle or open settings
- Those cards sit under an **Admin only** divider on the Features menu
- Admin-lock checkboxes inside HUD Hide / F3 Hide / Hide Names are visible only to operators

### Fixes
- Crash: mixin into abstract `AbstractContainerScreen.renderBg` (moved to after `renderBg` call in `render`)
- Curios soft-compat reflection updated for Forge `LazyOptional` (inventory bonuses + RFP head suppress)

## 1.1
- **Real First Person** — see your own body in first person (skin, armor, held items)
- Settings panel on the right with live FP view (no pause); pose chips to tune offsets
- Per-pose body offsets (stand / sneak / sit / swim / elytra), hide head, stuck arrows/stingers
- Soft compat: **PlayerAnimator**, **TacZ** (viewmodel keeps arms), **Better Combat** (arms-only during swings, no torso / double weapon), **Curios** head gear
- Menu text wraps so long RU/EN compat lines stay readable
- Feature icons use per-icon colored frames

### Dependencies
**Required**
- Minecraft 1.20.1
- Forge 47+
- Cloth Config API (Forge) 11+

**Optional** (soft compat — mod works without them)
- [Timeless and Classics Zero (TacZ)](https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero) — gun viewmodel owns FP arms
- [Player Animator](https://www.curseforge.com/minecraft/mc-mods/playeranimator) — custom poses pass through
- [Better Combat](https://www.curseforge.com/minecraft/mc-mods/better-combat-by-daedelus) — arms-only during weapon swings
- [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) — FP head suppress; Inventory Revamp slot bonuses from worn curios

**Not compatible (pick one)**
- First-person Model — disable it or Real First Person

## 1.0
- Initial CurseForge release
- **Inventory Revamp** — hotbar-first inventory; armor unlocks slots
- **Mob Spawn Block** — block selected mob spawns
- **Border Danger Zone** — danger near the world border
- **HUD Hide** / **F3 Hide** / **Hide Names** — UI and nametag control
- **True Night** — darker nights with torch/moon light preserved
- **Local Chat** — nearby chat; `!` for global
- **Wetness** — rain/snow/water soak, armor slow, heat drying, freeze
- **Item Remover** — ban items from crafts, loot, and inventories
- In-game Features menu (Cloth Config)
