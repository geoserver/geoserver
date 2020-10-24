/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs.h3;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.GeoCoord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.geotools.data.store.EmptyIterator;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.Zone;
import org.geotools.dggs.gstore.DGGSStore;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.LiteCoordinateSequenceFactory;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.predicate.RectangleContains;
import org.locationtech.jts.operation.predicate.RectangleIntersects;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

public class H3DGGSInstance implements DGGSInstance {

    final H3Core h3;
    final GeometryFactory gf = new GeometryFactory(new LiteCoordinateSequenceFactory());
    final Set<Long> northPoleZones;
    final Set<Long> southPoleZones;

    H3DGGSInstance(H3Core h3) {
        this.h3 = h3;

        int[] resolutions = getResolutions();
        this.northPoleZones =
                Arrays.stream(resolutions)
                        .mapToObj(r -> getZone(90, 0, r).id)
                        .collect(Collectors.toSet());
        this.southPoleZones =
                Arrays.stream(resolutions)
                        .mapToObj(r -> getZone(-90, 0, r).id)
                        .collect(Collectors.toSet());
    }

    @Override
    public String getIdentifier() {
        return "H3";
    }

    @Override
    public void close() {
        // nothing to do here
    }

    @Override
    public int[] getResolutions() {
        int[] result = new int[16];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        return result;
    }

    @Override
    public Zone getZone(String id) throws IllegalArgumentException {
        try {
            long lid = h3.stringToH3(id);
            return new H3Zone(this, lid);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not build zone from id, is the id valid?", e);
        }
    }

    @Override
    public H3Zone getZone(double lat, double lon, int resolution) {
        long id = h3.geoToH3(lat, lon, resolution);
        return new H3Zone(this, id);
    }

    @Override
    public Iterator<Zone> zonesFromEnvelope(
            Envelope envelope, int targetResolution, boolean compact) {
        Envelope intersection = envelope.intersection(WORLD);
        if (intersection.isNull()) {
            return new EmptyIterator();
        }
        // When crossing the dateline, the parent and child are not always sitting on the
        // same side of the dateline, need to drill down.
        // TODO: determine a safe distance from the envelope to the dateline at which this
        // won't cause mis-matches (depends on the current resolution)
        // Also, a parent does not cover the exact same area as
        // the children, so we need to check if a 1-ring overlaps to dedice whether to skip
        // a cell.
        if (compact) {
            // we pick parents when they are contained in the envelope, there is a start of
            // compaction
            // but it's not complete
            List<Long> zones = new ArrayList<>();
            new H3ZoneIterator<>(
                            h3,
                            id -> {
                                // drill if it's a parent that is not fully contained in the target
                                // envelope, but still overlaps it, or is spanning the dateline
                                int r = h3.h3GetResolution(id);
                                return r < targetResolution
                                        && (ringOverlaps(id, envelope)
                                                && !containedInEnvelope(id, envelope));
                            },
                            id -> {
                                // accept if at target resolution and overlaps the envelope, or it's
                                // a
                                // parent fully contained in the envelope
                                int r = h3.h3GetResolution(id);
                                return (r == targetResolution && overlaps((long) id, envelope))
                                        || (r < targetResolution
                                                && containedInEnvelope(id, envelope));
                            },
                            id -> id)
                    .forEachRemaining(id -> zones.add(id));
            // full compaction using H3 library
            try {
                List<Long> compacted = h3.compact(zones);
                return compacted.stream().map(id -> (Zone) new H3Zone(this, id)).iterator();
            } catch (IllegalArgumentException e) {
                // we might get a "bad input to compact" with no explanation as to why...
                return zones.stream().map(id -> (Zone) new H3Zone(this, id)).iterator();
            }
        } else {
            return new H3ZoneIterator<>(
                    h3,
                    id ->
                            h3.h3GetResolution(id) < targetResolution
                                    && (ringOverlaps(id, envelope) || datelineCrossing(id)),
                    id ->
                            h3.h3GetResolution(id) == targetResolution
                                    && overlaps((long) id, envelope),
                    id -> new H3Zone(this, id));
        }
    }

    private boolean datelineCrossing(long id) {
        return new H3Zone(this, id).overlapsDateline();
    }

    private boolean ringOverlaps(Long zoneId, Envelope envelope) {
        if (overlaps(zoneId, envelope)) {
            return true;
        }

        // check the ring too
        List<Long> ringItems = h3.kRing(zoneId, 1);
        return ringItems.stream().anyMatch(id -> overlaps(id, envelope));
    }

    private boolean overlaps(Long zoneId, Envelope envelope) {
        // quick test, check center is in
        GeoCoord center = h3.h3ToGeo(zoneId);
        if (envelope.contains(center.lng, center.lat)) return true;

        // get the fixed boundary, check if there is an overlap
        H3Zone zone = new H3Zone(this, zoneId);
        Polygon polygon = zone.getBoundary();
        if (boundaryIntersects(envelope, polygon)) {
            return true;
        }

        return false;
    }

    private boolean boundaryIntersects(Envelope envelope, Polygon polygon) {
        if (!polygon.getEnvelopeInternal().intersects(envelope)) return false;

        // ok, go for a full test then...
        RectangleIntersects intersects = new RectangleIntersects(JTS.toGeometry(envelope));
        return intersects.intersects(polygon);
    }

    private boolean ringContained(Long zoneId, Envelope envelope) {
        if (!containedInEnvelope(zoneId, envelope)) {
            return false;
        }

        // check the ring too
        List<Long> ringItems = h3.kRing(zoneId, 1);
        return ringItems.stream().allMatch(id -> containedInEnvelope(id, envelope));
    }

    private boolean containedInEnvelope(Long zoneId, Envelope envelope) {
        // get the boundary, check if there is an overlap
        H3Zone zone = new H3Zone(this, zoneId);
        Polygon polygon = zone.getBoundary();
        if (envelope.contains(polygon.getEnvelopeInternal())) return true;

        // ok, go for a full test then...
        RectangleContains contains = new RectangleContains(JTS.toGeometry(envelope));
        return contains.contains(polygon);
    }

    @Override
    public long countZonesFromEnvelope(Envelope envelope, int resolution) {
        Envelope intersection = envelope.intersection(WORLD);
        if (intersection.isNull()) {
            return 0;
        }
        // abuse the iteration machinery to count fast
        AtomicLong counter = new AtomicLong();
        H3ZoneIterator<AtomicLong> iterator =
                new H3ZoneIterator<>(
                        h3,
                        // drill down if the zone is overlapping but not ring contained, need better
                        // accuracy
                        id ->
                                h3.h3GetResolution(id) < resolution
                                        // do not test single polygon, the cell might not be
                                        // overlapping the search area, but one of its child could
                                        // however if all its neibords are also contained, this one
                                        // is inside and can be counted quickly
                                        && !ringContained((long) id, envelope),
                        // if the zone is ring contained, just count all of its childern
                        id -> {
                            int currentResolution = h3.h3GetResolution(id);
                            if (currentResolution == resolution) {
                                if (overlaps(id, envelope)) {
                                    counter.addAndGet(1);
                                    return true;
                                }
                            } else if (ringContained(id, envelope)) {
                                counter.addAndGet(
                                        childrenCount(id, resolution - currentResolution));
                            }
                            return false;
                        },
                        // just return the current counter value
                        id -> counter);
        // make it visit
        while (iterator.hasNext()) iterator.next();
        return counter.get();
    }

    private long childrenCount(Long zoneId, int depth) {
        if (!h3.h3IsPentagon(zoneId)) {
            // easy, each hexagon contains 7 children, recursively
            return (long) Math.pow(7, depth);
        } else {
            return pentagonCount(depth);
        }
    }

    private long pentagonCount(int depth) {
        if (depth == 0) return 0;
        // each pentagon has 1 pentagon child in the middle, and 5 hexagons around it
        return 1 + pentagonCount(depth - 1) + 5 * ((long) Math.pow(7, depth - 1));
    }

    @Override
    public List<AttributeDescriptor> getExtraProperties() {
        List<AttributeDescriptor> result = new ArrayList<>();
        AttributeTypeBuilder builder = new AttributeTypeBuilder();
        builder.setName("shape");
        builder.setBinding(String.class);
        result.add(builder.buildDescriptor("shape"));

        return result;
    }

    @Override
    public Iterator<Zone> neighbors(String id, int radius) {
        // Using H3 facilities. Upside fast and accurate (considering dateline and pole neighbors
        // too), downside, will quickly go OOM, radius should be limited
        long h3Id = this.h3.stringToH3(id);
        return this.h3
                .kRing(h3Id, radius)
                .stream()
                .filter(zoneId -> h3Id != zoneId)
                .map(zoneId -> (Zone) new H3Zone(this, zoneId))
                .iterator();
    }

    @Override
    public Iterator<Zone> children(String zoneId, int resolution) {
        Zone zone = getZone(zoneId);
        if (zone.getResolution() >= resolution) return new EmptyIterator();

        // all the children of the given cell
        return new H3ZoneIterator<>(
                h3,
                id -> h3.h3GetResolution(id) < resolution,
                id -> h3.h3GetResolution(id) == resolution,
                id -> new H3Zone(this, id),
                Arrays.asList(h3.stringToH3(zoneId)));
    }

    @Override
    public Iterator<Zone> parents(String zoneId) {
        long id = h3.stringToH3(zoneId);
        return new H3ParentIterator(id, this);
    }

    @Override
    public Zone point(Point point, int resolution) {
        long id = h3.geoToH3(point.getY(), point.getX(), resolution);
        return new H3Zone(this, id);
    }

    @Override
    public Iterator<Zone> polygon(Polygon polygon, int resolution, boolean compact) {
        List<GeoCoord> shell = getGeoCoords(polygon.getExteriorRing());
        List<List<GeoCoord>> holes =
                IntStream.range(0, polygon.getNumInteriorRing())
                        .mapToObj(i -> getGeoCoords(polygon.getInteriorRingN(i)))
                        .collect(Collectors.toList());
        // TODO replace with a walk similar to RHealPix, this might be faster for
        // small numbers, but it's memory bound and gets slow
        // pretty quickly due to memory pressure,
        List<Long> zones = h3.polyfill(shell, holes, resolution);
        if (compact) {
            zones = h3.compact(zones);
        }
        return zones.stream().map(id -> (Zone) new H3Zone(this, id)).iterator();
    }

    private List<GeoCoord> getGeoCoords(LinearRing ring) {
        return Arrays.stream(ring.getCoordinates())
                .map(c -> new GeoCoord(c.y, c.x))
                .collect(Collectors.toList());
    }

    @Override
    public Filter getChildFilter(FilterFactory2 ff, String zoneId, int resolution, boolean upTo) {
        long id = h3.stringToH3(zoneId);
        H3Index idx = new H3Index(id);
        long lowest = idx.lowestIdChild(resolution);
        long highest = idx.highestIdChild(resolution);
        String lowestId = h3.h3ToString(lowest);
        String highestId = h3.h3ToString(highest);
        String prefix = getPrefix(lowestId, highestId);
        Filter prefixFilter =
                ff.like(ff.property(DGGSStore.ZONE_ID), prefix + "%", "%", "?", "\\", true);
        Filter matchFilter =
                ff.between(
                        ff.property(DGGSStore.ZONE_ID),
                        ff.literal(lowestId),
                        ff.literal(highestId));
        // return ff.and(prefixFilter, matchFilter);
        return matchFilter;
    }

    private String getPrefix(String a, String b) {
        int length = a.length();
        for (int i = 0; i < length; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.substring(0, i);
            }
        }
        return a;
    }
}
