/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geoserver.ManifestLoader;
import org.geoserver.ManifestLoader.AboutModel;
import org.geoserver.ManifestLoader.AboutModel.AboutModelType;
import org.geoserver.ManifestLoader.AboutModel.ManifestModel;
import org.geoserver.catalog.rest.CatalogFreemarkerHTMLFormat;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;

/**
 * 
 * @author Carlo Cancellieri - GeoSolutions SAS
 * 
 */
public class AboutManifest extends ReflectiveResource {

    private final AboutModelType type;

    public AboutManifest(Context context, Request request, Response response, AboutModelType type) {
        super(context, request, response);
        this.type = type;
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new AboutHTMLFormat(request, response, this);
    }

    @Override
    public boolean allowPut() {
        return false;
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        AboutModel model = null;

        // filter name by regex
        final String regex = getQueryStringValue("manifest", String.class, null);
        if (regex != null) {
            model = buildAboutModel(type).filterNameByRegex(regex);
        }

        // filter name by range
        final String from = getQueryStringValue("from", String.class, null);
        final String to = getQueryStringValue("to", String.class, null);
        if (from != null && to != null) {
            if (model != null) {
                model = model.filterNameByRange(from, to);
            } else {
                model = buildAboutModel(type).filterNameByRange(from, to);
            }
        }

        // filter by properties
        final String key = getQueryStringValue("key", String.class, null);
        final String value = getQueryStringValue("value", String.class, null);
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

        if (model != null)
            return model;
        else
            return buildAboutModel(type);
    }

    private static AboutModel buildAboutModel(AboutModelType type) {
        if (type.equals(AboutModelType.RESOURCES))
            // if request is for resource return the resources
            return ManifestLoader.getResources();
        else {
            // get the version
            return ManifestLoader.getVersions();
        }
    }

    /**
     * Method for subclasses to customize of modify the xstream instance being used to persist and depersist XML and JSON.
     */
    @Override
    protected void configureXStream(XStream xs) {
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

    @Override
    protected void handleObjectPut(Object obj) throws Exception {
        throw new UnsupportedOperationException("Not allowed");
    }

    /**
     * HTML format
     * 
     * @author carlo cancellieri - GeoSolutions SAS
     *
     */
    private static class AboutHTMLFormat extends CatalogFreemarkerHTMLFormat {

        public AboutHTMLFormat(Request request, Response response, Resource resource) {
            super(AboutModel.class, request, response, resource);
        }

        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            final Configuration cfg = super.createConfiguration(data, clazz);
            cfg.setClassForTemplateLoading(getClass(), "templates");

            cfg.setObjectWrapper(new ObjectToMapWrapper<AboutModel>(AboutModel.class) {
                @Override
                protected void wrapInternal(Map properties, SimpleHash model, AboutModel object) {
                    final List<Map<String, Object>> manifests = new ArrayList<Map<String, Object>>();
                    final Iterator<ManifestModel> it = object.getManifests().iterator();
                    while (it.hasNext()) {
                        final ManifestModel manifest = it.next();

                        final Map<String, Object> map = new HashMap<String, Object>();
                        map.put("name", manifest.getName());

                        final List<String> props = new ArrayList<String>();
                        map.put("properties", props);

                        final List<String> values = new ArrayList<String>();
                        map.put("valuez", values);

                        final Iterator<String> innerIt = manifest.getEntries().keySet().iterator();
                        while (innerIt.hasNext()) {
                            String key = innerIt.next();
                            props.add(key);
                            values.add(manifest.getEntries().get(key));
                        }
                        manifests.add(map);
                    }

                    properties.put("manifests", manifests);
                }
            });
            return cfg;
        }
    }
}
