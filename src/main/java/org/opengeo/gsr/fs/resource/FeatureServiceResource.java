/* Copyright (c) 2014 Boundless - boundlessgeo.com. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.fs.resource;

import org.opengeo.gsr.core.exception.ServiceError;
import org.opengeo.gsr.ms.resource.LayerNameComparator;

import org.geoserver.config.GeoServer;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.wfs.WFSInfo;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author David Winslow, Boundless
 */
public class FeatureServiceResource extends Resource {
    public static final Variant JSON = new Variant(new MediaType("application/json"));
    private final GeoServer geoserver;
    private final String format;

    public FeatureServiceResource(Context context, Request request, Response response, GeoServer geoserver, String format) {
        super(context, request, response);
        this.geoserver = geoserver;
        this.format = format;
        getVariants().add(JSON);
    }

    public Representation getRepresentation(Variant variant) {
        if (variant == JSON) {
            try {
                return buildJsonRepresentation();
            } catch (IllegalArgumentException e) {
                ServiceError error = new ServiceError(400, "Invalid arguments from client",
                        Arrays.asList(e.getMessage()));
            }
        }
        return super.getRepresentation(variant);
    }

    private Representation buildJsonRepresentation() {
        if (!"json".equals(format)) throw new IllegalArgumentException();
        String workspaceName = (String) getRequest().getAttributes().get("workspace");
        if (workspaceName == null) {
            throw new IllegalArgumentException("Workspace name is required");
        }
        WorkspaceInfo workspace = geoserver.getCatalog().getWorkspaceByName(workspaceName);
        if (workspace == null) {
            throw new NoSuchElementException("Workspace name " + workspaceName + " does not correspond to any workspace.");
        }
        WFSInfo service = geoserver.getService(workspace, WFSInfo.class);
        if (service == null) {
            service = geoserver.getService(WFSInfo.class);
        }
        List<LayerInfo> layersInWorkspace = new ArrayList<LayerInfo>();
        for (LayerInfo l : geoserver.getCatalog().getLayers()) {
            if (l.getType() == LayerInfo.Type.VECTOR && l.getResource().getStore().getWorkspace().equals(workspace)) {
                layersInWorkspace.add(l);
            }
        }
        Collections.sort(layersInWorkspace, LayerNameComparator.INSTANCE);
        return new FeatureRootRepresentation(service, Collections.unmodifiableList(layersInWorkspace));
    }

    private class FeatureRootRepresentation extends OutputRepresentation {
        private final WFSInfo service;
        private final List<LayerInfo> layers;

        public FeatureRootRepresentation(WFSInfo service, List<LayerInfo> layers) {
            super(new MediaType("application/json"));
            this.service = service;
            this.layers = layers;
        }

        @Override
        public void write(OutputStream out) throws IOException {
            Writer writer = new OutputStreamWriter(out, "UTF-8");
            JSONBuilder json = new JSONBuilder(writer);
            json.object();
            json.key("currentVersion").value(10.21);
            String description = service.getTitle() != null ? service.getTitle() :  service.getName();
            json.key("serviceDescription").value(description);
            // json.key("hasVersionedData").value(false);
            // json.key("supportsDisconnectedEditing").value(false);
            // json.key("hasStaticData").value(false);
            // json.key("maxRecordCount").value(0);
            json.key("supportedQueryFormats").value("JSON");
            // json.key("capabilities").value("query");
            // json.key("description").value("");
            // json.key("copyrightText").value("");
            // json.key("spatialReference").value(null);
            json.key("initialExtent").value(null);
            json.key("fullExtent").value(null);
            // json.key("allowGeometryUpdates").value(true);
            // json.key("units").value("");
            // json.key("syncEnabled").value(false);
            // json.key("syncCapabilities").value(null);
            // json.key("editorTrackingInfo").value(null);
            // json.key("documentInfo").value(null);
            json.key("layers").array();
            for (int i = 0 ; i < layers.size(); i++) {
                LayerInfo l = layers.get(i);
                json.object();
                json.key("id").value(i);
                json.key("name").value(l.getName());
                json.endObject();
            }
            json.endArray();
            json.key("tables").array().endArray();
            json.endObject();
            writer.flush();
        }
    }
}
