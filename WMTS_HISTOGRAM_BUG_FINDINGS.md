# WMTS GetHistogram Bug Analysis

## Summary

The WMTS multidimensional extension's GetHistogram operation fails when the `time` parameter
is in `start/end/period` format and expands to more than 100 dates. Instead of returning a
filtered histogram, the server returns an OWS ExceptionReport. This affects MapStore, which
sends this format when displaying time-based histograms.

The two-part `start/end` format (no period) works correctly — tests prove the filter is applied.
The real-world MapStore case where the histogram appeared "unfiltered" was because the requested
time range encompassed all data, so the result was identical to the unfiltered result.

## Confirmed Bug: Three-part `start/end/period` exceeding maxTimes

### What happens

When MapStore sends `time=start/end/period` format and the expansion generates >100 individual
dates, the server returns an OWS ExceptionReport instead of a histogram:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ows:ExceptionReport xmlns:ows="http://www.opengis.net/ows" version="1.0.0">
  <ows:Exception exceptionCode="InvalidParameterValue" locator="time">
    <ows:ExceptionText>More than 100 times specified in the request, bailing out.</ows:ExceptionText>
  </ows:Exception>
</ows:ExceptionReport>
```

Content-Type is `application/xml` instead of the expected `text/xml` for a valid histogram.

### Root cause chain

1. **`MultiDimensionalExtension.getConveyor()`** (line 132-141): calls `KvpUtils.parse()` but
   IGNORES the returned errors list
2. **`KvpUtils.parse()`** (line 403-453): finds the WMS `TimeKvpParser` (registered with no
   service restriction), delegates parsing. Catches `Throwable` silently at line 438
3. **`TimeParser.checkMaxTimes()`**: throws `ServiceException("More than 100 times specified")`
   when expansion exceeds `DEFAULT_MAX_ELEMENTS_TIMES_KVP = 100`
4. **Two possible outcomes**:
   - **OWS ExceptionReport** (confirmed in tests): exception propagates through GWC dispatch chain
   - **Silently dropped filter**: if caught, the raw String `"2024-01-01/2024-12-31/P1D"` stays
     in the KvpMap and `DimensionFilterBuilder` creates a nonsensical equality filter

### Tested and confirmed

- `testVectorGetHistogramWithLargeStartEndPeriodExceedsMaxTimes`: 200+ dates → OWS error (FAILS as expected)
- `testVectorGetHistogramWithLargeStartEndPeriodShouldExcludeFeatures`: 365 dates before data → OWS error (FAILS as expected)

## NOT a Bug: Two-part `start/end` format returning "full domain"

### Investigation

The real-world MapStore request uses two-part format:
```
time=2023-07-28T20:41:19.002Z/2027-09-26T23:42:27.726Z
```

Code trace confirms the filter IS correctly applied:
1. `DateTimeParser.parse()` detects two-part format → creates `DateRange(2023-07-28, 2027-09-26)`
2. `getDomainRestrictions()` at line 316: `conveyor.getParameter("time")` returns the Collection
3. `appendDomainRestrictionsFilter()` unwraps the DateRange from the Collection
4. `DimensionFilterBuilder.buildDimensionFilter()`: `DateRange instanceof Range` → creates proper
   intersection filter: `startAttr <= 2027-09-26 AND endAttr >= 2023-07-28`
5. All data (2025) passes this filter because 2023 < 2025 < 2027

### Tests prove this

- **`testVectorGetHistogramWithWideRangeTimeFilter` (PASSES)**: Two-part `time=2010-01-01/2015-01-01`
  encompassing all data → domain = full extent, total = 4 features. Expected behavior.
- **`testVectorGetHistogramWithNarrowTwoPartTimeFilter` (PASSES)**: Two-part `time=2012-02-12/2012-02-13`
  excluding 3 of 4 features → only 1 feature matches, domain is restricted. **Filter works correctly.**
- **`testVectorGetTimeHistogramWithRangeFilterInterval` (PASSES)**: Two-part range on range dimension
  → correctly excludes Feature 0

### Why it appears "unfiltered"

The histogram Domain represents the **actual data extent within the filter**, not the filter bounds.
When the filter range (2023-2027) encompasses all data (2025), the domain equals the full data
extent. This is correct behavior, not a bug.

To verify in the user's environment: test with `time=2025-06-01/2025-12-31`. If the domain still
starts at 2025-01-01, there's a separate bug. If it starts at ~2025-06-01, the filter is working.

### `version=1.1.0` parameter impact

The user's request has `version=1.1.0` (WMTS spec is 1.0.0). Investigation confirms this does
NOT affect parser selection:
- WMS TimeKvpParser has no service or version restriction (both are null)
- `KvpUtils.purgeParsers()` only removes parsers with non-null version that doesn't match
- Null version always passes through

## Reproduction

### Real-world reproduction (MapStore + GeoServer)

```
GET /geoserver/gwc/service/wmts?service=WMTS&REQUEST=GetHistogram&resolution=PT1825H
    &histogram=time&version=1.1.0&layer=it.geosolutions:ligne_metro_3
    &tileMatrixSet=EPSG:4326
    &time=2023-07-28T20:41:19.002Z/2027-09-26T23:42:27.726Z
```

Response:
```xml
<Histogram>
  <ows:Identifier>time</ows:Identifier>
  <Domain>2025-01-01T00:40:46.000Z/2025-12-31T22:06:34.000Z/PT1825H</Domain>
  <Values>914,912,913,912,729</Values>
</Histogram>
```

This is **correct** — the requested range (2023-2027) encompasses all 2025 data.

## Key Files

### Production code

| File | Role |
|------|------|
| `src/extension/wmts-multi-dimensional/src/main/java/org/geoserver/gwc/wmts/MultiDimensionalExtension.java` | Main extension: getConveyor() (L132), getDomainRestrictions() (L311), appendDomainRestrictionsFilter() (L440) |
| `src/main/src/main/java/org/geoserver/data/DimensionFilterBuilder.java` | Builds OGC filters from dimension restrictions. Supports Range objects (L68-78) and point values (L80-88) |
| `src/ows/src/main/java/org/geoserver/ows/util/KvpUtils.java` | KVP parameter parsing with silent error catching (L438) |
| `src/ows/src/main/java/org/geoserver/ows/kvp/TimeParser.java` | Time parsing with maxTimes=100 limit |
| `src/ows/src/main/java/org/geoserver/ows/kvp/TimeKvpParser.java` | OWS TimeKvpParser, delegates to TimeParser |
| `src/wms-core/src/main/java/org/geoserver/wms/kvp/TimeKvpParser.java` | WMS TimeKvpParser, uses wms.getMaxRequestedDimensionValues() |
| `src/extension/wmts-multi-dimensional/src/main/java/org/geoserver/gwc/wmts/dimensions/Dimension.java` | getHistogram() (L174), getRangeHistogram() (L277) |
| `src/extension/wmts-multi-dimensional/src/main/java/org/geoserver/gwc/wmts/Domains.java` | getHistogramValues() (L113) |

### Test code

| File | Role |
|------|------|
| `src/extension/wmts-multi-dimensional/src/test/java/org/geoserver/gwc/wmts/MultiDimensionalExtensionTest.java` | Integration tests (14 new tests added) |
| `src/extension/wmts-multi-dimensional/src/test/java/org/geoserver/gwc/wmts/VectorTimeDimensionTest.java` | Unit tests (2 new tests added) |
| `src/extension/wmts-multi-dimensional/src/test/resources/TimeElevationWithStartEnd.properties` | Test data with range dimensions |

### Test data (TimeElevationWithStartEnd.properties)

```
Feature 0: startTime=2012-02-11T00:00, endTime=2012-02-11T11:00, startElev=1, endElev=2
Feature 1: startTime=2012-02-12T00:00, endTime=2012-02-12T10:00, startElev=2, endElev=3
Feature 2: startTime=2012-02-11T00:00, endTime=2012-02-13T00:00, startElev=3, endElev=4
Feature 3: startTime=2012-02-11T00:00, endTime=2012-02-13T00:00, startElev=5, endElev=7
```

## Tests Added

### Integration tests (MultiDimensionalExtensionTest.java)

Tests that PASS (filter works correctly):

1. **testRasterGetHistogramWithSameElevationFilter** - elevation=100 filter on elevation histogram (raster)
2. **testRasterGetHistogramWithSameTimeFilter** - time filter on time histogram (raster)
3. **testVectorGetHistogramWithSameTimeFilter** - time=2012-02-11 on time histogram (vector, point dims)
4. **testVectorGetElevationHistogramFilteredByTime** - time=2012-02-11 on elevation histogram (vector, point dims)
5. **testVectorGetHistogramWithRangeDimensionsFilteredByElevation** - elevation=1 on time histogram (vector, range dims)
6. **testVectorGetElevationHistogramWithRangeTimeFilteredByTime** - time=2012-02-12 on elevation histogram (vector, range dims)
7. **testVectorGetTimeHistogramWithRangeTimeSameDimensionFilter** - time=2012-02-11 same-dimension filter (range dims)
8. **testVectorGetTimeHistogramWithRangeFilterInterval** - time=02-11T12:00/02-12 range filter (range dims)
9. **testVectorGetHistogramWithStartEndPeriodTimeFilter** - time=02-11/02-11/P1D (small start/end/period, point dims)
10. **testVectorGetHistogramWithStartEndPeriodOnRangeDimension** - time=02-12/02-12/P1D (small start/end/period, range dims)
11. **testVectorGetHistogramWithWideRangeTimeFilter** - time=2010/2015 two-part wide range, proves full domain is correct when range encompasses all data
12. **testVectorGetHistogramWithNarrowTwoPartTimeFilter** - time=02-12/02-13 two-part narrow range, proves filter works (only 1 of 4 features matches)

Tests that FAIL (documenting the confirmed bug):

13. **testVectorGetHistogramWithLargeStartEndPeriodExceedsMaxTimes** - time=2011-06-01/2012-06-01/P1D (366+ dates), server returns OWS error instead of histogram
14. **testVectorGetHistogramWithLargeStartEndPeriodShouldExcludeFeatures** - time=2010-01-01/2010-12-31/P1D (365 dates, all before data), server returns OWS error instead of histogram

### Unit tests (VectorTimeDimensionTest.java)

15. **testGetHistogramWithTimeFilter** - time filter restricts histogram domain
16. **testGetHistogramWithRangeValuesAndTimeFilter** - range dimension with time filter

## DDoS Concern

The `getRangeHistogram()` method (Dimension.java:277-302) runs **one separate database query per
histogram bucket**. With `HISTOGRAM_MAX_THRESHOLD` defaulting to 10,000 buckets, a single
GetHistogram request can trigger up to 10,000 DB queries. There is no authentication or rate
limiting on this endpoint.

## Proposed Fix

The WMTS multidimensional extension should NOT rely on `TimeKvpParser` to expand `start/end/period`
into individual dates. For GetHistogram/DescribeDomains/GetDomainValues operations, it only needs
the **start and end boundaries** as a range filter.

`DimensionFilterBuilder` already supports `Range` objects (line 68-78), creating proper
intersection filters: `startAttribute <= rangeMax AND endAttribute >= rangeMin`.

### Fix location

`MultiDimensionalExtension.getDomainRestrictions()` (line 311) or `getConveyor()` (line 132).

### Fix approach

When the `time` parameter is in `start/end/period` format:
1. Parse only the start and end boundaries
2. Create a `DateRange(start, end)`
3. Pass to `DimensionFilterBuilder` which already handles Range objects correctly

This avoids the maxTimes limit entirely and produces correct range-based filtering regardless
of the period length.

### Alternative fix approaches

1. **Register a custom KvpParser** in the WMTS extension that treats `start/end/period` as a
   DateRange instead of expanding it
2. **In getDomainRestrictions()**: detect when restriction is a raw String (failed parse) or a
   large Collection of dates (successful but expensive expansion), and collapse to a DateRange
3. **Increase the maxTimes limit** for WMTS requests (bandaid, doesn't fix the underlying issue
   of creating 365+ OR filters)
