/* Copyright (c) 2014 Boundless - boundlessgeo.com. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.fs.resource;

import org.opengeo.gsr.core.exception.ServiceError;
import org.opengeo.gsr.core.feature.FeatureEncoder;
import org.opengeo.gsr.core.format.GeoServicesJsonFormat;
import org.opengeo.gsr.ms.resource.LayersAndTables;
import org.opengeo.gsr.ms.resource.LayerOrTable;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import net.sf.json.util.JSONBuilder;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * @author David Winslow, Boundless
 */
public class FeatureResource extends Resource {
    private static final FilterFactory2 FILTERS = CommonFactoryFinder.getFilterFactory2();
    public static final Variant JSON = new Variant(new MediaType("application/json"));
    private final String format;
    private final String featureId;
    private final Catalog catalog;

    public FeatureResource(Context context, Request request, Response response, Catalog catalog, String format, String featureId) {
        super(context, request, response);
        this.catalog = catalog;
        this.format = format;
        this.featureId = featureId;
        getVariants().add(JSON);
    }

    @Override
    public Representation getRepresentation(Variant variant) {
        if (variant == JSON) {
            try {
                return buildJsonRepresentation();
            } catch (Exception e) {
                return buildJsonError(new ServiceError(400, "Invalid arguments from client", Arrays.asList(e.getMessage())));
            }
        }
        return super.getRepresentation(variant);
    }

    private Representation buildJsonError(ServiceError error) {
        getResponse().setStatus(new Status(error.getCode()));
        
        GeoServicesJsonFormat format = new GeoServicesJsonFormat();
        return format.toRepresentation(error);
    }

    private Representation buildJsonRepresentation() throws IOException {
        if (!"json".equals(format)) throw new IllegalArgumentException("json is the only supported format");
        String workspace = (String) getRequest().getAttributes().get("workspace");

        String layerOrTableId = (String) getRequest().getAttributes().get("layerOrTable");
        Integer layerOrTableIndex = Integer.valueOf(layerOrTableId);
        LayerOrTable l = LayersAndTables.find(catalog, workspace, layerOrTableIndex);

        if (null == l) {
            throw new NoSuchElementException("No table or layer in workspace \"" + workspace + " for id " + layerOrTableId);
        }

        FeatureTypeInfo featureType = (FeatureTypeInfo) l.layer.getResource();
        if (null == featureType) {
            throw new NoSuchElementException("No table or layer in workspace \"" + workspace + " for id " + layerOrTableId);
        }


        Filter idFilter = FILTERS.id(FILTERS.featureId(featureType.getFeatureType().getName().getLocalPart() + "." + featureId));

        return new JsonFeatureRepresentation(featureType, idFilter);
    }

    private static class JsonFeatureRepresentation extends OutputRepresentation {
        Filter filter;
        FeatureTypeInfo featureType;

        public JsonFeatureRepresentation(FeatureTypeInfo featureType, Filter filter) {
            super(MediaType.APPLICATION_JAVASCRIPT);
            this.featureType = featureType;
            this.filter = filter;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException {
            Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
            JSONBuilder json = new JSONBuilder(writer);
            FeatureSource<?, ?> source = featureType.getFeatureSource(null, null);
            json.object().key("feature");
            FeatureCollection<?, ?> featureColl = source.getFeatures(filter);
            Feature[] featureArr = featureColl.toArray(new Feature[0]);
            FeatureEncoder.featureToJson(featureArr[0], json, true);
            json.endObject();
            writer.flush();
            writer.close();
            outputStream.close();
        }
    }
}
