# Best Possible Luck

Best Possible Luck is a Minecraft Forge 1.12.2 mod experiment that now targets **vanilla Minecraft luck systems first** instead of globally overriding `java.util.Random`.

## Why it changed

The earlier universal RNG approach was too aggressive and could break core Minecraft calculations. This version pivots to **vanilla-specific** handling so the mod changes outcomes like spawns, block growth, loot, combat, and some world-generation events without rewriting every Java RNG call in the JVM.

## Current vanilla-focused coverage

### World generation and terrain events

- Allows favorable ore generation events.
- Allows favorable biome decoration/population events.
- Denies lava population during chunk population.
- Allows vanilla map-generation event hooks such as structures when Forge exposes them through terrain events.

### Mob spawning and hostile pressure

- Denies hostile natural spawn checks through `LivingSpawnEvent.CheckSpawn`.
- Cancels hostile projectile impacts on players, which is the first step toward the “skeleton should miss me” behavior you asked for.

### Block ticks and crop behavior

- Forces crop growth ticks to succeed through `CropGrowEvent.Pre`.

### Loot, drops, and combat

- Forces harvest drops to 100% drop chance and maximizes stack sizes.
- Maximizes living drops and experience drops.
- Raises looting and enchanting roll outcomes.
- Forces critical hits to succeed with boosted damage.

## What this version intentionally avoids

- No global `java.util.Random` override.
- No `ThreadLocalRandom` or `Math.random()` override.
- No blanket JVM-wide math patching.

That means this build is narrower, but it is much less likely to corrupt vanilla calculations.

## Forge MDK layout

This repository follows a **Forge MDK-style source layout**: source files and Gradle build scripts are committed, but generated wrapper artifacts are not.

## Build

This project is pinned to Forge `1.12.2-14.23.5.2847`, because you identified that build as working better than newer `1.12.2` Forge builds for this experiment.

### Windows

Run:

```bat
bootstrap-wrapper.bat
gradlew.bat build
```

### macOS / Linux

Run:

```bash
./bootstrap-wrapper.sh
./gradlew build
```

The bootstrap scripts download Gradle `4.10.3`, generate a local wrapper, and avoid the unsupported `--no-validate-url` flag.
