#!/usr/bin/env python3
"""
Independent validator for versions.json (schema v1).

Re-reads the written file and checks it against the schema v1 rules. It
deliberately imports nothing from check_versions.py, so a bug or compromise in
the generator cannot also weaken the check. Standard library only.

    python3 scripts/validate_feed.py [path]      # default: versions.json

Exit status 0 means valid. Messages never echo raw file content.
"""

import json
import re
import sys
import unicodedata
from datetime import datetime
from pathlib import Path

MAX_BYTES = 1024 * 1024
MAX_STRING = 2000
MAX_SUMMARY = 500
MAX_URL = 500
MAX_DEPTH = 8
HOSTS = ("github.com", "geoserver.org", "cdn.jsdelivr.net")

VERSION = re.compile(r"[0-9]+\.[0-9]+\.[0-9]+")
SERIES = re.compile(r"[0-9]+\.[0-9]+\.x")
GHSA = re.compile(r"GHSA-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}")
CVE = re.compile(r"CVE-[0-9]{4}-[0-9]{4,7}")
RANGE = re.compile(r"[0-9A-Za-z.<>=,~^ +\-]{1,200}")
RANGE_CLAUSE = re.compile(r"\s*(?:<=|>=|<|>|=|~|\^)?\s*[0-9][0-9A-Za-z.+\-]*\s*")
COMMIT_HASH = re.compile(r"[0-9a-fA-F]{7,64}")
STAMP = re.compile(r"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z")
SCHEME = re.compile(r"(?:javascript|vbscript|data)\s*:", re.IGNORECASE)
RELEASE_URL = re.compile(r"https://github\.com/geoserver/geoserver/releases/tag/v?[0-9]+\.[0-9]+\.[0-9]+")

TOP_KEYS = ["source_note", "schema_version", "generated", "series", "advisories"]
SERIES_KEYS = [
    "series", "phase", "latest_version", "published_at", "release_url", "blog_url",
    "security_flagged", "synchronized_release", "recent_releases",
]
RECENT_KEYS = ["version", "published_at", "blog_url", "security_flagged", "synchronized_release"]
MAX_RECENT_PER_SERIES = 50
MAX_RECENT_TOTAL = 300
ADVISORY_KEYS = ["ghsa_id", "cve_id", "summary", "severity", "published_at", "vulnerable_versions", "patched_versions"]
PHASES = ("stable", "maintenance", "archive")
SEVERITIES = ("critical", "high", "medium", "low")
UNSAFE = ("Cc", "Cf", "Cs", "Co")


def shown(value):
    text = ascii(value)
    return text if len(text) <= 50 else text[:50] + "..."


class Problems:
    def __init__(self):
        self.items = []

    def add(self, where, message):
        self.items.append(f"{where}: {message}")


def duplicate_free(pairs):
    seen = {}
    for key, value in pairs:
        if key in seen:
            raise ValueError("duplicate key")
        seen[key] = value
    return seen


def no_constants(name):
    raise ValueError("non-finite number")


def depth_of(value):
    deepest = 0
    stack = [(value, 1)]
    while stack:
        item, depth = stack.pop()
        deepest = max(deepest, depth)
        if isinstance(item, dict):
            stack.extend((v, depth + 1) for v in item.values())
        elif isinstance(item, list):
            stack.extend((v, depth + 1) for v in item)
    return deepest


def each_string(value, where="$"):
    if isinstance(value, str):
        yield where, value
    elif isinstance(value, dict):
        for key, item in value.items():
            yield from each_string(key, f"{where} (key)")
            yield from each_string(item, f"{where}.{key if isinstance(key, str) and len(key) < 40 else '?'}")
    elif isinstance(value, list):
        for index, item in enumerate(value):
            yield from each_string(item, f"{where}[{index}]")


def check_plain(problems, where, text, limit=MAX_STRING):
    if len(text) > limit:
        problems.add(where, f"longer than {limit} characters")
    if any(unicodedata.category(ch) in UNSAFE for ch in text):
        problems.add(where, "contains a control, format or private-use character")
    if "<" in text or ">" in text:
        problems.add(where, "contains < or >")
    if SCHEME.search(text):
        problems.add(where, "contains script-scheme text")
    if unicodedata.normalize("NFC", text) != text:
        problems.add(where, "is not NFC")
    if text != text.strip() or "  " in text:
        problems.add(where, "has untidy whitespace")


def check_url(problems, where, url):
    if not isinstance(url, str):
        problems.add(where, "URL is not a string")
        return
    if len(url) > MAX_URL or any(ch.isspace() for ch in url) or not url.startswith("https://"):
        problems.add(where, "URL is not a short https URL without whitespace")
        return
    netloc = url.split("/")[2]
    if "@" in netloc or netloc.lower() not in HOSTS:
        problems.add(where, "URL host is not allowed or has user info")


def exact_keys(problems, where, obj, expected, optional=()):
    if not isinstance(obj, dict):
        problems.add(where, "is not an object")
        return False
    actual = list(obj)
    wanted = [k for k in expected if k in obj or k not in optional]
    if actual != wanted:
        problems.add(where, f"keys are {shown(actual)}, expected {shown(wanted)}")
        return False
    return True


def check_stamp(problems, where, value):
    if not isinstance(value, str) or not STAMP.fullmatch(value):
        problems.add(where, "is not an ISO 8601 UTC timestamp")
        return
    try:
        datetime.strptime(value, "%Y-%m-%dT%H:%M:%SZ")
    except ValueError:
        problems.add(where, "is not a real date")


def version_key(version):
    return tuple(int(part) for part in version.split("."))


def check_recent(problems, where, entry):
    """Validate recent_releases of one series entry. Returns its length."""
    recent = entry["recent_releases"]
    if not isinstance(recent, list):
        problems.add(where, "recent_releases is not a list")
        return 0
    if len(recent) > MAX_RECENT_PER_SERIES:
        problems.add(where, f"has more than {MAX_RECENT_PER_SERIES} recent releases")
    series = entry["series"] if isinstance(entry["series"], str) else None
    seen, previous_key = [], None
    for j, item in enumerate(recent):
        here = f"{where}.recent_releases[{j}]"
        if not exact_keys(problems, here, item, RECENT_KEYS):
            continue
        version = item["version"]
        if not (isinstance(version, str) and VERSION.fullmatch(version)):
            problems.add(here, "invalid version")
            continue
        if series and version.rsplit(".", 1)[0] + ".x" != series:
            problems.add(here, "version is not in the series")
        seen.append(version)
        check_stamp(problems, f"{here}.published_at", item["published_at"])
        for flag in ("security_flagged", "synchronized_release"):
            if type(item[flag]) is not bool:
                problems.add(here, f"{flag} is not a boolean")
        if item["blog_url"] is not None:
            check_url(problems, f"{here}.blog_url", item["blog_url"])
        if isinstance(item["published_at"], str) and STAMP.fullmatch(item["published_at"]):
            key = (item["published_at"], version_key(version))
            if previous_key is not None and key > previous_key:
                problems.add(here, "is not newest first")
            previous_key = key
    if len(set(seen)) != len(seen):
        problems.add(where, "recent_releases has duplicate versions")
    latest = entry["latest_version"]
    match = next((i for i in recent if isinstance(i, dict) and i.get("version") == latest), None)
    if match is not None:
        for key in ("blog_url", "security_flagged", "synchronized_release"):
            if entry[key] != match.get(key):
                problems.add(where, f"{key} differs from the latest release in recent_releases")
    elif entry["blog_url"] is not None or entry["security_flagged"] is not False:
        problems.add(where, "latest release is not in recent_releases but has a blog_url or a security flag")
    return len(recent)


def check_series(problems, entries):
    if not isinstance(entries, list):
        problems.add("series", "is not a list")
        return
    if len(entries) > 100:
        problems.add("series", "has more than 100 entries")
    names = []
    stable = 0
    total = 0
    for i, entry in enumerate(entries):
        where = f"series[{i}]"
        if not exact_keys(problems, where, entry, SERIES_KEYS):
            continue
        if not (isinstance(entry["series"], str) and SERIES.fullmatch(entry["series"])):
            problems.add(where, "invalid series")
        else:
            names.append(entry["series"])
        if entry["phase"] not in PHASES:
            problems.add(where, "invalid phase")
        stable += entry["phase"] == "stable"
        latest = entry["latest_version"]
        if not (isinstance(latest, str) and VERSION.fullmatch(latest)):
            problems.add(where, "invalid latest_version")
        elif isinstance(entry["series"], str) and latest.rsplit(".", 1)[0] + ".x" != entry["series"]:
            problems.add(where, "latest_version is not in the series")
        check_stamp(problems, f"{where}.published_at", entry["published_at"])
        url = entry["release_url"]
        if not (isinstance(url, str) and RELEASE_URL.fullmatch(url)):
            problems.add(where, "release_url does not match the fixed template")
        for flag in ("security_flagged", "synchronized_release"):
            if type(entry[flag]) is not bool:
                problems.add(where, f"{flag} is not a boolean")
        if entry["blog_url"] is not None:
            check_url(problems, f"{where}.blog_url", entry["blog_url"])
        total += check_recent(problems, where, entry)
    if total > MAX_RECENT_TOTAL:
        problems.add("series", f"has more than {MAX_RECENT_TOTAL} recent releases in total")
    if len(set(names)) != len(names):
        problems.add("series", "has duplicate series")
    if entries and stable != 1:
        problems.add("series", "does not have exactly one stable series")


def check_advisories(problems, advisories):
    if not isinstance(advisories, list):
        problems.add("advisories", "is not a list")
        return
    if len(advisories) > 500:
        problems.add("advisories", "has more than 500 entries")
    seen = []
    for i, advisory in enumerate(advisories):
        where = f"advisories[{i}]"
        if not exact_keys(problems, where, advisory, ADVISORY_KEYS, optional=("cve_id",)):
            continue
        ghsa = advisory["ghsa_id"]
        if not (isinstance(ghsa, str) and GHSA.fullmatch(ghsa)):
            problems.add(where, "invalid ghsa_id")
            continue
        where = f"advisory {ghsa}"
        seen.append(ghsa)
        if "cve_id" in advisory and advisory["cve_id"] is not None:
            if not (isinstance(advisory["cve_id"], str) and CVE.fullmatch(advisory["cve_id"])):
                problems.add(where, "invalid cve_id")
        if not isinstance(advisory["summary"], str):
            problems.add(where, "summary is not a string")
        else:
            check_plain(problems, f"{where}.summary", advisory["summary"], MAX_SUMMARY)
        if advisory["severity"] not in SEVERITIES:
            problems.add(where, "invalid severity")
        check_stamp(problems, f"{where}.published_at", advisory["published_at"])
        ranges = advisory["vulnerable_versions"]
        if not isinstance(ranges, list) or len(ranges) > 50:
            problems.add(where, "vulnerable_versions is not a list of at most 50")
        else:
            for item in ranges:
                plain = isinstance(item, str) and (
                    item == ""
                    or (
                        RANGE.fullmatch(item)
                        and (COMMIT_HASH.fullmatch(item) or all(RANGE_CLAUSE.fullmatch(c) for c in item.split(",")))
                    )
                )
                if not plain:
                    problems.add(where, "a version range is not plainly a range")
        patched = advisory["patched_versions"]
        if not isinstance(patched, dict):
            problems.add(where, "patched_versions is not an object")
        else:
            for series, version in patched.items():
                ok = (
                    isinstance(series, str) and SERIES.fullmatch(series)
                    and isinstance(version, str) and VERSION.fullmatch(version)
                    and version.rsplit(".", 1)[0] + ".x" == series
                )
                if not ok:
                    problems.add(where, "invalid patched_versions entry")
    if len(set(seen)) != len(seen):
        problems.add("advisories", "has duplicate ids")


def validate_text(raw):
    problems = Problems()
    if len(raw) > MAX_BYTES:
        problems.add("file", "larger than 1 MiB")
    if raw.startswith(b"\xef\xbb\xbf"):
        problems.add("file", "starts with a byte order mark")
    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError:
        problems.add("file", "is not valid UTF-8")
        return problems.items
    for char, name in (("<", "<"), (">", ">"), ("&", "&")):
        if char in text:
            problems.add("file", f"contains a raw {name} (must be escaped inside strings)")
    if "\r" in text:
        problems.add("file", "contains a carriage return")
    try:
        decoder = json.JSONDecoder(object_pairs_hook=duplicate_free, parse_constant=no_constants)
        document, end = decoder.raw_decode(text)
    except (ValueError, RecursionError) as error:
        problems.add("file", f"is not one valid JSON document ({type(error).__name__})")
        return problems.items
    if text[end:].strip():
        problems.add("file", "has content after the JSON document")
    if not text.endswith("\n"):
        problems.add("file", "does not end with a newline")
    if depth_of(document) > MAX_DEPTH:
        problems.add("file", "is nested too deeply")
        return problems.items

    for where, value in each_string(document):
        if len(value) > MAX_STRING:
            problems.add(where, f"longer than {MAX_STRING} characters")
        if any(unicodedata.category(ch) in UNSAFE for ch in value):
            problems.add(where, "contains a control, format or private-use character")

    if not exact_keys(problems, "feed", document, TOP_KEYS):
        return problems.items
    if not isinstance(document["source_note"], str):
        problems.add("source_note", "is not a string")
    else:
        check_plain(problems, "source_note", document["source_note"], 500)
    if type(document["schema_version"]) is not int or document["schema_version"] != 1:
        problems.add("schema_version", "is not the integer 1")
    check_stamp(problems, "generated", document["generated"])
    check_series(problems, document["series"])
    check_advisories(problems, document["advisories"])
    return problems.items


def main(argv):
    path = Path(argv[1]) if len(argv) > 1 else Path("versions.json")
    try:
        raw = path.read_bytes()
    except OSError:
        print(f"INVALID: cannot read {shown(str(path))}")
        return 2
    problems = validate_text(raw)
    if problems:
        print(f"INVALID: {len(problems)} problem(s)")
        for problem in problems[:50]:
            print(f"  - {problem}")
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
