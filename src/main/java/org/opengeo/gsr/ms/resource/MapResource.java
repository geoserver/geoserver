/* Copyright (c) 2012 LMN Solutions, inc. - www.lmnsolutions.com. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.ms.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import net.sf.json.util.JSONBuilder;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMSInfo;
import org.opengeo.gsr.core.exception.ServiceError;
import org.opengeo.gsr.core.format.GeoServicesJsonFormat;
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
    public static final Variant JSON = new Variant(MediaType.APPLICATION_JAVASCRIPT);
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
            if (l.getResource().getStore().getWorkspace().equals(workspace)) {
                layersInWorkspace.add(l);
            } else {
                System.out.println(l.getResource().getStore().getWorkspace());
            }
        }
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
            json.key("mapName").value(service.getTitle());
            json.key("layers");
            encodeLayers(json, layers);
            json.key("singleFusedMapCache").value(false);
            json.key("capabilities").value("Query");
            json.endObject();
            writer.flush();
            writer.close();
        }
    }
    
    private static final void encodeLayers(JSONBuilder json, List<LayerInfo> layers) {
        json.array();
        for (LayerInfo l : layers) {
            json.object();
            json.key("id").value(l.getId());
            json.endObject();
        }
        json.endArray();
    }
     
}
