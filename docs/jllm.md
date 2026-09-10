# jllm

LLM tool family — a single binary with subcommands, based on
[langchain4j](https://docs.langchain4j.dev/). Providers: **openai**, **ollama**.

Being one binary means the (large) langchain4j runtime is paid only once; new llm
commands are added as subcommands.

## Usage

```
jllm ask [options] [prompt]         # high level: complete response (no streaming)
jllm stream [options] [prompt]      # high level: tokens live as they arrive
jllm request [options] [body]       # raw: provider json passthrough, curl for llms
jllm models [options] [model]       # discovery: list models or show model details
```

## ask vs stream vs request

| | `ask` | `stream` | `request` | `models` |
| --- | --- | --- | --- | --- |
| input | prompt (arg/stdin) | prompt (arg/stdin) | provider json (arg/stdin) | optional model name |
| output | response text when complete | tokens live | raw response bytes, as they arrive | model list / pretty json |
| langchain4j | `ChatModel` | `StreamingChatModel` | none (plain http) | none (plain http) |
| extras | `-v` metadata | `--perf` measurement | byte exact, sse passthrough | `-v` details, `--raw` passthrough |

Streaming is a genuinely different process in langchain4j (separate interfaces,
separate model classes, different wire protocol) — hence separate subcommands.

## Examples

```shell
jllm ask --llm '{provider: ollama, model: llama3.1}' "explain quantum computing"
jllm ask --llm-config llm.yaml --system "You are a pirate." "say hello"

# live tokens while the model writes
jllm stream --llm-config llm.yaml "write a poem"

# with performance measurement (block on stderr)
jllm stream --llm-config llm.yaml --perf "write a haiku"

# raw: send the openai chat completion json directly, response passthrough
jllm request --llm-config llm.yaml << 'EOF'
{"model": "gpt-4o-mini", "messages": [{"role": "user", "content": "hi"}]}
EOF

# raw with streaming: "stream": true makes the provider answer with a live
# sse/ndjson stream, which is passed through chunk by chunk
echo '{"model":"llama3.1","messages":[...],"stream":true}' | jllm request --llm-config ollam.yaml

# openai api key from env OPENAI_API_KEY or config
jllm ask --llm '{provider: openai, model: gpt-4o-mini}' "hi"
```

## Model discovery

```shell
jllm models --llm-config ollam.yaml          # list models, one per line
jllm models --llm-config ollam.yaml -v       # with details
# llama3.1:latest | family=llama | params=8.0B | quant=Q4_K_M | size=4.6GB

jllm models --llm-config ollam.yaml llama3.1:latest   # details of one model (pretty json)
jllm models --llm-config openai.yaml gpt-4o-mini
jllm models --llm-config ollam.yaml --raw             # raw endpoint response
```

Endpoints used: openai `GET <baseUrl>/models` and `GET <baseUrl>/models/{model}`;
ollama `GET <baseUrl>/api/tags` and `POST <baseUrl>/api/show`. Like `request`, the
`models` command uses only `provider`, `baseUrl`, `apiKey` and `timeoutSeconds` from
the config.

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

`request` uses only `provider`, `baseUrl`, `apiKey` and `timeoutSeconds` from the
config — the rest comes from the json body you send. Endpoints: openai
`<baseUrl>/chat/completions` (with `Authorization: Bearer <apiKey>`), ollama
`<baseUrl>/api/chat`.

## Performance measurement (stream)

`jllm stream --perf` uses the streaming internals to measure and prints a block to
stderr — the tokens still go to stdout, so the output stays script friendly:

```
--- performance ---
provider: openai
model: gpt-4o-mini
baseUrl: https://api.openai.com/v1
params: temperature=null maxTokens=null timeout=60s
latency: 1.234s
ttft: 0.412s
tokens: in=5 out=12 total=17
output tokens/sec: 14.1
finish reason: STOP
response model: gpt-4o-mini
response id: chatcmpl-abc123
```

`ttft` is the time to first token, `tokens` the token usage reported by the provider
(when available) and `output tokens/sec` is derived from both.

`ask -v` prints token usage, finish reason and response model after the answer.

## Options

- `--llm CONFIG` inline llm config (yaml or json), merged over `--llm-config`
- `--llm-config FILE` llm config file (yaml or json)
- `ask`/`stream`: `--system TEXT` system prompt
- `stream`: `--perf` performance block (see above)
- `-t, --text-file FILE`, `-c, --charset`, `-v, --verbose` common input options
  (see [jregex](jregex.md))

Exit codes: `0` success, `2` error (missing/invalid config, empty prompt/body,
request failure, http error status).

## Install

```shell
brew tap mhus/jknife
brew install jllm
```

Or download the binary for your platform from the
[releases](https://github.com/mhus/jknife/releases).
