/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download.rest;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.lang.reflect.Type;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.rest.wrapper.RestWrapperAdapter;
import org.geoserver.wps.gs.download.DownloadServiceConfiguration;
import org.geoserver.wps.gs.download.DownloadServiceConfigurationWatcher;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
@RequestMapping(
        path = RestBaseController.ROOT_PATH + "/services/wps/download",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        })
public class WPSDownloadController extends RestBaseController {

    private final DownloadServiceConfigurationWatcher watcher;

    public WPSDownloadController(DownloadServiceConfigurationWatcher watcher) {
        this.watcher = watcher;
    }

    @ResponseBody
    @GetMapping
    public RestWrapper<DownloadServiceConfiguration> getConfiguration() {
        return new RestWrapperAdapter<>(
                watcher.getConfiguration(), DownloadServiceConfiguration.class, this);
    }

    @PutMapping(
            consumes = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.TEXT_XML_VALUE
            })
    public void setConfiguration(@RequestBody DownloadServiceConfiguration configuration)
            throws IOException {
        watcher.setConfiguration(configuration);
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return DownloadServiceConfiguration.class.isAssignableFrom(
                methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xs = persister.getXStream();
        xs.alias("DownloadServiceConfiguration", DownloadServiceConfiguration.class);
        xs.allowTypes(new Class[] {DownloadServiceConfiguration.class});
    }
}
