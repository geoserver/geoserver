# WMTS GetHistogram Investigation — Session Summary

## Context

Investigation of WMTS multidimensional extension's GetHistogram operation not correctly
handling time filters sent by MapStore. Two separate issues were identified and analyzed.

## Real-world curl example provided by user

```bash
curl 'http://localhost:8081/proxy/?url=http%3A%2F%2Flocalhost%3A8080%2Fgeoserver%2Fgwc%2Fservice%2Fwmts%3Fservice%3DWMTS%26REQUEST%3DGetHistogram%26resolution%3DPT1825H%26histogram%3Dtime%26version%3D1.1.0%26layer%3Dit.geosolutions%3Aligne_metro_3%26tileMatrixSet%3DEPSG%3A4326%26time%3D2023-07-28T20%3A41%3A19.002Z%2F2027-09-26T23%3A42%3A27.726Z'
```

Decoded request parameters:
```
service=WMTS
REQUEST=GetHistogram
resolution=PT1825H
histogram=time
version=1.1.0
layer=it.geosolutions:ligne_metro_3
tileMatrixSet=EPSG:4326
time=2023-07-28T20:41:19.002Z/2027-09-26T23:42:27.726Z
```

Response received:
```xml
<?xml version='1.0' encoding='UTF-8'?>
<Histogram>
  <ows:Identifier>time</ows:Identifier>
  <Domain>2025-01-01T00:40:46.000Z/2025-12-31T22:06:34.000Z/PT1825H</Domain>
  <Values>914,912,913,912,729</Values>
</Histogram>
```

User observation: "the whole Domain despite the date range" — the histogram returns the same
values whether the time filter is set or not.

## Issue 1: Two-part `start/end` format returning "full domain" — NOT A BUG

### Analysis

The user's curl example uses a **two-part** time format: `time=2023-07-28/2027-09-26` (no period).

Code trace confirms the filter IS correctly applied:
1. `DateTimeParser.parse()` detects two-part format → creates `DateRange(2023-07-28, 2027-09-26)`
2. `MultiDimensionalExtension.getDomainRestrictions()` passes it to `DimensionFilterBuilder`
3. `DimensionFilterBuilder.buildDimensionFilter()` creates proper range intersection filter
4. The filter passes all 2025 data because the range 2023–2027 encompasses everything in 2025

### Why it looks unfiltered

The histogram Domain represents the **actual data extent within the filter**, not the filter
bounds. Since the filter (2023-2027) encompasses ALL data (2025), the result is identical to
an unfiltered request. This is correct behavior.

### Tests written to prove this

- `testVectorGetHistogramWithWideRangeTimeFilter` — PASSES: wide two-part range encompassing all
  data returns full domain (expected behavior)
- `testVectorGetHistogramWithNarrowTwoPartTimeFilter` — PASSES: narrow two-part range correctly
  excludes features, proving the filter works

### Suggested verification for user's environment

Test with a narrower range that should exclude some data:
```
time=2025-06-01T00:00:00.000Z/2025-12-31T00:00:00.000Z
```
If the domain no longer starts at 2025-01-01, the filter is working correctly.

### `version=1.1.0` parameter

WMTS spec version is 1.0.0 but using 1.1.0 does NOT affect parser selection — the WMS
TimeKvpParser has no version restriction (null), so it passes through `purgeParsers()`.

## Issue 2: Three-part `start/end/period` exceeding maxTimes — CONFIRMED BUG

### Reproduction

When MapStore sends `time=start/end/period` and the expansion exceeds 100 individual dates:

```
GET /geoserver/gwc/service/wmts?service=WMTS&REQUEST=GetHistogram
    &resolution=P1D&histogram=elevation&resolution=1
    &layer=...&tileMatrixSet=EPSG:4326
    &time=2011-06-01T00:00:00.000Z/2012-06-01T00:00:00.000Z/P1D
```

Server returns OWS ExceptionReport instead of a histogram:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<ows:ExceptionReport xmlns:ows="http://www.opengis.net/ows" version="1.0.0">
  <ows:Exception exceptionCode="InvalidParameterValue" locator="time">
    <ows:ExceptionText>More than 100 times specified in the request, bailing out.</ows:ExceptionText>
  </ows:Exception>
</ows:ExceptionReport>
```

Content-Type is `application/xml` instead of `text/xml`.

### Root cause

1. `MultiDimensionalExtension.getConveyor()` calls `KvpUtils.parse()` but **ignores** errors
2. `KvpUtils.parse()` delegates to WMS `TimeKvpParser` → `TimeParser` → `DateTimeParser`
3. `DateTimeParser` expands `start/end/period` into individual dates
4. `TimeParser.checkMaxTimes()` throws `ServiceException` when count exceeds 100
5. Exception either propagates as OWS ExceptionReport OR is caught silently leaving raw String
   in the KvpMap, which then creates a nonsensical equality filter

### Tests confirming the bug

- `testVectorGetHistogramWithLargeStartEndPeriodExceedsMaxTimes` — FAILS (expected):
  366+ dates → OWS error instead of histogram
- `testVectorGetHistogramWithLargeStartEndPeriodShouldExcludeFeatures` — FAILS (expected):
  365 dates all before data → OWS error instead of empty histogram

### Proposed fix (not yet implemented)

Fix in `MultiDimensionalExtension.getDomainRestrictions()` (line 311):
- When the restriction is still a raw String (failed parse), detect the `start/end/period`
  format and parse just the start/end boundaries into a `DateRange`
- `DimensionFilterBuilder` already handles `Range` objects correctly, creating proper
  intersection filters

This avoids the maxTimes limit entirely and produces correct range-based filtering.

## Files modified during investigation

### Test files (new tests added)

`src/extension/wmts-multi-dimensional/src/test/java/org/geoserver/gwc/wmts/MultiDimensionalExtensionTest.java`
- 14 new integration tests added covering various filter scenarios
- 12 PASS (proving filter works for various formats)
- 2 FAIL (documenting the maxTimes bug)

`src/extension/wmts-multi-dimensional/src/test/java/org/geoserver/gwc/wmts/VectorTimeDimensionTest.java`
- 2 new unit tests added

`src/extension/wmts-multi-dimensional/src/test/resources/TimeElevationWithStartEnd.properties`
- New test data file for range dimension testing

### Documentation files

- `WMTS_HISTOGRAM_BUG_FINDINGS.md` — detailed technical analysis
- `WMTS_SESSION_SUMMARY.md` — this file

## Key source files examined

| File | Lines | Purpose |
|------|-------|---------|
| `MultiDimensionalExtension.java` | 132-141, 311-324, 440-448 | getConveyor, getDomainRestrictions, appendDomainRestrictionsFilter |
| `DimensionFilterBuilder.java` | 64-92 | Range filter (L68-78), point filter (L80-88) |
| `KvpUtils.java` | 403-453, 463-477 | parse() with silent error catching, purgeParsers() |
| `TimeParser.java` | 23-46 | maxTimes=100 limit, checkMaxTimes() |
| `TimeKvpParser.java` (OWS) | 21-44 | Delegates to TimeParser |
| `TimeKvpParser.java` (WMS) | 11-30 | Uses wms.getMaxRequestedDimensionValues() |
| `Dimension.java` | 174-244, 277-302 | getHistogram(), getRangeHistogram() |
| `SimpleConveyor.java` | 42 | getParameter() uppercases key |
| `KvpMap.java` | - | Case-insensitive HashMap |

## Next step

Implement the fix for Issue 2 (three-part format exceeding maxTimes) in
`MultiDimensionalExtension.getDomainRestrictions()`.
