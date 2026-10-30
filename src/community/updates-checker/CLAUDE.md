# Check for Updates module: design constraints

The module exists to reach administrators of **old GeoServer 2.x servers** and get them onto a supported release. Most
rules below follow from that: the code must run where those servers are, and a backport must stay cheap, because
every feature is written once on `main` and copied to `3.0.x`, `2.28.x` and `2.27.x`.

## One source for every branch

The module source is the same on all four branches. Only the edits that `backport-checks.sh` makes may differ:

| Edit | Branches | Why |
|---|---|---|
| Parent version in `pom.xml` | all | Every module has it |
| `wicket-tester` test dependency: none on 2.28.x, `junit-jupiter` instead on 2.27.x | 2.x | Wicket 9 keeps the tester in `wicket-core` |
| `javax.servlet.Filter` instead of `jakarta.servlet.Filter` in `CheckControllerTest` | 2.x | GeoServer 3 moved to Jakarta; `getFilters()` returns that type |
| No `category` and `contact` properties on the module status bean | 2.x | `ModuleStatusImpl` has them from 3.0 only |
| `"PMD.MissingOverride"` dropped from the suppression on `HomePageContentProvider.getOrder()` | 2.x | The method overrides nothing there, and the 2.28.x PMD reports an unneeded suppression |
| User guide page copied from `doc/index.rst` | 2.x | 2.x documentation is reStructuredText |

Run `./backport-checks.sh 2.27.x` (add `QA=1` for the QA checks) before opening a pull request on `main`: it builds and
tests this source on 2.27.x, the strictest branch (Java 11, Error Prone). If a change needs a new edit, the edit goes
into the script and into this table, or the change is rewritten so it needs none. Each new edit costs every future
backport.

### Java 11, GeoServer 2.23

- `<release>11</release>`, so the jar loads on Java 11 and the compiler rejects newer API. No records, switch
  expressions, text blocks, `Stream.toList()` or pattern matching for `instanceof`. Data classes are final classes with
  getters.
- Only GeoServer API that exists from 2.23. Where 3.x adds something worth using, use it so the same source still
  compiles on 2.x: `HomePageContentProvider.getOrder()` has no `@Override` (with `@SuppressWarnings` for Error Prone and
  PMD), and the menu category is picked in `applicationContext.xml` by bean name.
- No reflection to bridge versions. Use XML, an annotation-free override, or a `backport-checks.sh` edit instead.
- No servlet API in main code: `javax` on 2.x, `jakarta` on 3.x. `CheckController` reads the body as an `InputStream`
  for this reason, and because GeoServer's message converters read any JSON body as an XStream type.
- Only Wicket API common to Wicket 9 (2.x) and 10 (3.x). Lambda models (`() -> ...`) work on both.
- JSON is read with `org.json`, which GeoWebCache brings to every GeoServer from 2.23. `com.fasterxml` Jackson is
  missing from 3.x and `tools.jackson` from 2.x.

### Web pages

- No inline scripts or `style` attributes: GeoServer 3's content security policy drops them. Script and CSS go in
  `notifications.js`, `notifications.css` and `UpdatesCheckerPage.css`, rendered as header items.
- CSS works with both themes: 3.x classes such as `button` and `w-lg` get a fallback in `:where()`, which has no
  specificity, so the 3.x theme wins where it applies.

## Documentation

Change `doc/en/user/community/updates-checker/index.md` (Markdown, `main` and `3.0.x`) and `doc/index.rst` (the 2.x copy)
in the same commit. See `doc/README.md`.

## The feed is untrusted

The feed URL can be overridden and the feed is fetched over the internet, so treat every byte as hostile:

- At most 1 MiB, 5 second timeouts and deadline, no redirects, only HTTP 200 and 304.
- Strings are truncated to 2000 characters. Links must be `https` to geoserver.org or the `geoserver` organisation on
  GitHub or jsDelivr. Advisory links are built from the validated id, never taken from the feed.
- `Feed.parseJson` refuses a NUL character (org.json reads it as the end of input) and a run of 64 digits (org.json
  takes about 40 seconds over a 1 MiB number). Keep both.
- Feed text is escaped wherever it is shown. Messages may carry only `<i>` and `<b>`, and arguments are always escaped.
- Text written to the log goes through `Messages.forLog`, so a feed cannot forge log lines.
- Never downgrade an advisory: an unknown severity is HIGH, and a range that cannot be read is shown for manual review,
  never treated as "not affected".
- A new feed key must be optional: installed modules ignore unknown keys. A change old modules would misread needs a
  new `schema_version`, which they report as "update the module".

## Privacy

The module only downloads a static file. It sends nothing about the server: no version, id, query parameter or header
that could identify it. Keep it that way; the user guide and the settings page promise it.

## Contracts outside the code

Changing any of these affects administrators or their scripts:

- Browser storage keys `updates-checker|TYPE|discriminator`, values `dismissed` or `until:<epoch ms>`. A new format
  shows every dismissed notification again. `Notification` computes the keys; `notifications.js` knows no types.
- REST: `POST /rest/updates-checker/check`, its JSON fields and the `Status.UpdateState` names, which also appear in
  log lines.
- Properties `UPDATES_CHECKER_FEED_URL` and `UPDATES_CHECKER_SUPPRESSED_CVES`, the logger name
  `org.geoserver.updates-checker` and the data directory file `updates-checker/updates-checker.xml`.
- Message keys under `UpdatesChecker.` in `GeoServerApplication.properties`, which translations use.

## Code style

- Class names inside `org.geoserver.updateschecker` carry no `UpdatesChecker` prefix, except `UpdatesCheckerService` and
  `UpdatesCheckerPage` (Wicket finds the page's `UpdatesCheckerPage.*` keys by class name).
- Javadoc states the contract. Add a one-line reason only where the code would surprise a reader. No history ("was",
  "used to"), no restating the code.
- The REST controller and the Check Now button stay; checks run at startup, on the first view of an admin session, and
  on demand. A scheduled daily check was proposed and is on hold.

## Tests

- `reuseForks=false` in `pom.xml`: a GeoServer test application resets the logging when it stops (seen on 2.27.x),
  which breaks the log capture of later test classes in the same JVM.
- Run the QA build (`-Dqa`) on `main` and through `QA=1 ./backport-checks.sh 2.27.x` before a pull request.
