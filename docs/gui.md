# GUI

HeadDB's GUI is the main player-facing interface. It provides browsing, search, favorites, player heads, More Heads, More Categories, settings, and staff administration tools.

## Opening

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
| Left-click head | Obtains the head if permission and economy checks pass. |
| Right-click head | Toggles favorite when permitted. |
| Q/drop on head | Opens edit actions in Admin Mode for permitted staff. |
| Previous/Next | Changes page. |
| Back | Returns to the previous menu. |

## Search

Search supports text query, category filters, tag filters, collection filters, sort option, and sort direction.

Permissions:

```text
headdb.gui.search
headdb.gui.filter
```

## Favorites

Favorites are per-player saved heads. Right-click a head in result menus to favorite or unfavorite it, then open Favorites from the main menu.

## Player Heads

The Player Heads menu displays player-profile heads when enabled.

Permission:

```text
headdb.gui.player-heads
```

## More Heads

More Heads are local custom heads created by staff. They are stored locally and can be browsed through the GUI when enabled.

Permission:

```text
headdb.gui.custom-heads
```

## More Categories

More Categories are server-local custom categories for shop groups, event sets, staff picks, donor cosmetics, or server-themed collections.

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

GUI filler, navigation, and configuration items are not intended to be taken. If players can move GUI items unexpectedly, check server version, conflicting inventory plugins, and console errors from inventory events.
