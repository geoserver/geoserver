/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.Map.Entry;

import org.geoserver.ManifestLoader;
import org.geoserver.ManifestLoader.AboutModel;
import org.geoserver.ManifestLoader.AboutModel.AboutModelType;
import org.geoserver.ManifestLoader.AboutModel.ManifestModel;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@RestController @RequestMapping(path = RestBaseController.ROOT_PATH, 
    produces = {MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_HTML_VALUE})
public class AboutController extends RestBaseController {
        
    @GetMapping(value = "/about/manifest") 
    public RestWrapper<AboutModel> getManifest(@RequestParam(name = "manifest", required = false) String regex,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String key, @RequestParam(required = false) String value) {
        return wrapObject(getModel(AboutModelType.RESOURCES, regex, from, to, key, value), AboutModel.class);
    }
    
    @GetMapping(value = "/about/version") 
    public RestWrapper<AboutModel> getVersion(@RequestParam(name = "manifest", required = false) String regex,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String key, @RequestParam(required = false) String value) {
        return wrapObject(getModel(AboutModelType.VERSIONS, regex, from, to, key, value), AboutModel.class);
    }
    
    protected AboutModel getModel(AboutModelType type, String regex, String from,  String to, String key, String value) {
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

    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xs = persister.getXStream();
        
        // AboutModel
        xs.processAnnotations(AboutModel.class);
        xs.allowTypes(new Class[] { AboutModel.class });
        xs.addImplicitCollection(AboutModel.class, "manifests");
        xs.alias("about", AboutModel.class);

        // ManifestModel Xstream converter
        xs.registerConverter(new Converter() {

            @Override
            public boolean canConvert(Class type) {
                return type.equals(ManifestModel.class);
            }

            @Override
            public void marshal(Object source, HierarchicalStreamWriter writer,
                    MarshallingContext context) {
                ManifestModel model = (ManifestModel) source;
                writer.addAttribute("name", model.getName());
                for (java.util.Map.Entry<String, String> entry : model.getEntries().entrySet())
                    context.convertAnother(entry, new Converter() {

                        @Override
                        public boolean canConvert(Class type) {
                            if (java.util.Map.Entry.class.isAssignableFrom(type))
                                return true;
                            return false;
                        }

                        @Override
                        public void marshal(Object source, HierarchicalStreamWriter writer,
                                MarshallingContext context) {
                            @SuppressWarnings("unchecked")
                            Entry<String, String> e = (Entry<String, String>) source;
                            writer.startNode(e.getKey());
                            writer.setValue(e.getValue());
                            writer.endNode();
                        }

                        @Override
                        public Object unmarshal(HierarchicalStreamReader reader,
                                UnmarshallingContext context) {
                            throw new UnsupportedOperationException("Not implemented");
                        }

                    });
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
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
