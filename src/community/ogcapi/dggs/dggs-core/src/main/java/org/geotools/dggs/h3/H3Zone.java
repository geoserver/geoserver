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

import com.uber.h3core.util.GeoCoord;
import java.util.List;
import java.util.Objects;
import org.geotools.dggs.Zone;
import org.geotools.dggs.ZoneWrapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public class H3Zone implements Zone {

    long id;
    H3DGGSInstance dggs;

    public H3Zone(H3DGGSInstance dggs, long zoneId) {
        this.id = zoneId;
        this.dggs = dggs;
    }

    @Override
    public String getId() {
        return dggs.h3.h3ToString(id);
    }

    @Override
    public int getResolution() {
        return dggs.h3.h3GetResolution(id);
    }

    @Override
    public Point getCenter() {
        GeoCoord gc = dggs.h3.h3ToGeo(id);
        return dggs.gf.createPoint(new Coordinate(gc.lng, gc.lat));
    }

    @Override
    public Polygon getBoundary() {
        CoordinateSequence cs = getCoordinateSequence();

        // rewrap to avoid dateline jumps
        if (dggs.northPoleZones.contains(id)) {
            cs = ZoneWrapper.includePole(dggs.gf, cs, true);
        } else if (dggs.southPoleZones.contains(id)) {
            cs = ZoneWrapper.includePole(dggs.gf, cs, false);
        } else {
            cs = ZoneWrapper.wrap(cs);
        }

        LinearRing ring = dggs.gf.createLinearRing(cs);
        return dggs.gf.createPolygon(ring);
    }

    private CoordinateSequence getCoordinateSequence() {
        List<GeoCoord> coords = dggs.h3.h3ToGeoBoundary(id);
        CoordinateSequenceFactory csf = dggs.gf.getCoordinateSequenceFactory();

        int size = coords.size();
        CoordinateSequence cs = csf.create(size + 1, 2);
        for (int i = 0; i < size; i++) {
            GeoCoord coord = coords.get(i);
            cs.setOrdinate(i, 0, coord.lng);
            cs.setOrdinate(i, 1, coord.lat);
        }
        GeoCoord first = coords.get(0);
        cs.setOrdinate(size, 0, first.lng);
        cs.setOrdinate(size, 1, first.lat);
        return cs;
    }

    @Override
    public String toString() {
        return "H3Zone{" + getId() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        H3Zone h3Zone = (H3Zone) o;
        return id == h3Zone.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean overlapsDateline() {
        CoordinateSequence cs = getCoordinateSequence();
        return ZoneWrapper.isDatelineCrossing(cs) != ZoneWrapper.DatelineLocation.NotCrossing;
    }

    @Override
    public Object getExtraProperty(String name) {
        if ("shape".equals(name)) {
            boolean pentagon = dggs.h3.h3IsPentagon(id);
            return pentagon ? "pentagon" : "hexagon";
        }
        throw new IllegalArgumentException("Invalid extra property value " + name);
    }
}
