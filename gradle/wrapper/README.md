# Gradle wrapper note

The normal `gradle-wrapper.jar` file is intentionally **not committed** in this repository because the target PR workflow rejects binary files.

## Local setup

Install **Gradle 4.10.3** locally, then run one of the following from the project root to regenerate the missing wrapper JAR before building:

### macOS / Linux

```bash
gradle wrapper --gradle-version 4.10.3 --distribution-type all --no-validate-url
./gradlew build
```

### Windows

```bat
gradle wrapper --gradle-version 4.10.3 --distribution-type all --no-validate-url
gradlew.bat build
```

ForgeGradle `2.3` is tied to older Gradle releases, so **do not** regenerate the wrapper with Gradle 6/7/8 defaults.
