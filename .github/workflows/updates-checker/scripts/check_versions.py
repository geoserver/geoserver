#!/usr/bin/env python3
"""
GeoServer version feed generator (schema v1).

Reads public GeoServer release, security-advisory and blog data and writes
versions.json: one entry per release series (with an inferred lifecycle phase)
plus the published security advisories.

The upstream data is untrusted. Every output object is built by copying only
allow-listed fields, every value is type- and format-checked (never coerced),
free text is cleaned to plain text and URLs are built rather than copied.

One bad item never blocks the feed. A value that cannot be used is replaced by
a safe neutral one and the advisory is still published, in a form that tells the
administrator to review it manually; the problem is logged as a warning. The run
fails only when GitHub cannot be reached, or when there is nothing usable and
nothing previously published to fall back on. The output is checked again by
scripts/validate_feed.py, which shares no code with this file.

All requests are unauthenticated and use a generic User-Agent. The script runs
on GitHub-hosted runners and has no dependency beyond the Python standard
library.

Phase inference (no configuration file):
  1. Group proper releases into events: releases within 24 hours of the
     earliest release in the group.
  2. The most recent event containing two or more series is the coordinated
     event. Its N distinct series are the maintained set.
  3. Series newer than anything in that set (for example a new series released
     on its own) join the pool. The top N of the pool by version are active:
     the highest is "stable", the rest "maintenance". Every other series is
     "archive". A new series therefore displaces the oldest maintained one.
  4. With no coordinated event at all: highest = stable, next = maintenance,
     the rest archive.
"""

import html
import json
import os
import re
import sys
import time
import unicodedata
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import NamedTuple

GITHUB_RELEASES = "https://api.github.com/repos/geoserver/geoserver/releases?per_page=100"
GITHUB_ADVISORIES = "https://api.github.com/repos/geoserver/geoserver/security-advisories?per_page=100"
BLOG_POSTS_API = "https://api.github.com/repos/geoserver/geoserver.github.io/contents/_posts"
RAW_POST_BASE = "https://raw.githubusercontent.com/geoserver/geoserver.github.io/main/_posts/"
RELEASE_URL_BASE = "https://github.com/geoserver/geoserver/releases/tag/"

USER_AGENT = "gs-updates-checker-feed"
# the workflow points this at the feed branch checkout; run by hand, it writes ./versions.json
OUTPUT_PATH = Path(os.environ.get("VERSIONS_JSON") or "versions.json")

SCHEMA_VERSION = 1
SOURCE_NOTE = (
    "The version feed is a static file on GitHub, delivered directly (raw.githubusercontent.com) "
    "or through the public jsDelivr content delivery network (cdn.jsdelivr.net). "
    "The GeoServer team does not run these hosts and receives no request logs at all. "
    "We cannot see which GeoServer versions are running. This module sends nothing about your server. "
    "However, the feed hosts can see the public IP address of your server making the request."
)
MAX_NOTE = 500
COORDINATION_WINDOW = timedelta(hours=24)
RECENT_DAYS = 365
MAX_ADVISORY_PAGES = 3

MAX_SERIES = 100
MAX_RECENT_PER_SERIES = 50
MAX_RECENT_TOTAL = 300
MAX_ADVISORIES = 500
MAX_RANGES = 50
MAX_FILE_BYTES = 1024 * 1024
MAX_STRING = 2000
MAX_SUMMARY = 500
MAX_URL = 500
MAX_API_BYTES = 10 * 1024 * 1024
MAX_JSON_DEPTH = 20

SEVERITIES = ("critical", "high", "medium", "low")
SEVERITY_RANK = {name: rank for rank, name in enumerate(SEVERITIES)}
UNREADABLE_ID = "GHSA-0000-0000-0000"
NO_SUMMARY = "Summary unavailable. See the advisory on GitHub."
UNREADABLE_SEVERITY_NOTE = "[Severity unreadable] "
DEFAULT_STAMP = "1970-01-01T00:00:00Z"
URL_HOSTS = ("github.com", "geoserver.org", "cdn.jsdelivr.net")

RELEASE_TAG_RE = re.compile(r"v?([0-9]+)\.([0-9]+)\.([0-9]+)")
PATCHED_TOKEN_RE = re.compile(r"(?<![0-9.])([0-9]+)\.([0-9]+)\.([0-9]+)(?![0-9.])")
GHSA_RE = re.compile(r"GHSA-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}")
CVE_RE = re.compile(r"CVE-[0-9]{4}-[0-9]{4,7}")
CVE_FIND_RE = re.compile(r"CVE-[0-9]{4}-[0-9]{4,7}")
RANGE_RE = re.compile(r"[0-9A-Za-z.<>=,~^ +\-]{1,200}")
RANGE_CLAUSE_RE = re.compile(r"\s*(?:<=|>=|<|>|=|~|\^)?\s*[0-9][0-9A-Za-z.+\-]*\s*")
HASH_RE = re.compile(r"[0-9a-fA-F]{7,64}")
TIMESTAMP_RE = re.compile(r"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z")
TIMESTAMP_FORMAT = "%Y-%m-%dT%H:%M:%SZ"
POST_NAME_RE = re.compile(r"([0-9]{4})-([0-9]{2})-([0-9]{2})-([a-z0-9-]+)\.md")
CATEGORY_RE = re.compile(r"[a-z0-9-]{1,40}")

_SPACE_LIKE = re.compile("[\t\n\r\v\f\x85  ]")
_MARKDOWN_LINK = re.compile(r"!?\[([^\]]*)\]\((?:[^()]|\([^()]*\))*\)")
_TAG = re.compile(r"<[^>]*>")
_PLACEHOLDER = re.compile(r"<([A-Za-z][A-Za-z0-9_-]{0,29})>")
HTML_ELEMENTS = frozenset(
    """a abbr acronym address applet area article aside audio b base basefont bdi bdo bgsound big blink
    blockquote body br button canvas caption center cite code col colgroup command content data datalist dd del
    details dfn dialog dir div dl dt element em embed fieldset figcaption figure font footer form frame frameset
    h1 h2 h3 h4 h5 h6 head header hgroup hr html i iframe image img input ins isindex kbd keygen label legend li
    link listing main map mark marquee math menu menuitem meta meter multicol nav nextid nobr noembed noframes
    noscript object ol optgroup option output p param picture plaintext pre progress q rb rbc rp rt rtc ruby s
    samp script search section select shadow slot small source spacer span strike strong style sub summary sup
    svg table tbody td template textarea tfoot th thead time title tr track tt u ul var video wbr xmp""".split()
)
_SCHEME = re.compile(r"(javascript|vbscript|data)\s*:", re.IGNORECASE)
_WHITESPACE = re.compile(r"\s+")
_UNSAFE_CATEGORIES = ("Cc", "Cf", "Cs", "Co")


class FeedError(Exception):
    """Nothing usable can be published. Messages never echo raw input."""


class UpstreamError(Exception):
    """GitHub could not be reached or refused the request: the run fails."""


class Poisoned:
    """Stands in for a value that cannot be trusted: a duplicate key, a
    non-finite number, or JSON nested too deeply. It is never a dict, list or
    string, so the field checks treat it as an unreadable value."""

    __slots__ = ()


POISON = Poisoned()


def safe_repr(value, limit=60):
    text = ascii(value)
    return text if len(text) <= limit else text[:limit] + "..."


class Release(NamedTuple):
    version: str
    major: int
    minor: int
    patch: int
    published: datetime
    published_at: str
    url: str

    @property
    def series(self):
        return (self.major, self.minor)


def series_name(series):
    return f"{series[0]}.{series[1]}.x"


def parse_timestamp(value):
    return datetime.strptime(value, TIMESTAMP_FORMAT).replace(tzinfo=timezone.utc)


def checked_timestamp(value, label):
    if not isinstance(value, str) or not TIMESTAMP_RE.fullmatch(value):
        raise FeedError(f"{label}: invalid timestamp {safe_repr(value)}")
    try:
        parse_timestamp(value)
    except ValueError:
        raise FeedError(f"{label}: not a real date {safe_repr(value)}")
    return value


# ---------------------------------------------------------------- strict JSON

def _no_duplicate_keys(pairs):
    result = {}
    poisoned = False
    for key, value in pairs:
        if key in result:
            poisoned = True
        result[key] = value
    return POISON if poisoned else result


def _poison_constant(name):
    return POISON


def _checked_float(text):
    value = float(text)
    return POISON if value != value or value in (float("inf"), float("-inf")) else value


def _limit_depth(data):
    stack = [(data, 1)]
    while stack:
        container, depth = stack.pop()
        if isinstance(container, dict):
            keys = list(container)
            get = container.__getitem__
        elif isinstance(container, list):
            keys = range(len(container))
            get = container.__getitem__
        else:
            continue
        for key in keys:
            child = get(key)
            if isinstance(child, (dict, list)):
                if depth + 1 > MAX_JSON_DEPTH:
                    container[key] = POISON
                else:
                    stack.append((child, depth + 1))
    return data


def parse_api_json(text):
    """Parse an API response. Bad pieces (duplicate keys, non-finite numbers,
    over-deep nesting) become POISON so that only the item holding them is
    affected. A response that is not JSON at all, or too large, raises FeedError."""
    if len(text) > MAX_API_BYTES:
        raise FeedError("an API response is too large")
    try:
        data = json.loads(
            text,
            object_pairs_hook=_no_duplicate_keys,
            parse_constant=_poison_constant,
            parse_float=_checked_float,
        )
    except RecursionError:
        raise FeedError("an API response is nested too deeply")
    except ValueError:
        raise FeedError("an API response is not valid JSON")
    return _limit_depth(data)


# ------------------------------------------------------------------ cleaning

def _remove_unsafe(text):
    return "".join(ch for ch in text if unicodedata.category(ch) not in _UNSAFE_CATEGORIES)


def _placeholder_word(match):
    """A bare <name> that is not an HTML element is a placeholder in prose, such
    as sld=<url>: keep the word and drop the brackets. Real elements are removed."""
    name = match.group(1)
    return "" if name.lower() in HTML_ELEMENTS else name


def _strip_tags(text):
    while True:
        previous = text
        while True:
            replaced = _PLACEHOLDER.sub(_placeholder_word, text)
            if replaced == text:
                break
            text = replaced
        text = _TAG.sub("", text)
        if text == previous:
            return text


def clean_text(value, max_len=MAX_STRING):
    """Reduce untrusted text to plain text: no control, line-separator, zero-width
    or bidirectional characters, no markup, no <, > or script-scheme text, NFC,
    single spaces, truncated to max_len characters."""
    if not isinstance(value, str):
        raise FeedError("a text field is not a string")
    text = _SPACE_LIKE.sub(" ", value)
    text = _remove_unsafe(text)
    text = unicodedata.normalize("NFC", text)
    text = _MARKDOWN_LINK.sub(r"\1", text)
    text = text.replace("`", "").replace("**", "").replace("~~", "")
    text = _strip_tags(text)
    text = html.unescape(text)
    text = _SPACE_LIKE.sub(" ", text)
    text = _remove_unsafe(text)
    text = _strip_tags(text)
    text = text.replace("<", "").replace(">", "")
    previous = None
    while previous != text:
        previous = text
        text = _SCHEME.sub(r"\1 ", text)
    text = unicodedata.normalize("NFC", text)
    text = _WHITESPACE.sub(" ", text).strip()
    text = text[:max_len].rstrip()
    if "<" in text or ">" in text or _SCHEME.search(text) or _remove_unsafe(text) != text:
        raise FeedError("text could not be cleaned")
    return text


def clean_range(value):
    """Keep the GitHub wording only if it is plainly a version range; otherwise
    publish an empty string so the plugin shows the advisory for manual review."""
    if not isinstance(value, str):
        raise FeedError("a version range is not a string")
    if not RANGE_RE.fullmatch(value):
        return ""
    if HASH_RE.fullmatch(value) or all(RANGE_CLAUSE_RE.fullmatch(c) for c in value.split(",")):
        return value
    return ""


def checked_url(url):
    if (
        not isinstance(url, str)
        or len(url) > MAX_URL
        or not url.startswith("https://")
        or any(ch.isspace() for ch in url)
        or "@" in url.split("/")[2]
    ):
        return None
    host = url.split("/")[2].lower()
    return url if host in URL_HOSTS else None


# ------------------------------------------------------------------ releases

def parse_release(raw):
    """A proper release only: MAJOR.MINOR.PATCH, not a draft. RCs, milestones and
    four-part tags such as 2.7.1.1 are ignored. The URL is built from the tag."""
    if not isinstance(raw, dict):
        raise FeedError("a release is not an object")
    draft = raw.get("draft")
    if draft is not None and not isinstance(draft, bool):
        raise FeedError("a release has a non-boolean draft flag")
    tag = raw.get("tag_name")
    if not isinstance(tag, str):
        raise FeedError("a release tag is not a string")
    published_at = raw.get("published_at")
    if published_at is not None and not isinstance(published_at, str):
        raise FeedError("a release publish time is not a string")
    if draft or not published_at:
        return None
    match = RELEASE_TAG_RE.fullmatch(tag)
    if not match:
        return None
    checked_timestamp(published_at, f"release {tag}")
    major, minor, patch = (int(g) for g in match.groups())
    return Release(
        version=f"{major}.{minor}.{patch}",
        major=major,
        minor=minor,
        patch=patch,
        published=parse_timestamp(published_at),
        published_at=published_at,
        url=RELEASE_URL_BASE + tag,
    )


def cluster_releases(releases):
    """Group releases into events. A window opens at its earliest release and
    takes every release within COORDINATION_WINDOW of it."""
    clusters = []
    current = []
    for release in sorted(releases, key=lambda r: r.published):
        if current and release.published - current[0].published > COORDINATION_WINDOW:
            clusters.append(current)
            current = []
        current.append(release)
    if current:
        clusters.append(current)
    return clusters


def is_coordinated(cluster):
    return len({r.series for r in cluster}) >= 2


def infer_phases(releases):
    """Return {series tuple: phase}."""
    all_series = {r.series for r in releases}
    if not all_series:
        return {}
    coordinated = [c for c in cluster_releases(releases) if is_coordinated(c)]
    if coordinated:
        members = {r.series for r in coordinated[-1]}
        size = len(members)
        pool = members | {s for s in all_series if s > max(members)}
    else:
        size = 2
        pool = all_series
    active = sorted(pool, reverse=True)[:size]
    phases = {}
    for series in all_series:
        if series not in active:
            phases[series] = "archive"
        elif series == active[0]:
            phases[series] = "stable"
        else:
            phases[series] = "maintenance"
    return phases


def synchronized_versions(releases):
    """Versions that were released inside any coordinated event."""
    result = set()
    for cluster in cluster_releases(releases):
        if is_coordinated(cluster):
            result.update(r.version for r in cluster)
    return result


def latest_per_series(releases):
    latest = {}
    for release in releases:
        current = latest.get(release.series)
        if current is None or release.patch > current.patch:
            latest[release.series] = release
    return latest


# ---------------------------------------------------------------- advisories

def derive_patched_versions(patched_strings):
    """Map series to the lowest patched version found across all strings."""
    best = {}
    for patched in patched_strings:
        for match in PATCHED_TOKEN_RE.finditer(patched):
            version = tuple(int(g) for g in match.groups())
            series = version[:2]
            if series not in best or version < best[series]:
                best[series] = version
    return {
        series_name(series): ".".join(str(part) for part in version)
        for series, version in sorted(best.items(), reverse=True)
    }


def warning(log, message):
    log("WARNING " + message)


def _safe_summary(value, label, log):
    try:
        return clean_text(value, MAX_SUMMARY)
    except FeedError:
        warning(log, f"{label}: summary unreadable, replaced by a neutral text")
        return NO_SUMMARY


def transform_advisory(raw, index=0, stamp=DEFAULT_STAMP, log=print):
    """Return a valid advisory, degrading any unusable field to a safe value so
    the advisory is still published (for manual review). Returns None only when
    the advisory cannot be identified at all."""
    if not isinstance(raw, dict):
        warning(log, f"advisory #{index}: not an object, cannot be identified")
        return None
    ghsa_id = raw.get("ghsa_id")
    if not isinstance(ghsa_id, str) or not GHSA_RE.fullmatch(ghsa_id):
        warning(log, f"advisory #{index}: invalid ghsa_id {safe_repr(ghsa_id)}, cannot be identified")
        return None
    label = f"advisory {ghsa_id}"

    cve_id = raw.get("cve_id")
    if cve_id is not None and not (isinstance(cve_id, str) and CVE_RE.fullmatch(cve_id)):
        warning(log, f"{label}: invalid cve_id omitted")
        cve_id = False

    summary = _safe_summary(raw.get("summary"), label, log)
    severity = raw.get("severity")
    if severity not in SEVERITIES:
        warning(log, f"{label}: severity {safe_repr(severity)} unreadable, published as high for review")
        severity = "high"
        summary = (UNREADABLE_SEVERITY_NOTE + summary)[:MAX_SUMMARY].rstrip()

    try:
        published_at = checked_timestamp(raw.get("published_at"), label)
    except FeedError:
        warning(log, f"{label}: published_at unreadable, replaced by the generation time")
        published_at = stamp

    vulnerabilities = raw.get("vulnerabilities")
    ranges = []
    patched_strings = []
    if vulnerabilities is None:
        vulnerabilities = []
    if not isinstance(vulnerabilities, list):
        warning(log, f"{label}: vulnerabilities unreadable, published for manual review")
        ranges.append("")
        vulnerabilities = []
    for vulnerability in vulnerabilities:
        if not isinstance(vulnerability, dict):
            warning(log, f"{label}: a vulnerability entry is unreadable, published for manual review")
            cleaned = ""
        else:
            version_range = vulnerability.get("vulnerable_version_range")
            if version_range is None:
                cleaned = None
            elif isinstance(version_range, str):
                cleaned = clean_range(version_range)
            else:
                warning(log, f"{label}: a version range is unreadable, published for manual review")
                cleaned = ""
            patched = vulnerability.get("patched_versions")
            if isinstance(patched, str):
                patched_strings.append(patched)
            elif patched is not None:
                warning(log, f"{label}: a patched_versions value is unreadable and ignored")
        if cleaned is not None and cleaned not in ranges:
            ranges.append(cleaned)
    if len(ranges) > MAX_RANGES:
        warning(log, f"{label}: more than {MAX_RANGES} version ranges, published for manual review")
        ranges = [""]

    advisory = {"ghsa_id": ghsa_id}
    if cve_id is not False:
        advisory["cve_id"] = cve_id
    advisory["summary"] = summary
    advisory["severity"] = severity
    advisory["published_at"] = published_at
    advisory["vulnerable_versions"] = ranges
    advisory["patched_versions"] = derive_patched_versions(patched_strings)
    return advisory


def applies_to_releases(advisory):
    """False when every range is a commit hash (for example a flaw in the
    project's own CI): it can never match a running release. Anything else that
    names no version, including an empty or rejected range, is kept so a human
    can review it."""
    ranges = advisory["vulnerable_versions"]
    return not ranges or not all(HASH_RE.fullmatch(r) for r in ranges)


def unreadable_placeholder(count, stamp):
    """One entry standing for every advisory that could not be identified, so
    that none disappears without trace. It is published for manual review."""
    return {
        "ghsa_id": UNREADABLE_ID,
        "cve_id": None,
        "summary": f"{count} security advisor{'y' if count == 1 else 'ies'} could not be read. "
        "See the GeoServer security advisories on GitHub.",
        "severity": "high",
        "published_at": stamp,
        "vulnerable_versions": [""],
        "patched_versions": {},
    }


def ranked(advisories):
    """Most important first: the placeholder, then by severity, then newest."""
    ordered = sorted(advisories, key=lambda a: a["ghsa_id"])
    ordered.sort(key=lambda a: a["published_at"], reverse=True)
    ordered.sort(key=lambda a: (a["ghsa_id"] != UNREADABLE_ID, SEVERITY_RANK[a["severity"]]))
    return ordered


def canonical_order(advisories):
    ordered = sorted(advisories, key=lambda a: a["ghsa_id"])
    ordered.sort(key=lambda a: a["published_at"], reverse=True)
    return ordered


def transform_advisories(raw_advisories, stamp=DEFAULT_STAMP, log=print):
    advisories = []
    seen = set()
    unidentified = 0
    for index, raw in enumerate(raw_advisories):
        advisory = transform_advisory(raw, index, stamp, log)
        if advisory is None:
            unidentified += 1
        elif advisory["ghsa_id"] in seen:
            warning(log, f"advisory {advisory['ghsa_id']}: duplicate id, later entry ignored")
        elif not applies_to_releases(advisory):
            seen.add(advisory["ghsa_id"])
            log(f"excluded {advisory['ghsa_id']}: its only ranges are commit hashes")
        else:
            seen.add(advisory["ghsa_id"])
            advisories.append(advisory)
    if unidentified:
        advisories.append(unreadable_placeholder(unidentified, stamp))
    if len(advisories) > MAX_ADVISORIES:
        best = ranked(advisories)
        kept, dropped = best[:MAX_ADVISORIES], best[MAX_ADVISORIES:]
        warning(
            log,
            f"more than {MAX_ADVISORIES} advisories: published the most severe and newest, "
            f"dropped {len(dropped)} (first: {', '.join(a['ghsa_id'] for a in dropped[:5])})",
        )
        advisories = kept
    return canonical_order(advisories)


# -------------------------------------------------------------------- series

def valid_blog(known):
    """Revalidate blog data reused from the previous feed; never trust it. Only a
    post that was actually found (a non-null, valid blog_url) is reused."""
    try:
        url = known["blog_url"]
        return (
            isinstance(url, str)
            and checked_url(url) == url
            and isinstance(known["security_flagged"], bool)
        )
    except (KeyError, TypeError):
        return False


def previous_blog_cache(previous_entries):
    """version -> reusable blog data, from the series entries and their recent
    releases in the previous feed."""
    cache = {}
    if not isinstance(previous_entries, list):
        return cache
    for entry in previous_entries:
        if not isinstance(entry, dict):
            continue
        candidates = [(entry.get("latest_version"), entry)]
        recent = entry.get("recent_releases")
        if isinstance(recent, list):
            candidates += [(item.get("version"), item) for item in recent if isinstance(item, dict)]
        for version, item in candidates:
            if isinstance(version, str) and valid_blog(item):
                cache[version] = {"blog_url": item["blog_url"], "security_flagged": item["security_flagged"]}
    return cache


NO_BLOG = {"blog_url": None, "security_flagged": False}


def build_series_entries(releases, blog_lookup, previous_entries, now, log=print):
    phases = infer_phases(releases)
    latest = latest_per_series(releases)
    synchronized = synchronized_versions(releases)
    cache = previous_blog_cache(previous_entries)
    window_start = now - timedelta(days=RECENT_DAYS)
    phase_order = {"stable": 0, "maintenance": 1, "archive": 2}

    by_series = {}
    for release in releases:
        by_series.setdefault(release.series, []).append(release)

    entries = []
    for series in sorted(latest, key=lambda s: (phase_order[phases[s]], -s[0], -s[1])):
        release = latest[series]
        in_window = sorted(
            (r for r in by_series[series] if r.published >= window_start),
            key=lambda r: (r.published, r.patch),
            reverse=True,
        )
        if len(in_window) > MAX_RECENT_PER_SERIES:
            warning(log, f"series {series_name(series)}: more than {MAX_RECENT_PER_SERIES} recent releases, dropped the oldest")
            in_window = in_window[:MAX_RECENT_PER_SERIES]
        recent = []
        for item in in_window:
            blog = cache.get(item.version) or blog_lookup(item) or NO_BLOG
            recent.append(
                {
                    "version": item.version,
                    "published_at": item.published_at,
                    "blog_url": blog["blog_url"],
                    "security_flagged": blog["security_flagged"],
                    "synchronized_release": item.version in synchronized,
                }
            )
        newest = next((i for i in recent if i["version"] == release.version), None)
        entries.append(
            {
                "series": series_name(series),
                "phase": phases[series],
                "latest_version": release.version,
                "published_at": release.published_at,
                "release_url": release.url,
                "blog_url": newest["blog_url"] if newest else None,
                "security_flagged": newest["security_flagged"] if newest else False,
                "synchronized_release": release.version in synchronized,
                "recent_releases": recent,
            }
        )

    total = sum(len(e["recent_releases"]) for e in entries)
    if total > MAX_RECENT_TOTAL:
        warning(log, f"more than {MAX_RECENT_TOTAL} recent releases in total: dropped the oldest")
        flat = sorted(
            ((item["published_at"], item["version"], index) for index, e in enumerate(entries) for item in e["recent_releases"]),
            reverse=True,
        )
        keep = {(version, index) for _, version, index in flat[:MAX_RECENT_TOTAL]}
        for index, e in enumerate(entries):
            e["recent_releases"] = [i for i in e["recent_releases"] if (i["version"], index) in keep]
            if not any(i["version"] == e["latest_version"] for i in e["recent_releases"]):
                e["blog_url"], e["security_flagged"] = None, False
    return entries


def build_feed(releases, raw_advisories, blog_lookup, previous, now, log=print):
    """previous is the prior feed (or None). `releases` or `raw_advisories` may be
    None when GitHub's response could not be read: the previously published
    section is kept. `generated` only moves when the content changes, so an
    unchanged feed produces no commit."""
    if not isinstance(previous, dict):
        previous = None
    stamp = now.strftime(TIMESTAMP_FORMAT)
    previous_entries = (previous or {}).get("series") or []

    if releases is None:
        if not previous:
            raise FeedError("the release data is unreadable and there is no previous feed to fall back on")
        warning(log, "release data unreadable: keeping the previously published series")
        series = previous["series"]
    else:
        series = build_series_entries(releases, blog_lookup, previous_entries, now, log)
        if len(series) > MAX_SERIES:
            warning(log, f"more than {MAX_SERIES} series: published the first {MAX_SERIES}")
            series = series[:MAX_SERIES]

    if raw_advisories is None:
        if not previous:
            raise FeedError("the advisory data is unreadable and there is no previous feed to fall back on")
        warning(log, "advisory data unreadable: keeping the previously published advisories")
        advisories = previous.get("advisories")
    else:
        advisories = transform_advisories(raw_advisories, stamp, log)

    feed = {
        "source_note": SOURCE_NOTE,
        "schema_version": SCHEMA_VERSION,
        "generated": None,
        "series": series,
        "advisories": advisories,
    }
    if previous and {k: v for k, v in previous.items() if k != "generated"} == {
        k: v for k, v in feed.items() if k != "generated"
    }:
        feed["generated"] = previous["generated"]
    else:
        feed["generated"] = stamp
    return feed


def _render(feed):
    text = json.dumps(feed, indent=2, ensure_ascii=False, allow_nan=False)
    return text.replace("&", "\\u0026").replace("<", "\\u003c").replace(">", "\\u003e") + "\n"


def serialize(feed, log=print):
    """UTF-8, stable key order, one document, with <, > and & escaped inside
    strings. If the file would exceed the size limit, the least important
    advisories are dropped, loudly, until it fits."""
    text = _render(feed)
    if len(text.encode("utf-8")) <= MAX_FILE_BYTES:
        return text
    remaining = ranked(feed["advisories"])
    dropped = []
    while remaining and len(text.encode("utf-8")) > MAX_FILE_BYTES:
        dropped.append(remaining.pop()["ghsa_id"])
        text = _render(dict(feed, advisories=canonical_order(remaining)))
    if len(text.encode("utf-8")) > MAX_FILE_BYTES:
        raise FeedError("the feed exceeds 1 MiB even without advisories")
    warning(log, f"size limit: dropped {len(dropped)} least important advisories (first: {', '.join(dropped[:5])})")
    return text


# ------------------------------------------------------------------- network

def http_get(url, accept, retries=3, backoff=2.0, allow_404=False):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": accept})
    for attempt in range(retries):
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                body = response.read(MAX_API_BYTES + 1)
                if len(body) > MAX_API_BYTES:
                    raise FeedError("a response is too large")
                return body.decode("utf-8")
        except urllib.error.HTTPError as error:
            if allow_404 and error.code == 404:
                return None
            if error.code == 403 and "rate limit" in error.read(4096).decode("utf-8", "ignore").lower():
                raise UpstreamError(
                    "GitHub rate limit hit on an unauthenticated request. The script is "
                    "deliberately unauthenticated; reduce the run frequency instead of adding a token."
                )
            if attempt == retries - 1:
                raise
        except (urllib.error.URLError, TimeoutError):
            if attempt == retries - 1:
                raise
        time.sleep(backoff * (attempt + 1))
    return None


def parse_releases(raw_releases, log=print):
    """A malformed release is skipped with a warning; it does not stop the feed."""
    releases = []
    for index, raw in enumerate(raw_releases):
        try:
            release = parse_release(raw)
        except FeedError as error:
            warning(log, f"release #{index} skipped: {error}")
            continue
        if release:
            releases.append(release)
    return releases


def fetch_json_list(url_for_page, pages, log, what):
    """Return the items of a paged list response, or None if GitHub answered but
    the content cannot be read. Network and HTTP failures propagate and fail the run."""
    items = []
    for page in range(1, pages + 1):
        text = http_get(url_for_page(page), "application/vnd.github+json")
        try:
            batch = parse_api_json(text)
        except FeedError as error:
            warning(log, f"{what} response unreadable: {error}")
            return None
        if not isinstance(batch, list):
            warning(log, f"{what} response is not a list")
            return None
        items.extend(batch)
        if len(batch) < 100:
            break
    return items


def fetch_releases(log=print):
    raw = fetch_json_list(lambda page: GITHUB_RELEASES, 1, log, "release")
    return None if raw is None else parse_releases(raw, log)


def fetch_raw_advisories(log=print):
    return fetch_json_list(lambda page: f"{GITHUB_ADVISORIES}&page={page}", MAX_ADVISORY_PAGES, log, "advisory")


def parse_frontmatter(markdown):
    if not markdown.startswith("---"):
        return {}, markdown
    parts = markdown.split("---", 2)
    if len(parts) < 3:
        return {}, markdown
    categories = []
    in_categories = False
    for line in parts[1].splitlines():
        stripped = line.strip()
        if stripped.startswith("categories:"):
            in_categories = True
        elif in_categories and stripped.startswith("- "):
            categories.append(stripped[2:].strip().lower())
        elif in_categories:
            in_categories = False
    return {"categories": categories}, parts[2]


def build_blog_url(post_filename, categories):
    """Built from a validated post name and validated categories, never copied."""
    match = POST_NAME_RE.fullmatch(post_filename)
    if not match:
        return None
    year, month, day, slug = match.groups()
    usable = [c for c in categories if c != "release" and CATEGORY_RE.fullmatch(c)] or ["announcements"]
    return checked_url(f"https://geoserver.org/{'/'.join(usable)}/{year}/{month}/{day}/{slug}.html")


class BlogLookup:
    """Finds the blog post for a release. The post index is fetched at most once
    per run, and only when a recent release has no confirmed post yet."""

    def __init__(self, now, log=print):
        self.now = now
        self.log = log
        self.names = None

    def _index(self):
        if self.names is None:
            cutoff = self.now - timedelta(days=RECENT_DAYS + 14)
            self.names = []
            listing = parse_api_json(http_get(BLOG_POSTS_API, "application/vnd.github+json"))
            if not isinstance(listing, list):
                raise FeedError("the blog post index is not a list")
            for entry in listing:
                name = entry.get("name") if isinstance(entry, dict) else None
                match = POST_NAME_RE.fullmatch(name) if isinstance(name, str) else None
                if match:
                    year, month, day = (int(g) for g in match.groups()[:3])
                    try:
                        posted = datetime(year, month, day, tzinfo=timezone.utc)
                    except ValueError:
                        continue
                    if posted >= cutoff:
                        self.names.append(name)
        return self.names

    def __call__(self, release):
        """A blog lookup failure leaves the release unconfirmed; it never blocks the feed."""
        try:
            return self._lookup(release)
        except (FeedError, OSError, urllib.error.URLError, ValueError) as error:
            warning(self.log, f"blog lookup for {release.version} failed ({type(error).__name__}); left unconfirmed")
            return None

    def _lookup(self, release):
        if self.now - release.published > timedelta(days=RECENT_DAYS):
            return None
        needle = f"geoserver-{release.version.replace('.', '-')}-released"
        matches = [n for n in self._index() if needle in n]
        if not matches:
            return None
        body = http_get(RAW_POST_BASE + matches[0], "text/plain", allow_404=True)
        if body is None:
            return None
        frontmatter, text = parse_frontmatter(body)
        categories = frontmatter.get("categories", [])
        names_a_cve = bool(CVE_FIND_RE.search(text))
        return {
            "blog_url": build_blog_url(matches[0], categories),
            "security_flagged": bool(
                names_a_cve or "vulnerability" in categories or "Security Considerations" in text
            ),
        }


def load_previous():
    if not OUTPUT_PATH.exists():
        return None
    try:
        previous = parse_api_json(OUTPUT_PATH.read_text(encoding="utf-8"))
    except (FeedError, UnicodeDecodeError, OSError):
        return None
    if (
        not isinstance(previous, dict)
        or previous.get("schema_version") != SCHEMA_VERSION
        or not isinstance(previous.get("series"), list)
        or not isinstance(previous.get("advisories"), list)
        or not isinstance(previous.get("generated"), str)
    ):
        return None
    return previous


def emit(message):
    """Warnings become GitHub Actions annotations. Messages are built from
    escaped, truncated text only, and are restricted to a safe character set."""
    safe = re.sub(r"[^A-Za-z0-9 _.,:;()#'\[\]/=-]", "?", message)[:300]
    print(f"::warning::{safe[len('WARNING '):]}" if safe.startswith("WARNING ") else safe)


def main():
    now = datetime.now(timezone.utc)
    try:
        feed = build_feed(
            fetch_releases(emit), fetch_raw_advisories(emit), BlogLookup(now, emit), load_previous(), now, emit
        )
        text = serialize(feed, emit)
    except FeedError as error:
        print(f"ERROR: version check failed: {error}", file=sys.stderr)
        sys.exit(1)
    except UpstreamError as error:
        print(f"ERROR: GitHub unavailable: {error}", file=sys.stderr)
        sys.exit(1)
    except urllib.error.HTTPError as error:
        print(f"ERROR: GitHub returned HTTP {error.code}", file=sys.stderr)
        sys.exit(1)
    except (urllib.error.URLError, OSError) as error:
        print(f"ERROR: GitHub could not be reached ({type(error).__name__})", file=sys.stderr)
        sys.exit(1)
    except Exception as error:
        print(f"ERROR: version check failed: {type(error).__name__}", file=sys.stderr)
        sys.exit(1)

    existing = OUTPUT_PATH.read_bytes().decode("utf-8", "replace") if OUTPUT_PATH.exists() else None
    if existing != text:
        OUTPUT_PATH.write_bytes(text.encode("utf-8"))
    print(f"{len(feed['series'])} series, {len(feed['advisories'])} advisories, generated {feed['generated']}")


if __name__ == "__main__":
    main()
