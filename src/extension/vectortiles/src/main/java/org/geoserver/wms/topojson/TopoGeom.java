/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.topojson;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public abstract class TopoGeom {

    private Map<String, Object> properties = ImmutableMap.of();

    private String id;

    public abstract String getGeometryType();

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Nullable
    public String getId() {
        return id;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = ImmutableMap.copyOf(properties);
    }

    public static class Point extends TopoGeom {
        private double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        @Override
        public String getGeometryType() {
            return "Point";
        }
    }

    public static class MultiPoint extends TopoGeom {
        private List<Point> points = ImmutableList.of();

        public MultiPoint(List<TopoGeom.Point> points) {
            this.points = ImmutableList.copyOf(points);
        }

        public Iterable<Point> getPoints() {
            return points;
        }

        @Override
        public String getGeometryType() {
            return "MultiPoint";
        }
    }

    public static class LineString extends TopoGeom {
        private List<Integer> indexes;

        public LineString(List<Integer> indexes) {
            this.indexes = ImmutableList.copyOf(indexes);
        }

        public List<Integer> getIndexes() {
            return indexes;
        }

        @Override
        public String getGeometryType() {
            return "LineString";
        }
    }

    public static class MultiLineString extends TopoGeom {
        private List<LineString> arcs = ImmutableList.of();

        public MultiLineString(List<LineString> arcs) {
            this.arcs = ImmutableList.copyOf(arcs);
        }

        public Iterable<LineString> getArcs() {
            return arcs;
        }

        @Override
        public String getGeometryType() {
            return "MultiLineString";
        }
    }

    public static class Polygon extends TopoGeom {
        private List<LineString> rings = ImmutableList.of();

        public Polygon(List<LineString> rings) {
            this.rings = ImmutableList.copyOf(rings);
        }

        public Iterable<LineString> getRings() {
            return rings;
        }

        @Override
        public String getGeometryType() {
            return "Polygon";
        }
    }

    public static class MultiPolygon extends TopoGeom {
        private List<Polygon> polygons = ImmutableList.of();

        public MultiPolygon(List<TopoGeom.Polygon> polygons) {
            this.polygons = ImmutableList.copyOf(polygons);
        }

        public Iterable<TopoGeom.Polygon> getPolygons() {
            return polygons;
        }

        @Override
        public String getGeometryType() {
            return "MultiPolygon";
        }
    }

    public static class GeometryColleciton extends TopoGeom {
        private List<TopoGeom> geometries = ImmutableList.of();

        public GeometryColleciton(Collection<? extends TopoGeom> collection) {
            this.geometries = ImmutableList.copyOf(collection);
        }

        public Iterable<TopoGeom> getGeometries() {
            return geometries;
        }

        @Override
        public String getGeometryType() {
            return "GeometryCollection";
        }
    }
}
