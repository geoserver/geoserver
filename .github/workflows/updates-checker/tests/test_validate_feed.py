"""The independent validator must reject every kind of tampered or malformed feed."""

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent.parent / "scripts"
sys.path.insert(0, str(SCRIPTS))

import check_versions as cv  # noqa: E402
import validate_feed  # noqa: E402
from test_hardening import generate  # noqa: E402
from test_check_versions import raw_advisory  # noqa: E402


def valid_text():
    return generate([raw_advisory("GHSA-abcd-efgh-ijkl"), raw_advisory("GHSA-mnop-qrst-uvwx", published_at="2025-01-01T00:00:00Z")])[1]


def problems(text):
    return validate_feed.validate_text(text.encode("utf-8") if isinstance(text, str) else text)


def mutated(change):
    document = json.loads(valid_text())
    change(document)
    return cv.serialize(document)


class ValidatorTest(unittest.TestCase):
    def assert_rejected(self, text, fragment=None):
        found = problems(text)
        self.assertTrue(found, "the validator accepted a bad feed")
        if fragment:
            self.assertTrue(any(fragment in p for p in found), f"{fragment!r} not in {found}")

    def test_a_generated_feed_is_valid(self):
        self.assertEqual([], problems(valid_text()))

    def test_file_level_problems(self):
        text = valid_text()
        self.assert_rejected("﻿" + text, "byte order mark")
        self.assert_rejected(text.replace("\n", "\r\n"), "carriage return")
        self.assert_rejected(text + "garbage", "after the JSON document")
        self.assert_rejected(text + text, "after the JSON document")
        self.assert_rejected(text.rstrip("\n"), "newline")
        self.assert_rejected(b"\xff\xfe" + text.encode(), "UTF-8")
        self.assert_rejected("", "valid JSON")
        self.assert_rejected(text.replace("Example", "a<b"), "raw <")
        self.assert_rejected(text.replace("Example", "a>b"), "raw >")
        self.assert_rejected(text.replace("Example", "a&b"), "raw &")

    def test_json_level_problems(self):
        text = valid_text()
        self.assert_rejected(text.replace('"schema_version": 1,', '"schema_version": 1, "schema_version": 1,'), "valid JSON")
        self.assert_rejected(text.replace('"schema_version": 1', '"schema_version": NaN'), "valid JSON")
        # Python before 3.12 fails to parse this; later versions parse it and the depth check rejects it.
        self.assert_rejected('{"a": ' + "[" * 5000 + "]" * 5000 + "}\n")
        self.assert_rejected('{"a": ' + "[" * 200000 + "]" * 200000 + "}\n")
        deep = mutated(lambda d: d.update(series=[[[[[[[[[[1]]]]]]]]]]))
        self.assert_rejected(deep, "nested too deeply")

    def test_over_one_mebibyte(self):
        self.assert_rejected(valid_text() + " " * (1024 * 1024), "1 MiB")

    def test_top_level(self):
        self.assert_rejected(mutated(lambda d: d.update(extra=1)), "keys")
        self.assert_rejected(mutated(lambda d: d.pop("source_note")), "keys")
        self.assert_rejected(cv.serialize(dict(reversed(list(json.loads(valid_text()).items())))), "keys")
        for bad in ("1", True, 2, 1.0, None):
            self.assert_rejected(mutated(lambda d, b=bad: d.update(schema_version=b)), "schema_version")
        for bad in ("yesterday", "2026-13-45T00:00:00Z", 5, "2026-01-01T00:00:00+00:00"):
            self.assert_rejected(mutated(lambda d, b=bad: d.update(generated=b)), "generated")
        self.assert_rejected(mutated(lambda d: d.update(source_note="javascript:alert(1)")), "script")
        self.assert_rejected(mutated(lambda d: d.update(source_note="x" * 501)), "source_note")

    def test_series(self):
        def first(change):
            return mutated(lambda d: change(d["series"][0]))

        self.assert_rejected(first(lambda e: e.update(phase="weird")), "phase")
        self.assert_rejected(first(lambda e: e.update(latest_version="3.0")), "latest_version")
        self.assert_rejected(first(lambda e: e.update(latest_version="2.28.5")), "not in the series")
        self.assert_rejected(first(lambda e: e.update(series="3.0")), "series")
        self.assert_rejected(first(lambda e: e.update(release_url="https://evil.example/x")), "release_url")
        self.assert_rejected(first(lambda e: e.update(release_url="https://github.com/geoserver/geoserver/releases/tag/3.0.1/../x")), "release_url")
        for url in ("http://geoserver.org/x", "https://evil.example/x", "https://geoserver.org@evil.example/x",
                    "https://geoserver.org/a b", "javascript:alert(1)", 5):
            self.assert_rejected(first(lambda e, u=url: e.update(blog_url=u)), "blog_url")
        self.assert_rejected(first(lambda e: e.update(security_flagged="yes")), "boolean")
        self.assert_rejected(first(lambda e: e.update(synchronized_release=1)), "boolean")
        self.assert_rejected(first(lambda e: e.update(cve_ids=["CVE-2026-00000"])), "keys")
        self.assert_rejected(first(lambda e: e.update(blog_confirmed=True)), "keys")
        self.assert_rejected(first(lambda e: e.update(extra="x")), "keys")
        self.assert_rejected(mutated(lambda d: d["series"].append(dict(d["series"][0]))), "duplicate series")
        self.assert_rejected(mutated(lambda d: d["series"].__setitem__(1, dict(d["series"][1], phase="stable"))), "exactly one stable")
        self.assert_rejected(mutated(lambda d: d.update(series="x")), "not a list")

    def test_advisories(self):
        def first(change):
            return mutated(lambda d: change(d["advisories"][0]))

        for bad in ("GHSA-ABCD-EFGH-IJKL", "GHSA-xxxx-xxxx-xxxx/../../x", "", 5):
            self.assert_rejected(first(lambda a, b=bad: a.update(ghsa_id=b)), "ghsa_id")
        self.assert_rejected(first(lambda a: a.update(severity="urgent")), "severity")
        self.assert_rejected(first(lambda a: a.update(severity="CRITICAL")), "severity")
        self.assert_rejected(first(lambda a: a.update(cve_id="<b>")), "cve_id")
        for bad in ("javascript:alert(1)", "a" * 501, "\x07bell", "a‮b", "a​b", "a<b", "  padded", "two  spaces",
                    "é", "data:text/html", 5):
            self.assert_rejected(first(lambda a, b=bad: a.update(summary=b)), None)
        self.assert_rejected(first(lambda a: a.update(published_at="2026-13-45T00:00:00Z")), "published_at")
        self.assert_rejected(first(lambda a: a.update(vulnerable_versions=["<script>"])), "range")
        self.assert_rejected(first(lambda a: a.update(vulnerable_versions=["; rm -rf /"])), "range")
        self.assert_rejected(first(lambda a: a.update(vulnerable_versions=["x" * 300])), "range")
        self.assert_rejected(first(lambda a: a.update(vulnerable_versions=[f"< 1.0.{i}" for i in range(51)])), "50")
        self.assert_rejected(first(lambda a: a.update(vulnerable_versions="< 1.0.0")), "list")
        self.assert_rejected(first(lambda a: a.update(patched_versions={"3.0.x": "2.28.5"})), "patched")
        self.assert_rejected(first(lambda a: a.update(patched_versions={"3.0": "3.0.1"})), "patched")
        self.assert_rejected(first(lambda a: a.update(patched_versions=[])), "patched_versions")
        self.assert_rejected(first(lambda a: a.update(credits=[{"login": "someone"}])), "keys")
        self.assert_rejected(mutated(lambda d: d["advisories"].append(dict(d["advisories"][0]))), "duplicate")
        self.assert_rejected(mutated(lambda d: d.update(advisories=d["advisories"] * 300)), "more than 500")

    def test_accepts_the_allowed_shapes(self):
        for ranges in ([], [""], ["< 2.23.5"], [">=2.24.0, <2.24.4"], ["df11a650c650ff895977c5440427c239671ee649"], ["", "3.0.0"]):
            text = mutated(lambda d, r=ranges: d["advisories"][0].update(vulnerable_versions=r))
            self.assertEqual([], problems(text), ranges)
        text = mutated(lambda d: d["advisories"][0].pop("cve_id"))
        self.assertEqual([], problems(text))
        text = mutated(lambda d: d["advisories"][0].update(cve_id=None))
        self.assertEqual([], problems(text))

    def test_the_validator_shares_no_code_with_the_generator(self):
        import ast

        tree = ast.parse((SCRIPTS / "validate_feed.py").read_text())
        imported = set()
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                imported.update(a.name.split(".")[0] for a in node.names)
            elif isinstance(node, ast.ImportFrom):
                imported.add((node.module or "").split(".")[0])
        self.assertEqual({"json", "re", "sys", "unicodedata", "datetime", "pathlib"}, imported)

    def test_command_line(self):
        with tempfile.TemporaryDirectory() as directory:
            good = Path(directory) / "good.json"
            bad = Path(directory) / "bad.json"
            good.write_text(valid_text(), encoding="utf-8")
            bad.write_text(valid_text().replace('"high"', '"urgent"'), encoding="utf-8")
            run = lambda path: subprocess.run(  # noqa: E731
                [sys.executable, str(SCRIPTS / "validate_feed.py"), str(path)], capture_output=True, text=True
            )
            self.assertEqual((0, "OK\n"), (run(good).returncode, run(good).stdout))
            self.assertEqual(1, run(bad).returncode)
            self.assertIn("severity", run(bad).stdout)
            self.assertEqual(2, run(Path(directory) / "missing.json").returncode)


if __name__ == "__main__":
    unittest.main()
