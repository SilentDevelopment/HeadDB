# Heads and Local Data

HeadDB separates public database data from server-local mutable data. Public data is loaded and verified by HeadDB. Local data is stored in your plugin data folder and can be customized by staff.

## Head ID types

| Type | Example | Description |
| --- | --- | --- |
| Remote head | `123` | Public database head. |
| Custom head | `custom:melon` | Server-local custom head. |
| Player head | `player:f16df3ef-06b8-443e-9166-fba6689585b4` | Player-profile head. |

## Remote heads

Remote heads come from the public HeadDB catalog. Server owners can browse, search, give, price, favorite, locally override, and hide/show them.

Server owners do not need to manage the public database implementation.

## Local overrides

Local overrides customize public remote heads for your server only.

Supported concepts include:

- display name;
- lore;
- category;
- tags;
- collections;
- visibility.

Use cases:

- hide a head unsuitable for your server;
- rename a head to match server terminology;
- add local lore for shop information;
- group heads differently for an event.

Config:

```yaml
remote-overrides:
  enabled: true
```

## Player heads

Player heads are generated from player profiles.

Config:

```yaml
player-heads:
  enabled: true
  cache-ttl-hours: 12
  failed-cache-ttl-minutes: 10
  allow-external-lookup: true
```

Command:

```text
/hdb player <name|uuid> [player] [amount]
```

Permissions:

```text
headdb.player-heads
headdb.player-heads.others
```

Disable external lookup if you want HeadDB to use only local/known player profiles.

## More Heads

More Heads are server-local custom heads.

Config:

```yaml
custom-heads:
  enabled: true
```

Common commands:

```text
/hdb custom list
/hdb custom info <id>
/hdb custom create <id> <texture> [name]
/hdb custom createheld <id> [name]
/hdb custom give <id> [player] [amount]
/hdb custom delete <id>
/hdb custom rename <id> <name>
```

Recommended ID style:

```text
custom:event_dragon
custom:shop_token
custom:staff_pick_01
```

## More Categories

More Categories are server-local custom categories. They are useful for shop groups, event rewards, seasonal menus, staff picks, donor cosmetics, and server-themed collections.

Permissions:

```text
headdb.more-categories
headdb.more-categories.admin
```

## Favorites

Favorites are stored per player and let players quickly reopen frequently used heads.

Permissions:

```text
headdb.favorites
headdb.gui.favorites
headdb.gui.favorites.toggle
```

## Local storage

Local mutable data is stored in SQLite by default:

```text
plugins/HeadDB/storage/headdb.db
```

This can include favorites, custom heads, local overrides, custom categories, and player-head cache data.

## Cache vs storage

| Area | Purpose | Safe to delete? |
| --- | --- | --- |
| `cache/` | Verified public database cache and runtime cache files. | Usually yes, but the plugin must refresh again. |
| `storage/headdb.db` | Server-local durable data. | No, unless you intentionally want to delete local data. |

## Backup recommendation

Back up:

```text
plugins/HeadDB/config.yml
plugins/HeadDB/economy.yml
plugins/HeadDB/gui.yml
plugins/HeadDB/messages/
plugins/HeadDB/storage/headdb.db
```
