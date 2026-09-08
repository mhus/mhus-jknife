# jperiod

Period/duration converter and parser tool.

## Usage

```
jperiod parse <duration> [options]  # parse and convert a duration
jperiod add <d1> <d2>... [options]  # sum up durations
jperiod sub <d1> <d2>... [options]  # subtract durations (may be negative)
```

## Input formats

| Input          | Meaning                             |
| -------------- | ----------------------------------- |
| `2h30m`        | shorthand, mixed units              |
| `90s`, `1.5d`  | shorthand with decimals (or `,`)     |
| `90`           | bare number = seconds               |
| `P1DT2H30M`   | ISO-8601                            |
| `-- -90s`     | negative durations (needs `--`)      |

Units: `ns`, `us`, `ms`, `s`, `m`, `h`, `d`, `w` (weeks). Years and months are not
supported (ambiguous length) — convert to days first.

## Examples

```shell
jperiod parse 2h30m
# iso: PT2H30M
# human: 2h 30m
# seconds: 9000
# millis: 9000000
# minutes: 150
# hours: 2.5
# days: 0.104166666667
# weeks: 0.014880952381

jperiod parse -u m 2h30m     # 150          (conversion only, script friendly)
jperiod parse -u h 90m       # 1.5
jperiod parse -u d 1w        # 7
jperiod parse P1DT2H30M      # ISO-8601 input
jperiod parse 1m30.5s         # human: 1m 30.5s

jperiod add 2h30m 45m 1h     # iso: PT4H15M
jperiod add -u h 2h30m 45m 1h  # 4.25
jperiod sub 5h 90m           # iso: PT3H30M
jperiod sub 90m 2h           # iso: PT-30M (negative)
```

## Options

- `-u, --unit UNIT` print only the value in this unit: `ns`, `us`, `ms`, `s`, `m`,
  `h`, `d`, `w` (long forms like `minutes` work too)

Exit codes: `0` success, `2` error (unrecognized duration, unknown unit).

## Install

```shell
brew tap mhus/jknife
brew install jperiod
```

Or download the binary for your platform from the
[releases](https://github.com/mhus/jknife/releases).
