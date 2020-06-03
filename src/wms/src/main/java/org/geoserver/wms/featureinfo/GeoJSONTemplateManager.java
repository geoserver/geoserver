/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import freemarker.template.Template;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wfs.json.GeoJSONBuilder;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * This class allows the management of freemarker templates to customize geoJSON output in
 * getFeatureInfo request.
 */
public class GeoJSONTemplateManager extends FreeMarkerTemplateManager {

    public GeoJSONTemplateManager(
            OutputFormat format, WMS wms, GeoServerResourceLoader resourceLoader) {
        super(format, wms, resourceLoader);
    }

    @Override
    protected boolean templatesExist(
            Template header, Template footer, List<FeatureCollection> collections)
            throws IOException {
        int collSize = collections.size();
        if (header == null || footer == null) return false;
        else {
            for (int i = 0; i < collSize; i++) {
                FeatureCollection fc = collections.get(i);
                Template content = getContentTemplate(fc, wms.getCharSet());
                if (content != null) return true;
            }
        }
        return false;
    }

    @Override
    protected void handleContent(
            List<FeatureCollection> collections,
            OutputStreamWriter osw,
            GetFeatureInfoRequest request)
            throws IOException {
        for (int i = 0; i < collections.size(); i++) {
            FeatureCollection fc = collections.get(i);
            Template content = getContentTemplate(fc, wms.getCharSet());
            if (i > 0) {
                // appending a comma between json object representation
                // of a feature
                osw.write(',');
            }
            if (content == null) {
                handleJSONWithoutTemplate(fc, osw);
            } else {
                String typeName = request.getQueryLayers().get(i).getName();
                processTemplate(typeName, fc, content, osw);
            }
        }
    }

    @Override
    protected String getTemplateFileName(String filename) {
        return filename + "_json.ftl";
    }

    /** Write a FeatureCollection using normal GeoJSON encoding */
    private void handleJSONWithoutTemplate(FeatureCollection collection, OutputStreamWriter osw)
            throws IOException {
        GeoJSONGetFeatureResponse format =
                new GeoJSONGetFeatureResponse(wms.getGeoServer(), OutputFormat.JSON.getFormat());
        boolean isComplex = collection.getSchema() instanceof SimpleFeatureType;
        Writer outWriter = new BufferedWriter(osw);
        final GeoJSONBuilder jsonWriter = new GeoJSONBuilder(outWriter);
        format.writeFeatures(Arrays.asList(collection), null, isComplex, jsonWriter);
        outWriter.flush();
    }

    /**
     * Encode geometry to a valid geoJSON representation. The method is aimed to be used in free
     * marker templates.
     */
    public static String geomToGeoJSON(Geometry geometry) {
        GeometryJSON geomJSON = new GeometryJSON();
        return geomJSON.toString(geometry);
    }
}
