/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.service;

import freemarker.template.ObjectWrapper;
import java.util.Collections;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.AbstractGeoServerController;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.RestException;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Service Settings controller */
public abstract class ServiceSettingsController extends AbstractGeoServerController {
    private Class clazz;

    @Autowired
    public ServiceSettingsController(@Qualifier("geoServer") GeoServer geoServer, Class clazz) {
        super(geoServer);
        this.clazz = clazz;
    }

    @GetMapping(
        value = {"/settings", "/workspaces/{workspaceName}/settings"},
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper serviceSettingsGet(@PathVariable(required = false) String workspaceName) {
        ServiceInfo service;
        if (workspaceName != null) {
            WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspaceName);
            if (ws == null) {
                throw new RestException(
                        "Workspace " + workspaceName + " does not exist", HttpStatus.NOT_FOUND);
            }
            service = geoServer.getService(ws, clazz);
        } else {
            service = geoServer.getService(clazz);
        }
        if (service == null) {
            String errorMessage =
                    "Service does not exist"
                            + (workspaceName == null ? "" : " for workspace " + workspaceName);
            throw new RestException(errorMessage, HttpStatus.NOT_FOUND);
        }

        return wrapObject(service, clazz);
    }

    public void serviceSettingsPut(ServiceInfo info, String workspaceName) {
        WorkspaceInfo ws = null;
        if (workspaceName != null) ws = geoServer.getCatalog().getWorkspaceByName(workspaceName);

        ServiceInfo originalInfo;
        if (ws != null) {
            originalInfo = geoServer.getService(ws, clazz);
        } else {
            originalInfo = geoServer.getService(clazz);
        }
        if (originalInfo != null) {
            OwsUtils.copy(info, originalInfo, clazz);
            geoServer.save(originalInfo);
        } else {
            if (ws != null) {
                info.setWorkspace(ws);
            }
            geoServer.add(info);
        }
    }

    @DeleteMapping(value = "/workspaces/{workspaceName}/settings")
    public void serviceDelete(@PathVariable String workspaceName) {
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspaceName);
        if (ws == null) {
            throw new RestException(
                    "Workspace " + workspaceName + " does not exist", HttpStatus.NOT_FOUND);
        }
        ServiceInfo serviceInfo = geoServer.getService(ws, clazz);
        if (serviceInfo != null) {
            geoServer.remove(serviceInfo);
        }
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<>(clazz, Collections.singletonList(WorkspaceInfo.class));
    }
}
