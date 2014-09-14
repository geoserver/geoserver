/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.json.simple.JSONObject;
import org.opengis.referencing.FactoryException;
import org.restlet.data.MediaType;

/**
 * Streams out all the enabled feture type layers into a Simple Feature Service capabilities
 * document
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class CapabilitiesJSONFormat extends StreamDataFormat {
    protected CapabilitiesJSONFormat() {
        super(MediaType.APPLICATION_JSON);
    }

    @Override
    protected Object read(InputStream in) throws IOException {
        throw new UnsupportedOperationException("Can't read capabilities documents with this class");
    }

    @Override
    protected void write(Object object, OutputStream out) throws IOException {
        Catalog catalog = (Catalog) object;
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(out);
            writer.write("[");

            // write out the layers
            List<LayerInfo> layers = getFeatureTypeLayers(catalog);
            for (Iterator it = layers.iterator(); it.hasNext();) {
                LayerInfo layerInfo = (LayerInfo) it.next();
                Map json = toJSON(layerInfo);
                JSONObject.writeJSONString(json, writer);
                if (it.hasNext()) {
                    writer.write(",\n");
                }
            }

            writer.write("]");
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    /**
     * Maps a layer info to the capabilities json structure. Using a linked hash map under covers to
     * preserve the order of the attributes
     * 
     * @param layerInfo
     * @return
     * @throws IOException
     */
    private Map toJSON(LayerInfo layerInfo) throws IOException {
        final ResourceInfo resource = layerInfo.getResource();

        try {
            LinkedHashMap json = new LinkedHashMap();
            json.put("name", resource.getPrefixedName());
            try {
                json.put("bbox", toJSON(resource.boundingBox()));
            } catch(Exception e) {
                throw ((IOException) new IOException("Failed to get the resource bounding box of:" + resource.getPrefixedName()).initCause(e));
            }
            json.put("crs", "urn:ogc:def:crs:EPSG:" + CRS.lookupEpsgCode(resource.getCRS(), false));
            json.put("axisorder", "xy");

            return json;
        } catch (FactoryException e) {
            throw ((IOException) new IOException("Failed to lookup the EPSG code").initCause(e));
        }
    }

    /**
     * Maps a referenced envelope into a json bbox
     * 
     * @param bbox
     * @return
     */
    private JSONArray toJSON(ReferencedEnvelope bbox) {
        JSONArray json = new JSONArray();
        json.add(bbox.getMinX());
        json.add(bbox.getMinY());
        json.add(bbox.getMaxX());
        json.add(bbox.getMaxY());
        return json;
    }

    /**
     * Finds all the enabled layers mapped to a FeatureTypeInfo
     * 
     * @param catalog
     * @return
     */
    List<LayerInfo> getFeatureTypeLayers(Catalog catalog) {
        // TODO: use a query/streaming mechanism once we have catalog queries to avoid 
        // loading a million layers in memory
        List<LayerInfo> layers = new ArrayList(catalog.getLayers());
        for (Iterator it = layers.iterator(); it.hasNext();) {
            LayerInfo layerInfo = (LayerInfo) it.next();
            if (!layerInfo.isEnabled()) {
                it.remove();
            } else if (!(layerInfo.getResource() instanceof FeatureTypeInfo)) {
                it.remove();
            }
        }
        return layers;
    }

}
