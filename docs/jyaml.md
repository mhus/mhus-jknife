# jyaml

YAML helper tool — validate yaml and convert it to json.

## Usage

```
jyaml validate [yaml]    # validate, silent on success
jyaml tojson [yaml]       # convert to json
```

If no `YAML` argument is given, the input is read from **stdin**.

## Examples

```shell
jyaml validate config.yaml && echo "valid"

jyaml tojson 'a: 1'
# {"a":1}

jyaml tojson config.yaml | jjson pretty
jyaml tojson -p config.yaml > config.json   # pretty output

cat values.yaml | jyaml tojson | jq .

# multi documents are not merged: only the first is loaded
```

Notes:

- YAML is loaded safely (no arbitrary type instantiation).
- Non-string keys (e.g. `1: one`) are converted to strings.
- YAML aliases and anchors are resolved by the parser.
- `null` / `~` and empty input convert to `null`.

## Options

- `tojson`: `-p, --pretty` pretty print the json
- all: `-t, --text-file FILE`, `-c, --charset`, `-v, --verbose`
  (see [jregex](jregex.md) for the common options)

Exit codes: `0` success, `2` invalid yaml.

## Install

```shell
brew tap mhus/jknife
brew install jyaml
```

Or download the binary for your platform from the
[releases](https://github.com/mhus/jknife/releases).
