/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.topojson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.geoserver.wms.topojson.TopoGeom.GeometryColleciton;
import org.geoserver.wms.topojson.TopoGeom.LineString;
import org.geoserver.wms.topojson.TopoGeom.MultiLineString;
import org.geoserver.wms.topojson.TopoGeom.MultiPoint;
import org.geoserver.wms.topojson.TopoGeom.MultiPolygon;
import org.geoserver.wms.topojson.TopoGeom.Point;
import org.geoserver.wms.topojson.TopoGeom.Polygon;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.PrecisionModel;

public class TopoJSONEncoder {

    private static class TopologyAdapter
            implements JsonSerializer<Topology>, JsonDeserializer<Topology> {

        @Override
        public JsonElement serialize(
                Topology topology, Type typeOfSrc, JsonSerializationContext context) {

            JsonObject root = new JsonObject();
            root.addProperty("type", "Topology");
            root.addProperty("count", topology.getArcs().size());
            AffineTransform transform = topology.getScreenToWorldTransform();
            if (!transform.isIdentity()) {
                addTransform(root, transform);
            }
            addArcs(root, topology);

            addLayers(root, topology.getLayers());
            return root;
        }

        private void addLayers(JsonObject root, Map<String, GeometryColleciton> layers) {

            JsonObject objects = new JsonObject();
            root.add("objects", objects);

            for (Map.Entry<String, GeometryColleciton> e : layers.entrySet()) {
                String name = e.getKey();
                GeometryColleciton geometries = e.getValue();

                JsonObject layer = TopologyEncoder.encode(geometries);
                objects.add(name, layer);
            }
        }

        private void addTransform(JsonObject root, AffineTransform transform) {
            if (transform.isIdentity()) {
                return;
            }
            JsonObject tx = new JsonObject();
            JsonArray scale = new JsonArray();
            scale.add(new JsonPrimitive(transform.getScaleX()));
            scale.add(new JsonPrimitive(transform.getScaleY()));
            tx.add("scale", scale);

            JsonArray translate = new JsonArray();
            translate.add(new JsonPrimitive(transform.getTranslateX()));
            translate.add(new JsonPrimitive(transform.getTranslateY()));
            tx.add("translate", translate);

            root.add("transform", tx);
        }

        private void addArcs(JsonObject root, Topology topology) {

            JsonArray arcs = new JsonArray();

            JsonArray jsonArc;
            for (org.locationtech.jts.geom.LineString arc : topology.getArcs()) {
                if (topology.getScreenToWorldTransform().isIdentity()) {
                    jsonArc = TopoJSONEncoder.serialize(arc.getCoordinateSequence());
                } else {
                    jsonArc =
                            TopoJSONEncoder.quantize(
                                    arc.getCoordinateSequence(),
                                    arc.getFactory().getPrecisionModel());
                }
                arcs.add(jsonArc);
            }
            root.add("arcs", arcs);
        }

        @Override
        public Topology deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
                throws JsonParseException {
            throw new UnsupportedOperationException();
        }
    }

    private static final TopologyAdapter TOPOLOGY_ADAPTER = new TopologyAdapter();

    private static final GsonBuilder gsonBuilder = new GsonBuilder();

    static {
        gsonBuilder.registerTypeAdapter(Topology.class, TOPOLOGY_ADAPTER);
    }

    public void encode(Topology topology, Writer writer) throws IOException {

        Gson gson = gsonBuilder /* .setPrettyPrinting() */.create();

        // gson.fromjson
        gson.toJson(topology, writer);
        writer.flush();
    }

    private abstract static class TopologyEncoder {
        private static Map<String, TopologyEncoder> encoders = new HashMap<>();

        static {
            encoders.put("Point", new PointEncoder());
            encoders.put("MultiPoint", new MultiPointEncoder());
            encoders.put("LineString", new LineStringEncoder());
            encoders.put("MultiLineString", new MultiLineStringEncoder());
            encoders.put("Polygon", new PolygonEncoder());
            encoders.put("MultiPolygon", new MultiPolygonEncoder());
            encoders.put("GeometryCollection", new GeometryCollecitonEncoder());
        }

        public static JsonObject encode(TopoGeom geom) {
            JsonObject obj = new JsonObject();
            String geometryType = geom.getGeometryType();
            TopologyEncoder encoder = encoders.get(geometryType);
            encoder.encode(geom, obj);
            return obj;
        }

        public void encode(TopoGeom geom, JsonObject target) {
            target.addProperty("type", geom.getGeometryType());
            if (geom.getId() != null) {
                target.addProperty("id", geom.getId());
            }
            JsonObject properties = properties(geom.getProperties());
            if (properties != null) {
                target.add("properties", properties);
            }
            encodeInternal(geom, target);
        }

        protected abstract void encodeInternal(TopoGeom geom, JsonObject target);

        @SuppressWarnings("unchecked")
        @Nullable
        private JsonObject properties(Map<String, Object> properties) {
            if (properties.isEmpty()) {
                return null;
            }
            JsonObject props = new JsonObject();
            for (Map.Entry<String, Object> e : properties.entrySet()) {
                String name = e.getKey();
                Object value = e.getValue();

                JsonElement jsonValue;
                if (value instanceof Map) {
                    jsonValue = properties((Map<String, Object>) value);
                } else if (value instanceof Boolean) {
                    jsonValue = new JsonPrimitive((Boolean) value);
                } else if (value instanceof Number) {
                    Number n = (Number) value;
                    if (n instanceof Double && n.doubleValue() % 1 == 0) {
                        n = Long.valueOf(n.longValue());
                    } else if (n instanceof Float && n.floatValue() % 1 == 0) {
                        n = Integer.valueOf(n.intValue());
                    }
                    jsonValue = new JsonPrimitive(n);
                } else {
                    jsonValue = new JsonPrimitive(String.valueOf(value));
                }
                props.add(name, jsonValue);
            }
            return props;
        }
    }

    private static class GeometryCollecitonEncoder extends TopologyEncoder {

        @Override
        protected void encodeInternal(TopoGeom geom, JsonObject target) {
            GeometryColleciton coll = (GeometryColleciton) geom;
            JsonArray geoms = new JsonArray();
            target.add("geometries", geoms);

            for (TopoGeom obj : coll.getGeometries()) {
                geoms.add(TopologyEncoder.encode(obj));
            }
        }
    }

    private static class PointEncoder extends TopologyEncoder {

        @Override
        protected void encodeInternal(TopoGeom geom, JsonObject target) {
            TopoGeom.Point point = (Point) geom;
            JsonArray coordinates = new JsonArray();
            target.add("coordinates", coordinates);
            coordinates.add(new JsonPrimitive(point.getX()));
            coordinates.add(new JsonPrimitive(point.getY()));
        }
    }

    private static class MultiPointEncoder extends TopologyEncoder {

        @Override
        protected void encodeInternal(TopoGeom geom, JsonObject target) {
            TopoGeom.MultiPoint multipoint = (MultiPoint) geom;
            JsonArray coordinates = new JsonArray();
            target.add("coordinates", coordinates);
            Iterable<Point> points = multipoint.getPoints();
            for (Point p : points) {
                JsonArray point = new JsonArray();
                coordinates.add(point);
                point.add(new JsonPrimitive(p.getX()));
                point.add(new JsonPrimitive(p.getY()));
            }
        }
    }

    private static class LineStringEncoder extends TopologyEncoder {

        @Override
        protected void encodeInternal(TopoGeom geom, JsonObject target) {
            TopoGeom.LineString arc = (LineString) geom;
            target.add("arcs", LineStringEncoder.indexes(arc));
        }

        public static JsonArray indexes(LineString arc) {
            JsonArray arcs = new JsonArray();
            for (Integer index : arc.getIndexes()) {
                arcs.add(new JsonPrimitive(index));
            }
            return arcs;
        }
    }

    private static class MultiLineStringEncoder extends TopologyEncoder {

        @Override
        protected void encodeInternal(TopoGeom geom, JsonObject target) {
            TopoGeom.MultiLineString marc = (MultiLineString) geom;
            JsonArray arcs = new JsonArray();
            target.add("arcs", arcs);
            for (LineString arc : marc.getArcs()) {
                arcs.add(LineStringEncoder.indexes(arc));
            }
        }
    }

    private static class PolygonEncoder extends TopologyEncoder {

        @Override
        protected void encodeInternal(TopoGeom geom, JsonObject target) {
            TopoGeom.Polygon poly = (Polygon) geom;
            target.add("arcs", PolygonEncoder.indexes(poly));
        }

        public static JsonArray indexes(TopoGeom.Polygon poly) {
            JsonArray arcs = new JsonArray();
            Iterable<LineString> rings = poly.getRings();
            for (LineString ring : rings) {
                arcs.add(LineStringEncoder.indexes(ring));
            }
            return arcs;
        }
    }

    private static class MultiPolygonEncoder extends TopologyEncoder {

        @Override
        protected void encodeInternal(TopoGeom geom, JsonObject target) {
            TopoGeom.MultiPolygon poly = (MultiPolygon) geom;
            JsonArray polygons = new JsonArray();
            target.add("arcs", polygons);
            for (Polygon p : poly.getPolygons()) {
                polygons.add(PolygonEncoder.indexes(p));
            }
        }
    }

    public static JsonArray serialize(final CoordinateSequence coords) {
        JsonArray arc = new JsonArray();
        final int size = coords.size();

        if (size > 0) {
            Coordinate buff = new Coordinate();

            coords.getCoordinate(0, buff);
            addCoordinate(arc, buff); // first coordinate as-is

            for (int i = 0; i < size; i++) { // subsequent coordinates delta encoded
                coords.getCoordinate(i, buff);
                addCoordinate(arc, buff);
            }
        }
        return arc;
    }

    public static JsonArray quantize(
            final CoordinateSequence coords, PrecisionModel precisionModel) {
        JsonArray arc = new JsonArray();
        final int size = coords.size();

        if (size > 0) {
            Coordinate buff = new Coordinate();

            coords.getCoordinate(0, buff);
            precisionModel.makePrecise(buff);
            addCoordinate(arc, buff); // first coordinate as-is

            double lastX;
            double lastY;

            lastX = buff.x;
            lastY = buff.y;

            for (int i = 1; i < size; i++) { // subsequent coordinates delta encoded
                coords.getCoordinate(i, buff);
                precisionModel.makePrecise(buff);
                double deltaX = buff.x - lastX;
                double deltaY = buff.y - lastY;
                lastX = buff.x;
                lastY = buff.y;
                buff.x = deltaX;
                buff.y = deltaY;
                precisionModel.makePrecise(buff);
                if (buff.x == 0d && buff.y == 0d) {
                    continue;
                }
                addCoordinate(arc, buff);
            }
        }
        return arc;
    }

    private static void addCoordinate(JsonArray arc, Coordinate c) {
        JsonArray coord = new JsonArray();

        double x = c.x;
        double y = c.y;
        Number X, Y;
        if (x % 1 == 0) {
            X = Integer.valueOf((int) x);
        } else {
            X = Double.valueOf(x);
        }
        if (y % 1 == 0) {
            Y = Integer.valueOf((int) y);
        } else {
            Y = Double.valueOf(y);
        }

        coord.add(new JsonPrimitive(X));
        coord.add(new JsonPrimitive(Y));
        arc.add(coord);
    }
}
