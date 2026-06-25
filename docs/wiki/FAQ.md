# FAQ

## What server software does HeadDB support?

HeadDB targets modern Paper and Folia servers.

## What Java version do I need?

The Paper/Folia plugin requires Java 25. The API and core modules target Java 21.

## Does HeadDB require Vault?

No. Vault is optional and is required only when economy support is enabled.

## Does HeadDB work without internet access?

HeadDB can load from the last verified local cache if one exists. A fresh installation needs network access to load the public database for the first time.

## What happens if remote refresh fails?

If a verified cache exists, HeadDB can continue using cached data. If no cache exists, the database may be unavailable until a successful refresh.

Useful commands:

```text
/hdb status
/hdb verify
/hdb refresh
```

## Can I change the public database source?

Normal servers should leave remote source settings at their defaults. The plugin does not require server owners to manage database source internals.

## Can I hide specific heads?

Yes. Staff can use local visibility overrides in Admin Mode or edit commands when they have the required permissions.

## Do local overrides change the public database?

No. Local overrides only affect your server.

## What are More Heads?

More Heads are server-local custom heads created by staff.

## What are More Categories?

More Categories are server-local custom categories that let you group heads in ways specific to your server.

## Are Favorites global?

No. Favorites are stored per player.

## Why does the update command say to restart?

After a new JAR is downloaded or staged, the server must restart to load the new version.

## Should I enable automatic update installation?

For most production servers, keep automatic installation disabled and use notifications/manual updates.

Recommended:

```yaml
update-checker:
  enabled: true

auto-updater:
  install-updates: false
```

## What should I back up?

Back up:

```text
plugins/HeadDB/config.yml
plugins/HeadDB/economy.yml
plugins/HeadDB/gui.yml
plugins/HeadDB/messages/
plugins/HeadDB/storage/headdb.db
```

The most important local-data file is `plugins/HeadDB/storage/headdb.db`.

## Can I delete the cache?

Usually yes, but HeadDB will need to refresh the public database again. Do not delete storage unless you intentionally want to remove local data.

## Can another plugin use HeadDB?

Yes. Other plugins should integrate through the `headdb-api` module and avoid depending on internal implementation classes.
