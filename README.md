# Best Possible Luck

Best Possible Luck is a Minecraft Forge 1.12.2 coremod experiment that biases `java.util.Random` toward the highest practical outcome it can safely return.

## What it does

Instead of hard-coding every lucky event in vanilla or in specific mods, this mod patches `java.util.Random` itself during startup. Anything that relies on the normal Java RNG path inherits the behavior automatically, including many vanilla systems and many older mods.

Current behavior:

- `nextInt(bound)` returns `bound - 1` when possible.
- `nextBoolean()` always returns `true`.
- `nextFloat()` and `nextDouble()` return the largest value below `1.0`.
- `nextLong()` returns `Long.MAX_VALUE`.
- `nextGaussian()` returns a large positive value.
- `setSeed(long)` is neutralized so the RNG cannot drift away from the best result profile.

## Caveats

This is intentionally extreme and can absolutely make some content behave strangely. It aims to be broad and mod-compatible by hooking the common RNG implementation instead of hard-coding every loot table, mob drop, or world event, but it cannot guarantee perfect semantics for every mod.

## Build

```bash
./gradlew build
```

The project targets Forge `1.12.2-14.23.5.2860` with ForgeGradle `2.3`.
