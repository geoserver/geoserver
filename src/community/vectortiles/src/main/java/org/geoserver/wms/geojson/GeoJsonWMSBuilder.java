package org.geoserver.wms.geojson;

import static org.geoserver.wms.geojson.GeoJsonBuilderFactory.MIME_TYPE;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import javax.measure.unit.SI;
import javax.measure.unit.Unit;

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
import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.precision.CoordinatePrecisionReducerFilter;

public class GeoJsonWMSBuilder implements VectorTileBuilder {

    private Writer writer;

    private CoordinatePrecisionReducerFilter precisionReducerFilter;

    private DeferredFileOutputStream out;

    private org.geoserver.wfs.json.GeoJSONBuilder jsonWriter;

    public GeoJsonWMSBuilder(Rectangle mapSize, ReferencedEnvelope mapArea) {

        final int memotyBufferThreshold = 8096;
        out = new DeferredFileOutputStream(memotyBufferThreshold, "geojson", ".geojson", null);
        writer = new OutputStreamWriter(out, Charsets.UTF_8);
        jsonWriter = new org.geoserver.wfs.json.GeoJSONBuilder(writer);
        jsonWriter.object();// start root object
        jsonWriter.key("type").value("FeatureCollection");
        jsonWriter.key("totalFeatures").value("unknown");
        jsonWriter.key("features");
        jsonWriter.array();

        CoordinateReferenceSystem mapCrs = mapArea.getCoordinateReferenceSystem();
        jsonWriter.setAxisOrder(CRS.getAxisOrder(mapCrs));

        Unit<?> unit = mapCrs.getCoordinateSystem().getAxis(0).getUnit();
        Unit<?> standardUnit = unit.getStandardUnit();

        PrecisionModel pm = null;
        if (SI.RADIAN.equals(standardUnit)) {
            pm = new PrecisionModel(1e6);// truncate coords at 6 decimals
        } else if (SI.METRE.equals(standardUnit)) {
            pm = new PrecisionModel(100);// truncate coords at 2 decimals
        }
        if (pm != null) {
            precisionReducerFilter = new CoordinatePrecisionReducerFilter(pm);
        }
    }

    @Override
    public void addFeature(SimpleFeature feature) {

        final SimpleFeatureType fType = feature.getFeatureType();
        final GeometryDescriptor defaultGeomType = fType.getGeometryDescriptor();
        Preconditions.checkNotNull(defaultGeomType);

        Geometry aGeom = (Geometry) feature.getDefaultGeometry();
        if (aGeom == null) {
            return;
        }
        if (aGeom instanceof GeometryCollection && aGeom.getNumGeometries() == 1) {
            aGeom = aGeom.getGeometryN(0);
        }
        if (precisionReducerFilter != null) {
            aGeom.apply(precisionReducerFilter);
        }

        jsonWriter.object();
        jsonWriter.key("type").value("Feature");

        List<AttributeDescriptor> types = fType.getAttributeDescriptors();

        jsonWriter.key("id").value(feature.getID());

        jsonWriter.key("geometry");

        // Write the geometry, whether it is a null or not
        jsonWriter.writeGeom(aGeom);
        jsonWriter.key("geometry_name").value(defaultGeomType.getLocalName());

        jsonWriter.key("properties");
        jsonWriter.object();

        for (int j = 0; j < types.size(); j++) {
            Object value = feature.getAttribute(j);
            AttributeDescriptor attributeDescriptor = types.get(j);

            if (value != null) {
                if (value instanceof Geometry) {
                    // This is an area of the spec where they
                    // decided to 'let convention evolve',
                    // that is how to handle multiple
                    // geometries. My take is to print the
                    // geometry here if it's not the default.
                    // If it's the default that you already
                    // printed above, so you don't need it here.
                    if (attributeDescriptor.equals(defaultGeomType)) {
                        // Do nothing, we wrote it above
                        // jsonWriter.value("geometry_name");
                    } else {
                        jsonWriter.key(attributeDescriptor.getLocalName());
                        jsonWriter.writeGeom((Geometry) value);
                    }
                } else {
                    jsonWriter.key(attributeDescriptor.getLocalName());
                    jsonWriter.value(value);
                }

            } else {
                jsonWriter.key(attributeDescriptor.getLocalName());
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
