# HeadDB

HeadDB is a Paper/Folia plugin for browsing, searching, and giving Minecraft head items from a public remote database.

## Requirements

- Java 25 for the Paper plugin.
- Paper/Folia.
- Network access.
- Vault and an economy provider if economy support is enabled.

The API and core modules target Java 21. The Paper plugin targets Java 25.

## Features

- Public remote head database.
- Verified remote downloads using SHA-256.
- Local cache with startup cache loading.
- Search and category browsing.
- Player heads.
- Local custom heads.
- Favorites.
- Custom categories.
- Admin editing tools.
- Configurable GUI layout and messages.
- Optional Vault economy integration.
- Paper/Folia support.
- API/core modules for external integrations.

## Modules

```text
headdb-api
  Public API: models, identifiers, queries, and database interfaces.

headdb-core
  Database implementation: remote loading, verification, parsing, cache, and refresh.

headdb-platforms/headdb-paper
  Paper/Folia plugin: commands, GUI, local storage, messages, economy, and item generation.
```

## Plugin usage

Documentation: https://silentdevelopment.github.io/HeadDB/

Build or download the Paper plugin jar and place it in your server `plugins` folder.

Start the server once to generate config files.

```text
plugins/HeadDB/
  config.yml
  gui.yml
  economy.yml
  messages/
    en-US.yml
```

Basic player usage:

```text
/hdb
```

Useful admin commands:

```text
/hdb status
/hdb verify
/hdb refresh
/hdb reload
/hdb debug
```

The main GUI provides browsing, searching, favorites, player heads, custom heads, custom categories, and settings.

## API usage

Add the API module as a dependency if you want to integrate with HeadDB from another plugin.

```xml
<dependency>
    <groupId>io.github.silentdevelopment.headdb</groupId>
    <artifactId>headdb-api</artifactId>
    <version>7.0.0-rc.3</version>
    <scope>provided</scope>
</dependency>
```

Example: search the database.

```java
import io.github.silentdevelopment.headdb.database.HeadDatabase;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.query.HeadQuery;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;

public final class HeadSearchExample {

    private final HeadDatabase database;

    public HeadSearchExample(HeadDatabase database) {
        this.database = database;
    }

    public void search() {
        HeadQuery query = HeadQuery.builder()
                .query("stone")
                .page(1)
                .pageSize(28)
                .build();

        HeadQueryResult result = database.search(query);

        for (Head head : result.heads()) {
            System.out.println(head.id().display() + " - " + head.name());
        }
    }
}
```

Example: find a head by ID.

```java
import io.github.silentdevelopment.headdb.database.HeadDatabase;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;

import java.util.Optional;

public final class HeadLookupExample {

    private final HeadDatabase database;

    public HeadLookupExample(HeadDatabase database) {
        this.database = database;
    }

    public Optional<Head> find(String input) {
        HeadId id = HeadId.parse(input);
        return database.find(id);
    }
}
```

Remote heads can usually be referenced by their visible ID. Custom and player heads use typed IDs.

```text
123
custom:melon
player:f16df3ef-06b8-443e-9166-fba6689585b4
```

## Build

Verify API/core with Java 21:

```powershell
mvn -B -ntp -pl headdb-api,headdb-core -am verify
```

Package the Paper plugin with Java 25:

```powershell
mvn -B -ntp -pl headdb-platforms/headdb-paper -am clean package
```

The plugin jar is produced at:

```text
headdb-platforms/headdb-paper/target/HeadDB.jar
```

Build the documentation site locally:

```powershell
cd docs
npm install
npm run docs:build
```

## License

HeadDB is a multi-license project.

```text
headdb-api
  Apache-2.0

headdb-core
  Apache-2.0

headdb-platforms/headdb-paper
  GPL-3.0-or-later
```

Full license texts are stored in:

```text
LICENSES/Apache-2.0.txt
LICENSES/GPL-3.0-or-later.txt
```

Unless a file states otherwise, files are licensed under the license of the module they belong to.
