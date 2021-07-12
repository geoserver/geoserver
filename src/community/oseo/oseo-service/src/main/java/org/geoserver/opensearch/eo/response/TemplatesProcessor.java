/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import freemarker.ext.beans.SimpleMapModel;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.geoserver.opensearch.eo.FreemarkerTemplateSupport;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.util.Converters;
import org.opengis.feature.Feature;

/**
 * Loads, caches and processes HTML description templates against a stream of features. It's meant
 * to be used for a single request, as it caches the template and won't react to on disk template
 * changes.
 */
class TemplatesProcessor {

    FreemarkerTemplateSupport support;
    Map<String, Template> templateCache = new HashMap<>();

    public TemplatesProcessor(FreemarkerTemplateSupport support) {
        this.support = support;
    }

    /**
     * Processes a template and returns the result as a string
     *
     * @param collection The collection name used to lookup templates in the data dir
     * @param templateName The template name (with no extension)
     * @param feature The feature to be applied
     */
    public String processTemplate(String collection, String templateName, Feature feature)
            throws IOException {
        Template template = getTemplate(collection, templateName);

        StringWriter sw = new StringWriter();
        HashMap<String, Object> model = setupModel(feature);
        try {
            template.process(model, sw);
        } catch (TemplateException e) {
            throw new IOException("Error occurred processing template " + templateName, e);
        }
        return sw.toString();
    }

    private Template getTemplate(String collection, String templateName) throws IOException {
        String key = templateName;
        if (collection != null) key = collection + "/" + templateName;
        Template t = templateCache.get(key);
        if (t == null) {
            t = support.getTemplate(collection, templateName, TemplatesProcessor.class);
            templateCache.put(key, t);
        }
        return t;
    }

    protected HashMap<String, Object> setupModel(Feature feature) {
        HashMap<String, Object> model = new HashMap<>();
        model.put("model", feature);
        if (Dispatcher.REQUEST.get() != null) {
            final String baseURL = ResponseUtils.baseURL(Dispatcher.REQUEST.get().getHttpRequest());
            model.put("baseURL", baseURL);
            addLinkFunctions(baseURL, model);
        }

        return model;
    }

    /**
     * Adds the <code>oseoLink</code> and <code>oseoLink</code> functions to the model, for usage in
     * the template
     */
    protected void addLinkFunctions(String baseURL, Map<String, Object> model) {
        model.put(
                "oseoLink",
                (TemplateMethodModelEx)
                        arguments -> {
                            Map<String, String> kvp = new LinkedHashMap<>();
                            if (arguments.size() > 1 && (arguments.size() % 2) != 1) {
                                throw new IllegalArgumentException(
                                        "Expected a path argument, followed by an optional list of keys and values. Found a key that is not matched to a value: "
                                                + arguments);
                            }
                            int i = 1;
                            while (i < arguments.size()) {
                                kvp.put(toString(arguments.get(i++)), toString(arguments.get(i++)));
                            }
                            return ResponseUtils.buildURL(
                                    baseURL,
                                    ResponseUtils.appendPath("oseo", toString(arguments.get(0))),
                                    kvp,
                                    URLMangler.URLType.SERVICE);
                        });
        model.put(
                "resourceLink",
                (TemplateMethodModelEx)
                        arguments ->
                                ResponseUtils.buildURL(
                                        baseURL,
                                        toString(arguments.get(0)),
                                        null,
                                        URLMangler.URLType.RESOURCE));
    }

    private String toString(Object argument) throws TemplateModelException {
        if (argument instanceof TemplateScalarModel) {
            return ((TemplateScalarModel) argument).getAsString();
        }
        // in case it's an attribute, unwrap the raw value and convert
        if (argument instanceof SimpleMapModel) {
            argument = ((SimpleMapModel) argument).get("rawValue");
        }
        return Converters.convert(argument, String.class);
    }
}
