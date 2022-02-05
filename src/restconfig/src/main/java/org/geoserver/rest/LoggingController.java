/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.lang.reflect.Type;
import org.geoserver.config.GeoServer;
import org.geoserver.config.LoggingInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/logging")
public class LoggingController extends AbstractGeoServerController {

    @Autowired
    public LoggingController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.TEXT_HTML_VALUE
            })
    public RestWrapper<LoggingInfo> settingsGet() {
        return wrapObject(geoServer.getLogging(), LoggingInfo.class);
    }

    @PutMapping(
            consumes = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.TEXT_XML_VALUE
            })
    public void settingsPut(@RequestBody LoggingInfo loggingInfo) {
        LoggingInfo original = geoServer.getLogging();
        OwsUtils.copy(loggingInfo, original, LoggingInfo.class);
        geoServer.save(original);
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return LoggingInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }
}
