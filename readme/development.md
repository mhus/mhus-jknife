# Development

## Prerequisites

- JDK 21+ (for build and tests)
- Maven 3.9+
- [GraalVM](https://www.graalvm.org/) with `native-image` (only for native builds,
  e.g. via [sdkman](https://sdkman.io/): `sdk install java 25.0.3-graal`). Important: use
  the **same GraalVM version as the release CI** (see the release workflow) — the
  recorded native-image metadata (jxpath, jllm) is specific to the jdk version.

## Build & test

The easy way is the build script (all options: `./scripts/build.sh --help`):

```shell
./scripts/build.sh                  # compile + run all tests (incremental)
./scripts/build.sh --clean --native # full build incl. native binaries
./scripts/build.sh --fast           # skip tests
```

Or plain maven:

```shell
# compile + run all tests
mvn verify

# run a single tool from source (useful while developing)
mvn -q -pl jregex exec:java -Dexec.mainClass=de.mhus.jknife.jregex.JRegexCmd -Dexec.args="match 'a+' aaa"
```

## Native build

```shell
mvn -Pnative package
```

The binaries end up in `<tool>/target/<tool>-<version>` (e.g.
`jregex/target/jregex-0.0.1-SNAPSHOT`).

Notes:

- `native-image` cannot cross-compile to other operating systems/architectures. A local
  native build only ever produces a binary for the current platform. That is why the
  [release workflow](../.github/workflows/release.yml) builds the release binaries in a
  matrix (macOS aarch64/x86_64, Linux x86_64/aarch64).
- The `picocli-codegen` annotation processor (configured in the parent pom) generates
  the reflection configuration for `native-image`. When adding libraries with
  reflection, check the native build early.

## Native tests

The `native` profile does not only build the binaries — it also runs the **complete
JUnit test suite as a native test image** (`test-native` execution of the
native-maven-plugin):

```shell
mvn -Pnative test          # JVM tests + native tests
```

The same tests then run inside a GraalVM native binary, which catches
reflection/resource/runtime problems that only occur in native images.

On top of that, both `scripts/build.sh --native` and the release workflow run **smoke
tests against the built binaries** (startup, `--version` and one functional command per
tool), so a broken binary can never reach a GitHub release.

### Reflection-heavy libraries: the native-image agent

When a tool uses a library that reflects heavily at runtime (like the JDK's internal
xpath engine), native tests may fail with `UnsupportedFeatureError` or missing
reflection entries. The way out is the **native-image agent**:

1. write a harness that exercises ALL code paths (functions, error cases, locales) in
   one JVM process — see `jxpath/src/test/java/.../NativeImageAgentHarness.java`
2. run it with the agent:
   `java -agentlib:native-image-agent=config-output-dir=<dir> -cp ... <Harness>`
3. filter the generated `reachability-metadata.json` (keep the library internals, drop
   ballast) and put it into the tool's
   `src/main/resources/META-INF/native-image/de.mhus.jknife/<tool>/`
4. resource bundles (`-H:IncludeResourceBundles` buildArg, all locale variants) may be
   needed additionally — see `jxpath/pom.xml`
5. verify with `mvn -Pnative test` and different locales (`LANG=de_DE.UTF-8 ...`)

## Project structure

```
pom.xml          parent pom: shared plugin & dependency management
jknife-shared/   shared helpers for all tools (no CLI logic)
jregex/          one module per tool = one binary = one formula in the tap
packaging/       packaging templates (homebrew formula example)
scripts/         release helper scripts (see readme/release.md)
readme/          maintainer documentation (this folder)
docs/            user-facing tool documentation, served as GitHub Pages
```

Conventions:

- Tool modules define the properties `mainClass` and `appName` and carry the `native`
  build profile (copy from `jregex/pom.xml`).
- Package per tool: `de.mhus.jknife.<tool>`, main class `<Tool>Cmd`.
- Every `*Cmd.java` main class must contain the constant
  `public static final String VERSION = "...";` — `scripts/set-version.sh` keeps it in
  sync with the maven version during a release (used for `--version` output and the
  homebrew formula test).

## GitHub Pages (docs/)

The `docs/` folder contains the user-facing tool documentation and is served as
GitHub Pages at **https://jknife.mhus.de**.

Setup (already done):

- DNS: `jknife.mhus.de` CNAME -> `mhus.github.io`
- `docs/CNAME` contains the domain (`jknife.mhus.de`) — **must stay in the repo**,
  otherwise GitHub Pages drops the custom domain on the next deploy
- Repo setting: *Settings -> Pages -> Custom domain* must be set to `jknife.mhus.de`
  (only needed once, GitHub adds the `CNAME` file for it) and *Enforce HTTPS* should
  be enabled once the certificate is provisioned (takes a few minutes after the
  first successful deploy)

Every tool needs a `docs/<tool>.md` and an entry in `docs/index.md` (part of the
[add-tool checklist](add-tool.md)).

Note: the site is plain markdown served by GitHub Pages, no build step and no theme
config yet. If a nicer layout is wanted later, a Jekyll theme (e.g. `docs/_config.yml`
with `remote_theme`) can be added without changing the structure.

## CI

- [build.yml](../.github/workflows/build.yml): runs `mvn verify` on every push/PR.
- [release.yml](../.github/workflows/release.yml): builds native binaries for all
  platforms on a `v*` tag — see [release.md](release.md).
