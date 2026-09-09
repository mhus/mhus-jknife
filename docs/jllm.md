# jllm

LLM tool family — a single binary with subcommands, based on
[langchain4j](https://docs.langchain4j.dev/). Providers: **openai**, **ollama**.

Being one binary means the (large) langchain4j runtime is paid only once; new llm
commands are added as subcommands.

## Usage

```
jllm request [options] [prompt]    # text in, text out
```

## Examples

```shell
jllm request --llm '{provider: ollama, model: llama3.1}' "explain quantum computing"

echo "translate to german: hello" | jllm request --llm-config llm.yaml

# system prompt
jllm request --llm-config llm.yaml --system "You are a pirate." "say hello"

# openai (api key from env OPENAI_API_KEY or config)
jllm request --llm '{provider: openai, model: gpt-4o-mini}' "hi"
```

## LLM configuration

All llm parameters come from the config — either inline (`--llm`, yaml or json) and/or
from a file (`--llm-config`). If both are given, the inline config is **merged over**
the file content, so a file can hold the base config and the inline value can override
single fields (e.g. just the model).

```yaml
# llm.yaml
provider: openai        # openai | ollama
model: gpt-4o-mini      # e.g. gpt-4o-mini, llama3.1
baseUrl: https://api.openai.com/v1   # optional, per provider default
apiKey: sk-...          # optional, openai also falls back to $OPENAI_API_KEY
temperature: 0.7        # optional
maxTokens: 1024         # optional (openai)
timeoutSeconds: 60      # optional
```

Defaults: `baseUrl` `https://api.openai.com/v1` (openai) or
`http://localhost:11434` (ollama), `timeoutSeconds` 60.

Config file + inline override example:

```shell
jllm request --llm-config llm.yaml --llm '{model: gpt-4o}' "hi"
```

## Options (request)

- `--llm CONFIG` inline llm config (yaml or json), merged over `--llm-config`
- `--llm-config FILE` llm config file (yaml or json)
- `--system TEXT` system prompt / instruction
- `-t, --text-file FILE`, `-c, --charset`, `-v, --verbose` common input options
  (see [jregex](jregex.md))

Exit codes: `0` success, `2` error (missing/invalid config, empty prompt, request
failure).

## Install

```shell
brew tap mhus/jknife
brew install jllm
```

Or download the binary for your platform from the
[releases](https://github.com/mhus/jknife/releases).
