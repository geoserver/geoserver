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

Changing the feed format means raising `schema_version` in the generator, the validator and the module together.
