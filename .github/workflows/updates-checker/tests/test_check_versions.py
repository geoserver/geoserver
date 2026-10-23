import json
import sys
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "scripts"))

import check_versions as cv  # noqa: E402

FIXTURES = Path(__file__).resolve().parent / "fixtures"
UTC = timezone.utc
AS_OF = datetime(2026, 10, 8, 6, 0, 0, tzinfo=UTC)


def raw_release(tag, published_at):
    return {
        "tag_name": tag,
        "draft": False,
        "published_at": published_at,
        "html_url": f"https://github.com/geoserver/geoserver/releases/tag/{tag}",
    }


def releases_from(raws):
    return [r for r in (cv.parse_release(raw) for raw in raws) if r]


def real_releases(as_of=None):
    raws = json.loads((FIXTURES / "releases.json").read_text())
    if as_of:
        raws = [r for r in raws if r["published_at"] <= as_of]
    return releases_from(raws)


def phases_by_name(releases):
    return {cv.series_name(s): p for s, p in cv.infer_phases(releases).items()}


def no_blog(release):
    return None


class ParseReleaseTest(unittest.TestCase):
    def test_accepts_proper_releases(self):
        self.assertEqual("2.27.6", cv.parse_release(raw_release("2.27.6", "2026-08-14T23:51:17Z")).version)
        self.assertEqual("3.0.1", cv.parse_release(raw_release("v3.0.1", "2026-08-14T22:44:22Z")).version)

    def test_rejects_everything_else(self):
        for tag in ("3.0-RC", "2.25-RC", "3.0.0-M1", "2.28.0-beta", "3.1.0-SNAPSHOT", "2.7.1.1", "latest"):
            self.assertIsNone(cv.parse_release(raw_release(tag, "2026-01-01T00:00:00Z")), tag)

    def test_rejects_drafts_and_unpublished(self):
        draft = raw_release("2.27.6", "2026-08-14T23:51:17Z")
        draft["draft"] = True
        self.assertIsNone(cv.parse_release(draft))
        self.assertIsNone(cv.parse_release(raw_release("2.27.6", None)))


class ClusterTest(unittest.TestCase):
    def test_window_crosses_midnight(self):
        releases = releases_from(
            [raw_release("3.0.1", "2026-08-14T23:50:00Z"), raw_release("2.28.5", "2026-08-15T01:10:00Z")]
        )
        clusters = cv.cluster_releases(releases)
        self.assertEqual(1, len(clusters))
        self.assertTrue(cv.is_coordinated(clusters[0]))

    def test_window_is_anchored_on_the_earliest_release(self):
        releases = releases_from(
            [
                raw_release("3.0.1", "2026-08-14T00:00:00Z"),
                raw_release("2.28.5", "2026-08-14T23:59:00Z"),
                raw_release("2.27.6", "2026-08-15T00:01:00Z"),
            ]
        )
        clusters = cv.cluster_releases(releases)
        self.assertEqual([2, 1], [len(c) for c in clusters])

    def test_same_series_twice_is_not_coordinated(self):
        releases = releases_from(
            [raw_release("3.0.1", "2026-08-14T10:00:00Z"), raw_release("3.0.2", "2026-08-14T12:00:00Z")]
        )
        self.assertFalse(cv.is_coordinated(cv.cluster_releases(releases)[0]))


class PhaseTest(unittest.TestCase):
    def test_real_history_today_has_three_maintained_series(self):
        phases = phases_by_name(real_releases())
        self.assertEqual("stable", phases["3.0.x"])
        self.assertEqual("maintenance", phases["2.28.x"])
        self.assertEqual("maintenance", phases["2.27.x"])
        for series in ("2.26.x", "2.25.x", "2.24.x", "2.23.x", "2.22.x"):
            self.assertEqual("archive", phases[series], series)

    def test_new_series_released_alone_displaces_the_oldest(self):
        # 3.0.0 came out on its own on 2026-06-11; the last coordinated event was
        # 2025-05-14 (2.27, 2.26, 2.25).
        phases = phases_by_name(real_releases(as_of="2026-06-12T00:00:00Z"))
        self.assertEqual("stable", phases["3.0.x"])
        self.assertEqual("maintenance", phases["2.28.x"])
        self.assertEqual("maintenance", phases["2.27.x"])
        self.assertEqual("archive", phases["2.26.x"])
        self.assertEqual("archive", phases["2.25.x"])

    def test_before_the_next_series(self):
        phases = phases_by_name(real_releases(as_of="2025-09-03T00:00:00Z"))
        self.assertEqual("stable", phases["2.27.x"])
        self.assertEqual("maintenance", phases["2.26.x"])
        self.assertEqual("maintenance", phases["2.25.x"])
        self.assertEqual("archive", phases["2.24.x"])

    def test_extended_series_drops_out_when_no_longer_released_with_the_others(self):
        releases = real_releases() + releases_from(
            [raw_release("3.0.3", "2026-12-15T10:00:00Z"), raw_release("2.28.7", "2026-12-15T11:00:00Z")]
        )
        phases = phases_by_name(releases)
        self.assertEqual("stable", phases["3.0.x"])
        self.assertEqual("maintenance", phases["2.28.x"])
        self.assertEqual("archive", phases["2.27.x"])

    def test_new_series_after_a_two_series_event(self):
        releases = releases_from(
            [
                raw_release("3.0.2", "2026-10-19T09:00:00Z"),
                raw_release("2.28.6", "2026-10-19T10:00:00Z"),
                raw_release("3.1.0", "2027-03-10T10:00:00Z"),
            ]
        )
        phases = phases_by_name(releases)
        self.assertEqual({"3.1.x": "stable", "3.0.x": "maintenance", "2.28.x": "archive"}, phases)

    def test_fallback_without_any_coordinated_event(self):
        releases = releases_from(
            [
                raw_release("3.0.1", "2026-01-01T00:00:00Z"),
                raw_release("2.28.1", "2026-03-01T00:00:00Z"),
                raw_release("2.27.1", "2026-05-01T00:00:00Z"),
            ]
        )
        self.assertEqual(
            {"3.0.x": "stable", "2.28.x": "maintenance", "2.27.x": "archive"}, phases_by_name(releases)
        )

    def test_numeric_not_lexicographic(self):
        releases = releases_from(
            [
                raw_release("2.9.1", "2026-01-01T00:00:00Z"),
                raw_release("2.10.0", "2026-01-01T01:00:00Z"),
            ]
        )
        phases = phases_by_name(releases)
        self.assertEqual("stable", phases["2.10.x"])

    def test_empty(self):
        self.assertEqual({}, cv.infer_phases([]))


class SynchronizedTest(unittest.TestCase):
    def test_flag_follows_the_latest_release_of_each_series(self):
        entries = {
            e["series"]: e for e in cv.build_series_entries(real_releases(), no_blog, [], AS_OF)
        }
        self.assertTrue(entries["3.0.x"]["synchronized_release"])
        self.assertTrue(entries["2.27.x"]["synchronized_release"])
        self.assertTrue(entries["2.25.x"]["synchronized_release"])  # 2.25.7, coordinated in 2025-05
        self.assertFalse(entries["2.26.x"]["synchronized_release"])  # 2.26.4 released alone


class SeriesEntriesTest(unittest.TestCase):
    def test_order_and_latest(self):
        entries = cv.build_series_entries(real_releases(), no_blog, [], AS_OF)
        self.assertEqual(
            ["3.0.x", "2.28.x", "2.27.x", "2.26.x", "2.25.x", "2.24.x", "2.23.x", "2.22.x"],
            [e["series"] for e in entries],
        )
        latest = {e["series"]: e["latest_version"] for e in entries}
        self.assertEqual("3.0.1", latest["3.0.x"])
        self.assertEqual("2.25.7", latest["2.25.x"])

    def test_blog_data_already_found_is_reused_without_a_lookup(self):
        previous = [
            {
                "series": "3.0.x",
                "latest_version": "3.0.1",
                "blog_url": "https://geoserver.org/x",
                "security_flagged": True,
                "recent_releases": [],
            }
        ]
        calls = []

        def lookup(release):
            calls.append(release.version)
            return None

        entries = {e["series"]: e for e in cv.build_series_entries(real_releases(), lookup, previous, AS_OF)}
        self.assertEqual("https://geoserver.org/x", entries["3.0.x"]["blog_url"])
        self.assertTrue(entries["3.0.x"]["security_flagged"])
        self.assertNotIn("3.0.1", calls)
        self.assertIn("2.28.5", calls)
        self.assertIsNone(entries["2.28.x"]["blog_url"])


class PatchedVersionsTest(unittest.TestCase):
    def test_one_entry_per_package_and_series(self):
        # Shape of GHSA-6jj6-gm7p-fcvv: several packages, one fixed version per series.
        patched = []
        for fixed in ("2.24.4", "2.25.2", "2.23.6", "2.22.6"):
            patched.extend([fixed] * 3)
        self.assertEqual(
            {"2.25.x": "2.25.2", "2.24.x": "2.24.4", "2.23.x": "2.23.6", "2.22.x": "2.22.6"},
            cv.derive_patched_versions(patched),
        )

    def test_lowest_per_series_wins(self):
        self.assertEqual({"2.28.x": "2.28.5"}, cv.derive_patched_versions(["2.28.6", "2.28.5"]))

    def test_several_versions_in_one_string(self):
        self.assertEqual(
            {"3.0.x": "3.0.1", "2.28.x": "2.28.5"},
            cv.derive_patched_versions(["3.0.1, 2.28.5"]),
        )

    def test_unusable_values_are_ignored(self):
        for value in ("", "latest", "2.7.1.1", "3.0"):
            self.assertEqual({}, cv.derive_patched_versions([value]), value)
        self.assertEqual({}, cv.derive_patched_versions([]))


def raw_advisory(ghsa="GHSA-abcd-efgh-ijkl", **overrides):
    advisory = {
        "ghsa_id": ghsa,
        "cve_id": "CVE-2026-00000",
        "summary": "Example",
        "severity": "high",
        "published_at": "2026-01-01T00:00:00Z",
        "vulnerabilities": [{"vulnerable_version_range": "< 2.28.5", "patched_versions": "2.28.5"}],
    }
    advisory.update(overrides)
    return advisory


class AdvisoryTest(unittest.TestCase):
    def test_transform(self):
        raw = raw_advisory(
            "GHSA-jvpx-6qxg-whgc",
            cve_id=None,
            severity="critical",
            published_at="2026-08-24T15:50:11Z",
            vulnerabilities=[
                {"vulnerable_version_range": "3.0.0", "patched_versions": "3.0.1"},
                {"vulnerable_version_range": "3.0.0", "patched_versions": "3.0.1"},
                {"vulnerable_version_range": ">=2.28.0", "patched_versions": "2.28.5"},
            ],
        )
        advisory = cv.transform_advisory(raw)
        self.assertEqual("critical", advisory["severity"])
        self.assertIsNone(advisory["cve_id"])
        self.assertEqual(["3.0.0", ">=2.28.0"], advisory["vulnerable_versions"])
        self.assertEqual({"3.0.x": "3.0.1", "2.28.x": "2.28.5"}, advisory["patched_versions"])
        self.assertEqual(
            ["ghsa_id", "cve_id", "summary", "severity", "published_at", "vulnerable_versions", "patched_versions"],
            list(advisory),
        )

    def test_only_allow_listed_fields_are_copied(self):
        raw = raw_advisory(
            credits=[{"user": {"login": "someone", "email": "someone@example.com"}}],
            cvss={"vector_string": "CVSS:3.1/AV:N"},
            references=["https://evil.example"],
            description="long text",
        )
        raw["vulnerabilities"][0]["package"] = {"name": "gs-wms"}
        advisory = cv.transform_advisory(raw)
        self.assertNotIn("credits", advisory)
        self.assertNotIn("someone", json.dumps(advisory))
        self.assertNotIn("evil.example", json.dumps(advisory))

    def test_missing_vulnerabilities_is_an_empty_list(self):
        advisory = cv.transform_advisory(raw_advisory(vulnerabilities=None))
        self.assertEqual([], advisory["vulnerable_versions"])
        self.assertEqual({}, advisory["patched_versions"])

    def test_advisories_that_are_only_commit_hashes_are_left_out_and_logged(self):
        logged = []
        advisories = cv.transform_advisories(
            [
                raw_advisory("GHSA-aaaa-aaaa-aaaa", vulnerabilities=[{"vulnerable_version_range": "df11a650c650ff895977c5440427c239671ee649"}]),
                raw_advisory("GHSA-bbbb-bbbb-bbbb", vulnerabilities=[{"vulnerable_version_range": "3.0.0"}]),
                raw_advisory("GHSA-cccc-cccc-cccc", vulnerabilities=[{"vulnerable_version_range": "<= 2.26.3"}]),
                raw_advisory("GHSA-dddd-dddd-dddd", vulnerabilities=[{"vulnerable_version_range": "df11a650c650ff895977c5440427c239671ee649"}, {"vulnerable_version_range": ">= 2.28.0"}]),
                raw_advisory("GHSA-eeee-eeee-eeee", vulnerabilities=[]),
                raw_advisory("GHSA-ffff-ffff-ffff", vulnerabilities=[{"vulnerable_version_range": "rm -rf"}]),
            ],
            log=logged.append,
        )
        self.assertEqual(
            ["GHSA-bbbb-bbbb-bbbb", "GHSA-cccc-cccc-cccc", "GHSA-dddd-dddd-dddd", "GHSA-eeee-eeee-eeee", "GHSA-ffff-ffff-ffff"],
            sorted(a["ghsa_id"] for a in advisories),
        )
        self.assertEqual(["excluded GHSA-aaaa-aaaa-aaaa: its only ranges are commit hashes"], logged)

    def test_newest_first(self):
        advisories = cv.transform_advisories(
            [
                raw_advisory("GHSA-aaaa-aaaa-aaaa", published_at="2025-01-01T00:00:00Z"),
                raw_advisory("GHSA-bbbb-bbbb-bbbb", published_at="2026-01-01T00:00:00Z"),
            ]
        )
        self.assertEqual(["GHSA-bbbb-bbbb-bbbb", "GHSA-aaaa-aaaa-aaaa"], [a["ghsa_id"] for a in advisories])


class FeedTest(unittest.TestCase):
    NOW = datetime(2026, 10, 8, 6, 0, 0, tzinfo=UTC)

    def test_shape(self):
        feed = cv.build_feed(real_releases(), [], no_blog, None, self.NOW)
        self.assertEqual(1, feed["schema_version"])
        self.assertEqual("2026-10-08T06:00:00Z", feed["generated"])
        self.assertEqual(
            {
                "series", "phase", "latest_version", "published_at", "release_url", "blog_url",
                "security_flagged", "synchronized_release", "recent_releases",
            },
            set(feed["series"][0]),
        )

    def test_source_note_comes_first(self):
        feed = cv.build_feed(real_releases(), [], no_blog, None, self.NOW)
        self.assertEqual(["source_note", "schema_version", "generated", "series", "advisories"], list(feed))
        self.assertEqual(cv.SOURCE_NOTE, feed["source_note"])
        self.assertLessEqual(len(cv.SOURCE_NOTE), cv.MAX_NOTE)
        for host in ("raw.githubusercontent.com", "cdn.jsdelivr.net"):
            self.assertIn(host, cv.SOURCE_NOTE)
        self.assertIn("public IP address", cv.SOURCE_NOTE)

    def test_generated_only_moves_when_content_changes(self):
        first = cv.build_feed(real_releases(), [], no_blog, None, self.NOW)
        later = self.NOW + timedelta(minutes=15)
        unchanged = cv.build_feed(real_releases(), [], no_blog, first, later)
        self.assertEqual(first, unchanged)
        more = real_releases() + releases_from([raw_release("3.0.2", "2026-10-19T09:00:00Z")])
        changed = cv.build_feed(more, [], no_blog, first, later)
        self.assertEqual("2026-10-08T06:15:00Z", changed["generated"])

    def test_json_round_trip(self):
        feed = cv.build_feed(real_releases(), [], no_blog, None, self.NOW)
        self.assertEqual(feed, json.loads(json.dumps(feed)))


class FrontmatterTest(unittest.TestCase):
    def test_categories_and_url(self):
        post = "---\ntitle: x\ncategories:\n- announcements\n- Vulnerability\n---\nBody CVE-2026-76904 here"
        frontmatter, body = cv.parse_frontmatter(post)
        self.assertEqual(["announcements", "vulnerability"], frontmatter["categories"])
        self.assertEqual(
            "https://geoserver.org/announcements/vulnerability/2026/08/14/geoserver-3-0-1-released.html",
            cv.build_blog_url("2026-08-14-geoserver-3-0-1-released.md", frontmatter["categories"]),
        )
        self.assertEqual(["CVE-2026-76904"], cv.CVE_RE.findall(body))


if __name__ == "__main__":
    unittest.main()
