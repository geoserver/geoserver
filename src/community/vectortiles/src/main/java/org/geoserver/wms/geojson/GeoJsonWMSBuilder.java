package org.geoserver.wms.geojson;

import static org.geoserver.wms.geojson.GeoJsonBuilderFactory.MIME_TYPE;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.apache.commons.io.output.DeferredFileOutputStream;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.vector.DeferredFileOutputStreamWebMap;
import org.geoserver.wms.vector.VectorTileBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Charsets;
import com.vividsolutions.jts.geom.Geometry;

public class GeoJsonWMSBuilder implements VectorTileBuilder {

    private Writer writer;

    DeferredFileOutputStream out;

    private org.geoserver.wfs.json.GeoJSONBuilder jsonWriter;

    public GeoJsonWMSBuilder(Rectangle mapSize, ReferencedEnvelope mapArea) {

        final int threshold = 8096;
        out = new DeferredFileOutputStream(threshold, "geojson", ".geojson", null);
        writer = new OutputStreamWriter(out, Charsets.UTF_8);
        jsonWriter = new org.geoserver.wfs.json.GeoJSONBuilder(writer);
        jsonWriter.object();// start root object
        jsonWriter.key("type").value("FeatureCollection");
        jsonWriter.key("totalFeatures").value("unknown");
        jsonWriter.key("features");
        jsonWriter.array();
    }

    @Override
    public void addFeature(SimpleFeature feature) {
        CoordinateReferenceSystem crs = null;
        boolean hasGeom = false;
        jsonWriter.object();
        jsonWriter.key("type").value("Feature");

        SimpleFeatureType fType = feature.getFeatureType();
        List<AttributeDescriptor> types = fType.getAttributeDescriptors();

        jsonWriter.key("id").value(feature.getID());

        GeometryDescriptor defaultGeomType = fType.getGeometryDescriptor();
        if (defaultGeomType != null) {
            CoordinateReferenceSystem featureCrs = defaultGeomType.getCoordinateReferenceSystem();

            jsonWriter.setAxisOrder(CRS.getAxisOrder(featureCrs));

            if (crs == null)
                crs = featureCrs;
        } else {
            // If we don't know, assume EAST_NORTH so that no swapping occurs
            jsonWriter.setAxisOrder(CRS.AxisOrder.EAST_NORTH);
        }

        jsonWriter.key("geometry");
        Geometry aGeom = (Geometry) feature.getDefaultGeometry();

        if (aGeom == null) {
            // In case the default geometry is not set, we will
            // just use the first geometry we find
            for (int j = 0; j < types.size() && aGeom == null; j++) {
                Object value = feature.getAttribute(j);
                if (value != null && value instanceof Geometry) {
                    aGeom = (Geometry) value;
                }
            }
        }
        // Write the geometry, whether it is a null or not
        if (aGeom != null) {
            jsonWriter.writeGeom(aGeom);
            hasGeom = true;
        } else {
            jsonWriter.value(null);
        }
        if (defaultGeomType != null)
            jsonWriter.key("geometry_name").value(defaultGeomType.getLocalName());

        jsonWriter.key("properties");
        jsonWriter.object();

        for (int j = 0; j < types.size(); j++) {
            Object value = feature.getAttribute(j);
            AttributeDescriptor ad = types.get(j);

            if (value != null) {
                if (value instanceof Geometry) {
                    // This is an area of the spec where they
                    // decided to 'let convention evolve',
                    // that is how to handle multiple
                    // geometries. My take is to print the
                    // geometry here if it's not the default.
                    // If it's the default that you already
                    // printed above, so you don't need it here.
                    if (ad.equals(defaultGeomType)) {
                        // Do nothing, we wrote it above
                        // jsonWriter.value("geometry_name");
                    } else {
                        jsonWriter.key(ad.getLocalName());
                        jsonWriter.writeGeom((Geometry) value);
                    }
                } else {
                    jsonWriter.key(ad.getLocalName());
                    jsonWriter.value(value);
                }

            } else {
                jsonWriter.key(ad.getLocalName());
                jsonWriter.value(null);
            }
        }

        jsonWriter.endObject(); // end the properties
        jsonWriter.endObject(); // end the feature

    }

    @Override
    public WebMap build(WMSMapContent mapContent) throws IOException {
        jsonWriter.endArray(); // end features
        jsonWriter.endObject();// end root object
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

}
