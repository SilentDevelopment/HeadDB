# Getting Started

This is the recommended first configuration pass after installation.

## 1. Start once

Start the server once so HeadDB can generate its data folder:

```text
plugins/HeadDB/
```

Important files:

| File | Purpose |
| --- | --- |
| `config.yml` | Core plugin behavior, cache, storage, messages, player heads, GUI runtime, and update settings. |
| `economy.yml` | Optional Vault pricing. |
| `gui.yml` | GUI filler and icon definitions. |
| `messages/en-US.yml` | Default message file. |
| `storage/headdb.db` | Local durable data such as favorites, custom heads, overrides, and custom categories. |

## 2. Check status

```text
/hdb status
/hdb verify
```

A healthy install should show a loaded database and non-zero counts.

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

Grant admin permissions only to trusted staff.

## 4. Decide whether heads cost money

Economy is disabled by default. To charge players, install Vault and a compatible economy plugin, enable economy in `economy.yml`, then configure prices.

## 5. Review local features

HeadDB can store local custom heads, custom categories, favorites, player-head cache entries, and local metadata overrides. These are stored in `storage/headdb.db` and should be backed up.

## 6. Review update behavior

Recommended production default:

```yaml
update-checker:
  enabled: true

auto-updater:
  install-updates: false
```

This notifies staff without replacing the plugin automatically.
