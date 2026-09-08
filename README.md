# mhus-jknife

Java Developer's Swiss Army Knife — small CLI helper tools written in Java, compiled to
native binaries with [GraalVM native-image](https://www.graalvm.org/native-image/) and
distributed via a [Homebrew tap](https://github.com/mhus/homebrew-jknife).

No Java runtime is needed to use the tools — they are self-contained native binaries
with instant startup.

## Tools

| Tool      | Description                                              | Status  |
| --------- | -------------------------------------------------------- | ------- |
| `jregex`  | Java regex helper: `match`, `find`, `replace`            | ready   |
| `jbase64` | Base64 encode/decode                                     | ready   |
| `juuid`   | UUID generator (v4, v7) + parser                            | ready   |
| `jtime`   | Timestamp converter (epoch <-> ISO-8601, parse/format)     | ready   |
| `jperiod` | Period/duration converter and parser                     | planned |

Tool documentation: [docs/](docs/) — also served as GitHub Pages at <https://jknife.mhus.de>.

Quick example:

```shell
echo "a 12 b 345 c" | jregex find '\d+'
```

## Install

```shell
brew tap mhus/jknife
brew install jregex
```

## Project structure & development

```
pom.xml          parent pom, shared plugin & dependency management
jknife-shared/   shared helpers for all tools
jregex/          the regex tool (one module per tool / per binary)
docs/            user-facing tool documentation (GitHub Pages)
readme/          maintainer documentation (release process, how to add a tool)
scripts/         release helper scripts
```

```shell
mvn verify            # compile + tests
mvn -Pnative package  # native binaries (requires GraalVM)
```

More: [readme/development.md](readme/development.md).

## Release

```shell
./scripts/release.sh 0.1.0
```

Full process (workflow, tap update, verification): [readme/release.md](readme/release.md).

## License

Apache License 2.0 — see [LICENSE](LICENSE).
