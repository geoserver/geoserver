/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.service;

import java.lang.reflect.Type;
import java.util.Map;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.WCSXStreamLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.*;

/** WCS Settings controller */
@RestController
@ControllerAdvice
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/services/wcs",
    produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_HTML_VALUE
    }
)
public class WCSSettingsController extends ServiceSettingsController {

    @Autowired
    public WCSSettingsController(GeoServer geoServer) {
        super(geoServer, WCSInfo.class);
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
            @RequestBody WCSInfo info, @PathVariable(required = false) String workspaceName) {
        super.serviceSettingsPut(info, workspaceName);
    }

    @Override
    public String getTemplateName(Object object) {
        return "wcsSettings";
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return WCSInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setHideFeatureTypeAttributes();
        persister.setCallback(
                new XStreamPersister.Callback() {
                    @Override
                    protected ServiceInfo getServiceObject() {
                        Map<String, String> uriTemplateVars = getURITemplateVariables();
                        String workspace = uriTemplateVars.get("workspaceName");
                        ServiceInfo service;
                        if (workspace != null) {
                            WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspace);
                            service = geoServer.getService(ws, WCSInfo.class);
                        } else {
                            service = geoServer.getService(WCSInfo.class);
                        }
                        return service;
                    }

                    @Override
                    protected Class<WCSInfo> getObjectClass() {
                        return WCSInfo.class;
                    }
                });
        WCSXStreamLoader.initXStreamPersister(persister);
    }
}
