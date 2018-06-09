/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.geoserver.ManifestLoader;
import org.geoserver.ManifestLoader.AboutModel;
import org.geoserver.ManifestLoader.AboutModel.AboutModelType;
import org.geoserver.ManifestLoader.AboutModel.ManifestModel;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/about",
    produces = {
        MediaType.TEXT_HTML_VALUE,
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
    }
)
@ControllerAdvice
public class AboutController extends RestBaseController {

    @GetMapping(value = "/manifest")
    public RestWrapper<AboutModel> manifestGet(
            @RequestParam(name = "manifest", required = false) String regex,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String value) {

        return wrapObject(
                getModel(AboutModelType.RESOURCES, regex, from, to, key, value), AboutModel.class);
    }

    @GetMapping(value = "/version")
    public RestWrapper<AboutModel> versionGet(
            @RequestParam(name = "manifest", required = false) String regex,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String value) {

        return wrapObject(
                getModel(AboutModelType.VERSIONS, regex, from, to, key, value), AboutModel.class);
    }

    protected AboutModel getModel(
            AboutModelType type, String regex, String from, String to, String key, String value) {
        AboutModel model = null;

        // filter name by regex
        if (regex != null) {
            model = buildAboutModel(type).filterNameByRegex(regex);
        }

        // filter name by range
        if (from != null && to != null) {
            if (model != null) {
                model = model.filterNameByRange(from, to);
            } else {
                model = buildAboutModel(type).filterNameByRange(from, to);
            }
        }

        // filter by properties
        if (model == null) {
            model = buildAboutModel(type);
        }
        if (key != null && value != null) {
            model = model.filterPropertyByKeyValue(value, key);
        } else if (key != null) {
            model = model.filterPropertyByKey(key);
        } else if (value != null) {
            model = model.filterPropertyByValue(value);
        }

        if (model != null) {
            return model;
        } else {
            return buildAboutModel(type);
        }
    }

    private static AboutModel buildAboutModel(AboutModelType type) {
        if (type.equals(AboutModelType.RESOURCES)) {
            // if request is for resource return the resources
            return ManifestLoader.getResources();
        } else {
            // get the version
            return ManifestLoader.getVersions();
        }
    }

    @Override
    protected String getTemplateName(Object object) {
        if (object instanceof AboutModel) {
            return "AboutModel.ftl";
        } else {
            return null;
        }
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return AboutModel.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {

        if (AboutModel.class.isAssignableFrom(clazz)) {
            return new ObjectToMapWrapper<AboutModel>(AboutModel.class) {
                @Override
                protected void wrapInternal(Map properties, SimpleHash model, AboutModel object) {
                    final List<Map<String, Object>> manifests = new ArrayList<>();
                    for (ManifestModel manifest : object.getManifests()) {
                        final Map<String, Object> map = new HashMap<>();
                        map.put("name", manifest.getName());

                        final List<String> props = new ArrayList<>();
                        map.put("properties", props);

                        final List<String> values = new ArrayList<>();
                        map.put("valuez", values);

                        for (String key : manifest.getEntries().keySet()) {
                            props.add(key);
                            values.add(manifest.getEntries().get(key));
                        }
                        manifests.add(map);
                    }

                    properties.put("manifests", manifests);
                }
            };
        } else {
            return null;
        }
    }

    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xs = persister.getXStream();

        // AboutModel
        xs.processAnnotations(AboutModel.class);
        xs.allowTypes(new Class[] {AboutModel.class});
        xs.addImplicitCollection(AboutModel.class, "manifests");
        xs.alias("about", AboutModel.class);

        // ManifestModel Xstream converter
        xs.registerConverter(
                new Converter() {

                    @Override
                    public boolean canConvert(Class type) {
                        return type.equals(ManifestModel.class);
                    }

                    @Override
                    public void marshal(
                            Object source,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
                        ManifestModel model = (ManifestModel) source;
                        writer.addAttribute("name", model.getName());
                        for (java.util.Map.Entry<String, String> entry :
                                model.getEntries().entrySet())
                            context.convertAnother(
                                    entry,
                                    new Converter() {

                                        @Override
                                        public boolean canConvert(Class type) {
                                            return Entry.class.isAssignableFrom(type);
                                        }

                                        @Override
                                        public void marshal(
                                                Object source,
                                                HierarchicalStreamWriter writer,
                                                MarshallingContext context) {
                                            @SuppressWarnings("unchecked")
                                            Entry<String, String> e =
                                                    (Entry<String, String>) source;
                                            writer.startNode(e.getKey());
                                            writer.setValue(e.getValue());
                                            writer.endNode();
                                        }

                                        @Override
                                        public Object unmarshal(
                                                HierarchicalStreamReader reader,
                                                UnmarshallingContext context) {
                                            throw new UnsupportedOperationException(
                                                    "Not implemented");
                                        }
                                    });
                    }

                    @Override
                    public Object unmarshal(
                            HierarchicalStreamReader reader, UnmarshallingContext context) {
                        throw new UnsupportedOperationException("Not implemented");
                    }
                });
        xs.alias("resource", ManifestModel.class);
        xs.addImplicitCollection(ManifestModel.class, "entries");
        xs.useAttributeFor(ManifestModel.class, "name");

        xs.alias("property", Entry.class);

        xs.autodetectAnnotations(true);
    }
}
