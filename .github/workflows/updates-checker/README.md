# Updates Checker feed

Docs: https://docs.geoserver.org/main/en/user/community/updates-checker/

Generates the version feed read by the `updates-checker` community module: the latest patch of each release
series, its lifecycle phase, and the published security advisories, built from public GitHub data and the
[geoserver.org](https://geoserver.org/blog/) announcements.

The feed is `versions.json` on the `updates-checker-v1` branch, published at
`https://raw.githubusercontent.com/geoserver/geoserver/updates-checker-v1/versions.json`.
That branch holds only `versions.json`; the generator and its tests are here.

| Path | Purpose |
|---|---|
| `../updates-checker.yml` | Regenerates the feed every 15 minutes, validates it and commits it to the branch if it changed |
| `../updates-checker-tests.yml` | Runs the tests for pull requests that change this folder |
| `scripts/check_versions.py` | The generator |
| `scripts/validate_feed.py` | An independent validator that imports nothing from the generator |
| `tests/` | Unit tests, standard library only |

Run the tests from the repository root:

```bash
python3 -m unittest discover -s .github/workflows/updates-checker/tests
```

To generate a feed by hand, run the generator from a scratch directory. It writes `./versions.json`, or the file
named by the `VERSIONS_JSON` environment variable, and keeps the last good data from that file if GitHub cannot
be read:

```bash
VERSIONS_JSON=/tmp/versions.json python3 .github/workflows/updates-checker/scripts/check_versions.py
python3 .github/workflows/updates-checker/scripts/validate_feed.py /tmp/versions.json
```

A version counts as released once it is tagged, even before GitHub has a release record for it. A tag with no release
record is dated by its commit, once: the feed remembers the date. A run makes two tag-list requests.

The top-level `main` element, `{"series": "3.1.x", "version": "3.1.0-SNAPSHOT"}`, is the version of the development
branch, read from `src/pom.xml` (the `<revision>` property, or the project `<version>` while it is still literal). The feed
does not list that series otherwise, and the documentation and Docker workflows need it. If the pom cannot be read the
previous value is kept.

Adding an optional element, as `main` was, does not need a new `schema_version`, because the module ignores elements
it does not know. Renaming, removing or changing the meaning of an element means raising `schema_version` in the
generator, the validator and the module together.
