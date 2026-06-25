# Installation

## Requirements

| Requirement | Notes |
| --- | --- |
| Java 25 | Required for the Paper/Folia plugin runtime. |
| Paper or Folia | HeadDB targets modern Paper-compatible servers and declares Folia support. |
| Network access | Needed for first database download, refresh checks, update checks, and optional player-profile lookup. |
| Vault and economy provider | Optional. Required only when economy support is enabled. |

The API and core modules target Java 21. Server owners running the plugin should use Java 25.

## Download

Download the plugin JAR from an official public release location.

Preferred assets:

```text
HeadDB-<version>.jar
HeadDB-<version>.jar.sha256
```

Verify the checksum when available:

```bash
sha256sum -c HeadDB-<version>.jar.sha256
```

## Install

1. Stop the server.
2. Place the HeadDB JAR in the server `plugins` folder.
3. Start the server.
4. Wait for HeadDB to generate files and load the database.
5. Review the console startup output.

Expected data folder:

```text
plugins/HeadDB/
  config.yml
  economy.yml
  gui.yml
  messages/
    en-US.yml
  cache/
  storage/
    headdb.db
```

## Confirm installation

Run:

```text
/hdb version
/hdb status
/hdb verify
```

`/hdb version` shows the running plugin version and update status. `/hdb status` shows the database state, source, and counts. `/hdb verify` checks the public database without replacing the active database.

## Uninstalling

Remove the HeadDB JAR from `plugins`. Remove `plugins/HeadDB/` only if you intentionally want to delete configuration, local custom heads, favorites, overrides, custom categories, cache, and storage data.
