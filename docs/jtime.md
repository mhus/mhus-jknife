# jtime

Timestamp converter tool — parses epoch seconds, epoch milliseconds and ISO-8601
date-times and prints all common representations.

## Usage

```
jtime now [options]          # print the current time
jtime parse <value> [options] # parse and convert a timestamp
```

## Examples

```shell
jtime now
# 2024-09-08T11:56:40.123+02:00[Europe/Berlin]

jtime now --epoch            # 1725787000
jtime now --millis           # 1725787000123
jtime now -z UTC             # 2024-09-08T09:56:40.123Z[UTC]
jtime now -f 'yyyy-MM-dd HH:mm:ss'

jtime parse 1725787000
# epoch: 1725787000
# epoch-millis: 1725787000000
# iso-utc: 2024-09-08T09:56:40Z
# iso[Europe/Berlin]: 2024-09-08T11:56:40+02:00[Europe/Berlin]

jtime parse 1725787000123    # epoch millis (11-13 digits)
jtime parse 2024-09-08T09:56:40Z
jtime parse '2024-09-08T11:56:40+02:00'
jtime parse -z UTC 2024-09-08T12:00:00   # local time, interpreted in UTC
jtime parse -f 'yyyy-MM-dd' 1725787000   # custom output format
```

## Input detection (parse)

| Input                    | Parsed as                             |
| ------------------------ | ------------------------------------- |
| 1-10 digits              | unix epoch seconds                    |
| 11-13 digits             | unix epoch milliseconds               |
| ISO instant (`...Z`)     | ISO-8601 instant                      |
| ISO with offset (`±hh:mm`) | ISO-8601 with offset               |
| local date-time          | interpreted in the selected zone      |

## Options

- `-z, --zone ZONE` time zone id, e.g. `UTC` or `Europe/Berlin` (default: system zone)
- `-f, --format PATTERN` custom
  [java time pattern](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html)
  output, e.g. `yyyy-MM-dd HH:mm:ss`
- `now` only: `--epoch` print epoch seconds, `--millis` print epoch milliseconds

Exit codes: `0` success, `2` error (unrecognized timestamp, unknown zone, bad pattern).

## Install

```shell
brew tap mhus/jknife
brew install jtime
```

Or download the binary for your platform from the
[releases](https://github.com/mhus/jknife/releases).
