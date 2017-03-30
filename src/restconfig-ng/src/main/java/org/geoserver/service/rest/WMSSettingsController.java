/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.service.rest;

import freemarker.template.ObjectWrapper;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.CatalogController;
import org.geoserver.catalog.rest.LayerGroupController;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.GeoServerController;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSXStreamLoader;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

/**
 * WMS Settings controller
 */
@RestController
@ControllerAdvice
@RequestMapping(path = "/restng/services/wms", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_HTML_VALUE})
public class WMSSettingsController extends GeoServerController {
    private static final Logger LOGGER = Logging.getLogger(LayerGroupController.class);

    @Autowired
    public WMSSettingsController(GeoServer geoServer) { super(geoServer); };

    @GetMapping( value = {"/settings", "/settings/workspaces/{workspace}/settings"},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE} )
    public RestWrapper getWmsSettings(@PathVariable ( name = "workspace", required = false) String workspaceName) {
        WMSInfo service;
        if (workspaceName != null) {
            WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspaceName);
            if (ws == null) {
                throw new RestException("Workspace " + workspaceName + " does not exist", HttpStatus.NOT_FOUND);
            }
            service = geoServer.getService(ws, WMSInfo.class);
        } else {
            service = geoServer.getService(WMSInfo.class);
        }
        if (service == null) {
            String errorMessage = "Service does not exist" +
                    (workspaceName == null ? "" : " for workspace " + workspaceName);
            throw new RestException(errorMessage, HttpStatus.NOT_FOUND);
        }

        return wrapObject(service, WMSInfo.class);
    }

    @PutMapping( value = {"/settings", "/settings/workspaces/{workspace}/settings"},
            consumes = {MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
                    MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public void putWmsSettings(@RequestBody WMSInfo info,
                               @PathVariable ( name = "workspace", required = false) String workspaceName) {
        WorkspaceInfo ws = null;
        if(workspaceName!=null) ws = geoServer.getCatalog().getWorkspaceByName(workspaceName);

        WMSInfo originalInfo;
        if(ws!=null){
            originalInfo = geoServer.getService(ws, WMSInfo.class);
        } else {
            originalInfo = geoServer.getService(WMSInfo.class);
        }
        if (originalInfo != null) {
            OwsUtils.copy(info, originalInfo, WMSInfo.class);
            geoServer.save(originalInfo);
        } else {
            if(ws!=null) {
                info.setWorkspace(ws);
            }
            geoServer.add(info);
        }
    }

    @Override
    public String getTemplateName(Object object) {
        return "wmsSettings";
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return WMSInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<>(clazz, Arrays.asList(WorkspaceInfo.class));
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setHideFeatureTypeAttributes();
        persister.setCallback( new XStreamPersister.Callback() {
            @Override
            protected ServiceInfo getServiceObject() {
                Map<String, String> uriTemplateVars = (Map<String, String>) RequestContextHolder.getRequestAttributes().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
                String workspace = uriTemplateVars.get("workspace");
                ServiceInfo service;
                if (workspace != null) {
                    WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspace);
                    service = geoServer.getService(ws, WMSInfo.class);
                } else {
                    service = geoServer.getService(WMSInfo.class);
                }
                return service;
            }
            @Override
            protected Class<WMSInfo> getObjectClass() {
                return WMSInfo.class;
            }
        });
        WMSXStreamLoader.initXStreamPersister(persister);
    }


}
