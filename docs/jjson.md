# jjson

JSON helper tool.

## Usage

```
jjson validate [json]        # validate, silent on success
jjson pretty [json]          # pretty print (indent)
jjson compact [json]         # compact (minify)
jjson get <path> [json]      # extract a value by path
```

If no `JSON` argument is given, the input is read from **stdin**.

## Examples

```shell
jjson validate config.json && echo "valid"
cat config.json | jjson pretty
jjson pretty -i 4 '{"a":[1,2]}'

echo '{"store":{"book":[{"title":"A","price":9.9},{"title":"B"}]}}' \
  | jjson get 'store.book[0].title'
# A

jjson get 'store.book[1]' config.json   # container nodes print as compact json
jjson get '$.store.book[0].price' config.json
jjson get '[0]' '[10,20,30]'            # root arrays
```

## Path syntax (get)

Simple dot paths with array indexes and quoted keys, optional leading `$.`:

| Path                    | Selects                            |
| ----------------------- | ---------------------------------- |
| `a.b.c`                 | nested object keys                 |
| `a.b[0].c`              | array element by index             |
| `a['key with space']`   | quoted keys                        |
| `$.a.b`, `$`            | optional jsonpath-style prefix     |

- Value nodes print raw (`hello`, `42`, `true`, `null`)
- Objects/arrays print as compact json (or pretty with `-p`)

## Options

- `pretty`: `-i, --indent N` indentation width (default: 2)
- `get`: `-p, --pretty` pretty print containers
- all: `-t, --text-file FILE`, `-c, --charset`, `-v, --verbose`
  (see [jregex](jregex.md) for the common options)

Exit codes: `0` success/found, `1` path not found (get), `2` invalid json/path.

## Install

```shell
brew tap mhus/jknife
brew install jjson
```

Or download the binary for your platform from the
[releases](https://github.com/mhus/jknife/releases).
