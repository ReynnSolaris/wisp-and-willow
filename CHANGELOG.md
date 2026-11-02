# Changelog

## 2.1.0 — Scripts and Castings

**Minecraft 26.2 · NeoForge 26.2.0.88** and **Minecraft 1.21.1 · NeoForge 21.1.213**

Two things pack makers have asked for: KubeJS support, and a Soul Gateway that can produce more than familiars. This release has both, on both versions, plus the 2.0.1 gateway fixes if you skipped that one.

> **Your worlds are safe.** Nothing changes id and nothing is removed. Existing gateway recipes (including any datapack ones written with `"familiar"`) still load.

> **1.21.1:** this is a feature release for 1.21.1 as well, because both features are data and recipe changes that back-port cleanly. New *content* is still 26.2 only.

---

### 🧩 KubeJS

Every custom recipe type in the mod now ships a KubeJS recipe schema, so with KubeJS installed they are first-class in `ServerEvents.recipes`:

```js
ServerEvents.recipes(event => {
  // Soul Gateway: soul, summons, fluxia — then .count() and .results()
  event.recipes.wispwillow.gateway('minecraft:chicken', 'minecraft:zombie', 1234)
    .count(2).results(['minecraft:diamond'])
  event.recipes.wispwillow.gateway('minecraft:cow').results(['3x minecraft:emerald'])
  event.remove({ id: 'wispwillow:gateway/bat' })

  // Mana Pool infusion: result, inputs, fluxia
  event.recipes.wispwillow.infusion('minecraft:netherite_ingot', ['minecraft:iron_ingot', 'minecraft:gold_ingot'], 2000)

  // Alchemical Cauldron: result, ingredients, fluid_input, fluxia, ticks — .fluid_result() for a fluid out
  event.recipes.wispwillow.alchemy('minecraft:gold_block', ['9x minecraft:gold_ingot']).ticks(40)

  // Altar ritual: result_item, grid, pattern, inputs, fluxia_required
  event.recipes.wispwillow.ritual('minecraft:emerald_block', '3x3', [
    { dx: 0, dz: -1, type: 'star', color: 'white' },
    { dx: 1, dz: 0, type: 'star', color: 'white' },
    { dx: 0, dz: 1, type: 'star', color: 'white' },
    { dx: -1, dz: 0, type: 'star', color: 'white' }
  ], ['minecraft:emerald']).fluxia(100)
})
```

Rituals also take `.result_mob()`, `.result_mob_count()`, `.min_tier()`, `.awakens('#tag')`, `.imbues('name')` and `.vigil({ effect, duration_ticks, upkeep_per_second })`. Sigil types are `base, runes, star, triad, serpent, eclipse, hex`; colours are dye names. The `id` field of infusion, ritual and gateway recipes is now optional, so scripted recipes don't have to invent one.

**There is no code dependency on KubeJS.** The schemas are data files KubeJS reads from the mod jar. That is also why the 26.2 jar carries them already: KubeJS has no 26.2 build yet, and when it does, this should work without an update from me. Tested in-game against KubeJS 2101.7.2 on 1.21.1.

### 🌀 Castings beyond familiars

A `wispwillow:gateway` recipe is no longer "one soul becomes one familiar". It can now:

- **summon any entity type**, and more than one of it (`summons`, `count`);
- **drop items** in front of the core (`results`), with or without a creature;
- cost whatever you like (`fluxia`).

```json
{ "type": "wispwillow:gateway", "soul": "minecraft:chicken",
  "summons": "minecraft:zombie", "count": 2,
  "results": [{ "id": "minecraft:diamond" }], "fluxia": 1234 }
```

**A primed Soul Pact now accepts any creature that has a casting.** Before, the pact only answered the four built-in bonds no matter what recipes a pack added, which made custom castings unreachable. Now the recipes decide: add a casting for cows and cows answer; remove the bat casting and bats don't. The JEI page shows the creature's egg with its count and the dropped items side by side.

The four built-in bonds (allay, axolotl, frog, bat) are unchanged and still produce familiars.

### 🔧 Also in this release (from 2.0.1)

- The Gateway Core finds its willow wall in either plane and turns to face whoever opens it.
- Grass, snow, vines and water in the opening no longer count as an unfinished frame.
- Clearer "frame unfinished" / "opening blocked" messages and a goggle **Frame** line.

---

### 🧩 Versions

- **26.2:** GeckoLib 5.5+ required. JEI and Patchouli optional. KubeJS: ready for it when a 26.2 build exists.
- **1.21.1:** GeckoLib 4.8+ and Patchouli required. JEI 19.19.2+ optional, Ponder optional, KubeJS 2101.7+ optional.

## 2.0.1 — The Gateway Finds Its Wall

**Minecraft 26.2 · NeoForge 26.2.0.88** and **Minecraft 1.21.1 · NeoForge 21.1.213**

A small fix release for the Soul Gateway. Several of you built the willow wall, charged the core, right-clicked it and were told *"The willow frame is unfinished"* while staring at fifteen perfectly good logs. I found two ways that happened, and the core no longer does either.

> **Your worlds are safe.** Nothing changes id, nothing is removed. Cores and gateways you already have keep working, and ones that refused to open should open now without being rebuilt.

> **1.21.1:** I said 2.0.0 was the last 1.21.1 update. A gateway that won't open is a bug, not a feature, so this fix ships for 1.21.1 too. New content is still 26.2 only.

---

### 🌀 Fixed: the Gateway Core refusing a finished frame

**The core only looked for the wall in the one plane it happened to be facing.** The core turns to face you when you place it, and it only ever checked for logs to its left and right. Build the wall the other way — because the ground suited it, or because you placed the core from the side — and it never saw a single log.

**The core now looks in both planes.** Whichever one holds a finished wall is the gateway. When it opens, the core turns to face the side *you* are standing on, and that is the side familiars step out of. You don't have to think about which way you placed it any more.

**The opening had to be completely empty.** Not "nothing solid" — *air*. A snow layer on the sill, a fern, a strand of vines, water in a wet fen: any of those in the three-by-three hole counted as an unfinished frame, with nothing to tell you so.

**The opening now only has to be free of things a portal can't push aside.** Grass, flowers, snow layers, vines and water are fine; the light replaces them when the gateway opens. Anything solid still blocks it, and now the core says so.

### 💬 Better messages

- **"The willow frame is unfinished"** now tells you *how many* of the fifteen logs are missing, measured against whichever plane is closest to done.
- **New: "The opening is not clear"**, when the logs are all there but something solid is in the hole, with a count of how many cells it fills.
- **Arcane Goggles** show a **Frame** line on a closed core: `complete`, `12 / 15 logs`, or `opening blocked`, so you can see what it wants before you spend the Fluxia.
- The grimoire page and the Ponder scene for the gateway no longer tell you to place the core facing a particular way.

---

### 🧩 Versions

- **26.2:** GeckoLib 5.5+ required. JEI and Patchouli optional.
- **1.21.1:** GeckoLib 4.8+ and Patchouli required. JEI 19.19.2+ optional, Ponder optional.

## 2.0.0 — Root & Branch

**Minecraft 26.2 · NeoForge 26.2.0.88** and **Minecraft 1.21.1 · NeoForge 21.1.213**

This is the biggest update I've ever made to Wisp & Willow, and it's coming out on two versions at once. On 26.2 it's the mod's first release; on 1.21.1 it's the last one.

The short version: I cut the storage network so the mod can be about magic and nothing else, rebuilt Empyrite as real 3D armour, added a cloth armour set you spin from glimmerfawn down, gave the Spirit Realm a skyline, and redrew every item and block texture by hand at vanilla resolution.

> **⚠️ I've removed the Crystalline Network. Empty yours before you update.** Controllers, cables, drives, disks, terminals, importers, exporters and storage buses are all gone (full list below). Any of them still in your world will disappear when you load it, **and so will everything stored on their disks**. Move your things into chests, or into Refined Storage or AE2, first.

> **📌 2.0.0 is the last update for Minecraft 1.21.1.** From here on I'm developing Wisp & Willow for 26.2 only. The 1.21.1 build has everything in this changelog; it just won't get anything after it.

> **Apart from that, your worlds are safe.** Every other block and item keeps its id. Empyrite, the lamp, the pool, the spitter and the cauldron keep theirs too and pick up their new looks on their own. Moving a world from 1.21.1 to 26.2? It upgrades like any other world. Back it up first, as you would for any Minecraft version jump.

---

### 🌿 Magic, and Only Magic

I added the Crystalline Network back when Wisp & Willow didn't know what it wanted to be. It knows now: it's a magic mod. Refined Storage and AE2 already do storage better than I ever would, and I'd rather spend my time on rituals, Fluxia and the realm.

**Removed:**
- Amethyst Controller, Network Cable and Cable Blocker
- Amethyst Disk Drive and Amethyst Fluid Disk Drive
- Crystalline Disks (1K, 4K, 16K, 64K), Fluid Crystalline Disks (16K, 32K, 64K, 128K) and the Disk Case
- Amethyst Terminal, Crafting Terminal and Fluid Terminal
- Network Importer, Exporter and Storage Bus, and their fluid versions
- The Crystalline Network chapter of the grimoire and the terminal's client config options

**Kept on purpose:** the Chunk Loader; the cauldron's upgrade tray (Ember Plate, Miser's Lining and Deepening Sigil); and Amethyst Coal, Paper, Sugar Cane and Farmland, because they feed rituals, alchemy and Luxinia's Flower. The Alchemical Cauldron still takes hoppers and pipes from any mod. I rewrote the grimoire pages that pointed at the network (Luxinia's Flower, the Chunk Loader, Astralite).

---

### 🧵 Glimmerweave

Spirit Down has always baled into wool. Now you can spin it instead, into a magical cloth set.

| Step | Recipe |
|:--|:--|
| **Glimmer Thread** | 3 Spirit Down in a column → 2 thread |
| **Glimmerweave** (a bolt of cloth) | 2×2 Glimmer Thread |
| **Hood, Robe, Leggings, Slippers** | Glimmerweave in the usual armour shapes |

A full set takes 24 bolts, which is 144 down. Bring shears, and be patient with the herd.

The cloth gives you **iron's protection** (2 / 6 / 5 / 2) with no toughness and no knockback resistance. It's also the **easiest thing in the mod to enchant**: 30, where gold is 25. You mend it with a bolt of Glimmerweave on an anvil. It's a mage's set, not a soldier's.

**Set bonus** (shown on every piece's tooltip):
- **2 pieces, Downsoft:** you land like down, with no fall damage from any height.
- **4 pieces, Glimmer-sight:** you see in the dark, the way the herd does in its realm.

It counts for anything wearing it, so a zombie that picks up a hood and robe lands softly too.

When you wear it, it's fully 3D: a hood with a leaning cowl, a mantle and brooch, a sash with knotted tails, long skirt panels, bell cuffs and soft slippers. It never quite stops moving, and the glimmer stitched into the hems glows in the dark. It has its own grimoire entry under *Willow & Wilds*.

---

### 🏛️ Empyrite, Remade in 3D

Empyrite armour isn't a flat recolour any more. It's a full 3D gladiator's kit:

- **Helm:** a galea with a crimson horsehair crest, a gold brow band and gem, cheek guards, a nasal bar, a flared neck guard and gold laurels at the temples.
- **Cuirass:** a muscled breastplate with a gold medallion, gold shoulder straps, a studded belt and leather strips hanging below it, plus segmented shoulder guards and a jade-and-gold sleeve down the arm.
- **Tassets:** bound thighs with a gold band and knee guards.
- **Greaves:** ridged shin guards over sandals with gilded toes.

**Dormant** plate holds still. **Awakened** plate has a look of its own: brighter gold, filigree and muscle lines that glow enchantment-gold, and a crest lit gold at the tips. It moves, too: the crest, medallion, shoulder guards and skirt breathe on a slow loop.

If you play with the slim (Alex) arms, the sleeves are narrowed to fit instead of hanging off you.

> **Renamed to match:** the leggings are now **Empyrite Tassets** and the boots are **Empyrite Greaves** (they used to be Greaves and Sabatons). Only the names changed, so the gear you already have is untouched. The imbuing pages now say *Stride on tassets* and *Skystep on greaves*.

I redrew the item icons to match, and the sword is a short Greek xiphos now.

---

### 🌌 The Spirit Realm Gets a Skyline

The islands were flat-topped and low, so there was nothing tall to steer by. Now there is.

- **Spirit Spires:** tapering spiritstone pillars with a band of old brickwork near the base, moss at the foot and something glowing on the tip. They're thickest in the Hollow Barrens, with a few across the Spirit Meadow and the Mistfall Shallows.
- **Ancient Soulwood:** huge trees with a 2×2 trunk, usually forked into several crowns, with willow vines trailing off them. Look in the Spirit Meadow and the Soulwood Grove.
- **Soulwood, regrown:** every soulwood now has a full trunk with buttress roots, and grows one of three ways: straight with a side branch, forked into two or three crowns, or bent by a wind that never stops. The old broken trunk with a crown floating above it looked like a bug, so it's gone.
- **Wispcap glades:** tight clusters of the glowing mushroom, the realm's night-lights.
- **Spirit veils:** fields of glimmerbloom, moonlily, fern and duskbloom, thick enough to read as a meadow from above.
- **Atmosphere:** the fog and sky lean violet now, more motes drift through the Spirit Meadow and the Soulwood Grove, and ash drifts over the Barrens.

> New features only grow in chunks that haven't been generated yet. If you've already explored part of the realm, head somewhere new to find them.

---

### 🎨 Every Item and Block Texture, Redrawn

I redrew every item sprite and block texture by hand at **vanilla resolution (16×)**, in the spirit of Botania, Thaumcraft and Blood Magic. That includes the Empyrite, Glimmerweave and Arcane Goggles icons. The natural blocks start from real vanilla sprites recoloured into the mod's palette, so they sit comfortably next to vanilla: willow bark, soulwood with glowing cyan veins, willow and soulwood leaves, fenstone, peat, spiritstone, astralite and willow amber.

- **Spawn eggs:** each one is a little drawing of its creature now, instead of a two-colour egg.
- I redrew the **portal** surfaces and the cauldron's rune ring to match.
- I repainted the Alchemical Cauldron's window, the Fluxia bar, the mod's buttons, the advancement background, the Fluxia beam, the portal sparkles, the awakened glint, and the Arcane Goggles as worn.

Mobs, the 3D armour, the Thornrose Staff and the effects stay high-resolution on purpose.

---

### ⚙️ The Machines, Remodelled

- **Fluxia Lamp:** a proper lamp block now, shaped like a redstone lamp but glowing Fluxia cyan. Full brightness (15, up from 10), a full-block hitbox, and it breaks quickly and sounds like glass.
- **Willow Fluxia Pool:** a low basin with gold-capped corner posts, with the Fluxia sitting inside it.
- **Fluxia Spitter:** an open cage of pillars with a gold canopy and a lens, so you can see the orb inside.
- **Alchemical Cauldron:** cauldron-shaped, in the mod's own dark iron. Complete the ring and it gains gold bands and glowing runes.

---

### 🐾 Creatures

Every creature got a shading pass: darker edges and soft light from above, so heads, legs and bodies read as separate shapes instead of flat slabs. The glimmerfawn's coat is proper fleece now, I tidied up the wisp familiar's head, and the frog familiar hops instead of trotting.

---

### 🧩 Version Notes

| | **26.2** | **1.21.1** |
|:--|:--|:--|
| Minecraft · NeoForge | 26.2 · 26.2.0.88+ | 1.21.1 · 21.1.213 |
| GeckoLib | required (5.5+) | required (4.8+) |
| Patchouli (the grimoire) | optional\* | required |
| Recipe pages | JEI (optional) | JEI (optional, 19.19.2 or newer) |
| Ponder scenes | not yet | yes, with Ponder (comes with Create) |

\*Patchouli doesn't have a 26.2 build yet, so on 26.2 it's optional. Everything else works without it, the book's data still ships, and the grimoire comes back as soon as Patchouli does. Until then the root advancement uses the Omni Chalk as its icon. Ponder doesn't have a 26.2 build either; I'll bring its scenes back when it does.

**Recipe pages are JEI on both versions now.** On 1.21.1 I've switched the mod's recipe pages from EMI to JEI, so both versions share the same ones: every ritual circle drawn to scale with its glyphs, reagents and Fluxia cost, plus Fluxia Infusion and the cauldron's brews. Hover a glyph to see its shape and colour. EMI doesn't get pages of its own from Wisp & Willow any more.

Also on 26.2: the fen's liquids have movement of their own. Bog water is a little thicker than water, and willow sap is slow going, but you can always wade or climb out of it.

---

### 🔧 Smaller Things

- Botany Pots recipes only load now when Botany Pots is installed, so a pack without it no longer logs recipe errors at startup.

---

*Thanks for playing, and thank you to everyone who stuck with the mod on 1.21.1. See you on 26.2!* 💜

## 1.4.5 — Housekeeping

**Minecraft 1.21.1 · NeoForge 21.1.213**

A maintenance pass: no new content, just a sweep for bugs, art that was still a placeholder, and things that were the wrong size.

> **Your existing worlds are safe.** Nothing here changes a recipe, a block id or a save format. Disks, drives and chunk loaders you already have carry on as they were — the chunk loaders just start surviving restarts.

> **Patchouli is now a declared dependency.** It always was one in practice (the grimoire, the root advancement icon and the first-join book all come from it); the loader now says so instead of silently dropping every advancement when it's missing.

---

### 🐛 Fixes

#### The Crystalline Network
- **Swapping a disk in a drive could write the old disk's contents into the new one.** Hydrating the new bay fired the save callback while the drive still pointed at the previous bay. The new disk's tooltip showed the wrong "Used" count immediately, and a crash before the next change could lose or duplicate items. Saving is now suspended while a bay loads.
- **A chest behind a Storage Bus ate your disk space.** "Used" counted bussed inventories against a capacity that only counts disks, so a full external chest made every drive insert bounce. Used is disks-only now, like capacity.
- **Clicking the Amethyst Terminal's grid did nothing.** The screen swallowed the click and the packet it meant to send was never registered. Clicks go through the ordinary container path now, as the crafting terminal's already did.
- **Opening a Crafting Terminal whose controller wasn't loaded crashed the client.**
- **Fluid Exporters poured water into any empty cauldron next to them**, ignoring their own filter, and only filled it a third of the way for the 1,000 mB they took. They respect the filter and fill it properly.
- **Fluid Importer and Fluid Exporter windows showed a raw translation key as their title.**
- A Controller or Drive placed from a command with an empty NBT tag no longer throws.

#### Fluxia
- **Chunk loaders died on restart.** They refreshed a short-lived ticket every few ticks, and a block in an unloaded chunk never ticks. They now hold persistent tickets through NeoForge's forced-chunk API, which the level saves and re-issues on load.
- **Luxinias short-changed the last coal.** It read the fuel's burn time *after* shrinking the stack, so a single dropped Amethyst Coal (the common case) burned 600 ticks instead of 3,200.
- Fluxia is now taken from the source before it is handed to a target, so a rate clamp can never mint Fluxia out of nothing.
- A Fluxia node loaded from a tag with no tuning keys keeps its own capacity and rates instead of becoming a 1-Fluxia brick.

#### Creatures
- **Willow Hounds no longer play fetch with themselves.** A hound would drop its delivery at its own feet, decide 20 ticks later that it was loose, and pick it straight back up, forever. It ignores its own deliveries and anything already at your feet, and nudges the drop into your pickup reach.
- **Familiars can be `/kill`ed** (and creative-mode attacks and the void work on them). They are still deathless to everything else, but a homeless familiar that fell out of the world used to fall forever.
- **A dying heron played its strike animation instead of the death poof**: its custom entity event used an id vanilla already uses. Heron and glimmerfawn events moved above vanilla's range.
- **The Axolotl familiar froze mid-air.** Its paddle-through-the-air state matched neither the "in water" nor the "on ground" animation branch, so it hung rigid. It swims now.
- **Every entity in the world lost its shadow after you looked at an altar previewing a mob.** The preview turned the renderer's global shadow flag off and never turned it back on.
- The altar's widened render box for large ghost circles was declared in a place the renderer never read; it now lives on the renderer, so big circles stop being culled when the altar block is off-screen.
- Familiar model layers are registered from the client class, not from a common-side event subscriber.

#### Blocks
- **Willow logs can be stripped on any axis.** Only upright logs responded to an axe (and lost their axis when they did). Stripping now goes through the standard axe hook.

---

### 🐦‍⬛ The Raven, Remade

The author's familiar has a real body now: a 189-cube GeckoLib rig with layered primaries, secondaries and coverts, a jointed neck and jaw, and proper feet, animated in flight and at rest. It is painted as a raven should be — near-black, with an oil-slick sheen that walks from blue through violet to teal across every feather, a lilac eye with a hotspot, and the rachis and barbs drawn on each flight feather. The shimmer it always carried now tints that sheen rather than a flat parrot.

> **GeckoLib is now a required dependency** for this rig and the staff below.

### 🌹 The Thornrose Staff

The Fluxia Linking Wand is now the **Thornrose Staff**: a crook of gnarled old willow, dark and fissured with a live vine wound round it, crimson thorns, a leather grip with a bronze charm, and a faceted rose-quartz crystal held in the hook that glows in the dark (a real glow mask, so it shines at night and under shaders). It renders as a full 3D model in hand, on the ground and in the inventory.

#### At the altar

The raven's rig now knows what the bird is doing. It beats its wings when it is going somewhere, beats them harder in a stoop, folds them and pecks at the stone when it has settled, and it settles **on the altar's own slab** now rather than hovering a block and a half over the ground beside it.

### 🏛️ The Altar & the Cauldron, Rebuilt

- **The Altar** is a proper pedestal: a wide chamfered foot, a column with four corner pillars, a carved capital, and an amethyst basin on top with crystal points at the corners. A band of runes around the column and the basin itself are lit from within (full-bright faces), so the altar reads at night. The stone is deepslate recoloured toward the fen's violet.
- **The Alchemical Cauldron** is an iron pot on three feet with a stepped belly, riveted sides, two handle loops and a thick rim. Forming the ring adds a **gilt inlay** under the lip, gilt caps on the rim corners, and a glowing band of runes around the belly. The basin stays open exactly where the renderer draws the brew and the rune ring, and the collision shape is unchanged.

Both are generated by `tools/gen_altar_cauldron.py` from vanilla sprites; the old `willow_alter` and `alchemical_cauldron_*` textures are retired.

### ✨ Effects & Portals

- **Both portals repainted and rebuilt.** The Open Way is a violet dusk seen through a doorway — indigo depths, lilac cloud, pale-gold threads of light and cold stars — and the Soul Gateway is smoke over a violet void shot through with amber. Each is a 128/256px seamless tile drawn at three parallax depths.
- **Rim of light.** Every open edge of a portal sheet now burns with a soft glowing frame that pulses, so the opening reads as a gate rather than wallpaper.
- **Sparks.** A sheet of bright motes drifts against the field's motion and slides with the camera more than anything behind it, giving the eye a second depth cue.
- **Breathing.** The front layers of the field swell and dim slowly, offset across the gate so a wide opening ripples instead of blinking.
- **The bolt from the sky.** Completing a rite now calls down a column of light from seventy blocks up: a white-hot core in a violet halo that slams onto the altar, flares, and thins away, with a shockwave ring racing to the edge of your circle and rays bursting from the altar. A **vigil** holds a steadier column the whole time it burns, pulsing, with a halo ring breathing about it.
- **The Fluxia beam** (altar columns, rays and links) has a new core-and-halo texture with flowing streaks so the scroll reads as motion.

### 🎨 Art & Models

- **Fluid disks have their own icons.** The 16k, 32k and 128k fluid disks were a vanilla end crystal, an apple and a golden apple; the 64k was a byte-for-byte copy of the empty disk case. All four are now the disk sprite in water colours, one stripe per tier.
- **The Cable Blocker has a texture** instead of a flat white square: a lilac-grey plate derived from polished deepslate.
- **The Fluxia Lamp fits inside its block.** The cap sat two pixels above the top of the block, into whatever was placed above it. The model is pulled down into the block and the block now has an outline matching the lantern (8×8 footprint) instead of a full cube.
- **Wisp familiar hitbox** raised from 0.4 to 0.6 blocks to match the model; the top half of it was unclickable.
- Fen heron feet lifted a hundredth of a pixel off the leg so the two no longer z-fight.

All nine entity models were rebuilt in Blockbench from their Java definitions and checked against their sheets: every UV region is painted and inside its texture, and every other hitbox is within the tolerances vanilla's own animals use.

---

*Thanks for playing! Let me know what you think.* 💜

## 1.4.4 — Unmoored

**Minecraft 1.21.1 · NeoForge 21.1.213**

Two big things this time. Awakened empyrite is no longer just a gold glint — it grows, it learns tricks, and a full set does something. And Liquid Fluxia has stopped behaving like water: go under in it and you stop agreeing with stone, which is how you get into the new thing hidden underground.

> **Your existing worlds are safe.** Everything here is added on top of what you already have. Awakened gear you already own loads unchanged — it just starts growing from now on.

---

### ✨ Awakened Empyrite Grows

Awakening used to be cosmetic. Now the rite is the start of something.

**Waking a piece gives it stats**, shown in the ordinary tooltip lines:

| Piece | What waking gives it |
|:--|:--|
| **Swords & axes** | +1 → +3 attack damage |
| **Tools** | +4 → +12 mining speed, and a little more reach |
| **Armour** | +1 heart per piece, and +0.3 → +1.5 toughness per piece |

#### Resonance I–V

Awakened pieces level up as you use them. Weapons count melee kills (monsters 2, bosses 25), tools count blocks they're the right tool for (ores 3), armour counts damage taken.

Levels land at **60, 300, 1,200 and 4,000**. You get a chime and a message each time, and everything the piece does scales with the level.

#### Ten Imbuements

New altar rites that teach an awakened piece one lasting trick. They all use the awakening circle in different colours and need a tier IV foundation. Imbuing again replaces the old trick and keeps the resonance.

| Piece | Imbuement | What it does |
|:--|:--|:--|
| **Blades** | Sunfire | Sets targets alight; extra damage to the undead |
| **Blades** | Siphon | Heals you for part of the damage you deal |
| **Blades** | Reaping | Bonus XP from kills |
| **Tools** | Kilnheart | Ores come out smelted |
| **Tools** | Veinsense | Breaks the rest of the vein; sneak to break one block |
| **Tools** | Tether | Drops and XP go straight into your inventory |
| **Helm** | Clarity | Night vision, no blindness or darkness, longer breath |
| **Cuirass** | Bulwark | Absorption hearts that refill while you're unhurt |
| **Greaves** | Stride | Faster movement, step up full blocks |
| **Sabatons** | Skystep | No fall damage, higher jump |

#### The Empyrean Regalia

Counting **awakened** pieces only:

- **2 pieces** — fire can't hurt you, and lava does half damage.
- **4 pieces** — 15% less damage taken, melee attackers catch fire, and resonance grows twice as fast.

Tooltips show resonance progress, the imbuement with its numbers at your current level, and whether the set bonus is live. Unawakened empyrite now says it's *Dormant*.

There's a new Grimoire category for all of it, **The Empyrean Arts**, and EMI and Ponder both know the new rites.

---

### 🌀 Fluxia Doesn't Hold You Up Anymore

**Put your head under Liquid Fluxia and you start phasing.** Stone stops being in your way: you drift down through it like a ghost, and you keep drifting until you come out somewhere open.

| | |
|:--|:--|
| **Starts** | When your eyes go under. Wading through a shallow puddle is safe |
| **Lasts** | 6 seconds after your eyes come back out, topped back up the whole time you're under |
| **Sinking** | You drift down on your own. **Sneak** to drop faster, **hold jump** to rise back the way you came |
| **Ends** | The moment you're in open air and out of the Fluxia |

Come out of the rock into open air and you get a soft landing rather than a drop. Be inside the rock when it runs out and you'll suffocate like anyone standing in a wall, so keep an eye on the timer.

You can't drift out of the bottom of the world, and bedrock won't let you through.

**Liquid Fluxia glows brighter now** — light 6 instead of 2. A pool of it reads as a light source from across a dark cavern, which is how you spot an oasis before you fall into it.

**If you'd rather Fluxia just be a liquid**, there's a server config toggle: `fluxia.phasing`. Turning it off also means the oasis keeps its treasure.

---

### 🕳️ The Fluxia Oasis

A new structure, generated **underground** anywhere in the overworld — roughly one per 1,024 blocks, and never breaking the surface. You find it by caving, or with `/locate structure wispwillow:fluxia_oasis`.

It's a mossy cavern grown around a pool of Liquid Fluxia: calcite and smooth basalt walls threaded with amethyst and fluxite, glow berries hanging from the ceiling, a willow leaning over the water, and lamps around the rim.

**There's a vault sealed underneath it**, and no way in that a pickaxe finds by accident.

- The pool's basin is lined with **Warded Slate** — the one stone Fluxia won't carry you through. Dive in and phase, and you land on the bottom instead of going through it.
- Set into that floor are **four weeping stones**. Three are backed by more ward a block down.
- The fourth opens onto four blocks of ordinary deepslate and then the vault. The sign by the pool tells you which one, if you're paying attention to where the willow is.

Getting out is a ladder shaft from the vault up to the cavern floor, capped by a single block of moss you break from underneath.

#### 🪨 Warded Slate

The new block, and it drops itself. Phasing players cannot pass through it, so once you've worked out what it does you can line your own floors with it. It's obsidian-tough and wants a diamond pickaxe.

---

### 🏆 Advancements

Eight new ones, in two branches.

| | |
|:--|:--|
| **Above the Nether** | Brew an Empyrite Ingot |
| **Wake the Metal** | Perform the Awakening rite |
| **One More Trick** | Lay an imbuement on a piece |
| **Fully Resonant** | Carry something to Resonance V *(challenge)* |
| **The Empyrean Regalia** | Wear all four awakened pieces at once *(challenge)* |
| **Unmoored** | Go under in Liquid Fluxia |
| **A Light Under the Hill** | Find a Fluxia Oasis |
| **Beneath the Veil** | Open the vault cache *(hidden challenge)* |

The two awakening ones are retroactive: if you already have the Regalia on, or something already at Resonance V, they'll pop next time you log in.

---

*Thanks for playing! Let me know what you think.* 💜

## 1.4.3 — The Alchemical Cauldron

**Minecraft 1.21.1 · NeoForge 21.1.213**

Hey everyone! This is a big one. I've added the mod's first multiblock, a new top-tier material to work toward, rites your altar keeps running for you, a chalk that draws whole circles by itself, and familiars that finally have some personality.

> **Your existing worlds are safe.** Everything here is added on top of what you already have. The only thing I changed is the Wisp Division recipe (more on that under *Brews*).

---

### 🜍 The Alchemical Cauldron

I wanted a machine you *build* rather than just place, so here it is. Set a cauldron in a circle of Fluxite and surround it with four Willow Log pillars topped with Astralite Blocks. When the structure is complete, the cauldron changes to match.

Right-click it to light it. A ring of runes spins up over the pot and glyphs drift in from the pillar tops. Sneak + right-click puts it out again.

| | |
|:--|:--|
| **Size** | 5×5, four blocks tall |
| **Blocks** | 12 Fluxite, 8 Willow Log, 4 Astralite Block |
| **To light** | 2,000 ƒ, one time |
| **Holds** | 4 ingredient slots, 2 output slots, two 8-bucket tanks |

A few things worth knowing:

- **It uses power while it brews, not all up front.** If your supply is weak it brews slower instead of refusing to start.
- **A full output pauses it, and nothing is lost.** Clear the output and it picks up where it stopped.
- **Hook it up like any other machine:** use the Flux Linking Wand on a pool, then on the cauldron.
- **It works with hoppers and pipes.** A hopper underneath pulls out finished items, and you can put chests or pipes right up against its sides.
- **It's still a cauldron,** so buckets work on it even before the structure is finished.

---

### 🪣 Fluxia by the Bucket

You can now scoop Liquid Fluxia out of a **Willow Fluxia Pool** with an empty bucket and pour a full bucket back in. Each bucket is worth **1,000 ƒ** either way, and nothing is lost.

A lot of the new cauldron brews need liquid Fluxia, and before this there was no good way to get your first bucket. Now there is.

---

### ⚗️ Brews

Thirteen brews across six tiers. Most of them are cheaper or better ways to make things you can already get, so the cauldron only becomes essential at the very top.

| Tier | What it does |
|:--|:--|
| **Steeping** | Amethyst Paper at double the yield · Fluxia Converter Dust from amethyst |
| **Leaching** | Double your raw astralite · turn four nuggets back into an ingot (instead of nine) |
| **Condensing** | Pack dust down with liquid Fluxia, release it again, and grow brand-new Fluxite |
| **Transmutation** | Astralite from Fluxite · a second Soul Wisp from one |
| **Throughput** | The cauldron's three upgrades |
| **Ascension** | Empyrite, which you can't get anywhere else |

**Wisp Division has a new recipe.** Instead of Condensed Dust, it now takes a Soul Wisp, four Astralite Ingots and four buckets of liquid Fluxia.

Every brew has its own page in the Grimoire, with ingredients, cost and brewing time.

---

### ✨ Empyrite

This is a new material tier above netherite, and the first tools and armour I've ever added to the mod. There's no ore, no smelting and no trading for it. **The only way to get Empyrite is the cauldron.**

| | Empyrite | Netherite |
|:--|:--|:--|
| **Enchantability** | 22 | 15 |
| **Tool durability** | 2,800 | 2,031 |
| **Mining speed** | 10.0 | 9.0 |
| **Attack bonus** | 4.5 | 4.0 |
| **Armour** | 4/8/6/4, 3.5 toughness | 3/6/8/3, 3.0 toughness |

It keeps netherite's knockback resistance, so it's never a step down. The tools use Astralite Ingots for handles instead of sticks.

The Empyrite brew needs **Amethyst Coal**, which only comes from the *Minor Arcana* rite at an altar. Most of the mod lets you stick to either rituals or machines. To reach the top tier, you'll need both.

I've also added a cauldron brew that grows **Fluxite from Condensed Dust and amethyst**. It's slower and pricier than mining, but it means Fluxite is finally renewable, which helps a lot now that every cauldron wants twelve of it.

#### 🔥 Awakening Empyrite

A new Tier IV rite for when you want your Empyrite to *look* the part. Lay the circle, put your Empyrite gear on the ground inside it, and offer a Gold Block, 2 Glowstone and 4 Blaze Powder (2,500 ƒ).

Every piece of Empyrite in the circle comes back **Awakened**. Its enchantment glint turns from the usual purple to **gold**, and its tooltip says so. The gear itself isn't used up: enchantments, durability, names and trims all stay exactly as they were. Awakening doesn't take an enchantment slot, and nothing can remove it.

It's purely cosmetic. It's a way to show off.

---

### ⚙️ Cauldron Upgrades

The cauldron has three upgrade slots. Put upgrades in and they take effect right away. Take them out and it goes back to normal.

| Upgrade | Effect |
|:--|:--|
| **Ember Plate** | Brews finish about 20% faster |
| **Miser's Lining** | Brews cost about 20% less Fluxia |
| **Deepening Sigil** | +8 buckets to both tanks |

You can stack them, but each extra one does a little less than the last, so you'll never get instant or free brewing. Hoppers can't reach the upgrade slots, so your upgrades stay put.

Wear the Arcane Goggles to see exactly what your upgrades are doing for you.

---

### 🕯️ Vigils

This is a new kind of rite. Most rites happen once and hand you a result. A vigil keeps running for a while and uses a small amount of Fluxia every second while it does.

The catch is that **an altar keeping a vigil can't do anything else** until the vigil ends. A vigil ends when its time runs out, when the circle under it is broken, or when the Fluxia runs dry. Sneak-click the altar if you want to end one early.

All four vigils use the same ring. Only the chalk colours and the ingredients change, so once you've learned one you can draw them all.

| Vigil | Tier | Lasts | What it does |
|:--|:--:|:--:|:--|
| **Clear Skies** | II | 20 min | Keeps rain and storms away |
| **Gathering Storm** | III | 15 min | Keeps a thunderstorm going overhead |
| **Verdant Tide** | II | 25 min | Speeds up crops, saplings, cane and anything else growing within 8 blocks |
| **Warding Light** | III | 20 min | Pushes hostile mobs out of an 8-block ring around the altar |

Look at an altar with the Arcane Goggles on and it shows which vigil it's keeping, how long is left, and what it costs per second. The circles and ingredients are all in the Grimoire.

---

### 🖍️ Omni Chalk

Drawing a forty-glyph Grand rite by hand is tedious, so I made a chalk that does it for you. **Omni Chalk draws an entire ritual circle in one click**, in the right shapes and colours.

To make it, perform the new rite **The Whole Spectrum** (Tier III, 5,000 ƒ). The circle uses sixteen glyphs, one for every colour, and you'll need to give up one stick of every colour of chalk. After that, you never have to draw a circle by hand again.

- **Sneak + scroll** to pick a rite. You can also leave it on *"whatever the altar has recognised"* and it will finish off a half-drawn circle for you.
- **Sneak + right-click the air** to jump to the next chapter.
- **Right-click an altar** to draw the circle.
- **Sneak + right-click an altar** to erase the whole circle.

It only draws on empty ground with something solid underneath, so it will never replace your blocks. Each glyph wears the chalk down a little. Erasing is free.

---

### 🦇 Familiars

Until now, every familiar did the same thing: flew in one circle, forever. If you had two, they moved in perfect sync. Each species now has its own behaviours, and no two familiars move exactly alike anymore.

| | |
|:--|:--|
| **Bat** | Flits around fast and erratically, or hangs high and still to roost |
| **Frog** | Hops along the ground with real jumps, or just sits there and croaks |
| **Wisp** | Drifts in smooth, shifting loops, or sinks down and hovers like a candle flame |
| **Axolotl** | Paddles through the air, or flops down on the floor and relaxes |

Every familiar will also occasionally stop and stare at you.

They stay closer to home now, within about 6 blocks of their altar. If one wanders too far (a frog hopping off a ledge, say), it flies back on its own.

---

### 🐦‍⬛ The Raven

If you ever see a black raven with a shimmering, rainbow trail circling someone's altar, you've found one of us. It's a developer-only familiar, so there's no recipe for it. Sorry!

---

### 🐛 Fixes

- **Cauldron recipes are less picky.** Ingredients can now be spread across several slots, so hoppers and shift-clicking work the way you'd expect.
- **The cauldron picks the right recipe.** If your ingredients matched more than one brew, it could make a smaller, cheaper one than you wanted. It now goes for the best match.
- **The cauldron wouldn't light** no matter how much Fluxia it had. Fixed.
- **Fluxia beams follow your actual links.** Before, the beams would draw straight lines to every connected machine, even ones you never linked directly. Now they travel along the links you made. How the power moves hasn't changed.
- **Buckets fill the right tank.** Full buckets now go into the input tank instead of the output tank, and empty buckets draw from the output tank first.
- **New arrow button** in the cauldron screen moves liquid from the output tank back into the input tank, for when one brew's result feeds into the next.

---

### 📖 Everything Else

- The cauldron's rune ring uses Minecraft's enchanting glyphs on a gold-and-violet band
- A full Grimoire entry for the cauldron with a 3D preview of the structure, plus a page for every brew and every new rite
- The Arcane Goggles show the cauldron's charge, fire, tanks and progress

*Thanks for playing! Let me know what you think.* 💜

## 1.4.1 — The Soul Gateway

**Minecraft 1.21.1 · NeoForge 21.1.213**

Familiars are no longer something you spawn — they are something you **negotiate for**. This update adds the whole chain that gets you one: a pact written at an altar, a soul taken from a living creature, and a portal built of willow logs that burns Fluxia for as long as it stays lit.

Feeding that portal needed real power, so generation got a serious pass too.

> **Existing worlds are safe.** Everything here is additive. Altars, pools and networks you already built keep working, and the rituals you know still match the same circles.

---

### 🕯️ Familiars, Properly Earned

Getting a familiar is now five steps, and every one of them costs something.

| Step | Where | Cost |
|:--|:--|:--|
| **Amethyst Paper** | Crafting — pressed from Amethyst Sugar Cane | 3 cane → 3 paper |
| **The Written Pact** | A new 6×6 rite at your altar (Tier II) | 500 ƒ → 4 blank pacts |
| **Prime the pact** | Drop in a charged Fluxia Pool | 5,000 ƒ + a ghast tear |
| **Take a soul** | Right-click a living creature | The creature is spent |
| **Seal it** | Back in the pool | 25,000 ƒ + echo shard + astralite |

Then cast the sealed pact into a lit **Soul Gateway** and the familiar steps out, binding itself to the nearest altar on its own.

**Only certain creatures answer a pact.** Press one against anything else and nothing answers; the creature is unharmed and the pact is unspent.

#### 🐸🦇 Two New Familiars

| Creature | Becomes | Boon |
|:--|:--|:--|
| Allay | Wisp Familiar | +20,000 Fluxia reserve |
| Axolotl | Axolotl Familiar | 50% cheaper rites |
| **Frog** | **Frog Familiar** | **+25% bonus output** |
| **Bat** | **Bat Familiar** | **15% cheaper, +10% bonus, +5,000 reserve** |

The **Frog Familiar** takes the *Bountiful* boon — a real chance of a second copy of whatever a rite produces. The **Bat Familiar** takes *Balanced*: a little of everything, none of it as much as a specialist gives.

Both boons existed in the code since 1.4.0 with nothing granting them. Every entry in the boon table is now something you can actually bind to an altar.

Bats live in every cave, and that is deliberate — the generalist should be the one you can get hold of first, and the specialists should be worth going looking for. Each new familiar has three colour variants.

#### One familiar per altar tier

An altar attends to **one familiar per tier** — one at Tier I, up to five at Tier V. Extras still orbit and still cannot be harmed; they simply contribute nothing until you lay another ring of runes beneath them. When an altar is over its limit, the closest familiars are the ones kept.

---

### 🌀 The Soul Gateway

A five-by-five wall of **Willow Logs** with a three-by-three opening, and a **Gateway Core** in the middle of the bottom row. Fifteen logs and the core.

- **To open:** 10,000 ƒ
- **To stay open:** 200 ƒ/second, every second
- **Per pact cast:** 25,000 ƒ

Run the core dry and the gateway shuts itself, loudly. Knock a single log out of the wall while it is lit and the whole thing collapses. Wear the Arcane Goggles and it will tell you how many seconds of fuel it has left.

The portal surface is drawn as a layered, drifting starfield that shifts as you walk past it.

---

### ⚡ Fluxia Generation — Two New Blocks, and Big Buffs

A gateway is a standing load, and the old flowers could not come close to carrying one.

#### New

- **Fluxia Dynamo** — burns anything a furnace would, including Amethyst Coal, at **300 ƒ/s**. One of these holds a gateway open indefinitely.
- **Astral Font** — no fuel at all, only an open sky. **75 ƒ/s** by day, **150 ƒ/s** at night. Halved in rain, halved again in thunder. Put anything solid above one and it stops.

#### Buffed

| Generator | Was | Now |
|:--|:--|:--|
| Solrosa | 0.33 ƒ/s | **4 ƒ/s** |
| Luxinias Flower | 10 ƒ/s, flat 30 s per fuel | **40 ƒ/s**, uses the fuel's real burn time |

Solrosa's rate constant was named "per second" but ran once every three seconds — it was producing a third of what it claimed. Luxinias burned every fuel for a flat 30 seconds regardless of what it was, so an Amethyst Coal gave it a fifth of what the same coal gives a furnace.

---

### 🔮 Rituals Worth Casting

Several rites did not pay for their own chalk. Fixed:

| Rite | Was | Now |
|:--|:--|:--|
| **Flame Bloom** | blaze rod → 3 fire charges | **blaze rod → 3 blaze rods** |
| **Minor Binding** | 3 glowstone dust | **12 glowstone dust** |
| **Magicka Solidificus** | diamond → 8 Fluxite | **diamond → 24 Fluxite** |
| **Lunar Gate** | 1 pearl → 2 eyes | **1 pearl → 3 eyes** |

Flame Bloom was the worst of them: you fed it a blaze rod and got back three fire charges, strictly less than you put in. It is a rod duplicator now. Fluxite got the largest bump because the Dynamo, the Font and every rune all draw on it.

---

### 🐛 Fixes

**Broken links stayed on the map.** Flux links are stored only on the source node, so breaking the block on the far end left the source holding a dead position — drawn by the goggles as a live link and counted in the link total. Nodes now tell their neighbours when they are removed, and the network prunes anything that slips past.

**Altars only ever noticed one familiar.** The altar was stacking boons correctly on the server, but the check deciding whether to tell the client had no familiar state in it — so the goggles kept showing whatever was true when the first familiar arrived. Binding or banishing a familiar now updates immediately.

**The altar's floating display ignored its own buffs.** The orbiting preview showed a rite's raw numbers, so an altar with three familiars serving it looked identical to a bare one — the discount was real but invisible unless you were wearing the goggles. The altar now shows the price you will actually pay, what the runes and familiars are taking off it, and how many familiars are in service, updating the moment one is bound or banished.

**Infusions wanting more than one of something could never fire.** The pool compared the number of *item entities* against the number of ingredients, and dropped items merge — four astralite ingots in a pool are one stack of four, so a recipe asking for four never matched. **Greater and Grand Tablets were both unobtainable.** The pool now counts items, leaves surplus behind, and takes tablets as ingredients instead of refusing to look at them.

**The linking wand could not reach several blocks.** A block's own right-click runs before the item's, even with a full hand, so the Mana Pool's charge adjustment and the Gateway Core's toggle were swallowing every wand click. The wand now gets through.

**The Fluxia Spitter never told the client about its links,** so the goggles reported every spitter as unlinked and drew no beam from one.

**Axolotl Familiars rendered as missing textures** — the variant names did not match the texture files. They also always spawned as the same variant; all six now appear.

---

### 🎨 Visual

- Goggle links and flux arcs **glow** again — a bright core inside a soft bloom, still properly hidden behind walls.
- The Wisp Familiar is a model now rather than a billboard, with three colour variants.
- Soul Gateway surfaces drift and parallax as you move.

---

### 📖 The Grimoire

New entries for **The Soul Gateway**, **Dynamo & Font**, and **The Written Pact**. The Familiars entry now lists what you actually need, which creatures answer a pact, and how many an altar will keep.

The list of creatures that answer is generated from the same table the pact itself reads, so the book cannot claim a species the pact would refuse.

## 1.4.0 — The Foundations Update

**Minecraft 1.21.1 · NeoForge 21.1.213**

The biggest update yet. Your altar is no longer a single block that does everything — it now sits on a **foundation of runes** that decides what rites it can perform and how well. Alongside that: pool crafting, three working flowers, bound familiars, and a completely rewritten in-game guide.

> **Existing worlds are safe.** Altars you already built keep working at Tier I. Lay runes underneath when you want more.

---

### ✨ Altar Tiers & Rune Foundations

Rites are now gated behind altar tier, and tier comes from the runes you lay **one block below** the altar in complete concentric rings.

| Tier | Rings | Runes needed |
|:----:|:-----:|:------------:|
| I    | —     | 0            |
| II   | 1     | 8            |
| III  | 2     | 24           |
| IV   | 3     | 48           |
| V    | 4     | 80           |

Rings must be **complete** — a gap drops you back a tier. Runes sit at ground level and never interfere with the ritual circle drawn on the altar's own layer.

**Four rune types**, and the mix decides your bonuses:

- **Blank Rune** — structure only, no bonus
- **Water Rune** — +2,000 Fluxia reserve each
- **Earth Rune** — +0.5% cost reduction each
- **Blood Rune** — +0.4% bonus-output chance each

Cost reduction and yield each cap at 50%, and 75% once familiar boons are stacked on top, so a maxed foundation is strong but never free.

### ⚗️ Fluxia Infusion — Crafting in the Pool

Throw items into a charged **Willow Fluxia Pool** and they transmute. **9 infusion recipes** ship in 1.4.0, covering all four runes, all three blooms, and both upgraded tablets.

The pool wants the exact ingredients and *nothing else* — stray items floating in it stop the reaction rather than being silently eaten. Short of charge, it puffs smoke and waits instead of failing quietly.

### 🌸 Functional Flora

Luxinia and Solrosa *make* Fluxia. These three *spend* it. Link them to a pool with the Flux Linking Wand like any other machine — each only draws charge on a tick where it actually did something, so a bloom sitting over barren ground costs you nothing.

- **Verdant Bloom** — bonemeals one growable within **5 blocks** every **2s**, for **60 ƒ**
- **Gathering Bloom** — sweeps loose items within **6 blocks** into the inventory directly beneath it, **twice a second**, for **10 ƒ**
- **Warding Bloom** — banishes freshly-spawned hostiles within **12 blocks**, once a second, for **40 ƒ**

The Warding Bloom only takes monsters at full health that spawned naturally — nothing you led home, and nothing mid-fight, is ever deleted out from under you.

### 🐾 Familiars

Familiars bind to an altar and grant it a permanent boon. They orbit their altar as an idle, are **immortal**, and **never despawn**.

- **Frugal** — rites cost 50% less
- **Bountiful** — +25% chance of bonus output
- **Capacious** — +20,000 Fluxia reserve
- **Balanced** — a little of all three

If their altar is destroyed they walk to the nearest other altar on their own, or sit and wait if there isn't one. To release a familiar, use the new **Soul Wisp** — it ghosts away in a swirl of soul-light rather than dying.

Two familiars ship in 1.4.0: the **Axolotl Familiar** and the **Wisp Familiar**.

### 📈 Greater Vessels

The Fluxia Tablet now upgrades through infusion:

| Tablet  | Capacity | Throughput |
|:--------|---------:|-----------:|
| Fluxia  | 20,000   | base       |
| Greater | 100,000  | 8,000      |
| Grand   | 500,000  | 32,000     |

Upgrading preserves whatever charge the tablet already held.

### 👓 Arcane Goggles & Network Visuals

New **Arcane Goggles** turn the Fluxia network into something you can actually read at a glance:

- **Floating fill gauges** above every node, with a bright leading edge so a slowly filling pool visibly moves, a shimmer when it tops out, and exact `amount / capacity` numbers once you're close enough to read them
- **Flowing links** between linked nodes, with evenly spaced pulses that make the direction of flow obvious
- **Transfer arcs** — every Fluxia transfer now draws a travelling mote of light with a comet tail, scaled to the size of the transfer

Look at an altar with the goggles on and it reports its tier, rune count, active boons, and — if the staged rite is out of reach — exactly which tier you still need. A rite you can't yet perform now says so plainly instead of failing silently.

### 📖 The Grimoire, Rewritten

The Patchouli guide has been rebuilt from scratch: **10 categories, 52 entries**, generated directly from the same definitions that drive the recipes, so the book can no longer drift out of sync with the mod.

- Tier foundations now render as **interactive multiblock previews** you can project into the world
- Every ritual and infusion has a real entry with its circle layout, cost, and tier requirement
- All 12 rituals are documented across Minor, Major and Grand chapters

---

### 🔧 Fixes

- **Shader compatibility** — goggle overlays, network links and transfer arcs were completely invisible with any shader pack (Iris/Optifine-style) enabled. Two causes: custom render types aren't in a shader pack's program mapping, and geometry drawn at the old render stage was being composited away. Everything now renders correctly with and without shaders.
- **Chunk Loader would crash on world reload** — the block entity was bound to the wrong class, throwing a `ClassCastException` the moment a saved chunk loader was read back.
- **Chunk Loader was re-registering 81 chunk tickets every single tick** — a severe hidden performance drain on any world with one placed.
- **The altar never actually spent Fluxia** performing rites.
- **The altar renderer read server-only state**, so ghost glyphs, staged reagents and the result preview never appeared for anyone.
- **Fluid Exporter crashed** when a bucket filter slot was left empty.
- **21 recipes were registered under the `minecraft` namespace** instead of `wispwillow`, risking collisions with other mods.
- **Willow Infusion was registered twice** under a wrong mod id, and now runs on a sane interval instead of every tick.
- **Transfer effects were sent twice** to every nearby player, doubling network traffic for no visual benefit.
- **Rune foundations no longer collide with ritual circles** — the two occupy separate layers by design.
- Soil tags (`Soils`, `Amethyst Farmland`, `Snad`) now show readable names in EMI and JEI instead of raw tag ids.

### ⚡ Performance

- Fluxia and storage network scans rewritten onto primitive collections, removing a large amount of per-tick object churn on big networks
- Network traversal converted to an iterative queue, so deep cable runs no longer risk deep recursion
- Overlay rendering batches and flushes only its own geometry, leaving vanilla's render order untouched

---

**Known issue:** a harmless `Missing sound for event: wispwillow:infusion` warning appears in dev environments. Patchouli's own sounds report the identical warning in the same setup — it does not affect gameplay.

*Report bugs and suggestions on the issue tracker. Thanks for playing!* 💜

