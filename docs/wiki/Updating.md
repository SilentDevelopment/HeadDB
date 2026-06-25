# Updating

HeadDB supports manual updates and an optional built-in update checker.

## Release assets

Public releases can include:

```text
HeadDB-<version>.jar
HeadDB-<version>.jar.sha256
```

Use the checksum file to verify the JAR when available:

```bash
sha256sum -c HeadDB-<version>.jar.sha256
```

Checksums help detect corrupted or incomplete downloads. They are not a replacement for artifact signing, but they are useful release hygiene.

## Manual update

Recommended process:

1. Stop the server.
2. Back up `plugins/HeadDB/`.
3. Download the new HeadDB JAR.
4. Verify checksum if provided.
5. Replace the old JAR.
6. Start the server.
7. Run `/hdb version` and `/hdb status`.

## Update checker

```yaml
update-checker:
  enabled: true
  check-on-startup: true
  notify-console: true
  notify-admins: true
  include-prereleases: true
  include-builds: true
```

Permission:

```text
headdb.admin.update
```

Useful commands:

```text
/hdb version
/hdb update
```

## Automatic installation

```yaml
auto-updater:
  install-updates: false
```

Recommended production default is `false`.

When automatic installation is enabled, HeadDB can retrieve a newer plugin JAR, validate that it is a HeadDB plugin JAR, keep a backup where possible, and place the new JAR where the server can load it on the next restart.

HeadDB does not restart the server automatically. Restart the server to load a downloaded or staged update.

## Messages

When an update is available:

```text
New version available: <version> | Download: <link>
```

When a new version is downloaded:

```text
Downloaded new version: <version> | Restart the server to load it.
```

## Troubleshooting

| Problem | Likely cause | Fix |
| --- | --- | --- |
| Update check fails | Network issue, release service unavailable, or rate limiting. | Try later and check console diagnostics. |
| Downloaded but not loaded | Server restart required. | Restart the server. |
| No prerelease shown | Prerelease checks disabled. | Enable `include-prereleases` if desired. |
| No build release shown | Build checks disabled. | Enable `include-builds` if desired. |
| JAR is rejected | Metadata validation failed. | Use the official HeadDB release JAR. |
