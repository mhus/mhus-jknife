# AGENT.md

Guidance for AI coding agents (and humans in a hurry) working on this repository.

## What this repo is

Small Java CLI helper tools ("Swiss Army knife"), compiled to native binaries with
GraalVM native-image, distributed via GitHub Releases and a Homebrew tap
(`mhus/homebrew-jknife`). One maven module per tool = one binary = one formula.

## Directory map — read this before adding files

| Path             | Purpose                                                            |
| ---------------- | ------------------------------------------------------------------ |
| `pom.xml`        | parent pom, shared plugin & dependency management                  |
| `jknife-shared/` | shared helpers for all tools (no CLI logic, no tool-specific code)  |
| `jregex/`        | tool module — pattern to copy for new tools                        |
| `docs/`          | **user-facing tool documentation, served as GitHub Pages** at <https://jknife.mhus.de> |
| `readme/`        | **maintainer/process documentation** (release, add-tool, dev setup) |
| `scripts/`       | release helper scripts (`release.sh`, `gen-formula.sh`, `set-version.sh`) |
| `packaging/`     | packaging references (homebrew formula layout example)             |

Do NOT mix these up: process docs go into `readme/`, user-facing tool docs into `docs/`.

## Build, test, verify

```shell
mvn verify                # compile + tests — must be green before any commit
mvn -Pnative package      # native binaries (requires local GraalVM, current platform only)
```

## Rules & conventions

- Tool module structure: package `de.mhus.jknife.<tool>`, main class `<Tool>Cmd`
  with a `public static final String VERSION = "...";` constant (required —
  `scripts/set-version.sh` keeps it in sync with the maven version; used for
  `--version` and the homebrew formula test).
- New tool: follow the checklist in `readme/add-tool.md` (module, parent `<modules>`,
  release workflow artifact loop, `gen-formula.sh` TOOLS list, README + docs page).
- CLI exit codes are part of the contract: `0` success/match, `1` no match,
  `2` error. Cover them in tests (see `JRegexCliTest`).
- Dependencies: JDK + picocli by default, keep it minimal — smaller binaries, fewer
  native-image problems. Accepted exceptions: `jackson-databind` (jjson/jyaml/jllm, tree
  model only — no pojo binding), `snakeyaml` (jyaml/jllm config) and the `langchain4j`
  stack, which lives in `jllm-shared` (shared by the llm tool family: one binary per
  command, e.g. `jllmrequest`). Shared code goes into `jknife-shared`.
- Testing: the `native` profile runs the full JUnit suite both on the JVM and as a
  GraalVM native test image (`mvn -Pnative test`); `scripts/build.sh --native` and the
  release workflow additionally smoke-test the built binaries. Keep all three green.
- When adding reflection-heavy libraries, record the needed metadata with the
  native-image agent (harness pattern in `jxpath`, details in
  [readme/development.md](readme/development.md)) and commit the generated
  `META-INF/native-image/.../reachability-metadata.json` to the tool module.
- Versions: never edit `pom.xml` versions or `VERSION` constants by hand when
  preparing a release — use `scripts/set-version.sh` (via `scripts/release.sh`).
- Homebrew formulas in the tap are **generated** by `scripts/gen-formula.sh`;
  `packaging/homebrew/*.rb` here is only a reference — do not maintain release
  checksums by hand.
- Releases are never cut from `-SNAPSHOT` versions and tags are never moved
  (details: `readme/release.md`).
- Keep documentation language consistent: English throughout the repo.

## Useful commands

```shell
# quick check: compile + tests
./scripts/build.sh

# full build incl. native binaries for the current platform
./scripts/build.sh --clean --native

# run a tool from source while developing
mvn -q -pl jregex exec:java -Dexec.mainClass=de.mhus.jknife.jregex.JRegexCmd -Dexec.args="--help"

# release (after merging everything to main)
./scripts/release.sh 0.1.0

# generate homebrew formulas once the GitHub release exists
./scripts/gen-formula.sh 0.1.0
```
