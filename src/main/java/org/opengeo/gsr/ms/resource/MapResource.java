/* Copyright (c) 2012 LMN Solutions, inc. - www.lmnsolutions.com. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.ms.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import net.sf.json.util.JSONBuilder;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMSInfo;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.opengeo.gsr.core.exception.ServiceError;
import org.opengeo.gsr.core.format.GeoServicesJsonFormat;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

/**
 * @author Brett Antonides, LMN Solutions
 */

public class MapResource extends Resource {
    public static final Variant JSON = new Variant(new MediaType("application/json"));
    private final GeoServer geoServer;
    private final String format;
    
    public MapResource(Context context, Request request, Response response, GeoServer geoServer, String format) {
        super(context, request, response);
        this.geoServer = geoServer;
        this.format = format;
        getVariants().add(JSON);
    }
    
    public Representation getRepresentation(Variant variant) {
        if (variant == JSON) {
            try {
                return buildJsonRepresentation();
            } catch (IllegalArgumentException e) {
                ServiceError error = new ServiceError(400,
                        "Invalid arguments from client",
                        Arrays.asList(e.getMessage()));
                return buildJsonError(error);
            }
        }
        return super.getRepresentation(variant);
    }
    
    private Representation buildJsonError(ServiceError error) {
        getResponse().setStatus(new Status(error.getCode()));
        GeoServicesJsonFormat format = new GeoServicesJsonFormat();
        return format.toRepresentation(error);
    }
    
    private Representation buildJsonRepresentation() {
        if (!"json".equals(format)) throw new IllegalArgumentException("json is the only supported format");
        String workspaceName = (String) getRequest().getAttributes().get("workspace");
        if (workspaceName == null) {
            throw new IllegalArgumentException("Workspace name is required.");
        }
        WorkspaceInfo workspace = geoServer.getCatalog().getWorkspaceByName(workspaceName);
        if (workspace == null) {
            throw new NoSuchElementException("Workspace name " + workspaceName + " does not correspond to any workspace.");
        }
        WMSInfo service = geoServer.getService(workspace, WMSInfo.class);
        if (service == null) {
            service = geoServer.getService(WMSInfo.class);
        }
        List<LayerInfo> layersInWorkspace = new ArrayList<LayerInfo>();
        for (LayerInfo l : geoServer.getCatalog().getLayers()) {
            if (l.getType() == PublishedType.VECTOR && l.getResource().getStore().getWorkspace().equals(workspace)) {
                layersInWorkspace.add(l);
            }
        }
        Collections.sort(layersInWorkspace, LayerNameComparator.INSTANCE);

        return new MapRootRepresentation(service, Collections.unmodifiableList(layersInWorkspace));
    }
    
    private static final class MapRootRepresentation extends
            OutputRepresentation {
        private final WMSInfo service;
        private final List<LayerInfo> layers;

        public MapRootRepresentation(WMSInfo service, List<LayerInfo> layers) {
            super(JSON.getMediaType());
            this.service = service;
            this.layers = layers;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException {
            Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
            JSONBuilder json = new JSONBuilder(writer);
            json.object();
            String title = service.getTitle() != null ? service.getTitle() : service.getName();
            json.key("mapName").value(title);
            json.key("layers");
            encodeLayers(json, layers);
            
            Date[] dateRange = getCumulativeDateRange(layers);
            if (dateRange != null) {
                json.key("timeInfo");
                json.object();
                json.key("timeExtent");
                json.array();
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                json.value(format.format(dateRange[0]));
                json.value(format.format(dateRange[1]));
                json.endArray();
                json.endObject();
            }
            
            json.key("singleFusedMapCache").value(false);
            json.key("capabilities").value("Query");
            
            json.endObject();
            writer.flush();
            writer.close();
        }
    }
    
    private static final void encodeLayers(JSONBuilder json, List<LayerInfo> layers) {
        json.array();
        int count = 0;
        for (LayerInfo l : layers) {
            json.object();
            json.key("id").value(count);
            json.key("name").value(l.getName());
            json.endObject();
            count ++;
        }
        json.endArray();
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final Date[] getCumulativeDateRange(List<LayerInfo> layers) throws IOException {
        Comparable overallMin = null;
        Comparable overallMax = null;
        for (LayerInfo l : layers) {
            FeatureTypeInfo ftInfo = (FeatureTypeInfo) l.getResource();
            DimensionInfo dimensionInfo = ftInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
            if (dimensionInfo != null && dimensionInfo.isEnabled()) {
                String timeProperty = dimensionInfo.getAttribute();
                FeatureSource<? extends FeatureType, ? extends Feature> source = ftInfo.getFeatureSource(null, null);
                FeatureCollection<? extends FeatureType, ? extends Feature> features = source.getFeatures();
                MaxVisitor max = new MaxVisitor(timeProperty, (SimpleFeatureType) features.getSchema());
                MinVisitor min = new MinVisitor(timeProperty, (SimpleFeatureType) features.getSchema());
                features.accepts(min, null);
                features.accepts(max, null);
                if (min.getResult() != CalcResult.NULL_RESULT) {
                    if (overallMin == null) {
                        overallMin = min.getMin();
                    } else {
                        overallMin = min.getMin().compareTo(overallMin) < 0 ? min.getMin() : overallMin;
                    }
                }
                
                if (max.getResult() != CalcResult.NULL_RESULT) {
                    if (overallMax == null) {
                        overallMax = max.getMax();
                    } else {
                        overallMax = max.getMax().compareTo(overallMax) > 0 ? max.getMax() : overallMax;
                    }
                }
            }
        }
        if (overallMin == null || overallMax == null) {
            return null;
        } else {
            return new Date[] { (Date) overallMin, (Date) overallMax };
        }
    }
     
}
