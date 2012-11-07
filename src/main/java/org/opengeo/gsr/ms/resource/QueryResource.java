package org.opengeo.gsr.ms.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.NoSuchElementException;

import net.sf.json.util.JSONBuilder;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.FeatureSource;
import org.opengeo.gsr.core.feature.FeatureEncoder;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

public class QueryResource extends Resource {
    public static Variant JSON = new Variant(MediaType.APPLICATION_JAVASCRIPT);
    public QueryResource(Context context, Request request, Response response, Catalog catalog, String format) {
        super(context, request, response);
        this.catalog = catalog;
        this.format = format;
        getVariants().add(JSON);
    }
    
    private final Catalog catalog;
    private final String format;
    
    @Override
    public Representation getRepresentation(Variant variant) {
        if (variant == JSON) {
            if (!"json".equals(format));
            String workspace = (String) getRequest().getAttributes().get("workspace");
            String layerOrTableName = (String) getRequest().getAttributes().get("layerOrTable");
            FeatureTypeInfo featureType = catalog.getFeatureTypeByName(workspace, layerOrTableName);
            if (null == featureType) {
                throw new NoSuchElementException("No known table or layer with qualified name \"" + workspace + ":" + layerOrTableName + "\"");
            }
            return new JsonQueryRepresentation(featureType);
        }
        return super.getRepresentation(variant);
    }
    
    private static class JsonQueryRepresentation extends OutputRepresentation {
        private final FeatureTypeInfo featureType;
        
        public JsonQueryRepresentation(FeatureTypeInfo featureType) {
            super(MediaType.APPLICATION_JAVASCRIPT);
            this.featureType = featureType;
        }
        
        @Override
        public void write(OutputStream outputStream) throws IOException {
            Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
            JSONBuilder json = new JSONBuilder(writer);
            FeatureSource<? extends FeatureType, ? extends Feature> source =
                    featureType.getFeatureSource(null, null);
            FeatureEncoder.featuresToJson(source.getFeatures(), json);
            writer.flush();
            writer.close();
        }
    }
}
