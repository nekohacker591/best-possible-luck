# Best Possible Luck

Best Possible Luck is a Minecraft Forge 1.12.2 coremod experiment that tries to force as many RNG-driven systems as possible toward their luckiest valid result.

## Coverage strategy

Instead of hard-coding only vanilla loot tables, the mod attacks RNG at multiple layers so modpacks and CurseForge-style packs get broad coverage too:

1. **Java RNG interception** via a coremod transformer.
2. **Alternate probability entry points** like `ThreadLocalRandom` and `Math.random()`.
3. **Forge gameplay events** for drops, looting, XP, enchanting, critical hits, and crop growth.

That layered approach gives the mod a much wider safety net for vanilla content and for older mods that rely on shared Java/Forge randomness instead of custom PRNG systems.

## Current behavior

### Core Java probability hooks

- `java.util.Random#next(int)` returns the maximum valid bit pattern.
- `java.util.Random#nextInt(bound)` returns `bound - 1` when possible.
- `java.util.Random#nextBytes(byte[])` fills the array with the highest byte values.
- `java.util.Random#nextBoolean()` always returns `true`.
- `java.util.Random#nextFloat()` and `nextDouble()` return the largest values below `1.0`.
- `java.util.Random#nextLong()` returns `Long.MAX_VALUE`.
- `java.util.Random#nextGaussian()` returns a large positive value.
- `java.util.Random#setSeed(long)` is neutralized.

### Expanded probability coverage

- `ThreadLocalRandom` bounded and ranged overloads are redirected to the same “best roll” policy.
- `Math.random()` and `StrictMath.random()` are forced to the highest safe result.

### Forge event coverage

- Harvested block drops are forced to 100% drop chance and their stacks are maximized.
- Living entity drops are expanded to their largest stack sizes.
- Experience drops are boosted heavily.
- Looting levels are increased.
- Enchanting table rolls are pushed to the max level.
- Critical hits are forced on the Forge event bus.
- Crop growth ticks are forced to succeed.

## Forge MDK layout

This repository now follows a **Forge MDK-style source layout**: the source files and Gradle build script are committed, but generated wrapper artifacts are **not**. That keeps the repo PR-friendly and avoids shipping broken local wrapper binaries.

## Build

### Windows

If `gradle.bat` on your machine is broken or missing, do **not** rely on it. Run the included bootstrap script instead:

```bat
bootstrap-wrapper.bat
gradlew.bat build
```

### macOS / Linux

```bash
./bootstrap-wrapper.sh
./gradlew build
```

The bootstrap script downloads **Gradle 4.10.3**, generates a fresh wrapper locally, and then you can use the generated `gradlew` / `gradlew.bat` normally.

ForgeGradle `2.3` is not compatible with newer Gradle 6/7/8 defaults, so the generated wrapper must stay pinned to Gradle `4.10.3`.
