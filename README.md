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
- `bplDiamondOre`
- `bplStructureBoost`

### Spawns / variants / projectiles

- `bplHostileSpawns` (**default: false**)
- `bplPassiveSpawns`
- `bplProjectiles`
- `bplPlayerProjectiles`
- `bplEggs`
- `bplSheepVariants`
- `bplSpiderEffects`
- `bplSpiderJockeys`
- `bplZombieVariants`
- `bplSlimeSize`
- `bplHostilePathing`
- `bplAnimalBehavior`
- `bplPlayerMelee`
- `bplEyeOfEnder`
- `bplFireballAim`
- `bplEndCrystalBow`
- `bplParkourAssist`
- `bplFishingHookLuck`
- `bplHazardEvasion`
- `bplFoodRng`
- `bplDragonPerching`
- `bplWhitelistOnly` (**default: false**)

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

`bplBlockDrops`, `bplExperience`, and `bplHostileSpawns` default to `false` so the mod does not silently wipe hostile spawns or create huge drop / XP spikes unless you opt into those behaviors.

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
- Villager trades are biased in the player's favor in lucky mode and against them in unlucky mode.
- Mineshaft chunks can luck into guaranteed chest minecarts with abandoned mineshaft loot in lucky mode.
- Loot tables are observed through `LootTableLoadEvent`.

### Tick / block / world RNG

- Weather cycling is biased.
- Crop growth and bonemeal are biased.
- Ore generation, decoration, chunk population, and mapgen hooks are biased.
- Diamond ore gets a larger extra bonus pass in lucky mode, and other overworld ores get a strong spawn boost as well.
- Lava population is denied in lucky mode.
- Ghast / blaze / dragon / wither fireballs and witch-thrown projectiles are kept off lucky players, with blaze shots especially biased to sail well past the player.
- Player arrows use a stronger skeleton-style aim correction, and in the End they get extra velocity/damage for crystal shots.
- Falling jumps get a stateful light parkour correction toward the nearest plausible landing block, which helps block-clutch / parkour chains stay believable without constantly firing on every jump.
- Fishing rods get lucky PvP outcomes for eligible players by turning hook connects into cleaner reels and pop-ups instead of awkward whiffs.
- Players get subtle hazard-evasion nudges near fire and lava, and lucky fire damage burns out much faster.
- Personal luck behaviors can be limited to server-whitelisted players with `bplWhitelistOnly`; world/global effects still apply normally.

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


## Commands

### Strict lucktype command

```mcfunction
/lucktype lucky
/lucktype unlucky
```

Unlike raw `/gamerule lucktype ...`, this command validates the input and rejects anything except `lucky` or `unlucky`. Invalid gamerule values are also auto-reset to `lucky` the next time the mod reads them.

### Debug commands

```mcfunction
/bpldebug skeleton
/bpldebug rabbit
/bpldebug bat
/bpldebug eye
/bpldebug stand
```

Debug entities last for 10 seconds and then despawn automatically. Use them to quickly test hostile pathing, projectile luck, animal behavior, eye-of-ender behavior, and melee reach behavior. The strict `/lucktype` command is the preferred way to change the mode because it rejects invalid input.
