/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.service;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.qos.wfs.WfsQosConfigurationLoader;
import org.geoserver.qos.xml.QosMainConfiguration;
import org.geoserver.rest.AbstractGeoServerController;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.wfs.WFSInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/services/qos/wfs",
    produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_HTML_VALUE
    }
)
public class QosWFSRestController extends AbstractGeoServerController {

    @Autowired
    public QosWFSRestController(GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(
        value = {"/settings", "/workspaces/{workspaceName}/settings"},
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper<QosMainConfiguration> serviceSettingsGet(
            @PathVariable(required = false) String workspaceName) {
        ServiceInfo service = getServiceInfo(workspaceName);
        QosMainConfiguration qosConfig = getLoader().getConfiguration(service);
        return wrapObject(qosConfig, QosMainConfiguration.class);
    }

    @PutMapping(
        value = {"/settings", "/workspaces/{workspaceName}/settings"},
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public void serviceSettingsPut(
            @RequestBody QosMainConfiguration config,
            @PathVariable(required = false) String workspaceName) {
        WFSInfo service = getServiceInfo(workspaceName);
        getLoader().setConfiguration(service, config);
    }

    protected WFSInfo getServiceInfo(String workspaceName) {
        WFSInfo service;
        if (workspaceName != null) {
            WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspaceName);
            if (ws == null) {
                throw new RestException(
                        "Workspace " + workspaceName + " does not exist", HttpStatus.NOT_FOUND);
            }
            service = geoServer.getService(ws, WFSInfo.class);
        } else {
            service = geoServer.getService(WFSInfo.class);
        }
        return service;
    }

    private WfsQosConfigurationLoader getLoader() {
        return (WfsQosConfigurationLoader)
                GeoServerExtensions.bean(WfsQosConfigurationLoader.SPRING_BEAN_NAME);
    }

    @Override
    public String getTemplateName(Object object) {
        return "wfsQosSettings";
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        BeansWrapper wrapper = DefaultObjectWrapper.getDefaultInstance();
        wrapper.setExposureLevel(DefaultObjectWrapper.EXPOSE_ALL);
        return wrapper;
    }
}
