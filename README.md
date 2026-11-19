# SmartyPDT

Smarty template support for the Eclipse PHP Development Tools, revived for **Eclipse IDE for PHP Developers 2026-03 (4.39.0)**.

This fork is based on [pulse00/smartypdt](https://github.com/pulse00/smartypdt) and modernizes the build so that the plug-ins can be compiled and installed on current Eclipse releases:

- Tycho `5.0.4` (was `1.2.0`)
- Target platform: `https://download.eclipse.org/releases/2026-03` (PDT `8.4.0`)
- BREE and compiler settings: `JavaSE-21` (mandatory: PDT 8.4 declares a requirement on Java 21)
- Fixed `ActionContributorForPhp` -> `ActionContributorForPHP` (the class was renamed in PDT)
- New `org.eclipse.php.smarty.repository` module that produces an installable p2 repository

## Requirements

- JDK 21+ (e.g. `brew install openjdk@21`)
- Maven 3.9+ (e.g. `brew install maven`)
- Network access to `download.eclipse.org`: the first build resolves and downloads the target platform

## Build

```bash
./build.sh
```

The script runs `mvn clean verify` with a timestamped build qualifier and produces `dist/smartypdt-update-site-0.12.0-<qualifier>.zip`, a p2 update-site archive.

To pick a specific qualifier, set the `QUALIFIER` environment variable (it is sanitized to the OSGi allowed character set `[0-9a-zA-Z_-]`, so `v0.12.0` becomes `v0_12_0`):

```bash
QUALIFIER=build42 ./build.sh
```

## Installation in Eclipse 2026-03

1. `Help` > `Install New Software...`
2. `Add...` > `Archive...` and select `dist/smartypdt-update-site-*.zip`
3. Select `SmartyPDT (Smarty support for PDT)`, complete the wizard and restart Eclipse

Alternatively, extract the archive to a local folder and use `Add...` > `Local...`.

After the restart, Smarty support can be enabled per project with right click > `Configure` > `Add Smarty Support...`. Files with the `tpl` and `template` extensions are opened with the Smarty editor.

## Contents

- `org.eclipse.php.smarty.core`: `*.tpl` / `*.template` content type, WST model handler, Smarty tokenizer and partitioner, DLTK build participant, Smarty compiler invoked through the PHP CLI
- `org.eclipse.php.smarty.ui`: Smarty editor, syntax highlighting, completion for Smarty tags and expressions, templates, preference pages, `Add Smarty Support...` / `Remove Smarty Support...` actions

## Notes

- The original project relies on internal PDT and WST APIs. Most of them are still available in PDT 8.4, but they are not guaranteed to be stable and may change in future releases.
- The bundles are not signed: Eclipse will ask for confirmation about an untrusted publisher.
- The `org.eclipse.php.smarty.test` fragment is not part of the build (it depends on `org.eclipse.php.core.tests`, which is not shipped in the release repository).

## Installing from the online update site

The published releases are also served as a static p2 composite repository on GitHub Pages:

```
https://tonyhouse1982.github.io/smartypdt/
```

In Eclipse: `Help` > `Install New Software...` > `Add...` > paste that URL. All published versions are kept in the same repository, so the same URL can be used later for `Help` > `Check for Updates`: Eclipse always sees the newest release together with the previous ones.

## Releases and continuous integration

Every push to `master` builds the update site on GitHub Actions and stores it as a workflow artifact (`smartypdt-update-site`). The same `./build.sh` can always be run locally.

Pushing a tag named `v*` (for example `v0.12.0`) builds the update site and publishes a GitHub release with the p2 zip attached, so the packaged plug-in can be downloaded without compiling anything:

```bash
git tag v0.12.0
git push origin v0.12.0
```

Download the zip from the repository `Releases` page and follow the installation steps above.