/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.service.rest;

import java.io.File;
import java.util.List;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.RestletException;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class ServiceSettingsResource extends AbstractCatalogResource {

    protected GeoServer geoServer;

    private Class clazz;

    public ServiceSettingsResource(Context context, Request request, Response response,
            Class clazz, GeoServer geoServer) {
        super(context, request, response, clazz, geoServer.getCatalog());
        this.clazz = clazz;
        this.geoServer = geoServer;
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public boolean allowDelete() {
        String workspace = getAttribute("workspace");
        if (workspace != null) {
            WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspace);
            return geoServer.getService(ws, clazz) != null;
        }
        return false;
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        String workspace = getAttribute("workspace");
        ServiceInfo service;
        if (workspace != null) {
            WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspace);
            
            service = geoServer.getService(ws, clazz);
        } else {
            service = geoServer.getService(clazz);
        }
        if (service == null) {
            throw new RestletException("Service for workspace " + workspace + " does not exist",
                    Status.CLIENT_ERROR_NOT_FOUND);
        }

        return service;
    }

    @Override
    protected void handleObjectPut(Object object) throws Exception {
        String workspace = getAttribute("workspace");
        WorkspaceInfo ws = null;
        if(workspace!=null) ws = geoServer.getCatalog().getWorkspaceByName(workspace);

        ServiceInfo serviceInfo = (ServiceInfo) object;
        ServiceInfo originalInfo;
        if(ws!=null){
            originalInfo = geoServer.getService(ws, clazz);
        } else {
            originalInfo = geoServer.getService(clazz);
        }
        if (originalInfo != null) {
            OwsUtils.copy(serviceInfo, originalInfo, clazz);
            geoServer.save(originalInfo);
        } else {
            if(ws!=null) {
                serviceInfo.setWorkspace(ws);
            }
            geoServer.add(serviceInfo);
        }
    }

    @Override
    protected void handleObjectDelete() throws Exception {
        String workspace = getAttribute("workspace");
        if (workspace != null) {
            WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspace);
            ServiceInfo serviceInfo = geoServer.getService(ws, clazz);
            if (serviceInfo != null) {
                geoServer.remove(serviceInfo);
            }
        }
    }

}
