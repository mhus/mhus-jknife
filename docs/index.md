# jknife

Java Developer's Swiss Army Knife — small CLI helper tools written in Java, compiled
to native binaries with GraalVM and installable via [Homebrew](https://brew.sh).

No Java runtime is needed — the tools are self-contained native binaries with instant
startup.

## Tools

| Tool      | Description                                                    | Doc                                          |
| --------- | -------------------------------------------------------------- | -------------------------------------------- |
| `jregex`  | Java regex helper: `match`, `find`, `replace`                   | [jregex.md](jregex.md)                       |
| `jbase64` | Base64 encode/decode                                           | [jbase64.md](jbase64.md)                     |
| `juuid`   | UUID generator (v4, v7) + parser                                | [juuid.md](juuid.md)                         |
| `jtime`   | Timestamp converter (epoch <-> ISO-8601, parse/format)         | [jtime.md](jtime.md)                         |
| `jperiod` | Period/duration converter and parser                            | [jperiod.md](jperiod.md)                     |
| `jjson`   | JSON helper (validate, pretty, compact, get)                    | [jjson.md](jjson.md)                         |
| `jyaml`   | YAML helper (validate, tojson)                                  | [jyaml.md](jyaml.md)                         |
| `jxpath`  | XML xpath helper (select, exists)                                | [jxpath.md](jxpath.md)                       |
| `jllm`    | LLM tool family (request: text in, text out; openai, ollama) | [jllm.md](jllm.md)                           |
| `jsec`    | Security: hash, keys, encrypt/decrypt, sign/verify                | [jsec.md](jsec.md)                           |

## Install

```shell
brew tap mhus/jknife
brew install jregex
```

Binaries for macOS (aarch64, x86_64) and Linux (x86_64, aarch64) are attached to the
[GitHub releases](https://github.com/mhus/jknife/releases).

## Links

- [Homebrew tap](https://github.com/mhus/homebrew-jknife)
- [Source & releases](https://github.com/mhus/jknife)
- License: Apache License 2.0
- Docs: <https://jknife.mhus.de>
