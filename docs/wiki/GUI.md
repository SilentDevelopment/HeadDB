# GUI

HeadDB's GUI is the primary player-facing interface. It provides browsing, searching, favorites, player heads, More Heads, More Categories, settings, and staff administration tools.

## Opening

Players can open the main GUI with:

```text
/hdb
/hdb open
/headdb
```

Required permissions:

```text
headdb.open
headdb.gui.main
```

## Main menu

The main menu can show Browse All Heads, category buttons, Search, Favorites, Player Heads, More Heads, More Categories, Settings, and staff controls. Players only see actions they have permission to use.

## Browsing heads

| Action | Behavior |
| --- | --- |
| Left-click head | Obtains the head if the player has permission and economy checks pass. |
| Right-click head | Toggles favorite when permitted. |
| Q/drop on head | Opens edit actions in Admin Mode for permitted staff. |
| Previous/Next | Changes page. |
| Back | Returns to the previous menu. |

Required player permissions usually include:

```text
headdb.gui.browse
headdb.gui.head.take
```

Favorites require:

```text
headdb.gui.favorites
headdb.gui.favorites.toggle
```

## Search

Search supports text query, category filters, tag filters, collection filters, sort option, and sort direction.

Required permissions:

```text
headdb.gui.search
headdb.gui.filter
```

Players can also use `/hdb search <query>`.

## Favorites

Favorites are per-player saved heads. Right-click a head in result menus to favorite or unfavorite it, then open Favorites from the main menu.

## Player Heads

The Player Heads menu displays player-profile heads when enabled.

Required permission:

```text
headdb.gui.player-heads
```

Player-head behavior is configured under `player-heads` in `config.yml`.

## More Heads

More Heads are local custom heads created by staff. They are stored locally and can be browsed through the GUI when enabled.

Required browse permission:

```text
headdb.gui.custom-heads
```

## More Categories

More Categories are server-local custom categories. They are useful for event heads, shop groups, donor cosmetics, seasonal sets, or staff picks.

Permissions:

```text
headdb.gui.more-categories
headdb.gui.more-categories.admin
```

## Settings

The settings menu can include language selection, reset language, and Admin Mode toggle.

Permissions:

```text
headdb.gui.settings
headdb.gui.settings.language
headdb.gui.admin-mode
```

## Admin Mode

Admin Mode exposes additional information and editing actions for staff. It can show edit hints, local visibility controls, hidden heads, button configuration, and More Categories administration.

Required permission:

```text
headdb.gui.admin-mode
```

## Head edit GUI

The head edit GUI modifies local metadata overrides for public heads. It does not change the public database.

Common edit actions:

| Action | Description |
| --- | --- |
| Name | Override displayed name locally. |
| Lore | Override or reset local lore. |
| Category | Override category assignment locally. |
| Tags | Add or remove local tag overrides. |
| Collections | Add or remove local collection overrides. |
| Visibility | Hide or show the head locally. |
| Reset | Remove local overrides. |

## Button configuration

Staff with GUI admin permissions can edit some GUI button properties in-game. For bulk or version-controlled changes, edit `gui.yml` directly.

## Safety behavior

GUI filler, navigation, and configuration items are not intended to be taken. If players can move GUI items unexpectedly, check server version, conflicting inventory plugins, and console errors from inventory events.
