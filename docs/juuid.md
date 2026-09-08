# juuid

UUID generator and parser tool.

## Usage

```
juuid generate [options]    # generate UUIDs (alias: gen)
juuid parse <uuid>           # inspect a UUID
```

## Examples

```shell
juuid gen
# 3f8a2c1e-9b4d-4e7a-8f2b-1c5d6e7f8a9b

juuid gen -n 5               # five UUIDs, one per line
juuid gen --upper            # uppercase
juuid gen -t 7               # UUIDv7: time sortable (RFC 9562)

juuid gen -t 7                                  # v7 generation
juuid parse "$(juuid gen -t 7)"                 # inspect what v7 contains
juuid parse 0190b5c0-6f7f-7a3e-8d2c-1f2e3a4b5c6d
# value: 0190b5c0-6f7f-7a3e-8d2c-1f2e3a4b5c6d
# version: 7
# variant: 2 (RFC 4122 / RFC 9562)
# timestamp: 2024-07-01T12:00:00Z
```

## UUID v7

Version 7 UUIDs start with a 48 bit unix timestamp (milliseconds), followed by random
bits. Two properties make them useful as database keys:

- **sortable**: v7 UUIDs created later compare greater — index friendly
- **extractable timestamp**: the creation time can be recovered from the UUID itself

## Options (generate)

- `-n, --count N` number of UUIDs to generate (default: 1)
- `-t, --type VERSION` UUID version: `4` (random, default) or `7` (time sortable)
- `--upper` print uppercase UUIDs

Exit codes: `0` success, `2` error (invalid version, invalid count).

## Install

```shell
brew tap mhus/jknife
brew install juuid
```

Or download the binary for your platform from the
[releases](https://github.com/mhus/jknife/releases).
