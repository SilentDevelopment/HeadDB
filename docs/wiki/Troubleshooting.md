# Troubleshooting

Use this page to diagnose common HeadDB issues.

## First diagnostics

Run:

```text
/hdb version
/hdb status
/hdb verify
/hdb debug
```

Then check console for warnings around startup, configuration loading, database refresh, local storage, GUI clicks, economy detection, and update checks.

## Plugin does not start

| Check | Fix |
| --- | --- |
| Java version | Use Java 25 for the Paper/Folia plugin. |
| Server type | Use a supported modern Paper/Folia server. |
| JAR placement | Put the HeadDB JAR directly in `plugins/`. |
| Duplicate JARs | Remove old duplicate HeadDB JARs. |
| Dependency errors | Install Vault only if economy support is enabled. |

## Database unavailable

Symptoms:

- `/hdb status` shows failed or unloaded state;
- GUI has no heads;
- refresh fails in console.

Likely causes:

- first startup has no cache and refresh failed;
- network access blocked;
- timeout too low for the environment;
- local cache was deleted;
- server cannot write to the plugin data folder.

Fixes:

1. Check internet access from the server.
2. Run `/hdb verify`.
3. Run `/hdb refresh`.
4. Increase HTTP read timeout if the network is slow.
5. Check file permissions for `plugins/HeadDB/`.

## Server starts from cache only

This is not always a problem. If refresh fails but a verified cache exists, HeadDB can continue using cached data.

Recommended action:

```text
/hdb verify
/hdb refresh
```

## Config fails to load

Common causes:

- YAML indentation error;
- tabs instead of spaces;
- invalid number or boolean;
- invalid relative path;
- missing required value;
- invalid material in `gui.yml`.

Fix by restoring from backup, comparing with defaults, validating YAML, and checking the exact key mentioned in console.

## GUI opens but players cannot take heads

Check permissions:

```text
headdb.gui.head.take
headdb.head.take
```

If economy is enabled, also check player balance, Vault, the economy provider, and configured prices.

## Players cannot see categories

Check permissions:

```text
headdb.gui.category.view
headdb.gui.category.open
headdb.category.*
```

## Search returns no results

Check database state, query spelling, selected filters, hidden local overrides, and category permissions.

## Favorites do not work

Check:

```text
headdb.gui.favorites
headdb.gui.favorites.toggle
```

Also check local storage errors. Favorites require writable local storage.

## Player heads do not work

Check config:

```yaml
player-heads:
  enabled: true
  allow-external-lookup: true
```

Check permissions:

```text
headdb.player-heads
headdb.command.player
headdb.gui.player-heads
```

## Economy does not charge

Check `economy.yml`:

```yaml
enabled: true
provider: vault
```

Then check Vault, the economy provider, non-zero prices, and specific price overrides.

## Local data disappeared

Check whether this file was deleted or replaced:

```text
plugins/HeadDB/storage/headdb.db
```

Restore from backup if needed.

## Reporting issues

Include HeadDB version, server software/version, Java version, relevant console error, affected command/menu, and sanitized config sections. Do not include private tokens or unrelated server secrets.
