# jbase64

Base64 encode/decode helper tool.

## Usage

```
jbase64 encode [text]    # encode text or binary input to base64
jbase64 decode [text]    # decode base64 text to raw output
```

If no `TEXT` argument is given, the input is read from **stdin**.

## Examples

```shell
jbase64 encode 'hello world'
# aGVsbG8gd29ybGQ=

jbase64 decode aGVsbG8gd29ybGQ=
# hello world

# encode a binary file
jbase64 encode -t image.png

# pipe roundtrip
echo 'some text' | jbase64 encode | jbase64 decode
# some text

# urlsafe alphabet (for urls, filenames, jwt payloads)
jbase64 encode -u 'hello?world'

# wrap output at 76 characters per line (e.g. for PEM)
jbase64 encode -w 76 -t certificate.der

# decode ignores whitespace/newlines in the input
echo 'aGVs\nbG8=' | jbase64 decode
```

## Options

### encode

- `-u, --urlsafe` use the URL and filename safe alphabet (`-` and `_` instead of
  `+` and `/`)
- `-w, --wrap N` wrap output lines after N characters (0 = no wrapping, default)

### decode

- `-u, --urlsafe` use the URL and filename safe alphabet

### common

- `-t, --text-file FILE` read the input from FILE or `-` for stdin
- `-c, --charset` charset of the text file, the TEXT argument or stdin
  (default: UTF-8)
- `-v, --verbose` print processing information to stderr

`decode` writes the raw decoded bytes to stdout, so binary output is preserved.

Exit codes: `0` success, `2` error (e.g. invalid base64 input).

## Install

```shell
brew tap mhus/jknife
brew install jbase64
```

Or download the binary for your platform from the
[releases](https://github.com/mhus/mhus-jknife/releases).
