# SmartyPDT

Smarty template support for the Eclipse PHP Development Tools, revived for **Eclipse IDE for PHP Developers 2026-03 (4.39.0)**.

This fork is based on [pulse00/smartypdt](https://github.com/pulse00/smartypdt) and modernizes the build so that the plug-ins can be compiled and installed on current Eclipse releases:

- Tycho `5.0.4` (was `1.2.0`)
- Target platform: `https://download.eclipse.org/releases/2026-03` (PDT `8.4.0`)
- BREE and compiler settings: `JavaSE-21` (mandatory: PDT 8.4 declares a requirement on Java 21)
- Fixed `ActionContributorForPhp` -> `ActionContributorForPHP` (the class was renamed in PDT)
- New `org.eclipse.php.smarty.repository` module that produces an installable p2 repository

## Versioning

The plug-in version follows the PDT version it is built against, followed by our own release number in the OSGi qualifier:

```
8.4.0.v0_12_0
|-----| |-----|
  PDT    ours
```

- `8.4.0` is the PDT version from the target platform (`https://download.eclipse.org/releases/2026-03` ships PDT `8.4.0`). It is the base version of the two bundles, of the feature and the parent poms.
- `v0_12_0` is our release, derived by CI from the git tag (`v0.12.0`). OSGi qualifiers may only contain `[0-9a-zA-Z_-]`, so the dots of the tag become underscores. As a consequence `v0.13.0` produces the full version `8.4.0.v0_13_0`.
- Releasing a new SmartyPDT version therefore needs no source change at all: tag and push. Only when Eclipse moves to a new PDT do you run `./set-pdt-version.sh 8.5.0` once and update the target platform URL in `pom.xml`.

## Requirements

- JDK 21+ (e.g. `brew install openjdk@21`)
- Maven 3.9+ (e.g. `brew install maven`)
- Network access to `download.eclipse.org`: the first build resolves and downloads the target platform

## Build

```bash
./build.sh
```

The script runs `mvn clean verify` with a timestamped build qualifier and produces `dist/smartypdt-update-site-8.4.0-<qualifier>.zip`, a p2 update-site archive.

To pick a specific qualifier, set the `QUALIFIER` environment variable (it is sanitized to the OSGi allowed character set `[0-9a-zA-Z_-]`, so the tag `v0.12.0` becomes `v0_12_0`):

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
- `{literal} ... {/literal}` blocks are supported: Smarty code inside them is not parsed, so CSS/JS braces and `{$placeholder}` fragments stay plain HTML content and are highlighted like the rest of the template.
- `SmartyTokenizer.java` is maintained by hand; the JFlex specification in `devTools/` documents the original grammar but is not used to regenerate that file.

## Installing from the online update site

Every published release is served from the `update-site` branch as a static composite p2 repository, so one URL always contains all published versions:

```
https://raw.githubusercontent.com/tonyhouse1982/smartypdt/update-site/
```

In Eclipse: `Help` > `Install New Software...` > `Add...` and paste that URL. The same URL can later be used with `Help` > `Check for Updates`, because older releases stay in the composite repository next to the new ones.

The `publish` job in GitHub Actions rebuilds that branch on every `v*` tag, adding the new version next to the ones that are already published. No GitHub Pages configuration is needed: raw file access to the branch is enough for p2.

## Releases and continuous integration

Every push to `master` builds the update site on GitHub Actions and stores it as a workflow artifact (`smartypdt-update-site`). The same `./build.sh` can always be run locally.

Pushing a tag named `v*` (for example `v0.12.0`) builds the update site and publishes a GitHub release with the p2 zip attached, so the packaged plug-in can be downloaded without compiling anything:

```bash
git tag v0.12.0
git push origin v0.12.0
```

Download the zip from the repository `Releases` page and follow the installation steps above.