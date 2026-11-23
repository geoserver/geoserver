"""recent_releases: every release of the last 365 days, per series, without CVE ids."""

import json
import sys
import unittest
from datetime import datetime, timedelta
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "scripts"))

import check_versions as cv  # noqa: E402
import validate_feed  # noqa: E402
from test_check_versions import UTC, raw_release, real_releases, releases_from  # noqa: E402

NOW = datetime(2026, 10, 8, 6, 0, 0, tzinfo=UTC)
IN_WINDOW = {"3.0.1", "3.0.0", "2.28.5", "2.28.4", "2.28.2", "2.28.1", "2.28.0", "2.27.6", "2.27.5", "2.27.4", "2.27.3"}


def lookup_flagging(*flagged):
    """A blog lookup that has a post for every release; the named versions are flagged."""

    def lookup(release):
        slug = release.version.replace(".", "-")
        return {
            "blog_url": f"https://geoserver.org/announcements/2026/01/01/geoserver-{slug}-released.html",
            "security_flagged": release.version in flagged,
        }

    return lookup


def entries(releases=None, lookup=None, previous=None, now=NOW, log=None):
    built = cv.build_series_entries(
        real_releases() if releases is None else releases,
        lookup or (lambda r: None),
        previous or [],
        now,
        (log if log is not None else []).append,
    )
    return {e["series"]: e for e in built}


def versions(entry):
    return [i["version"] for i in entry["recent_releases"]]


class WindowTest(unittest.TestCase):
    def test_every_release_in_the_window_is_listed_newest_first(self):
        built = entries()
        self.assertEqual(["3.0.1", "3.0.0"], versions(built["3.0.x"]))
        self.assertEqual(["2.28.5", "2.28.4", "2.28.2", "2.28.1", "2.28.0"], versions(built["2.28.x"]))
        self.assertEqual(["2.27.6", "2.27.5", "2.27.4", "2.27.3"], versions(built["2.27.x"]))
        for archive in ("2.26.x", "2.25.x", "2.24.x", "2.23.x", "2.22.x"):
            self.assertEqual([], built[archive]["recent_releases"], archive)

    def test_releases_without_any_signal_are_listed_too(self):
        item = entries()["2.28.x"]["recent_releases"][1]
        self.assertEqual(
            {"version": "2.28.4", "published_at": "2026-05-27T10:43:49Z", "blog_url": None,
             "security_flagged": False, "synchronized_release": False},
            item,
        )

    def test_the_window_is_365_days_inclusive(self):
        edge = NOW - timedelta(days=365)
        releases = releases_from(
            [
                raw_release("3.0.2", edge.strftime(cv.TIMESTAMP_FORMAT)),
                raw_release("3.0.1", (edge - timedelta(seconds=1)).strftime(cv.TIMESTAMP_FORMAT)),
                raw_release("3.0.3", (NOW - timedelta(days=1)).strftime(cv.TIMESTAMP_FORMAT)),
            ]
        )
        self.assertEqual(["3.0.3", "3.0.2"], versions(entries(releases)["3.0.x"]))

    def test_synchronized_is_decided_per_release(self):
        flags = {i["version"]: i["synchronized_release"] for e in entries().values() for i in e["recent_releases"]}
        self.assertEqual(
            {"3.0.1": True, "3.0.0": False, "2.28.5": True, "2.28.4": False, "2.28.2": False, "2.28.1": False,
             "2.28.0": False, "2.27.6": True, "2.27.5": False, "2.27.4": False, "2.27.3": False},
            flags,
        )

    def test_there_are_no_cve_ids_or_confirmed_flags(self):
        for entry in entries(lookup=lookup_flagging("3.0.1")).values():
            self.assertNotIn("cve_ids", entry)
            self.assertNotIn("blog_confirmed", entry)
            for item in entry["recent_releases"]:
                self.assertEqual(
                    ["version", "published_at", "blog_url", "security_flagged", "synchronized_release"], list(item)
                )


class SeriesLevelFieldsTest(unittest.TestCase):
    def test_they_describe_the_latest_release(self):
        built = entries(lookup=lookup_flagging("3.0.1"))
        self.assertTrue(built["3.0.x"]["security_flagged"])
        self.assertTrue(built["3.0.x"]["blog_url"].endswith("geoserver-3-0-1-released.html"))
        self.assertFalse(built["2.28.x"]["security_flagged"])
        self.assertTrue(built["2.28.x"]["synchronized_release"])

    def test_an_earlier_security_release_is_visible_in_the_list_only(self):
        built = entries(lookup=lookup_flagging("2.28.4"))
        self.assertFalse(built["2.28.x"]["security_flagged"])
        flags = {i["version"]: i["security_flagged"] for i in built["2.28.x"]["recent_releases"]}
        self.assertEqual(
            {"2.28.5": False, "2.28.4": True, "2.28.2": False, "2.28.1": False, "2.28.0": False}, flags
        )

    def test_a_latest_release_outside_the_window_has_no_blog_data_and_is_not_looked_up(self):
        looked_up = []

        def lookup(release):
            looked_up.append(release.version)
            return {"blog_url": "https://geoserver.org/x", "security_flagged": True}

        built = entries(lookup=lookup)
        self.assertIsNone(built["2.26.x"]["blog_url"])
        self.assertFalse(built["2.26.x"]["security_flagged"])
        self.assertEqual(IN_WINDOW, set(looked_up))

    def test_a_hotfix_published_after_a_newer_patch_does_not_confuse_the_latest(self):
        releases = releases_from(
            [
                raw_release("2.27.6", "2026-08-14T23:51:17Z"),
                raw_release("2.27.5", "2026-09-01T10:00:00Z"),
            ]
        )
        entry = entries(releases, lookup_flagging("2.27.6"))["2.27.x"]
        self.assertEqual(["2.27.5", "2.27.6"], versions(entry))
        self.assertEqual("2.27.6", entry["latest_version"])
        self.assertTrue(entry["security_flagged"])
        self.assertTrue(entry["blog_url"].endswith("geoserver-2-27-6-released.html"))


class ReuseTest(unittest.TestCase):
    def previous(self):
        built = cv.build_series_entries(real_releases(), lookup_flagging("2.28.4"), [], NOW)
        for entry in built:
            if entry["latest_version"] == "2.28.5":
                entry["blog_url"], entry["security_flagged"] = None, False
            for item in entry["recent_releases"]:
                if item["version"] == "2.28.5":
                    item["blog_url"], item["security_flagged"] = None, False
        return built

    def test_posts_already_found_are_not_looked_up_again_but_missing_ones_are(self):
        looked_up = []

        def lookup(release):
            looked_up.append(release.version)
            return None

        built = {e["series"]: e for e in cv.build_series_entries(real_releases(), lookup, self.previous(), NOW)}
        self.assertNotIn("2.28.4", looked_up)
        self.assertIn("2.28.5", looked_up)
        flags = {i["version"]: i["security_flagged"] for i in built["2.28.x"]["recent_releases"]}
        self.assertTrue(flags["2.28.4"])

    def test_a_release_that_has_not_got_a_post_yet_is_retried_until_it_does(self):
        calls = []

        def lookup(release):
            calls.append(release.version)
            return None

        first = cv.build_series_entries(real_releases(), lookup, [], NOW)
        calls.clear()
        cv.build_series_entries(real_releases(), lookup, first, NOW)
        self.assertEqual(IN_WINDOW, set(calls))


class LimitsTest(unittest.TestCase):
    def test_more_than_50_in_a_series_drops_the_oldest(self):
        releases = releases_from(
            [raw_release(f"3.0.{n}", (NOW - timedelta(days=n % 170, hours=n)).strftime(cv.TIMESTAMP_FORMAT)) for n in range(60)]
        )
        log = []
        built = {e["series"]: e for e in cv.build_series_entries(releases, lambda r: None, [], NOW, log.append)}
        self.assertEqual(50, len(built["3.0.x"]["recent_releases"]))
        self.assertTrue(any("more than 50 recent releases" in m for m in log))

    def test_more_than_300_in_total_drops_the_oldest(self):
        raws = []
        for series in range(1, 11):
            for n in range(40):
                raws.append(raw_release(f"{series}.0.{n}", (NOW - timedelta(days=n, hours=series)).strftime(cv.TIMESTAMP_FORMAT)))
        log = []
        feed = cv.build_feed(releases_from(raws), [], lambda r: None, None, NOW, log.append)
        total = sum(len(e["recent_releases"]) for e in feed["series"])
        self.assertEqual(300, total)
        self.assertTrue(any("more than 300 recent releases" in m for m in log))
        self.assertEqual([], validate_feed.validate_text(cv.serialize(feed, log.append).encode()))
        for entry in feed["series"]:
            newest = {i["version"] for i in entry["recent_releases"]}
            if entry["latest_version"] not in newest:
                self.assertIsNone(entry["blog_url"])
                self.assertFalse(entry["security_flagged"])


class BlogLookupTest(unittest.TestCase):
    def run_lookup(self, body):
        release = cv.parse_release(raw_release("3.0.1", "2026-08-14T22:44:22Z"))
        name = "2026-08-14-geoserver-3-0-1-released.md"

        def fake_get(url, accept, **kwargs):
            if url == cv.BLOG_POSTS_API:
                return json.dumps([{"name": name}])
            if url == cv.RAW_POST_BASE + name:
                return body
            raise AssertionError(url)

        with mock.patch.object(cv, "http_get", side_effect=fake_get):
            return cv.BlogLookup(NOW, lambda m: None)(release)

    def test_a_cve_in_the_post_flags_the_release_but_is_not_published(self):
        result = self.run_lookup("---\ncategories:\n- announcements\n---\nFixes CVE-2026-76904 in the filter")
        self.assertEqual({"blog_url", "security_flagged"}, set(result))
        self.assertTrue(result["security_flagged"])
        self.assertEqual("https://geoserver.org/announcements/2026/08/14/geoserver-3-0-1-released.html", result["blog_url"])
        self.assertNotIn("CVE-2026-76904", json.dumps(result))

    def test_the_other_flag_rules(self):
        self.assertTrue(self.run_lookup("---\ncategories:\n- announcements\n- Vulnerability\n---\nBody")["security_flagged"])
        self.assertTrue(self.run_lookup("---\ncategories:\n- announcements\n---\n## Security Considerations\nx")["security_flagged"])
        plain = self.run_lookup("---\ncategories:\n- announcements\n---\nJust bug fixes")
        self.assertFalse(plain["security_flagged"])
        self.assertIsNotNone(plain["blog_url"])

    def test_no_post_means_no_blog_data(self):
        release = cv.parse_release(raw_release("3.0.1", "2026-08-14T22:44:22Z"))
        with mock.patch.object(cv, "http_get", return_value="[]"):
            self.assertIsNone(cv.BlogLookup(NOW, lambda m: None)(release))


class ValidatorTest(unittest.TestCase):
    def base(self):
        feed = cv.build_feed(real_releases(), [], lookup_flagging("3.0.1", "2.28.4"), None, NOW, lambda m: None)
        return json.loads(cv.serialize(feed))

    def problems(self, document):
        return validate_feed.validate_text(cv.serialize(document).encode("utf-8"))

    def assert_rejected(self, document, fragment):
        found = self.problems(document)
        self.assertTrue(any(fragment in p for p in found), f"{fragment!r} not in {found}")

    def test_the_generated_feed_is_valid(self):
        self.assertEqual([], self.problems(self.base()))

    def series(self, document, name):
        return next(e for e in document["series"] if e["series"] == name)

    def test_structure_problems(self):
        d = self.base()
        self.series(d, "3.0.x")["recent_releases"][0]["extra"] = 1
        self.assert_rejected(d, "keys")

        d = self.base()
        self.series(d, "3.0.x")["recent_releases"][0]["security_flagged"] = "yes"
        self.assert_rejected(d, "boolean")

        d = self.base()
        self.series(d, "3.0.x")["recent_releases"][0]["blog_url"] = "https://evil.example/x"
        self.assert_rejected(d, "URL")

        d = self.base()
        self.series(d, "3.0.x")["recent_releases"][0]["version"] = "3.0"
        self.assert_rejected(d, "version")

        d = self.base()
        self.series(d, "3.0.x")["recent_releases"][0]["version"] = "2.28.5"
        self.assert_rejected(d, "not in the series")

        d = self.base()
        self.series(d, "3.0.x")["recent_releases"] = "text"
        self.assert_rejected(d, "recent_releases is not a list")

    def test_ordering_and_duplicates(self):
        d = self.base()
        entry = self.series(d, "2.28.x")
        entry["recent_releases"].reverse()
        self.assert_rejected(d, "newest first")

        d = self.base()
        entry = self.series(d, "2.28.x")
        entry["recent_releases"].append(dict(entry["recent_releases"][0]))
        self.assert_rejected(d, "duplicate")

    def test_consistency_with_the_series_level_fields(self):
        d = self.base()
        self.series(d, "3.0.x")["security_flagged"] = False
        self.assert_rejected(d, "differs from the latest release")

        d = self.base()
        self.series(d, "3.0.x")["blog_url"] = None
        self.assert_rejected(d, "differs from the latest release")

        d = self.base()
        entry = self.series(d, "2.26.x")
        entry["security_flagged"] = True
        self.assert_rejected(d, "not in recent_releases")

        d = self.base()
        self.series(d, "2.26.x")["blog_url"] = "https://geoserver.org/x"
        self.assert_rejected(d, "not in recent_releases")

    def test_limits(self):
        d = self.base()
        entry = self.series(d, "3.0.x")
        item = entry["recent_releases"][0]
        entry["recent_releases"] = [dict(item, version=f"3.0.{n}", published_at=f"2026-08-{14 - n % 10:02d}T00:00:0{n % 10}Z") for n in range(51)]
        self.assert_rejected(d, "more than 50")

        d = self.base()
        template = self.series(d, "3.0.x")["recent_releases"][0]
        d["series"] = [
            dict(self.series(d, "3.0.x"), series=f"{n}.0.x", latest_version=f"{n}.0.0", phase="stable" if n == 1 else "archive",
                 release_url=f"https://github.com/geoserver/geoserver/releases/tag/{n}.0.0",
                 recent_releases=[dict(template, version=f"{n}.0.{k}", published_at=f"2026-08-14T00:00:{59 - k % 50:02d}Z") for k in range(40)])
            for n in range(1, 11)
        ]
        self.assert_rejected(d, "more than 300 recent releases in total")

    def test_removed_fields_are_rejected(self):
        for key, value in (("cve_ids", []), ("blog_confirmed", True)):
            d = self.base()
            self.series(d, "3.0.x")[key] = value
            self.assert_rejected(d, "keys")


if __name__ == "__main__":
    unittest.main()
