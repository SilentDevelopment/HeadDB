# HeadDB Wiki

HeadDB is a modern Minecraft head database plugin for Paper and Folia servers. It gives players a clean GUI for browsing, searching, favoriting, pricing, and obtaining head items, while giving server owners control over permissions, messages, GUI layout, local custom heads, custom categories, and update behavior.

This wiki documents the public plugin surface: installation, commands, permissions, configuration, GUI usage, economy setup, updates, and the public integration API.

> This wiki intentionally does not document private database build systems, internal hosting architecture, data pipeline internals, or web frontend implementation details.

## Quick navigation

| Page | Purpose |
| --- | --- |
| [[Installation]] | Requirements, plugin setup, first startup, and generated files. |
| [[Getting Started]] | Recommended first configuration pass for a new server. |
| [[Commands]] | Complete `/hdb` command reference with examples. |
| [[Permissions]] | Permission bundles and granular permission nodes. |
| [[Configuration]] | `config.yml`, cache, storage, messages, player heads, and update settings. |
| [[GUI]] | Main menu, browsing, search, favorites, settings, and admin mode. |
| [[Heads and Local Data]] | Remote heads, player heads, More Heads, More Categories, favorites, and overrides. |
| [[Economy]] | Vault pricing, category prices, custom-category prices, and per-head prices. |
| [[Updating]] | Version checks, auto-updater behavior, manual updates, and release checksums. |
| [[API]] | Public API usage for other plugins. |
| [[Troubleshooting]] | Common startup, database, GUI, permission, and updater issues. |
| [[FAQ]] | Short answers to common server-owner questions. |

## Core concepts

### Remote heads

Remote heads are the public head catalog loaded by HeadDB. Server owners and players can browse, search, inspect, and obtain these heads through commands and GUI menus.

HeadDB verifies downloaded database artifacts before using them and keeps a local cache so the plugin can start from the last verified data when remote refresh is unavailable.

### Player heads

Player heads are generated from Minecraft player profiles. They can be used through the Player Heads GUI or the `/hdb player` command when enabled.

### More Heads

More Heads are server-local custom heads created by administrators. They are stored locally and are not part of the public remote catalog.

### More Categories

More Categories are server-local custom categories. They let administrators group remote heads, player heads, and custom heads into server-specific collections.

### Favorites

Players can save favorite heads for faster access. Favorites are stored locally per player.

### Admin Mode

Admin Mode exposes extra GUI information and editing actions for staff. It is permission-gated and should only be granted to trusted users.

## Recommended server-owner flow

1. Install HeadDB and start the server once.
2. Review generated config files under `plugins/HeadDB/`.
3. Assign permission bundles to players and staff.
4. Configure economy pricing if heads should cost money.
5. Customize GUI icons/messages if needed.
6. Use `/hdb status` and `/hdb verify` to confirm the database is healthy.
7. Publish `/hdb` or `/headdb` to players.

## Support boundaries

HeadDB exposes configuration and local data controls for server owners. The public plugin does not require server owners to manage the remote database implementation. Leave remote source settings at their defaults unless you know exactly why you are changing them.
