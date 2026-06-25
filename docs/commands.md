# Commands

HeadDB commands are available under:

```text
/hdb
/headdb
```

The help menu is permission-aware. Players only see commands they can use.

## Syntax

| Syntax | Meaning |
| --- | --- |
| `(alias)` | Alias. |
| `<value>` | Required argument. |
| `[value]` | Optional argument. |

## General

| Command | Alias | Permission | Description |
| --- | --- | --- | --- |
| `/hdb` | `/headdb` | `headdb.open`, `headdb.gui.main` | Opens the main GUI when enabled. |
| `/hdb help` | `/hdb h` | `headdb.command.help` | Shows command help. |
| `/hdb version` | — | `headdb.command.version` | Shows version, build, and update status. |
| `/hdb open` | `/hdb o` | `headdb.command.open` | Opens the main GUI. |
| `/hdb open <category> [player]` | `/hdb o` | `headdb.command.open` | Opens a category GUI. |

## Heads

| Command | Alias | Permission | Description |
| --- | --- | --- | --- |
| `/hdb info [id]` | `/hdb i` | `headdb.command.info` | Inspects a head by ID or the held item. |
| `/hdb give <id> [player] [amount]` | `/hdb g` | `headdb.command.give` | Gives a remote HeadDB head. |
| `/hdb player <name|uuid> [player] [amount]` | `/hdb p` | `headdb.command.player` | Gives a player head. |
| `/hdb random [amount] [category] [player]` | `/hdb rnd` | `headdb.command.give` | Gives random heads. |

Examples:

```text
/hdb info 123
/hdb info
/hdb give 123 Steve 4
/hdb player Notch
/hdb random 3 blocks
```

## Search and browse

| Command | Alias | Permission | Description |
| --- | --- | --- | --- |
| `/hdb search <query>` | `/hdb s` | `headdb.command.search` | Searches heads by text. |
| `/hdb search tag <tag>` | `/hdb s tag` | `headdb.command.search` | Searches heads by tag. |
| `/hdb search category <category>` | `/hdb s category` | `headdb.command.search` | Searches heads by category. |
| `/hdb search collection <collection>` | `/hdb s collection` | `headdb.command.search` | Searches heads by collection. |
| `/hdb categories [page]` | `/hdb cat` | `headdb.search` | Lists categories. |
| `/hdb tags [query] [page]` | `/hdb t` | `headdb.search` | Lists or searches tags. |
| `/hdb collections [query] [page]` | `/hdb col` | `headdb.search` | Lists or searches collections. |

## More Heads

| Command | Permission | Description |
| --- | --- | --- |
| `/hdb custom list [page]` | `headdb.command.custom.list` | Lists local custom heads. |
| `/hdb custom info <id>` | `headdb.command.custom.info` | Shows custom head information. |
| `/hdb custom create <id> <texture> [name]` | `headdb.command.custom.create` | Creates a local custom head. |
| `/hdb custom createheld <id> [name]` | `headdb.command.custom.create` | Creates a custom head from your held head. |
| `/hdb custom give <id> [player] [amount]` | `headdb.command.custom.give` | Gives a local custom head. |
| `/hdb custom delete <id>` | `headdb.command.custom.delete` | Deletes a local custom head. |
| `/hdb custom rename <id> <name>` | `headdb.command.custom.rename` | Renames a local custom head. |

## Admin and database

| Command | Alias | Permission | Description |
| --- | --- | --- | --- |
| `/hdb edit <remote-id> <action> [value]` | — | `headdb.command.edit` | Edits local metadata overrides. |
| `/hdb status` | `/hdb st` | `headdb.command.status` | Shows database state and counts. |
| `/hdb debug` | `/hdb d` | `headdb.command.debug` | Shows runtime diagnostics. |
| `/hdb verify` | `/hdb v` | `headdb.command.verify` | Verifies the public database without replacing the active database. |
| `/hdb refresh` | `/hdb ref` | `headdb.command.refresh` | Fetches and activates the latest database. |
| `/hdb reload` | `/hdb rl` | `headdb.command.reload` | Reloads config, messages, GUI config, and runtime services. |
| `/hdb update` | — | `headdb.admin.update` | Checks for and downloads updates when configured. |
| `/hdb itemcache clear` | `/hdb ic clear` | `headdb.command.itemcache` | Clears generated item cache entries. |
