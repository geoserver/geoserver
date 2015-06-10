package org.geoserver.wms.topojson;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.geoserver.wms.topojson.TopoGeom.GeometryColleciton;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

public class TopologyBuilder {

    private AffineTransform worldToScreen;

    private AffineTransform screenToWorld;

    private List<LineString> arcs = new ArrayList<>();

    private Multimap<String, TopoGeom> layers = ArrayListMultimap.create();

    private MathTransform mathTransform;

    private GeometryCoordinateSequenceTransformer transformer;

    private GeometryFactory fixedGeometryFactory;

    private final Polygon clipBounds;

    public TopologyBuilder(AffineTransform worldToScreen, Envelope mapArea) {
        this.worldToScreen = worldToScreen;
        this.screenToWorld = new AffineTransform(this.worldToScreen);
        try {
            this.screenToWorld.invert();
        } catch (NoninvertibleTransformException e) {
            throw Throwables.propagate(e);
        }

        mathTransform = ProjectiveTransform.create(this.worldToScreen);
        transformer = new GeometryCoordinateSequenceTransformer();
        transformer.setMathTransform(mathTransform);

        PrecisionModel precisionModel = new PrecisionModel(10.0);
        fixedGeometryFactory = new GeometryFactory(precisionModel);

        Polygon bounds = JTS.toGeometry(mapArea, fixedGeometryFactory);
        try {
            bounds = (Polygon) transformer.transform(bounds);
        } catch (TransformException e) {
            throw Throwables.propagate(e);
        }
        bounds = (Polygon) fixedGeometryFactory.createGeometry(bounds);
        this.clipBounds = bounds;
    }

    public void addFeature(Feature feature) {
        String layer = feature.getName().getLocalPart();
        TopoGeom topoObj;
        try {
            topoObj = createObject(feature);
        } catch (MismatchedDimensionException | TransformException e) {
            e.printStackTrace();
            throw Throwables.propagate(e);
        }

        if (topoObj != null) {
            layers.put(layer, topoObj);
        }
    }

    @Nullable
    private TopoGeom createObject(Feature feature) throws MismatchedDimensionException,
            TransformException {
        Geometry geom;
        {// ignore geometry-less features
            GeometryAttribute defaultGeometryProperty = feature.getDefaultGeometryProperty();
            if (null == defaultGeometryProperty) {
                return null;
            }
            geom = (Geometry) defaultGeometryProperty.getValue();
            if (geom == null) {
                return null;
            }
        }

        // transform to screen coordinates
        geom = transformer.transform(geom);

        // clip
        if (clipBounds.overlaps(geom)) {
            geom = clipBounds.intersection(geom);
        }

        // snap to pixel
        geom = fixedGeometryFactory.createGeometry(geom);

        // simplify
        geom = TopologyPreservingSimplifier.simplify(geom, 0.8);

        TopoGeom geometry = createGeometry(geom);
        Map<String, Object> properties = getProperties(feature);
        geometry.setProperties(properties);

        geometry.setId(feature.getIdentifier().getID());
        return geometry;
    }

    private Map<String, Object> getProperties(ComplexAttribute feature) {
        Map<String, Object> props = new HashMap<>();
        for (Property p : feature.getProperties()) {
            if (!(p instanceof Attribute) || (p instanceof GeometryAttribute)) {
                continue;
            }
            String name = p.getName().getLocalPart();
            Object value;
            if (p instanceof ComplexAttribute) {
                value = getProperties((ComplexAttribute) p);
            } else {
                value = p.getValue();
            }
            if (value != null) {
                props.put(name, value);
            }
        }
        return props;
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

    public Topology build() {

        Map<String, TopoGeom.GeometryColleciton> layers = new HashMap<>();
        for (String layer : this.layers.keySet()) {
            Collection<TopoGeom> collection = this.layers.get(layer);
            GeometryColleciton layerCollection = new TopoGeom.GeometryColleciton(collection);
            layers.put(layer, layerCollection);
        }

        Topology topology = new Topology(screenToWorld, arcs, layers);
        return topology;
    }

}
