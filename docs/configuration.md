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

### Remote

| Key | Description |
| --- | --- |
| `remote.manifest-url` | Public database manifest source. Normal servers should leave this unchanged. |
| `remote.preferred-mirror-id` | Preferred source mirror ID. If unavailable, HeadDB falls back automatically. |

These settings are public plugin configuration, not a server-owner database management interface.

### Cache

```yaml
cache:
  directory: cache
  item:
    enabled: true
    max-size: 4096
```

`cache.directory` stores verified public database cache files. Item caching improves GUI and command performance by reusing generated item prototypes.

### Refresh

```yaml
refresh:
  load-cache-on-startup: true
  refresh-on-startup: true
```

Startup cache loading lets HeadDB become usable from the last verified data while refresh is running or if remote refresh fails.

### HTTP

| Key | Description |
| --- | --- |
| `http.connect-timeout-seconds` | HTTP connect timeout. |
| `http.read-timeout-seconds` | HTTP read timeout. |

Increase read timeout only if your server network is slow.

### Storage

```yaml
storage:
  sqlite:
    file: storage/headdb.db
```

This file stores local data such as favorites, More Heads, local overrides, and More Categories. Do not delete it unless you intentionally want to remove local data.

### Feature toggles

| Key | Description |
| --- | --- |
| `remote-overrides.enabled` | Enables local metadata overrides for public heads. |
| `custom-heads.enabled` | Enables More Heads. |
| `player-heads.enabled` | Enables Player Heads. |

### Player heads

```yaml
player-heads:
  enabled: true
  cache-ttl-hours: 12
  failed-cache-ttl-minutes: 10
  allow-external-lookup: true
```

Disable external lookup if you only want HeadDB to use local or known player profiles.

### Messages

| Key | Description |
| --- | --- |
| `messages.directory` | Directory for locale files. |
| `messages.default-locale` | Default player locale. |
| `messages.console-locale` | Console locale. |

### GUI runtime

`gui.open-main-command` controls whether `/hdb` opens the main GUI for players with permission.

### Updates

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

Recommended production behavior is update checking enabled and automatic installation disabled.

## `gui.yml`

`gui.yml` controls filler items and common GUI icons.

| Field | Description |
| --- | --- |
| `type` | `HEAD` or `ITEM`. |
| `head-id` | HeadDB head ID used when `type: HEAD`. |
| `material` | Bukkit material fallback or item material. |
| `name` | MiniMessage-style display name. |
| `lore` | MiniMessage-style lore lines. |

## Safe editing workflow

1. Back up the file.
2. Edit YAML with spaces, not tabs.
3. Run `/hdb reload` or restart.
4. Check console for warnings.
5. Test the affected command or GUI.
