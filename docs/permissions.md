# Permissions

HeadDB provides broad permission bundles and granular nodes.

Recommended simple setup:

```text
Players: headdb.basic
Staff:   headdb.admin
```

## Top-level bundles

| Permission | Description |
| --- | --- |
| `headdb.admin` | Grants every HeadDB permission. Staff only. |
| `headdb.basic` | Basic player usage: help, version, settings, open, browse, search, favorites, and taking heads. |
| `headdb.open` | Opens the main GUI. |
| `headdb.browse` | Browses categories and head lists. |
| `headdb.search` | Uses search commands, GUI search, and filters. |
| `headdb.head.take` | Takes heads from HeadDB GUIs. |
| `headdb.give` | Gives remote heads with commands. |
| `headdb.give.others` | Gives remote heads to other players. |
| `headdb.player-heads` | Uses player-head commands and GUI. |
| `headdb.more-heads` | Lists, inspects, and browses More Heads. |
| `headdb.more-heads.admin` | Creates, deletes, and renames More Heads. |
| `headdb.favorites` | Opens Favorites and toggles favorite heads. |
| `headdb.more-categories` | Opens More Categories. |
| `headdb.more-categories.admin` | Creates, edits, and deletes More Categories. |
| `headdb.settings` | Opens settings and changes language. |
| `headdb.admin-mode` | Toggles Admin Mode. |
| `headdb.head-edit` | Edits local metadata overrides. |
| `headdb.gui-admin` | Administers GUI/button configuration. |
| `headdb.database` | Uses database, reload, update, and cache administration commands. |

## Command permissions

| Permission | Allows |
| --- | --- |
| `headdb.command.help` | `/hdb help` |
| `headdb.command.version` | `/hdb version` |
| `headdb.command.status` | `/hdb status` |
| `headdb.command.debug` | `/hdb debug` |
| `headdb.command.verify` | `/hdb verify` |
| `headdb.command.refresh` | `/hdb refresh` |
| `headdb.command.reload` | `/hdb reload` |
| `headdb.admin.update` | `/hdb update` and admin update notifications. |
| `headdb.command.search` | `/hdb search` |
| `headdb.command.info` | `/hdb info` |
| `headdb.command.give` | `/hdb give` |
| `headdb.command.give.others` | Giving heads to other players. |
| `headdb.command.open` | `/hdb open` |
| `headdb.command.open.others` | Opening menus for other players. |
| `headdb.command.itemcache` | `/hdb itemcache clear` |
| `headdb.command.player` | `/hdb player` |
| `headdb.command.player.others` | Player-head commands targeting another player. |
| `headdb.command.custom.*` | More Heads command access. |
| `headdb.command.edit` | `/hdb edit` |

## GUI permissions

| Permission | Allows |
| --- | --- |
| `headdb.gui.main` | Opens the main GUI. |
| `headdb.gui.browse` | Browses result GUIs. |
| `headdb.gui.search` | Uses GUI search. |
| `headdb.gui.filter` | Uses sort and filter controls. |
| `headdb.gui.head.take` | Takes heads from HeadDB GUIs. |
| `headdb.gui.category.view` | Views category buttons. |
| `headdb.gui.category.open` | Opens category result GUIs. |
| `headdb.gui.player-heads` | Opens Player Heads. |
| `headdb.gui.custom-heads` | Opens More Heads. |
| `headdb.gui.favorites` | Opens Favorites. |
| `headdb.gui.favorites.toggle` | Adds or removes favorite heads. |
| `headdb.gui.more-categories` | Opens More Categories. |
| `headdb.gui.more-categories.admin` | Administers More Categories. |
| `headdb.gui.hidden-heads` | Opens Hidden Heads in Admin Mode. |
| `headdb.gui.edit` | Opens head edit menus. |
| `headdb.gui.button-config` | Edits GUI button configuration. |
| `headdb.gui.settings` | Opens settings. |
| `headdb.gui.settings.language` | Changes language. |
| `headdb.gui.admin-mode` | Toggles Admin Mode. |

Only trusted staff should receive `headdb.admin`, updater, reload, refresh, or Admin Mode permissions.
