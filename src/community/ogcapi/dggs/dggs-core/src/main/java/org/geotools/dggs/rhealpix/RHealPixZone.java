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

import static org.geotools.dggs.rhealpix.RHealPixUtils.setCellId;

import java.awt.Color;
import java.util.List;
import java.util.Objects;
import jep.JepException;
import jep.SharedInterpreter;
import org.geotools.dggs.Zone;
import org.geotools.dggs.ZoneWrapper;
import org.geotools.util.Converters;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public class RHealPixZone implements Zone {

    enum CellType {
        quad,
        cap,
        dart(null, "c.boundary(10, False)"), // we might get specialized calls for these two
        skew_quad(null, "c.boundary(10, False)");

        String setupScript = null;
        String verticesScript = "c.vertices(False)";

        CellType() {
            // use defaults
        }

        CellType(String setupScript, String verticesScript) {
            this.setupScript = setupScript;
            this.verticesScript = verticesScript;
        }

        public double[][] getVertices(SharedInterpreter interpreter) throws JepException {
            if (setupScript != null) interpreter.exec(setupScript);

            return interpreter.getValue(verticesScript, double[][].class);
        }
    }

    private final String id;
    private final RHealPixDGGSInstance dggs;
    private Polygon boundary;

    public RHealPixZone(RHealPixDGGSInstance dggs, String id) {
        this.dggs = dggs;
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getResolution() {
        // avoid python calls, use the naming conventions instead
        return id.length() - 1;
    }

    @Override
    public Point getCenter() {
        return dggs.runtime.runSafe(
                interpreter -> {
                    setCellId(interpreter, "id", id);
                    interpreter.exec("c = Cell(dggs, id)");
                    double[] points = interpreter.getValue("c.centroid(False)", double[].class);
                    return dggs.gf.createPoint(new Coordinate(points[0], points[1]));
                });
    }

    @Override
    public Polygon getBoundary() {
        // boundary computation is expensive, cache the result
        if (boundary == null) {
            boundary =
                    dggs.runtime.runSafe(
                            interpreter -> {
                                setCellId(interpreter, "id", id);
                                interpreter.exec("c = Cell(dggs, id)");
                                double[][] vertices = getShape().getVertices(interpreter);

                                return getPolygon(vertices);
                            },
                            e -> new RuntimeException("Failed to compute boundary for " + id, e));
        }
        return boundary;
    }

    @Override
    public Object getExtraProperty(String name) {
        if ("shape".equals(name)) {
            return dggs.runtime.runSafe(
                    si -> {
                        setCellId(si, "id", id);
                        si.exec("c = Cell(dggs, id)");
                        return si.getValue("c.ellipsoidal_shape()", String.class);
                    });
        } else if ("color".equals(name)) {
            return dggs.runtime.runSafe(
                    si -> {
                        setCellId(si, "id", id);
                        si.exec("c = Cell(dggs, id)");
                        List<Number> definition = si.getValue("c.color()", List.class);
                        int red = (int) Math.round(definition.get(0).doubleValue() * 255);
                        int green = (int) Math.round(definition.get(1).doubleValue() * 255);
                        int blue = (int) Math.round(definition.get(2).doubleValue() * 255);
                        return Converters.convert(new Color(red, green, blue), String.class);
                    });
        }
        throw new IllegalArgumentException("Invalid extra property value " + name);
    }

    private CellType getShape() {
        return dggs.runtime.runSafe(
                si -> {
                    return CellType.valueOf(si.getValue("c.ellipsoidal_shape()", String.class));
                });
    }

    private Polygon getPolygon(double[][] vertices) {
        CoordinateSequenceFactory csf = dggs.gf.getCoordinateSequenceFactory();
        CoordinateSequence cs;
        if (dggs.northPoleZones.contains(id)) {
            double latitude = vertices[0][1];
            cs = buildRectangle(csf, -180, latitude, 180, 90);
        } else if (dggs.southPoleZones.contains(id)) {
            double latitude = vertices[0][1];
            cs = buildRectangle(csf, -180, -90, 180, latitude);
        } else {
            int size = vertices.length;
            cs = csf.create(size + 1, 2);
            for (int i = 0; i < size; i++) {
                cs.setOrdinate(i, 0, vertices[i][0]);
                cs.setOrdinate(i, 1, vertices[i][1]);
            }
            cs.setOrdinate(size, 0, vertices[0][0]);
            cs.setOrdinate(size, 1, vertices[0][1]);

            // rewrap to avoid dateline jumps and pole issues
            cs = ZoneWrapper.wrap(cs);
        }

        LinearRing ring = dggs.gf.createLinearRing(cs);
        return dggs.gf.createPolygon(ring);
    }

    public CoordinateSequence buildRectangle(
            CoordinateSequenceFactory csf, double minX, double minY, double maxX, double maxY) {
        CoordinateSequence cs = csf.create(5, 2);
        cs.setOrdinate(0, 0, minX);
        cs.setOrdinate(0, 1, minY);
        cs.setOrdinate(1, 0, minX);
        cs.setOrdinate(1, 1, maxY);
        cs.setOrdinate(2, 0, maxX);
        cs.setOrdinate(2, 1, maxY);
        cs.setOrdinate(3, 0, maxX);
        cs.setOrdinate(3, 1, minY);
        cs.setOrdinate(4, 0, minX);
        cs.setOrdinate(4, 1, minY);
        return cs;
    }

    @Override
    public String toString() {
        return "RHealPixZone{" + "id='" + id + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RHealPixZone that = (RHealPixZone) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
