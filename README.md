# Best Possible Luck

Best Possible Luck is a Minecraft Forge 1.12.2 mod experiment that targets **vanilla Minecraft luck systems** instead of globally overriding `java.util.Random`.

## `lucktype` gamerule

The mod registers a string gamerule named `lucktype`.

```mcfunction
/gamerule lucktype lucky
/gamerule lucktype unlucky
```

- `lucky` biases supported vanilla events toward favorable outcomes.
- `unlucky` inverts those same systems toward unfavorable outcomes.

If the gamerule is missing or invalid, the mod falls back to `lucky`.

## Vanilla events currently targeted

### Global / entity random events

- Weather is biased every few seconds: `lucky` clears storms, `unlucky` pushes rain/thunder.
- Hostile natural spawn checks are denied in `lucky` mode.
- Passive animal spawns are denied in `unlucky` mode.
- Hostile projectiles that would hit a player are canceled in `lucky` mode.
- Player projectiles are canceled against mobs in `unlucky` mode.
- Thrown eggs in `lucky` mode are upgraded into the rare 4-chicken outcome.
- Naturally joining sheep are recolored pink in `lucky` mode and white in `unlucky` mode.
- Spider potion randomness is neutralized in `lucky` mode.

### Random-tick / block behavior

- Crop growth ticks are forced in `lucky` mode.
- Bonemeal succeeds in `lucky` mode.
- Harvested blocks use favorable or unfavorable stack sizing based on `lucktype`.

### Loot tables, drops, and combat

- Chest inventories are adjusted when a loot-generated chest container is opened.
- Living drops and harvested drops are adjusted through the same stack policy.
- Experience drops, looting, enchantment levels, and critical-hit behavior are all flipped by `lucktype`.
- Loot tables are also observed through `LootTableLoadEvent` so vanilla chest/table usage is part of the target surface.

### World generation hooks

- Ore generation, biome decoration, and chunk population are biased toward favorable results in `lucky` mode.
- Lava population is denied in `lucky` mode.
- Structure/mapgen event hooks are allowed when Forge exposes them during generation.

## Why the project changed

The earlier universal RNG approach was too aggressive and could break core Minecraft calculations. This version stays **vanilla-first** and uses Forge-visible events instead of patching the whole JVM.

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
