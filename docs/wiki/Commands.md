# Commands

HeadDB commands are available under:

```text
/hdb
/headdb
```

The in-game help menu is permission-aware. Players only see commands they can use.

## Syntax

| Syntax | Meaning |
| --- | --- |
| `(alias)` | Alias. |
| `<value>` | Required argument. |
| `[value]` | Optional argument. |

## General

| Command | Alias | Permission | Description |
| --- | --- | --- | --- |
| `/hdb` | `/headdb` | `headdb.open`, `headdb.gui.main` | Opens the main GUI for players when enabled. |
| `/hdb help` | `/hdb h` | `headdb.command.help` | Shows command help. |
| `/hdb version` | — | `headdb.command.version` | Shows version, build, and update status. |
| `/hdb open` | `/hdb o` | `headdb.command.open` | Opens the main GUI. |
| `/hdb open <category> [player]` | `/hdb o` | `headdb.command.open`, `headdb.command.open.others` for other players | Opens a category GUI. |

## Heads

| Command | Alias | Permission | Description |
| --- | --- | --- | --- |
| `/hdb info [id]` | `/hdb i` | `headdb.command.info` | Inspects a head by ID or the held item when no ID is provided. |
| `/hdb give <id> [player] [amount]` | `/hdb g` | `headdb.command.give`, `headdb.command.give.others` for other players | Gives a remote HeadDB head. |
| `/hdb player <name|uuid> [player] [amount]` | `/hdb p` | `headdb.command.player`, `headdb.command.player.others` for other players | Gives a player head. |
| `/hdb random [amount] [category] [player]` | `/hdb rnd` | `headdb.command.give` | Gives random HeadDB heads. |

Examples:

```text
/hdb info 123
/hdb info
/hdb give 123 Steve 4
/hdb player Notch
/hdb random 3 blocks
```

## Browse

| Command | Alias | Permission | Description |
| --- | --- | --- | --- |
| `/hdb categories [page]` | `/hdb cat`, `/hdb cats` | `headdb.search` | Lists available categories. |
| `/hdb tags [query] [page]` | `/hdb t` | `headdb.search` | Lists or searches tags. |
| `/hdb collections [query] [page]` | `/hdb col`, `/hdb cols` | `headdb.search` | Lists or searches collections. |

## Search

| Command | Alias | Permission | Description |
| --- | --- | --- | --- |
| `/hdb search <query>` | `/hdb s` | `headdb.command.search` | Searches heads by text. |
| `/hdb search text <query>` | `/hdb s text` | `headdb.command.search` | Explicit text search. |
| `/hdb search head <id>` | `/hdb s head` | `headdb.command.search` | Finds one head by ID. |
| `/hdb search tag <tag>` | `/hdb s tag` | `headdb.command.search` | Searches heads by tag. |
| `/hdb search category <category>` | `/hdb s category` | `headdb.command.search` | Searches heads by category. |
| `/hdb search collection <collection>` | `/hdb s collection` | `headdb.command.search` | Searches heads by collection. |

## More Heads

More Heads are local custom heads.

| Command | Permission | Description |
| --- | --- | --- |
| `/hdb custom list [page]` | `headdb.command.custom.list` | Lists local custom heads. |
| `/hdb custom info <id>` | `headdb.command.custom.info` | Shows custom head information. |
| `/hdb custom create <id> <texture> [name]` | `headdb.command.custom.create` | Creates a local custom head from texture input. |
| `/hdb custom createheld <id> [name]` | `headdb.command.custom.create` | Creates a custom head from your held head. |
| `/hdb custom give <id> [player] [amount]` | `headdb.command.custom.give`, `headdb.command.custom.give.others` for other players | Gives a local custom head. |
| `/hdb custom delete <id>` | `headdb.command.custom.delete` | Deletes a local custom head. |
| `/hdb custom rename <id> <name>` | `headdb.command.custom.rename` | Renames a local custom head. |

## Admin and database

| Command | Alias | Permission | Description |
| --- | --- | --- | --- |
| `/hdb edit <remote-id> <action> [value]` | — | `headdb.command.edit` plus action-specific edit permissions | Edits local metadata overrides for remote heads. |
| `/hdb status` | `/hdb st` | `headdb.command.status` | Shows database state and counts. |
| `/hdb debug` | `/hdb d` | `headdb.command.debug` | Shows detailed runtime diagnostics. |
| `/hdb verify` | `/hdb v` | `headdb.command.verify` | Verifies the public remote without replacing the active database. |
| `/hdb refresh` | `/hdb ref` | `headdb.command.refresh` | Fetches and activates the latest public database. |
| `/hdb reload` | `/hdb rl`, `/hdb rel` | `headdb.command.reload` | Reloads config, messages, GUI config, and runtime services. |
| `/hdb update` | — | `headdb.admin.update` | Checks for and downloads the latest plugin version when updater settings allow it. |
| `/hdb itemcache clear` | `/hdb ic clear` | `headdb.command.itemcache` | Clears generated item cache entries. |
