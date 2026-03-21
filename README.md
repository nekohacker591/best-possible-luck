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

## Caveats

This mod is intentionally extreme. It aims to cover **probability as a whole** as broadly as possible without hard-coding every single block, mob, loot table, or mod API, but some mods use custom RNG classes or custom deterministic logic that cannot be safely overridden from one generic hook.

## Build

This repository keeps the Gradle setup **text-only** so PR systems that reject binaries can still accept it. Because of that, `gradle-wrapper.jar` is not committed.

Before building, install **Gradle 4.10.3** locally and regenerate the wrapper:

```bash
gradle wrapper --gradle-version 4.10.3 --distribution-type all --no-validate-url
./gradlew build
```

On Windows:

```bat
gradle wrapper --gradle-version 4.10.3 --distribution-type all --no-validate-url
gradlew.bat build
```

ForgeGradle `2.3` is not compatible with newer system Gradle releases like Gradle 6/7/8, so the wrapper must stay pinned to Gradle `4.10.3`. See `gradle/wrapper/README.md` for the same setup note.
