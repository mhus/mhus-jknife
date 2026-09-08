# Release process

A release consists of three parts:

1. **Version tag** in this repo — triggers the native build workflow
2. **GitHub release** — automatically created with the native binaries attached
3. **Homebrew tap update** (repo `mhus/homebrew-jknife`) — one formula per tool

## Quick release

Everything below is automated by one script:

```shell
./scripts/release.sh 0.1.0
```

The script:

1. checks preconditions: on branch `main`, clean and synced worktree, tag not taken
2. runs all tests (`mvn verify`)
3. sets the version in all `pom.xml` **and** the `VERSION` constants in the `*Cmd.java`
   main classes (`scripts/set-version.sh`)
4. commits (`release: v0.1.0`), tags `v0.1.0` and pushes to origin
5. asks for confirmation before pushing (use `--yes` to skip)

## What the release workflow does

The [release workflow](../.github/workflows/release.yml) starts automatically on a
pushed `v*` tag and, per platform (macOS aarch64/x86_64, Linux x86_64/aarch64):

1. sets the release version (`scripts/set-version.sh`)
2. runs all tests **twice**: on the JVM and as GraalVM native test images
   (`mvn -Pnative package`)
3. packs each tool as `<tool>-<version>-<rid>.tar.gz` plus `.sha256` checksum file
4. runs smoke tests against the built binaries (startup, functional commands)
5. attaches everything to the GitHub release

Watch the run:

```shell
gh run watch
# or: https://github.com/mhus/jknife/actions/workflows/release.yml
```

## Update the homebrew tap

Once the GitHub release exists, generate the formulas with correct checksums:

```shell
./scripts/gen-formula.sh 0.1.0
# writes dist/jregex.rb (one .rb per tool)
```

Then in the tap repo (`mhus/homebrew-jknife`):

```shell
cp <path-to-jknife>/dist/jregex.rb Formula/jregex.rb
git add Formula && git commit -m "jregex 0.1.0" && git push
```

Verify the tap:

```shell
brew tap mhus/jknife
brew install jregex
jregex --version          # must print 0.1.0
brew test jregex
brew audit --tap=mhus/jknife jregex
```

## Rules

- **Versioning**: [semver](https://semver.org/), tags with `v` prefix (`v0.1.0`).
  Breaking CLI changes (removed/renamed options) bump at least the minor version.
- The release version must **never** be a `-SNAPSHOT`. Release from `main` only.
- If the release build fails: fix on `main`, then **bump to a new patch version** and
  release again (never move an existing tag).
- The `VERSION` constants in the `*Cmd.java` files and the maven versions are set by
  the scripts — do not maintain them by hand.
