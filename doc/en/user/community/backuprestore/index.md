# Backup and restore

!!! warning
    Backup and Restore is a community module. It is usable, but does not have the same support guarantees as official extensions.

The Backup and Restore module saves and reloads a GeoServer **catalog and configuration** — workspaces, stores, layers, styles, layer groups, GeoWebCache tile layers and (optionally) security — to and from a portable archive. It does **not** copy the underlying data: file-based and database-backed stores keep serving from their original locations, and the archive captures how GeoServer is configured to reach them.

Core capabilities:

- **Full or filtered backups** — archive the whole catalog, or a workspace subset that is made self-contained through dependency-closure resolution.
- **Cross-instance migration** — id-preserving archives let a catalog move into another, already-populated GeoServer with stable identities and re-linkable tile layers.
- **Security migration** — opt-in merge or replace of users, groups and roles, including keystore re-encryption across master passwords.
- **Validation and dry-run** — validate an archive non-destructively before committing, and optionally abort on the first invalid object.
- **REST API and UI** — drive and monitor jobs from the web admin interface or from automation.

<div class="grid cards" markdown>

- [Installation](installation.md)
- [Usage via the web interface](usagegui.md)
- [Usage via the REST API](usagerest.md)
- [Options reference](options.md)
- [Use cases](usecases.md)
- [What changed between 2.x and 3.x](migration.md)
- [ImageMosaic indexer extension](extensions.md)

</div>

For the differences between the 2.x and 3.x modules — engine, defaults and behaviour changes — see [what changed between 2.x and 3.x](migration.md).
