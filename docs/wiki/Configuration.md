# Configuration

HeadDB generates configuration files in `plugins/HeadDB/`.

| File | Purpose |
| --- | --- |
| `config.yml` | Core plugin behavior, cache, storage, messages, player heads, GUI runtime, and updater settings. |
| `economy.yml` | Optional Vault economy pricing. |
| `gui.yml` | GUI filler and icon configuration. |
| `messages/<locale>.yml` | Message text and localization files. |

Use `/hdb reload` after editing most configuration files. Restart after replacing the plugin JAR.

## `config.yml`

### Remote settings

| Key | Description |
| --- | --- |
| `remote.manifest-url` | Public database manifest source. Normal servers should leave this unchanged. |
| `remote.preferred-mirror-id` | Preferred source mirror ID. If unavailable, HeadDB falls back automatically. |

These settings are public plugin configuration, not a server-owner database management interface. Leave them at defaults unless instructed.

### Cache

| Key | Description |
| --- | --- |
| `cache.directory` | Directory inside `plugins/HeadDB/` for cached remote artifacts. |
| `cache.item.enabled` | Enables generated head item prototype caching. |
| `cache.item.max-size` | Maximum item cache size. `0` means unbounded lazy caching. |

Recommended:

```yaml
cache:
  directory: cache
  item:
    enabled: true
    max-size: 4096
```

### Refresh

| Key | Description |
| --- | --- |
| `refresh.load-cache-on-startup` | Loads the last verified cache before remote refresh. |
| `refresh.refresh-on-startup` | Refreshes the public database during startup. |

Recommended:

```yaml
refresh:
  load-cache-on-startup: true
  refresh-on-startup: true
```

### HTTP

| Key | Description |
| --- | --- |
| `http.connect-timeout-seconds` | HTTP connect timeout. |
| `http.read-timeout-seconds` | HTTP read timeout. |

Increase read timeout only if your server network is slow.

### Storage

| Key | Description |
| --- | --- |
| `storage.sqlite.file` | SQLite file inside `plugins/HeadDB/` for local durable data. |

Default concept:

```yaml
storage:
  sqlite:
    file: storage/headdb.db
```

This stores local data such as favorites, More Heads, local overrides, and More Categories. Do not delete it unless you intend to remove local data.

### Feature toggles

| Key | Description |
| --- | --- |
| `remote-overrides.enabled` | Enables local metadata overrides for public heads. |
| `custom-heads.enabled` | Enables More Heads. |
| `player-heads.enabled` | Enables Player Heads. |

### Player heads

| Key | Description |
| --- | --- |
| `player-heads.cache-ttl-hours` | TTL for successful player-head cache entries. |
| `player-heads.failed-cache-ttl-minutes` | TTL for failed lookup cache entries. |
| `player-heads.allow-external-lookup` | Allows explicit player lookups outside the local known-player list. |

### Messages

| Key | Description |
| --- | --- |
| `messages.directory` | Directory for locale files. |
| `messages.default-locale` | Default player locale. |
| `messages.console-locale` | Console locale. |

### GUI runtime

| Key | Description |
| --- | --- |
| `gui.open-main-command` | If true, `/hdb` opens the main GUI for players with permission. |

### Updates

| Key | Description |
| --- | --- |
| `update-checker.enabled` | Enables checking public releases for newer versions. |
| `update-checker.check-on-startup` | Checks after startup or reload. |
| `update-checker.notify-console` | Logs update notifications to console. |
| `update-checker.notify-admins` | Notifies players with `headdb.admin.update`. |
| `update-checker.include-prereleases` | Includes alpha, beta, and release-candidate releases. |
| `update-checker.include-builds` | Includes newer build-metadata releases. |
| `auto-updater.install-updates` | Downloads and installs/stages newer plugin JARs when enabled. Disabled by default. |

Recommended:

```yaml
update-checker:
  enabled: true
  check-on-startup: true
  notify-console: true
  notify-admins: true
  include-prereleases: true
  include-builds: true

auto-updater:
  install-updates: false
```

### Debug

`debug: true` enables additional diagnostics. Use it while investigating issues, then disable it to reduce console noise.

## `gui.yml`

`gui.yml` controls filler items and common GUI icons.

Icon fields:

| Field | Description |
| --- | --- |
| `type` | `HEAD` or `ITEM`. |
| `head-id` | HeadDB head ID used when `type: HEAD`. |
| `material` | Bukkit material fallback or item material. |
| `name` | MiniMessage-style display name. |
| `lore` | MiniMessage-style lore lines. |

## `messages/`

Message files are locale-specific YAML files. Keep placeholders intact and use `/hdb reload` after editing.

## Safe editing workflow

1. Back up the file.
2. Edit YAML with spaces, not tabs.
3. Run `/hdb reload` or restart.
4. Check console for warnings.
5. Test the affected command or GUI.

## Common mistakes

| Mistake | Result | Fix |
| --- | --- | --- |
| Broken YAML indentation | Config load failure. | Use spaces and validate YAML. |
| Invalid material name | GUI icon fallback or error. | Use valid Bukkit material names. |
| Deleting `storage/headdb.db` | Local data loss. | Restore from backup. |
| Disabling refresh and deleting cache | Database unavailable. | Re-enable refresh or restore cache. |
