# jregex

Java regex helper tool — a thin, script-friendly CLI around `java.util.regex`.

## Usage

```
jregex match   <regex> [text]                  # full match test
jregex find    <regex> [text]                  # print all matches
jregex replace <regex> <replacement> [text]    # replace matches
```

If no `TEXT` argument is given, the text is read from **stdin**.

## Subcommands

### match

Tests if the **complete** text matches the regex (`Matcher.matches`).

```shell
jregex match '^a+c$' aaac          # prints "true", exit code 0
jregex match '^a+c$' 'nope'        # prints "false", exit code 1
jregex match -q '^\d+$' 12345 && echo number   # quiet: no output, exit code only
```

Exit codes: `0` match, `1` no match, `2` error (e.g. invalid regex).

Options: `-q, --quiet` do not print the result.

### find

Finds **all** matches and prints one match per line.

```shell
echo 'a 12 b 345 c' | jregex find '\d+'
# 12
# 345

echo 'mike@web anna@foo' | jregex find -g 1 '(\w+)@(\w+)'
# mike
# anna

jregex find --count 'a' banana
# 3

jregex find -n 'TODO' src/Main.java
# 42:TODO refactor
```

Exit codes: `0` at least one match, `1` no match, `2` error.

Options:

- `-g, --group N` print capture group N instead of the whole match (0 = whole match)
- `--count` print only the number of matches
- `-n, --line-number` prefix each match with its line number

### replace

Replaces matches and prints the result. Supports `$1..$n` group references.

```shell
echo 'mike@web.de' | jregex replace '(\w+)@(\w+)\.de' '$2/$1'
# web/mike

jregex replace --first 'a' 'b' aaa
# baa
```

Options: `--first` replace only the first match.

## Common options

Available on all subcommands:

- `-f, --flags FLAGS` regex flags, comma separated: `CASE_INSENSITIVE`, `MULTILINE`,
  `DOTALL`, `UNICODE_CASE`, `CANON_EQ`, `UNIX_LINES`, `LITERAL`,
  `UNICODE_CHARACTER_CLASS`, `COMMENTS`
- `-t, --text-file FILE` read the text from FILE or `-` for stdin
- `-c, --charset` text charset (default: UTF-8)
- `-v, --verbose` print processing information to stderr

## Install

```shell
brew tap mhus/jknife
brew install jregex
```

Or download the binary for your platform from the
[releases](https://github.com/mhus/mhus-jknife/releases).
