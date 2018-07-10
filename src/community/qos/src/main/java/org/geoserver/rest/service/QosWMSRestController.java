/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.service;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import java.util.logging.Logger;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.qos.QosXstreamAliasConfigurator;
import org.geoserver.qos.wms.WmsQosConfigurationLoader;
import org.geoserver.qos.xml.QosMainConfiguration;
import org.geoserver.rest.AbstractGeoServerController;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.wms.WMSInfo;
import org.geotools.util.logging.Logging;
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
    path = RestBaseController.ROOT_PATH + "/services/qos/wms",
    produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_HTML_VALUE
    }
)
public class QosWMSRestController extends AbstractGeoServerController {
    private static final Logger LOGGER = Logging.getLogger(QosWMSRestController.class);

    protected QosXstreamAliasConfigurator aliasConfig = QosXstreamAliasConfigurator.instance();

    @Autowired
    public QosWMSRestController(GeoServer geoServer) {
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
        ServiceInfo service = getServiceInfo(workspaceName);
        getLoader().setConfiguration((WMSInfo) service, config);
    }

    protected ServiceInfo getServiceInfo(String workspaceName) {
        ServiceInfo service;
        if (workspaceName != null) {
            WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspaceName);
            if (ws == null) {
                throw new RestException(
                        "Workspace " + workspaceName + " does not exist", HttpStatus.NOT_FOUND);
            }
            service = geoServer.getService(ws, WMSInfo.class);
        } else {
            service = geoServer.getService(WMSInfo.class);
        }
        return service;
    }

    private WmsQosConfigurationLoader getLoader() {
        return (WmsQosConfigurationLoader)
                GeoServerExtensions.bean(WmsQosConfigurationLoader.SPRING_KEY);
    }

    protected QosXstreamAliasConfigurator getAliasConfig() {
        return aliasConfig;
    }

    @Override
    public String getTemplateName(Object object) {
        return "wmsQosSettings";
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        BeansWrapper wrapper = DefaultObjectWrapper.getDefaultInstance();
        wrapper.setExposureLevel(DefaultObjectWrapper.EXPOSE_ALL);
        return wrapper;
    }
}
