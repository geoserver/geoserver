/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.geojson;

import static org.geoserver.wms.geojson.GeoJsonBuilderFactory.MIME_TYPE;

import com.google.common.base.Charsets;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import javax.measure.Unit;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.vector.DeferredFileOutputStreamWebMap;
import org.geoserver.wms.vector.VectorTileBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.CoordinatePrecisionReducerFilter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import si.uom.SI;

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
        jsonWriter.object(); // start root object
        jsonWriter.key("type").value("FeatureCollection");
        jsonWriter.key("totalFeatures").value("unknown");
        jsonWriter.key("features");
        jsonWriter.array();

        CoordinateReferenceSystem mapCrs = mapArea.getCoordinateReferenceSystem();
        jsonWriter.setAxisOrder(CRS.getAxisOrder(mapCrs));

        Unit<?> unit = mapCrs.getCoordinateSystem().getAxis(0).getUnit();
        Unit<?> standardUnit = unit.getSystemUnit();

        PrecisionModel pm = null;
        if (SI.RADIAN.equals(standardUnit)) {
            pm = new PrecisionModel(1e6); // truncate coords at 6 decimals
        } else if (SI.METRE.equals(standardUnit)) {
            pm = new PrecisionModel(100); // truncate coords at 2 decimals
        }
        if (pm != null) {
            precisionReducerFilter = new CoordinatePrecisionReducerFilter(pm);
        }
    }

    @Override
    public void addFeature(
            String layerName,
            String featureId,
            String geometryName,
            Geometry aGeom,
            Map<String, Object> properties) {

        if (precisionReducerFilter != null) {
            aGeom.apply(precisionReducerFilter);
        }

        jsonWriter.object();
        jsonWriter.key("type").value("Feature");

        jsonWriter.key("id").value(featureId);

        jsonWriter.key("geometry");

        // Write the geometry, whether it is a null or not
        jsonWriter.writeGeom(aGeom);
        jsonWriter.key("geometry_name").value(geometryName);

        jsonWriter.key("properties");
        jsonWriter.object();

        for (Map.Entry<String, Object> e : properties.entrySet()) {
            String attributeName = e.getKey();
            Object value = e.getValue();

            jsonWriter.key(attributeName);
            if (value == null) {
                jsonWriter.value(null);
            } else {
                jsonWriter.value(value);
            }
        }

        jsonWriter.endObject(); // end the properties
        jsonWriter.endObject(); // end the feature
    }

    @Override
    public RawMap build(WMSMapContent mapContent) throws IOException {
        jsonWriter.endArray(); // end features
        jsonWriter.endObject(); // end root object
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
