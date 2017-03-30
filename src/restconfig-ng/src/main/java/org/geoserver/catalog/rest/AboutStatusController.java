/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.platform.RenderingEngineStatus;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thoughtworks.xstream.XStream;

@RestController @RequestMapping(path = RestBaseController.ROOT_PATH, 
produces = {MediaType.APPLICATION_JSON_VALUE,
    MediaType.APPLICATION_XML_VALUE,
    MediaType.TEXT_HTML_VALUE})
public class AboutStatusController extends RestBaseController {

    @GetMapping(value = "/about/status") 
    protected RestWrapper<ModuleStatus> getStatus() throws Exception {
        List<ModuleStatus> applicationStatus = GeoServerExtensions.extensions(ModuleStatus.class).stream()
                .map(ModuleStatusImpl::new).collect(Collectors.toList());
        return wrapList(applicationStatus, ModuleStatus.class);
    }

    @GetMapping(value = "/about/status/{target}") 
    protected RestWrapper<ModuleStatus> getStatus(@PathVariable String target) throws Exception {
        List<ModuleStatus>  applicationStatus = GeoServerExtensions.extensions(ModuleStatus.class).stream().map(ModuleStatusImpl::new).filter(
                getModule(target)).collect(Collectors.toList());
        if (applicationStatus.isEmpty()) {
            throw new RestException("No such module: " + target, HttpStatus.NOT_FOUND);
        }
        return wrapList(applicationStatus, ModuleStatus.class);
    }

    protected static Predicate<ModuleStatus> getModule(String target) {
        return m -> m.getModule().equalsIgnoreCase(target);
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xs = persister.getXStream();
        xs.processAnnotations(ModuleStatus.class);
        xs.allowTypes(new Class[] { ModuleStatus.class });
        xs.alias("about", List.class);
        xs.alias("status", ModuleStatus.class);
        xs.addDefaultImplementation(ModuleStatusImpl.class, ModuleStatus.class);
        xs.addDefaultImplementation(RenderingEngineStatus.class, ModuleStatus.class);
    }

}