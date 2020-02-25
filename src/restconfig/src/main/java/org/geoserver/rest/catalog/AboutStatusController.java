/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import com.thoughtworks.xstream.XStream;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.CollectionModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.text.StringEscapeUtils;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.platform.RenderingEngineStatus;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/about/status",
    produces = {
        MediaType.TEXT_HTML_VALUE,
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
    }
)
public class AboutStatusController extends RestBaseController {

    @GetMapping
    protected RestWrapper<ModuleStatus> statusGet() throws Exception {
        List<ModuleStatus> applicationStatus =
                GeoServerExtensions.extensions(ModuleStatus.class)
                        .stream()
                        .map(ModuleStatusImpl::new)
                        .collect(Collectors.toList());
        return wrapList(applicationStatus, ModuleStatus.class);
    }

    @GetMapping(value = "/{target}")
    protected RestWrapper<ModuleStatus> statusGet(@PathVariable String target) throws Exception {
        List<ModuleStatus> applicationStatus =
                GeoServerExtensions.extensions(ModuleStatus.class)
                        .stream()
                        .map(ModuleStatusImpl::new)
                        .filter(getModule(target))
                        .collect(Collectors.toList());
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
        xs.allowTypes(new Class[] {ModuleStatus.class});
        xs.alias("about", List.class);
        xs.alias("status", ModuleStatus.class);
        xs.addDefaultImplementation(ModuleStatusImpl.class, ModuleStatus.class);
        xs.addDefaultImplementation(RenderingEngineStatus.class, ModuleStatus.class);
    }

    @Override
    protected String getTemplateName(Object object) {
        return "ModuleStatusImpl";
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return ModuleStatus.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new BeansWrapper() {
            @Override
            public TemplateModel wrap(Object obj) throws TemplateModelException {
                if (obj instanceof List) { // we expect List of ModuleStatus
                    List<?> list = (List<?>) obj;
                    SimpleHash hash = new SimpleHash();
                    hash.put(
                            "values",
                            new CollectionModel(
                                    list,
                                    new BeansWrapper() {
                                        public TemplateModel wrap(Object object)
                                                throws TemplateModelException {
                                            if (object instanceof ModuleStatus) {
                                                ModuleStatus status = (ModuleStatus) object;
                                                SimpleHash hash = new SimpleHash();
                                                hash.put("module", status.getModule());
                                                hash.put("name", status.getName());
                                                hash.put(
                                                        "isAvailable",
                                                        Boolean.toString(status.isAvailable()));
                                                hash.put(
                                                        "isEnabled",
                                                        Boolean.toString(status.isEnabled()));
                                                status.getComponent()
                                                        .ifPresent(
                                                                component ->
                                                                        hash.put(
                                                                                "component",
                                                                                component));
                                                status.getVersion()
                                                        .ifPresent(
                                                                version ->
                                                                        hash.put(
                                                                                "version",
                                                                                version));
                                                // Make sure to escape the string, otherwise strange
                                                // chars here will bork the XML parser later

                                                status.getMessage()
                                                        .ifPresent(
                                                                message -> {
                                                                    String noControlChars =
                                                                            message.replaceAll(
                                                                                            "\u001b",
                                                                                            "ESC")
                                                                                    .replaceAll(
                                                                                            "\u0008",
                                                                                            "BACK")
                                                                                    .replaceAll(
                                                                                            "\u0007",
                                                                                            "BELL");
                                                                    String escaped =
                                                                            StringEscapeUtils
                                                                                    .escapeXml10(
                                                                                            noControlChars)
                                                                                    .replaceAll(
                                                                                            "\n",
                                                                                            "<br/>");
                                                                    hash.put("message", escaped);
                                                                });

                                                return hash;
                                            }
                                            return super.wrap(object);
                                        }
                                    }));
                    return hash;
                }
                return super.wrap(obj);
            }
        };
    }
}
