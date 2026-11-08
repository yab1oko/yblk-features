# Changelog

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
