# API

HeadDB exposes a public API module for other plugins that want to integrate with the database, models, identifiers, queries, and service interfaces.

## Modules

| Module | Purpose |
| --- | --- |
| `headdb-api` | Public API: models, IDs, queries, database interfaces, and service interfaces. |
| `headdb-core` | Core implementation used by HeadDB. External plugins normally do not depend on this. |
| `headdb-platforms/headdb-paper` | Paper/Folia plugin runtime. External plugins should treat this as the installed server plugin. |

Use `headdb-api` as a `provided` dependency when integrating from another plugin.

## Maven dependency

```xml
<dependency>
    <groupId>io.github.silentdevelopment.headdb</groupId>
    <artifactId>headdb-api</artifactId>
    <version>7.0.0-rc.2</version>
    <scope>provided</scope>
</dependency>
```

Use the version matching the HeadDB release you target.

## Head IDs

HeadDB uses typed IDs:

```text
123
custom:melon
player:f16df3ef-06b8-443e-9166-fba6689585b4
```

Example:

```java
import io.github.silentdevelopment.headdb.model.HeadId;

public final class HeadIdExample {

    public HeadId parse(String input) {
        return HeadId.parse(input);
    }
}
```

## Searching heads

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

## Finding by ID

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

## Integration guidance

Do:

- depend on `headdb-api`, not internal Paper classes;
- handle HeadDB being absent, disabled, loading, or failed;
- handle missing heads gracefully;
- store HeadDB IDs instead of raw textures where possible;
- keep server-thread work minimal.

Do not:

- read cache files directly;
- modify HeadDB storage while the server is running;
- assume GUI internals are stable API;
- assume a remote head ID will always exist forever.

The API module is the intended integration surface. Internal implementation packages can change between releases.
