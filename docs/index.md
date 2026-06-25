---
layout: home

hero:
  name: HeadDB
  text: Modern head database for Paper and Folia
  tagline: Browse, search, favorite, price, edit, and obtain Minecraft heads through a clean GUI with server-owner control.
  actions:
    - theme: brand
      text: Get Started
      link: /getting-started
    - theme: alt
      text: Commands
      link: /commands

features:
  - title: Player-focused GUI
    details: Browse categories, search heads, manage favorites, and obtain heads without command-heavy workflows.
  - title: Server-owner control
    details: Configure permissions, messages, GUI layout, local custom heads, custom categories, visibility, and economy pricing.
  - title: Verified remote data
    details: Loads the public head catalog with artifact verification and cache fallback for reliable startup behavior.
  - title: Paper and Folia
    details: Designed for modern Paper-compatible servers with Folia support declared by the plugin.
  - title: Local customization
    details: Add More Heads, More Categories, favorites, player heads, and local metadata overrides without modifying public data.
  - title: Public API
    details: Integrate from other plugins through the API module instead of relying on internal implementation classes.
---

## What HeadDB provides

HeadDB is a Minecraft head database plugin for Paper and Folia servers. It gives players a polished GUI for browsing, searching, favoriting, pricing, and obtaining head items, while giving server owners fine-grained control over access and behavior.

This documentation covers the public plugin surface only: installation, commands, permissions, configuration, GUI usage, economy, updates, local data, API integration, and troubleshooting.

Private database build systems, private infrastructure, web frontend internals, and hosting choices are intentionally not documented here.

## Recommended path

1. Install HeadDB and start the server once.
2. Review generated files under `plugins/HeadDB/`.
3. Assign player and staff permissions.
4. Configure economy pricing if heads should cost money.
5. Run `/hdb status` and `/hdb verify`.
6. Publish `/hdb` to players.

Continue with [Installation](./installation.md) or [Getting Started](./getting-started.md).
