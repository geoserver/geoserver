/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.topojson;

import static org.geoserver.wms.topojson.TopoJSONBuilderFactory.MIME_TYPE;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.topojson.TopoGeom.GeometryColleciton;
import org.geoserver.wms.vector.DeferredFileOutputStreamWebMap;
import org.geoserver.wms.vector.VectorTileBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.lite.RendererUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.TransformException;

public class TopologyBuilder implements VectorTileBuilder {

    private AffineTransform worldToScreen;

    private AffineTransform screenToWorld;

    private List<LineString> arcs = new ArrayList<>();

    private Multimap<String, TopoGeom> layers = ArrayListMultimap.create();

    private GeometryFactory fixedGeometryFactory;

    public TopologyBuilder(Rectangle mapSize, ReferencedEnvelope mapArea) {
        this.worldToScreen = RendererUtilities.worldToScreenTransform(mapArea, mapSize);
        this.screenToWorld = new AffineTransform(this.worldToScreen);
        try {
            this.screenToWorld.invert();
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }

        PrecisionModel precisionModel = new PrecisionModel(10.0);
        fixedGeometryFactory = new GeometryFactory(precisionModel);
    }

    @Override
    public void addFeature(
            String layerName,
            String featureId,
            String geometryName,
            Geometry geometry,
            Map<String, Object> properties) {
        TopoGeom topoObj;
        try {
            topoObj = createObject(featureId, geometry, properties);
        } catch (MismatchedDimensionException | TransformException e) {
            throw new RuntimeException(e);
        }

        if (topoObj != null) {
            layers.put(layerName, topoObj);
        }
    }

    @Override
    public RawMap build(WMSMapContent mapContent) throws IOException {

        Map<String, TopoGeom.GeometryColleciton> layers = new HashMap<>();
        for (String layer : this.layers.keySet()) {
            Collection<TopoGeom> collection = this.layers.get(layer);
            GeometryColleciton layerCollection = new TopoGeom.GeometryColleciton(collection);
            layers.put(layer, layerCollection);
        }

        List<LineString> arcs = this.arcs;
        this.arcs = null;
        this.layers = null;
        Topology topology = new Topology(screenToWorld, arcs, layers);

        final int threshold = 8096;
        DeferredFileOutputStream out =
                new DeferredFileOutputStream(threshold, "topology", ".topojson", null);
        TopoJSONEncoder encoder = new TopoJSONEncoder();

        Writer writer = new OutputStreamWriter(out, Charsets.UTF_8);
        encoder.encode(topology, writer);
        writer.flush();
        writer.close();
        out.close();

        long length;
        RawMap map;
        if (out.isInMemory()) {
            byte[] data = out.getData();
            length = data.length;
            map = new RawMap(mapContent, data, MIME_TYPE);
        } else {
            File f = out.getFile();
            length = f.length();
            map = new DeferredFileOutputStreamWebMap(mapContent, out, MIME_TYPE);
        }
        map.setResponseHeader("Content-Length", String.valueOf(length));

        return map;
    }

    @Nullable
    private TopoGeom createObject(String featureId, Geometry geom, Map<String, Object> properties)
            throws MismatchedDimensionException, TransformException {

        // // snap to pixel
        geom = fixedGeometryFactory.createGeometry(geom);

        if (geom.isEmpty()) {
            return null;
        }

        if (geom instanceof GeometryCollection && geom.getNumGeometries() == 1) {
            geom = geom.getGeometryN(0);
        }

        TopoGeom geometry = createGeometry(geom);

        geometry.setProperties(properties);

        geometry.setId(featureId);
        return geometry;
    }

    private TopoGeom createGeometry(Geometry geom) {
        Preconditions.checkNotNull(geom);

        TopoGeom topoGeom;

        if (geom instanceof Point) {
            topoGeom = createPoint((Point) geom);
        } else if (geom instanceof MultiPoint) {
            topoGeom = createMultiPoint((MultiPoint) geom);
        } else if (geom instanceof LineString) {
            topoGeom = createLineString((LineString) geom);
        } else if (geom instanceof MultiLineString) {
            topoGeom = createMultiLineString((MultiLineString) geom);
        } else if (geom instanceof Polygon) {
            topoGeom = createPolygon((Polygon) geom);
        } else if (geom instanceof MultiPolygon) {
            topoGeom = createMultiPolygon((MultiPolygon) geom);
        } else if (geom instanceof GeometryCollection) {
            topoGeom = createGeometryCollection((GeometryCollection) geom);
        } else {
            throw new IllegalArgumentException("Unknown geometry type: " + geom.getGeometryType());
        }

        return topoGeom;
    }

    private TopoGeom.LineString createLineString(LineString geom) {
        int arcIndex = this.arcs.size();
        this.arcs.add(geom);
        return new TopoGeom.LineString(ImmutableList.of(Integer.valueOf(arcIndex)));
    }

    private TopoGeom.Polygon createPolygon(Polygon geom) {
        List<TopoGeom.LineString> arcs = new ArrayList<>(1 + geom.getNumInteriorRing());

        arcs.add(createLineString(geom.getExteriorRing()));

        for (int n = 0; n < geom.getNumInteriorRing(); n++) {
            arcs.add(createLineString(geom.getInteriorRingN(n)));
        }
        return new TopoGeom.Polygon(arcs);
    }

    private TopoGeom.GeometryColleciton createGeometryCollection(GeometryCollection geom) {
        Collection<TopoGeom> members = new ArrayList<>(geom.getNumGeometries());
        for (int n = 0; n < geom.getNumGeometries(); n++) {
            TopoGeom o = createGeometry(geom.getGeometryN(n));
            members.add(o);
        }
        TopoGeom.GeometryColleciton collection = new TopoGeom.GeometryColleciton(members);
        return collection;
    }

    private TopoGeom.MultiPolygon createMultiPolygon(MultiPolygon geom) {
        List<TopoGeom.Polygon> polygons = new ArrayList<>(geom.getNumGeometries());
        for (int n = 0; n < geom.getNumGeometries(); n++) {
            polygons.add(createPolygon((Polygon) geom.getGeometryN(n)));
        }
        return new TopoGeom.MultiPolygon(polygons);
    }

    private TopoGeom.MultiLineString createMultiLineString(MultiLineString geom) {

        List<TopoGeom.LineString> arcs = new ArrayList<>(geom.getNumGeometries());
        for (int n = 0; n < geom.getNumGeometries(); n++) {
            arcs.add(createLineString((LineString) geom.getGeometryN(n)));
        }
        return new TopoGeom.MultiLineString(arcs);
    }

    private TopoGeom.MultiPoint createMultiPoint(MultiPoint geom) {
        List<TopoGeom.Point> points = new ArrayList<>(geom.getNumGeometries());
        for (int n = 0; n < geom.getNumGeometries(); n++) {
            points.add(createPoint((Point) geom.getGeometryN(n)));
        }
        return new TopoGeom.MultiPoint(points);
    }

    private TopoGeom.Point createPoint(Point geom) {
        return new TopoGeom.Point(geom.getX(), geom.getY());
    }
}
