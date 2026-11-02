# Wisp & Willow

A magic-and-logistics mod for **Minecraft 1.21.1 / NeoForge 21.1.213**.

Chalk circles powered by a subtle current called *Fluxia*, a willow tree that has to be
coaxed out of still water.

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.213 |
| Java | 21 |
| Mappings | Mojang official + Parchment 2024.11.17 |
| Mod id | `wispwillow` |

---

## Building

The Gradle build needs a **Java 21+** JVM. If `java -version` on your `PATH` reports
something older, point `JAVA_HOME` at a JDK 21 first:

```bash
# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"

./gradlew build          # produces build/libs/wispwillow-<version>.jar
./gradlew runClient      # dev client
./gradlew runData        # regenerate everything under src/generated/resources
```

`gradlew --refresh-dependencies` refreshes the local cache; `gradlew clean` resets build
output without touching source.

---

## Project layout

```
src/main/java/com/reynn/wispwillow/
├─ blocks/            Altar, sigils, pools, flowers, crops
│  ├─ fluxia/           Fluxia-producing and Fluxia-moving blocks
├─ client/            Screens, menus, renderers
├─ compat/jei/        JEI plugin: Ritual Circle, Fluxia Infusion and Alchemy pages
├─ datagen/           All data generation
│  └─ book/             The Patchouli grimoire generator
├─ fluxia/            Fluxia storage, node graph, transfer helpers
├─ registry/          Deferred registers
├─ ritual/            Ritual recipe type, serializer, and the definition table
└─ util/              Network graph traversal and shared helpers
```

---

## Adding a ritual

Rituals are defined **once**, in
[`ritual/RitualDefinitions.java`](src/main/java/com/reynn/wispwillow/ritual/RitualDefinitions.java).

```java
out.add(ritual("my_rite", "My Rite", Chapter.MINOR, RitualRecipe.Grid.G3)
        .cardinals(1, SigilType.BASE, DyeColor.WHITE)
        .reagent(Items.AMETHYST_SHARD, 2)
        .fluxia(100)
        .result(Items.DIAMOND, 1)
        .icon(Items.DIAMOND)
        .unlockedBy(Items.AMETHYST_SHARD)
        .lore("Flavour text for the grimoire.")
        .notes("An optional 'marginalia' page.")
        .build());
```

Then run `./gradlew runData`. That single entry produces:

* the `wispwillow:ritual/my_rite` recipe the altar matches against,
* its recipe-unlock advancement,
* the grimoire entry — layout diagram, glyph legend, offerings list, cost and result,
* the `en_us` display name,
* the JEI Ritual Circle page.

Because the book is derived from the same table, **the grimoire cannot describe a circle
the altar would reject**. Nothing about a ritual is written by hand twice.

---

## The Willow Fen

`wispwillow:willow_fen` is a biome of its own — its own terrain, water, mist and life. Find one
with `/locate biome wispwillow:willow_fen`. Ordinary swamps keep only *traces* of it (reeds and
lilies); everything else — the weeping trees, the bog cypress, the pools, the peat, the herons
and wispflies, and all three fen structures — belongs to the fen alone.

It is placed **without a biome library**. NeoForge 21.1 has no API for adding a biome to the
overworld, so
[`OverworldPresetMixin`](src/main/java/com/reynn/wispwillow/mixin/OverworldPresetMixin.java)
takes the finished climate list and splits vanilla's swamp entries along humidity: the swamp
keeps everything drier, the fen takes the wettest band. Total coverage is unchanged and no other
biome is touched, so the wettest swamps in the world simply become fens.

> Appending a climate entry does **not** work, which is why it is done this way. A biome is
> chosen by lowest `fitness`, the summed squared distance to each climate box, and a point inside
> a box scores zero — so a box placed inside vanilla swamp's wider one ties everywhere and never
> wins. See [`WillowFenClimate`](src/main/java/com/reynn/wispwillow/worldgen/fen/WillowFenClimate.java).

A mod that routes the overworld through TerraBlender replaces that list wholesale; the fen would
then be absent rather than broken. The biome itself is
[`WillowFenBiome`](src/main/java/com/reynn/wispwillow/worldgen/fen/WillowFenBiome.java) and its
features are in
[`FenFeatures`](src/main/java/com/reynn/wispwillow/worldgen/fen/FenFeatures.java).

**When adding features to the biome, follow vanilla's call order** (fossils first, then the
global helpers, then ores). Features shared between biomes must appear in the same relative order
in every biome that has them, or no world containing the fen will load: *"Feature order cycle
found"*. For the same reason the fen must never be pulled into its own `has_fen_traces` overlay.

Its art and small structures are generated, not hand-edited:

```bash
python tools/gen_fen_art.py          # every fen texture, including both entity sheets
python tools/gen_fen_structures.py   # fen_shrine, sunken_ruin, standing_stones + their worldgen JSON
```

### Adding a hand-built structure

1. Build it in a dev world and save it with a Structure Block as `wispwillow:<name>`.
   Give chests a loot table (`/data merge block x y z {LootTable:"wispwillow:<table>"}`)
   rather than filling them.
2. Run `python tools/wire_structure.py <name> --biomes "#wispwillow:is_willow_fen"`
   (see `--help` for spacing, heightmap and terrain options).
3. Add the loot table to `ChestSubLootProvider`, run `./gradlew runData`, and check it with
   `/locate structure wispwillow:<name>`.

Re-running step 2 after rebuilding overwrites the copy, so the in-game build stays the source
of truth.

---

## The Spirit Realm

`wispwillow:spirit_realm` is a second dimension: **End islands with the grass still on them**, in
the single biome `wispwillow:spirit_meadow`. Generation is the End's own
(`NoiseGeneratorSettings.END` over a `FixedBiomeSource`), so it is broken land adrift over nothing;
what makes it this place rather than the End is
[`SpiritFloorFeature`](src/main/java/com/reynn/wispwillow/worldgen/feature/SpiritFloorFeature.java),
which skins the island crowns with soil and grass. The biome's grass override then renders that
grass ash-green. It is reached only through the **Vigil of the Open Way**, which runs for thirty
minutes and then puts every traveller back exactly where they were standing. The realm is registered
in [`SpiritRealm`](src/main/java/com/reynn/wispwillow/worldgen/spirit/SpiritRealm.java) and the
travel itself in [`SpiritTravel`](src/main/java/com/reynn/wispwillow/world/SpiritTravel.java).

No carvers, no ores, no springs: the End's noise has nowhere to put a cave and no stone an overworld
ore could replace, and a lake over a void island only drains into it.

> **Arrival has to find land.** Most columns in the realm are empty all the way down, so taking the
> heightmap's word for where to put a traveller drops them out of the world. `SpiritTravel` tries the
> column overhead, sweeps outward a few chunks for an island, and failing that builds a pad — the
> same thing vanilla does when it puts you into the End with nothing underneath.

> **A save that has already visited the realm keeps the old terrain.** Minecraft loads existing
> chunks in preference to generating new ones, so changing this dimension's generator only affects
> ground nobody has been to yet. A world carrying `dimensions/wispwillow/spirit_realm/region` from
> before the switch to End noise will still show the old overworld-style realm around its previous
> arrival point. Delete that folder to force a regeneration — and delete it in any cloned world before
> testing worldgen changes, or the test measures the old dimension.

Players logged out inside the realm are put back out on login
([`SpiritRealmEvents`](src/main/java/com/reynn/wispwillow/world/SpiritRealmEvents.java)). A vigil can
only return the travellers who are actually online, so without this someone who quit mid-excursion
would come back stranded with no altar holding a way open — and since the realm's floor moved from
y=-64 to y=0, an old save left in there now lands *below the world* and dies on the first tick.

The terrain is the End's; **the sky over it is not**. The dimension uses `OVERWORLD_EFFECTS` with a
sky light and `fixedTime` held at dusk, so there is a real dome, horizon and cloud layer that the
biome's own `skyColor` and `fogColor` drive — where `END_EFFECTS` painted its own fixed starfield and
ignored them entirely. The result is islands over a void under a violet evening that never advances.

> `fixedTime` is a **time of day, not a brightness**. `18000` is midnight and renders pitch black
> however high `ambientLight` is; the realm uses `13000` — the sun below the horizon while the sky
> is still lit.

Because the day never advances, `Level#isNight()` is always false here. Any spawn rule written for
the overworld has to account for that or it will never fire — see `WispflyEntity.checkWispflySpawnRules`,
which treats a dimension with no day cycle as always eligible, and the glimmerfawn, which skips the
brightness test and accepts end stone as ground.

**Soulwood** is the one tree that grows on the islands
([`SpiritFeatures`](src/main/java/com/reynn/wispwillow/worldgen/spirit/SpiritFeatures.java)): tall,
straight and sparse, with leaves that emit light. Worldgen only — there is no sapling, and the leaves
drop sticks, because the realm is somewhere you visit rather than somewhere you farm.

### The Open Way

The vigil hangs a [`SpiritPortalBlock`](src/main/java/com/reynn/wispwillow/blocks/spirit/SpiritPortalBlock.java)
two blocks tall directly above its own altar, and walking into that sheet is what travels you —
`entityInside`, not a proximity check on the altar. It is re-asserted once a second while the vigil
burns, so mining or blasting the gate away cannot strand whoever is already through it, and `onEnd`
takes it down before returning anyone. Like the Soul Gateway it has no item, no loot table and no
model: [`SpiritPortalRenderer`](src/main/java/com/reynn/wispwillow/client/renderers/SpiritPortalRenderer.java)
draws it as a scrolling parallax field, using stock render types only so a shader pack cannot lose it.

The same block is **the way back**. `SpiritTravel.send` raises a second gate beside your arrival
point, and a gate standing *inside* the realm returns you instead of sending you on — so you can
leave early rather than waiting out the half hour. It is deliberately offset two blocks from where
you land, or you would arrive inside it and be bounced straight home, and it is left standing: the
way out should still be there when you come back to it.

> Never call `changeDimension` from `entityInside`. It runs mid-collision inside `Entity.move()`,
> and the movement pass then writes the old position back. The portal defers the actual travel to
> `server.execute(...)`, and the transition carries `PLACE_PORTAL_TICKET` so the destination chunk
> is held loaded — without it a traveller arrives correctly and then falls through terrain that has
> not finished loading.

### Companions

Two tameable creatures, deliberately distinct from familiars. A familiar is bound to an altar:
deathless, and unable to leave it. These belong to a *person* — they follow, they can be told to
stay, and they can die.

| | Where | Tamed with | Gives |
|---|---|---|---|
| **Willow Hound** | Willow Fen | Bottle of Willow Sap | fetches loose items to your feet; fights for you |
| **Glimmerfawn** | Spirit Realm | Moonlily | shearable for Spirit Down, which bales into wool |

A player's tamed animals travel with them through the Open Way and back, unless told to sit.
Everything the glimmerfawn does runs on Moonlily, which grows only in the fen — so the realm's
husbandry depends on flowers carried in from the overworld.

Their art is generated too:

```bash
python tools/gen_spirit_art.py       # willow_hound, glimmerfawn and spirit_down
```

Entity sheets are painted face by face onto the box UV layout the Java models declare. **Change a
box in `WillowHoundModel` or `GlimmerfawnModel` and change its region in the script with it.**

---

## Soul Gateway castings

What a sealed soul pact becomes when it is cast into an open Soul Gateway is a recipe,
`wispwillow:gateway`. Since 2.1.0 a casting can produce any creature, items, or both:

```json
{
  "type": "wispwillow:gateway",
  "soul": "minecraft:chicken",
  "summons": "minecraft:zombie",
  "count": 2,
  "results": [{ "id": "minecraft:diamond" }],
  "fluxia": 1234
}
```

* `soul` - the creature the pact must hold. **A primed pact only takes creatures that have a
  casting**, so adding one of these is also what makes a new creature capturable.
* `summons` - any entity type (`familiar` is still read as an alias); `count` of them step out.
* `results` - item stacks dropped in front of the core. At least one of `summons`/`results` is
  required.
* `fluxia` - the charge, default 25,000. `id` is optional and defaults to `wispwillow:gateway/<soul>`.

The built-in bonds (`entity/familiar/SoulPacts`) are written out by datagen as the default set,
the gateway reads whatever recipes the world has, and JEI lists them under the Soul Gateway page.

## KubeJS

Every custom recipe type ships a KubeJS recipe schema
(`src/main/resources/data/wispwillow/kubejs/recipe_schema/*.json`), so with KubeJS installed the
types are first-class in `ServerEvents.recipes`. No code dependency on KubeJS: the schemas are
plain data files that KubeJS reads, which is also why the 26.2 jar carries them before KubeJS has
a 26.2 build.

```js
ServerEvents.recipes(event => {
  // soul, summons, fluxia; .count() and .results() on top
  event.recipes.wispwillow.gateway('minecraft:chicken', 'minecraft:zombie', 1234)
    .count(2).results(['minecraft:diamond'])
  event.recipes.wispwillow.gateway('minecraft:cow').results(['3x minecraft:emerald'])
  event.remove({ id: 'wispwillow:gateway/bat' })

  // result, inputs, fluxia
  event.recipes.wispwillow.infusion('minecraft:netherite_ingot', ['minecraft:iron_ingot', 'minecraft:gold_ingot'], 2000)

  // result, ingredients (sized), fluid_input, fluxia, ticks; .fluid_result() for a fluid out
  event.recipes.wispwillow.alchemy('minecraft:gold_block', ['9x minecraft:gold_ingot']).ticks(40)

  // result_item, grid ('3x3' .. '18x18'), pattern cells {dx, dz, type, color, required}, inputs, fluxia_required;
  // .result_mob(), .result_mob_count(), .min_tier(), .awakens(tag), .imbues(name), .vigil({effect, duration_ticks, upkeep_per_second})
  event.recipes.wispwillow.ritual('minecraft:emerald_block', '3x3', [
    { dx: 0, dz: -1, type: 'star', color: 'white' },
    { dx: 1, dz: 0, type: 'star', color: 'white' },
    { dx: 0, dz: 1, type: 'star', color: 'white' },
    { dx: -1, dz: 0, type: 'star', color: 'white' }
  ], ['minecraft:emerald']).fluxia(100)
})
```

Sigil types are `base, runes, star, triad, serpent, eclipse, hex`; colours are dye names. On 1.21.1
the alchemy `ingredients` use NeoForge's flat sized-ingredient form; on 26.2 the nested
`{"ingredient": ..., "count": n}` form - the schemas differ only there.

## Developer kit

Creative-mode tools for testing one feature without playing up to it. They live in their own
creative tab, **Wisp & Willow: Developer**, have no recipes, and every tool refuses to act unless
the player is in creative mode or listed under `developer.uuids` in the server config
(`entity/familiar/Developers`). Code is in `dev/`; the tab's contents are `dev/DevKit.java`.

| Tool | Use | Sneak + use |
|---|---|---|
| Creative Fluxia Pool (block) | A pool that never empties; feeds every linked node at the node's own intake rate | |
| Creative Fluxia Tablet | Drop into a pool: charges it and stays full | Fill your own Fluxia reserve |
| Fluxia Probe | Right-click any Fluxia node: fill it and print its state to chat (lights a cold cauldron) | Drain it instead |
| Rite Primer | Right-click a chalked altar: drop the recognised rite's reagents and fill the altar | |
| Resonance Tuner | Hold Empyrite in the other hand: awaken it, then step resonance I to V | Cycle its imbuement |
| Hourglass | Right-click a block: 64 random ticks at once | 8 ticks on everything within 2 blocks |
| Spirit Key | Enter the Spirit Realm as the Open Way would; use again there to come home | |

The tab also carries charged tablets, a bound and a sealed soul pact for every creature the pact
table knows, and the Empyrite set already awake at full resonance.

## The grimoire

The Patchouli book (`wispwillow:wispwillow_grimoire`) is generated in full by
[`PatchouliBookProvider`](src/main/java/com/reynn/wispwillow/datagen/book/PatchouliBookProvider.java).

Do not edit the JSON under `src/generated/resources/**/patchouli_books/` — it is
overwritten by `runData`. Edit the provider instead.

Nine chapters: Whispers of Fluxia, Chalk & Sigilcraft, The Altar & the Rite, Fluxia
Networks, Willow & Wilds, and the three ritual chapters.

---

## Licence

All Rights Reserved. See `TEMPLATE_LICENSE.txt` for the MDK template terms.

Minecraft mapping names are covered by Mojang's mapping licence —
<https://github.com/NeoForged/NeoForm/blob/main/Mojang.md>.
