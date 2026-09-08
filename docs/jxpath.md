# jxpath

XML xpath helper tool, based on the JDK build-in xpath engine.

## Usage

```
jxpath select <xpath> [xml]   # print all matched nodes
jxpath exists <xpath> [xml]   # test if at least one node matches
```

If no `XML` argument is given, the input is read from **stdin**.

## Examples

```shell
jxpath select '//book/title' books.xml
# <title>A</title>
# <title>B</title>

jxpath select '//book/@id' books.xml       # attributes print raw
# 1
# 2

jxpath select '//book[@id="2"]/title' books.xml
jxpath select 'count(//book)' books.xml    # non-nodeset expressions
# 2

jxpath exists '//book[@id="3"]' books.xml && echo "found"
jxpath exists -q '//book' books.xml        # quiet, exit code only

# namespaces: map prefixes with --ns (repeatable)
jxpath select --ns x=http://example.com/ns '//x:item' doc.xml
```

## Output rules (select)

- attribute, text and cdata nodes print their **raw value**
- elements print as **xml** (compact, or pretty with `-p`)

## Security

DOCTYPE declarations are disabled (protection against XXE). Documents
with a DTD are rejected with exit code 2.

## Options

- `select`: `-p, --pretty` pretty print elements
- `select`/`exists`: `--ns PREFIX=URI` namespace mapping, repeatable
- `exists`: `-q, --quiet` no output, exit code only
- all: `-t, --text-file FILE`, `-c, --charset`, `-v, --verbose`
  (see [jregex](jregex.md) for the common options)

Exit codes: `0` matched/found, `1` no match (select/exists), `2` invalid
xml/xpath/namespace.

## Install

```shell
brew tap mhus/jknife
brew install jxpath
```

Or download the binary for your platform from the
[releases](https://github.com/mhus/jknife/releases).
