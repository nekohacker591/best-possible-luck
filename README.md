# Best Possible Luck

Best Possible Luck is a Minecraft Forge 1.12.2 mod experiment that targets **vanilla Minecraft luck systems** instead of globally overriding `java.util.Random`.

## Main gamerule

```mcfunction
/gamerule lucktype lucky
/gamerule lucktype unlucky
```

- `lucky` biases supported systems toward favorable outcomes.
- `unlucky` inverts the same systems.

## Per-feature gamerules

Every major handler now has its own boolean gamerule so you can disable anything that feels too invasive.

### World / weather / generation

- `bplWeather`
- `bplOreGen`
- `bplDecorate`
- `bplChunkPopulate`
- `bplMapGen`

### Spawns / variants / projectiles

- `bplHostileSpawns`
- `bplPassiveSpawns`
- `bplProjectiles`
- `bplEggs`
- `bplSheepVariants`
- `bplSpiderEffects`
- `bplSpiderJockeys`
- `bplZombieVariants`
- `bplSlimeSize`

### Blocks / loot / combat / progression

- `bplCropGrowth`
- `bplBonemeal`
- `bplBlockDrops` (**default: false**)
- `bplSpecialDrops`
- `bplEntityDrops`
- `bplExperience` (**default: false**)
- `bplLooting`
- `bplEnchanting`
- `bplCrits`
- `bplLootChests`
- `bplFishing`

`bplBlockDrops` and `bplExperience` default to `false` specifically so block drops do not jump to huge values and one kill does not dump absurd XP unless you explicitly opt into that behavior.

## Vanilla events currently targeted

### Rare mob / spawn RNG

- Sheep color bias (pink in lucky, white in unlucky).
- Spider effect cleanup in lucky mode.
- Spider jockey creation in unlucky mode.
- Zombie baby/chicken-jockey style bias in unlucky mode.
- Slime size bias.
- Hostile/passive spawn denial depending on `lucktype`.

### Interaction / loot RNG

- Egg throws can force the 4-chicken outcome in lucky mode.
- Fishing can be biased toward treasure or junk.
- Gravel can bias to flint and leaves can bias to apples when `bplSpecialDrops` is enabled.
- Chest inventories are post-processed when opened.
- Loot tables are observed through `LootTableLoadEvent`.

### Tick / block / world RNG

- Weather cycling is biased.
- Crop growth and bonemeal are biased.
- Ore generation, decoration, chunk population, and mapgen hooks are biased.
- Lava population is denied in lucky mode.

## Build

This project is pinned to Forge `1.12.2-14.23.5.2847`.

### Windows

```bat
bootstrap-wrapper.bat
gradlew.bat build
```

### macOS / Linux

```bash
./bootstrap-wrapper.sh
./gradlew build
```

The bootstrap scripts download Gradle `4.10.3`, generate a local wrapper, and avoid the unsupported `--no-validate-url` flag.
