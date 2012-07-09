/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.service.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.RestletException;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.GMLInfoImpl;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSInfoImpl;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
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

    private String serviceXmlFileName;

    private List<XStreamServiceLoader> loaders;

    private GeoServerResourceLoader resourceLoader;

    public ServiceSettingsResource(Context context, Request request, Response response,
            Class clazz, GeoServer geoServer) {
        super(context, request, response, clazz, geoServer.getCatalog());
        this.clazz = clazz;
        this.geoServer = geoServer;
        if (clazz.equals(WCSInfo.class)) {
            serviceXmlFileName = "wcs.xml";
        } else if (clazz.equals(WMSInfo.class)) {
            serviceXmlFileName = "wms.xml";
        } else if (clazz.equals(WFSInfo.class)) {
            serviceXmlFileName = "wfs.xml";
        }
        loaders = GeoServerExtensions.extensions(XStreamServiceLoader.class);
        resourceLoader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
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
        File root = resourceLoader.find("");
        if (workspace != null) {
            WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspace);
            if (geoServer.getService(ws, clazz) == null) {
                throw new RestletException(
                        "Service for workspace " + workspace + " does not exist",
                        Status.CLIENT_ERROR_NOT_FOUND);
            }
            File workspaces = resourceLoader.find("workspaces");
            File workspaceDir = resourceLoader.find(workspaces + "/" + ws.getName());
            for (XStreamServiceLoader<ServiceInfo> loader : loaders) {
                if (loader.getFilename().equals(serviceXmlFileName)) {
                    return loader.load(geoServer, workspaceDir);
                }
            }

        }
        if (geoServer.getService(clazz) == null) {
            throw new RestletException("Service for workspace " + workspace + " does not exist",
                    Status.CLIENT_ERROR_NOT_FOUND);
        }

        for (XStreamServiceLoader<ServiceInfo> loader : loaders) {
            if (loader.getFilename().equals(serviceXmlFileName)) {
                return loader.load(geoServer, root);
            }
        }
        return (ServiceInfo) geoServer.getService(clazz);
    }

    @Override
    protected void handleObjectPut(Object object) throws Exception {
        String workspace = getAttribute("workspace");
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspace);
        XStreamServiceLoader serviceLoader = null;
        for (XStreamServiceLoader<ServiceInfo> loader : loaders) {
            if (loader.getFilename().equals(serviceXmlFileName)) {
                serviceLoader = loader;
            }
        }
        ServiceInfo original = null;
        File root = resourceLoader.find("");
        File workspaces = resourceLoader.find("workspaces");
        File workspaceDir = resourceLoader.find(workspaces + "/" + ws.getName());
        if (workspace != null) {
            if (geoServer.getService(ws, clazz) != null) {
                original = serviceLoader.load(geoServer, workspaceDir);
                OwsUtils.copy(object, original, clazz);
                serviceLoader.save(original, geoServer, workspaceDir);
            } else {
                ServiceInfo serviceInfo = (ServiceInfo) object;
                addDefaultsIfMissing(serviceInfo);
                serviceInfo.setWorkspace(ws);
                geoServer.add(serviceInfo);
            }
        } else {
            original = serviceLoader.load(geoServer, root);
            OwsUtils.copy(object, original, clazz);
            serviceLoader.save(original, geoServer, root);
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

    private void addDefaultsIfMissing(ServiceInfo serviceInfo) {
        if (serviceInfo instanceof WMSInfoImpl) {
            WMSInfoImpl wmsInfo = (WMSInfoImpl) serviceInfo;
            if (wmsInfo.getAuthorityURLs() == null) {
                List<AuthorityURLInfo> authorityURLS = new ArrayList<AuthorityURLInfo>();
                wmsInfo.setAuthorityURLs(authorityURLS);
            }
            if (wmsInfo.getIdentifiers() == null) {
                List<LayerIdentifierInfo> identifiers = new ArrayList<LayerIdentifierInfo>();
                wmsInfo.setIdentifiers(identifiers);
            }
            if (wmsInfo.getSRS() == null) {
                List<String> srsList = new ArrayList<String>();
                wmsInfo.setSRS(srsList);
            }
        } else if (serviceInfo instanceof WFSInfoImpl) {
            WFSInfoImpl wfsInfo = (WFSInfoImpl) serviceInfo;
            if (wfsInfo.getGML() == null) {
                GMLInfoImpl gml3Info = new GMLInfoImpl();
                gml3Info.setOverrideGMLAttributes(true);
                Map<WFSInfo.Version, GMLInfo> gml = new HashMap<WFSInfo.Version, GMLInfo>();
                wfsInfo.setGML(gml);
                wfsInfo.getGML().put(WFSInfo.Version.V_11, gml3Info);
                wfsInfo.getGML().put(WFSInfo.Version.V_10, gml3Info);
                wfsInfo.getGML().put(WFSInfo.Version.V_20, gml3Info);
            }
        }
    }

}
