# Getting Started

This page is the recommended first configuration pass after installing HeadDB.

## 1. Start once and inspect files

Start the server once so HeadDB can generate:

```text
plugins/HeadDB/
```

Important files:

| File | Purpose |
| --- | --- |
| `config.yml` | Core plugin behavior, cache, storage, messages, player heads, GUI runtime, and updater settings. |
| `economy.yml` | Optional Vault pricing. |
| `gui.yml` | GUI filler and icon definitions. |
| `messages/en-US.yml` | Default message file. |
| `storage/headdb.db` | Local durable data such as favorites, custom heads, overrides, and custom categories. |

## 2. Check runtime status

Run:

```text
/hdb status
/hdb verify
```

A healthy install should show a loaded database and non-zero database counts. `/hdb verify` checks the public database without replacing the active database.

## 3. Assign permissions

Simple setup:

```text
Players: headdb.basic
Staff:   headdb.admin
```

Controlled setup:

```text
headdb.open
headdb.browse
headdb.search
headdb.favorites
headdb.player-heads
headdb.more-heads
```

Grant admin permissions only to trusted staff. See [[Permissions]].

## 4. Decide whether heads cost money

Economy is disabled by default. To charge players, install Vault and a Vault-compatible economy plugin, set `enabled: true` in `economy.yml`, then configure fallback, category, custom-category, or per-head prices. See [[Economy]].

## 5. Review local features

Decide whether your server should use player heads, More Heads, More Categories, favorites, and local metadata overrides. These are stored locally in HeadDB storage and can be backed up with the plugin data folder.

## 6. Review update behavior

Recommended production default:

```yaml
update-checker:
  enabled: true

auto-updater:
  install-updates: false
```

This notifies staff without automatically replacing the plugin JAR. See [[Updating]].

## 7. Publish player commands

Primary entrypoints:

```text
/hdb
/headdb
/hdb search <query>
/hdb info [id]
/hdb player <name|uuid>
```

## Recommended defaults

| Area | Recommendation |
| --- | --- |
| Database refresh | Keep startup refresh enabled. |
| Cache | Keep artifact and item cache enabled. |
| Economy | Enable only if heads should cost money. |
| Admin Mode | Staff only. |
| Custom heads | Staff-created only. |
| Player heads | Enable if your server wants player-profile heads. |
| Auto-updater | Check enabled, install disabled. |
