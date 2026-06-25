# Troubleshooting

Start with:

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

## Config fails to load

Common causes:

- YAML indentation error;
- tabs instead of spaces;
- invalid number or boolean;
- invalid relative path;
- missing required value;
- invalid material in `gui.yml`.

Restore from backup, compare with defaults, validate YAML, and check the exact key mentioned in console.

## GUI opens but players cannot take heads

Check permissions:

```text
headdb.gui.head.take
headdb.head.take
```

If economy is enabled, also check player balance, Vault, the economy provider, and configured prices.

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

## Local data disappeared

Check whether this file was deleted or replaced:

```text
plugins/HeadDB/storage/headdb.db
```

Restore from backup if needed.
