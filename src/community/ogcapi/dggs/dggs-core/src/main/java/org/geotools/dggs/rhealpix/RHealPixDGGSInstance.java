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
package org.geotools.dggs.rhealpix;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.geotools.dggs.rhealpix.RHealPixUtils.setCellId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jep.JepException;
import jep.SharedInterpreter;
import org.geotools.data.store.EmptyIterator;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.Zone;
import org.geotools.dggs.gstore.DGGSStore;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.filter.function.FilterFunction_offset;
import org.geotools.geometry.jts.JTS;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.locationtech.jts.operation.predicate.RectangleContains;
import org.locationtech.jts.operation.predicate.RectangleIntersects;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

public class RHealPixDGGSInstance implements DGGSInstance {

    static final Logger LOGGER = Logging.getLogger(RHealPixDGGSInstance.class);
    private final String identifier;

    final JEPWebRuntime runtime;
    final GeometryFactory gf = new GeometryFactory();
    final Set<String> northPoleZones;
    final Set<String> southPoleZones;
    final SoftValueHashMap<String, RHealPixZone> zoneCache = new SoftValueHashMap<>(10000);

    public RHealPixDGGSInstance(JEPWebRuntime runtime, String identifier) {
        this.runtime = runtime;
        this.identifier = identifier;

        int[] resolutions = getResolutions();
        this.northPoleZones =
                Arrays.stream(resolutions)
                        .mapToObj(r -> getZone(0, 90, r).getId())
                        .collect(Collectors.toSet());
        this.southPoleZones =
                Arrays.stream(resolutions)
                        .mapToObj(r -> getZone(0, -90, r).getId())
                        .collect(Collectors.toSet());
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void close() {
        this.runtime.dispose();
    }

    @Override
    public int[] getResolutions() {
        // is it really 14? Just a guess for the moment
        int[] result = new int[14];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        return result;
    }

    @Override
    public RHealPixZone getZone(String id) {
        RHealPixZone zone = zoneCache.get(id);
        if (zone == null) {
            try {
                @SuppressWarnings("PMD.CloseResource") // the runtime is responsible for closing it
                SharedInterpreter interpreter = runtime.getInterpreter();
                setCellId(interpreter, "id", id);
                interpreter.exec("c = dggs.cell(id)");
                zone = new RHealPixZone(this, id);
                zoneCache.put(id, zone);
            } catch (JepException e) {
                throw new IllegalArgumentException("Invalid zone identifier '" + id + "'", e);
            }
        }
        return zone;
    }

    @Override
    public Zone getZone(double lat, double lon, int resolution) {
        return runtime.runSafe(
                interpreter -> {
                    interpreter.set("p", Arrays.asList(lat, lon));
                    interpreter.set("r", (Integer) resolution);
                    interpreter.exec("c = dggs.cell_from_point(r, p, False)");
                    List<Object> idList = interpreter.getValue("c.suid", List.class);
                    String id = toZoneId(idList);
                    return new RHealPixZone(this, id);
                });
    }

    private String toZoneId(List<Object> idList) {
        String id = idList.stream().map(o -> String.valueOf(o)).collect(Collectors.joining(""));
        return id;
    }

    @Override
    public Iterator<Zone> zonesFromEnvelope(
            Envelope envelope, int targetResolution, boolean compact) {
        // WAY USING DIRECT LIBRARY CALLS. Faster, but memory bound.
        //        return runtime.runSafe(
        //                interpreter -> {
        //                    Envelope safe = envelope.intersection(DGGSInstance.WORLD);
        //                    interpreter.set("nw", Arrays.asList(safe.getMinX(), safe.getMaxY()));
        //                    interpreter.set("se", Arrays.asList(safe.getMaxX(), safe.getMinY()));
        //                    interpreter.set("r", (Integer) resolution);
        //                    interpreter.exec("cells = dggs.cells_from_region(r, nw, se, False)");
        //                    List<List<String>> idList = interpreter.getValue("cells", List.class);
        //                    return idList.stream()
        //                            .flatMap(l -> l.stream())
        //                            .map(id -> (Zone) new RHealPixZone(this, id))
        //                            .iterator();
        //                });
        Envelope intersection = envelope.intersection(WORLD);
        if (intersection.isNull()) {
            return new EmptyIterator();
        }
        if (compact) {
            // makes the representation more compact, and yet not as compact as it could be,
            // a parent is returned only if fully contained, but there are cases where the
            // envelope does not contain the parent while still overlapping all children
            List<String> identifiers = new ArrayList<>();
            new RHealPixZoneIterator<>(
                            this,
                            zone -> {
                                int r = zone.getResolution();
                                if (r >= targetResolution) return false;
                                Polygon boundary = zone.getBoundary();
                                return overlaps(boundary, envelope, true)
                                        && !contained(boundary, envelope, true);
                            },
                            zone -> {
                                int r = zone.getResolution();
                                Polygon boundary = zone.getBoundary();
                                return (r == targetResolution
                                                && overlaps(boundary, envelope, false))
                                        || (r < targetResolution
                                                && contained(boundary, envelope, true));
                            },
                            zone -> zone.getId())
                    .forEachRemaining(id -> identifiers.add(id));
            compact(identifiers);
            return identifiers.stream().map(id -> (Zone) new RHealPixZone(this, id)).iterator();
        } else {
            return new RHealPixZoneIterator<>(
                    this,
                    zone -> {
                        return zone.getResolution() < targetResolution
                                && (overlaps(zone.getBoundary(), envelope, true));
                    },
                    zone -> {
                        return zone.getResolution() == targetResolution
                                && overlaps(zone.getBoundary(), envelope, false);
                    },
                    zone -> (Zone) zone);
        }
    }

    public void compact(List<String> identifiers) {
        Collections.sort(identifiers);
        int r = maxResolution(identifiers);
        while (r > 0) {
            if (compact(identifiers, r)) r--;
            else break;
        }
    }

    private int maxResolution(List<String> identifiers) {
        return identifiers.stream().mapToInt(id -> id.length()).max().getAsInt() - 1;
    }

    boolean compact(List<String> identifiers, int resolution) {
        int position = identifiers.size() - 1;
        boolean compacted = false;
        String parent = null;
        int count = 0;
        while (position >= 0) {
            String id = identifiers.get(position);
            if ((id.length() - 1) != resolution) {
                parent = null;
                count = 0;
            }
            if (id.length() > 1) {
                String idParent = id.substring(0, id.length() - 1);
                if (parent == null || !idParent.equals(parent)) {
                    parent = idParent;
                    count = 1;
                } else {
                    count++;
                    if (count == 9) {
                        for (int i = 1; i < 9; i++) {
                            identifiers.remove(position + 1);
                        }
                        identifiers.set(position, parent);
                        compacted = true;
                        parent = null;
                        count = 0;
                    }
                }
            } else {
                parent = null;
            }
            position--;
        }
        return compacted;
    }

    private boolean overlaps(Polygon polygon, Envelope envelope, boolean testAcrossDateline) {
        // use a specialized rectangle intersection, should be faster
        RectangleIntersects tester = new RectangleIntersects(JTS.toGeometry(envelope));
        return testRelation(polygon, envelope, p -> tester.intersects(p), testAcrossDateline);
    }

    private boolean contained(Polygon polygon, Envelope envelope, boolean testAcrossDateline) {
        // use a specialized rectangle operation, should be faster
        RectangleContains tester = new RectangleContains(JTS.toGeometry(envelope));
        return testRelation(polygon, envelope, p -> tester.contains(p), testAcrossDateline);
    }

    /** Won't quite work with every type of relation, but does with contains/intersects */
    private boolean testRelation(
            Polygon polygon,
            Envelope envelope,
            Function<Polygon, Boolean> relation,
            boolean testAcrossDateline) {
        if (relation.apply(polygon)) {
            return true;
        } else if (!testAcrossDateline) {
            return false;
        }

        // The polygon might be crossing the dateline, in that case, we want to test also the
        // other representation at the other side
        Polygon otherSide = flipDatelineSide(polygon);
        if (otherSide == null) return false;
        return relation.apply(otherSide);
    }

    private <T extends Geometry> T flipDatelineSide(T g) {
        // check if the polygon is sitting across the dateline
        Envelope polyEnvelope = g.getEnvelopeInternal();
        double minX = polyEnvelope.getMinX();
        double maxX = polyEnvelope.getMaxX();
        if (minX >= -180 && maxX <= 180) return null;

        double offset = -360;
        if (minX < -180) offset = 360;
        T otherSide = (T) g.copy();
        otherSide.apply(new FilterFunction_offset.OffsetOrdinateFilter(offset, 0));
        return otherSide;
    }

    private boolean testDisjoint(PreparedPolygon reference, Geometry test) {
        boolean disjoint = reference.disjoint(test);
        if (!disjoint) return disjoint;

        // check the potential representation at the other end of the dateline
        Geometry otherSide = flipDatelineSide(test);
        if (otherSide == null) return true;
        return reference.disjoint(otherSide);
    }

    private boolean testContains(PreparedPolygon reference, Geometry test) {
        boolean contains = reference.contains(test);
        if (contains) return contains;

        // check the potential representation at the other end of the dateline
        Geometry otherSide = flipDatelineSide(test);
        if (otherSide == null) return false;
        return reference.contains(otherSide);
    }

    @Override
    public long countZonesFromEnvelope(Envelope envelope, int resolution) {
        Envelope intersection = envelope.intersection(WORLD);
        if (intersection.isNull()) {
            return 0;
        }
        AtomicLong counter = new AtomicLong();
        RHealPixZoneIterator<AtomicLong> iterator =
                new RHealPixZoneIterator<>(
                        this,
                        zone -> {
                            Polygon boundary = zone.getBoundary();
                            // skip count of completely ouside
                            if (!overlaps(boundary, envelope, true)) return false;
                            // skip drilling down if completely contained too
                            return (zone.getResolution() < resolution
                                    && !contained(boundary, envelope, true));
                        },
                        zone -> {
                            int currentResolution = zone.getResolution();
                            Polygon boundary = zone.getBoundary();
                            if (zone.getResolution() == resolution) {
                                if (overlaps(boundary, envelope, true)) {
                                    counter.addAndGet(1);
                                    return true;
                                }
                            } else if (contained(boundary, envelope, true)) {
                                counter.addAndGet(childrenCount(resolution - currentResolution));
                            }
                            return false;
                        },
                        // just return the current counter value
                        zone -> counter);
        // make it visit
        while (iterator.hasNext()) iterator.next();
        return counter.get();
    }

    private long childrenCount(int resolutionDifference) {
        // each parent contains exactly 9 children
        return (long) Math.pow(9, resolutionDifference);
    }

    @Override
    public List<AttributeDescriptor> getExtraProperties() {
        List<AttributeDescriptor> result = new ArrayList<>();
        AttributeTypeBuilder builder = new AttributeTypeBuilder();
        builder.setName("shape");
        builder.setBinding(String.class);
        result.add(builder.buildDescriptor("shape"));
        builder.setName("color");
        builder.setBinding(String.class);
        result.add(builder.buildDescriptor("color"));

        return result;
    }

    @Override
    public Iterator<Zone> neighbors(String id, int radius) {
        Set<String> result = new HashSet<>();
        // temporary add to work as an exclusion mask too
        result.add(id);
        // cells yet to explore
        Set<String> toExplore = new HashSet<>();
        toExplore.add(id);
        runtime.runSafe(
                interpreter -> {
                    for (int i = 0; i < radius; i++) {
                        Set<String> nextRound = new HashSet<>();
                        for (String cell : toExplore) {
                            setCellId(interpreter, "id", cell);
                            // find the neighbors of the cell
                            List<String> neighbors =
                                    interpreter.getValue(
                                            "list(Cell(dggs, id).neighbors(False).values())",
                                            List.class);
                            // compute the zones that we haven't hit yet
                            List<String> newZones = new ArrayList<>(neighbors);
                            newZones.removeAll(result);
                            // add to the result, schedule for next round of checks
                            result.addAll(newZones);
                            nextRound.addAll(newZones);
                        }
                        // done collecting the current ring, switch to the next
                        toExplore.clear();
                        toExplore.addAll(nextRound);
                    }
                    return null;
                });
        // remove the seed zone
        result.remove(id);
        return result.stream().map(zoneId -> (Zone) new RHealPixZone(this, zoneId)).iterator();
    }

    @Override
    public Iterator<Zone> children(String zoneId, int resolution) {
        Zone parent = getZone(zoneId);
        if (parent.getResolution() >= resolution) return new EmptyIterator();

        // all the children of the given cell
        return new RHealPixZoneIterator<>(
                this,
                zone -> zone.getResolution() < resolution,
                zone -> zone.getResolution() == resolution,
                zone -> (Zone) zone,
                Arrays.asList(zoneId));
    }

    @Override
    public Iterator<Zone> parents(String id) {
        // just as a validation
        getZone(id);
        return new RHealPixParentIterator(id, this);
    }

    @Override
    public Zone point(Point point, int resolution) {
        return runtime.runSafe(
                interpreter -> {
                    interpreter.set("p", Arrays.asList(point.getX(), point.getY()));
                    interpreter.set("r", Integer.valueOf(resolution));
                    List<Object> idList =
                            interpreter.getValue(
                                    "dggs.cell_from_point(r, p, False).suid", List.class);
                    return new RHealPixZone(RHealPixDGGSInstance.this, toZoneId(idList));
                });
    }

    @Override
    public Iterator<Zone> polygon(Polygon polygon, int resolution, boolean compact) {
        // get a set of seed cells at a resolution a few levels above the target one
        Envelope envelope = polygon.getEnvelopeInternal();
        Envelope intersection = envelope.intersection(WORLD);
        if (intersection.isNull()) {
            return new EmptyIterator();
        }

        // return each cells that is either fully contained (at possibly a lower resolution) and
        // all the ones at the desired resolution whose centroid is contained in the target polygon
        PreparedPolygon prepared = new PreparedPolygon(polygon);
        Iterator<Zone> compactIterator =
                new RHealPixZoneIterator<>(
                        this,
                        zone ->
                                // skip zones that are at target resolution, fully disjoint (no need
                                // for their children), or fully contained (collected, children
                                // expanded later)
                                zone.getResolution() < resolution
                                        && !testDisjoint(prepared, zone.getBoundary())
                                        && !testContains(prepared, zone.getBoundary()),
                        zone -> {
                            // return a zone if it's at the target resolution with centroid inside
                            // or it's fully contained
                            return (zone.getResolution() == resolution
                                            && testContains(prepared, zone.getCenter()))
                                    || testContains(prepared, zone.getBoundary());
                        },
                        zone -> (Zone) zone);
        // if compact iteration, we are done
        if (compact) return compactIterator;
        // otherwise expand the cells that are at a lower resolution using the fast children
        // computation
        return stream(spliteratorUnknownSize(compactIterator, Spliterator.ORDERED), false)
                .flatMap(
                        z -> {
                            if (z.getResolution() < resolution) {
                                // use children to efficiently get an iterator of all children of a
                                // zone
                                return stream(
                                        spliteratorUnknownSize(
                                                children(z.getId(), resolution),
                                                Spliterator.ORDERED),
                                        false);
                            } else {
                                // the zone itself is already at the desired target level
                                return Stream.of(z);
                            }
                        })
                .iterator();
    }

    @Override
    public Filter getChildFilter(FilterFactory2 ff, String zoneId, int resolution, boolean upTo) {
        return ff.like(ff.property(DGGSStore.ZONE_ID), zoneId + "%", "%", "?", "\\", true);
    }
}
