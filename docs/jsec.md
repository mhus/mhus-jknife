# jsec

Security tool family — a single binary with subcommands, based on the
**java security framework** (MessageDigest, KeyPairGenerator, AES-GCM, PBKDF2,
Signature). No external dependencies, no network needed.

## Usage

```
jsec hash [options] [input]           # hash input (default sha256)
jsec create key [options]              # asymmetric key pair (rsa, ec, ed25519) as pem
jsec create secret [options]           # random symmetric secret (base64)
jsec encrypt [options] [input]         # aes-gcm encrypt, envelope as base64
jsec decrypt [options] [envelope]     # aes-gcm decrypt, plain bytes to stdout
jsec sign [options] [input]            # sign with a private key
jsec verify [options] [input]          # verify a signature, exit code 0/1
```

## Examples

```shell
# hash (sha256 default, hex default; -a md5/sha512/..., -o base64)
jsec hash "hello"
echo "hello" | jsec hash -a sha512 -o base64
jsec hash --list                  # available MessageDigest algorithms

# symmetric: secret + encrypt/decrypt roundtrip
KEY=$(jsec create secret -s 256)  # random 256 bit aes key, base64
jsec encrypt --key "$KEY" "secret message" > msg.enc
jsec decrypt --key "$KEY" "$(cat msg.enc)"

# or password based (pbkdf2, salt is part of the envelope)
jsec encrypt --password geheim "secret message"
jsec decrypt --password geheim <envelope>

# asymmetric: key pair + sign/verify
jsec create key -a rsa -s 2048 --out-private priv.pem --out-public pub.pem
SIG=$(jsec sign --key-file priv.pem "important document")
jsec verify --key-file pub.pem --sig "$SIG" "important document" && echo valid

# curves / ed25519
jsec create key -a ec --curve secp384r1
jsec create key -a ed25519
```

## Details

- **hash**: MessageDigest (default `sha256`), input as argument, file (`-t`) or
  stdin — binary safe. Output hex (default) or base64 (`-o`).
- **create key**: KeyPairGenerator — `rsa` (`-s` key size), `ec` (`--curve`, default
  secp256r1) or `ed25519`. Output: private key pkcs#8 pem + public key x.509 pem
  (stdout, or files with `--out-private`/`--out-public`).
- **create secret**: SecureRandom bytes, base64 output — directly usable as
  `encrypt --key`.
- **encrypt/decrypt**: AES-GCM (128 bit tag, 12 byte random nonce). Key from
  `--key <base64>`, `--key-file <file>` (base64 or pem) or derived from `--password`
  (PBKDF2WithHmacSHA256, 100k iterations, 16 byte salt inside the envelope). The
  envelope (base64) is self-contained — decrypt only needs the same key source.
  Output of decrypt is the raw plain bytes.
- **sign/verify**: Signature — algorithm auto-detected by key type
  (rsa → SHA256withRSA, ec → SHA256withECDSA, ed25519 → Ed25519), `-a` overrides.
  `verify` prints `true`/`false` and exits 0/1 (script friendly).

Exit codes: `0` success/valid, `1` verification failed, `2` error (bad input,
unknown algorithm, wrong key, corrupted data).

## Install

```shell
brew tap mhus/jknife
brew install jsec
```

Or download the binary for your platform from the
[releases](https://github.com/mhus/jknife/releases).
