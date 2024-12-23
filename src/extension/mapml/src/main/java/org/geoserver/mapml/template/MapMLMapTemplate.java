/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.template.DirectTemplateFeatureCollectionFactory;
import org.geoserver.template.FeatureWrapper;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.template.TemplateUtils;
import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

/** A template engine for generating MapML content. */
public class MapMLMapTemplate {
    /** The template configuration */
    static Configuration templateConfig;

    static DirectTemplateFeatureCollectionFactory FC_FACTORY = new DirectTemplateFeatureCollectionFactory();

    static {
        // initialize the template engine, this is static to maintain a cache
        templateConfig = TemplateUtils.getSafeConfiguration();

        templateConfig.setLocale(Locale.US);
        templateConfig.setNumberFormat("0.###########");
        templateConfig.setObjectWrapper(new FeatureWrapper(FC_FACTORY));

        // encoding
        templateConfig.setDefaultEncoding("UTF-8");
    }

    /** The template used to add to the head of the preview viewer. */
    public static final String MAPML_PREVIEW_HEAD_FTL = "mapml-preview-head.ftl";

    /** The template used to add to the head of the xml representation */
    public static final String MAPML_XML_HEAD_FTL = "mapml-head.ftl";

    public static final String MAPML_FEATURE_HEAD_FTL = "mapml-feature-head.ftl";

    public static final String MAPML_FEATURE_FTL = "mapml-feature.ftl";

    /** Template cache used to avoid paying the cost of template lookup for each GetMap call */
    Map<MapMLMapTemplate.TemplateKey, Template> templateCache = new ConcurrentHashMap<>();

    /**
     * Generates the preview content for the given feature type.
     *
     * @param model the model to use for the template
     * @param featureType the feature type to use for the template
     * @param writer the writer to write the output to
     * @throws IOException in case of an error
     */
    public void preview(Map<String, Object> model, SimpleFeatureType featureType, Writer writer) throws IOException {
        execute(model, featureType, writer, MAPML_PREVIEW_HEAD_FTL);
    }

    /**
     * Generates the preview content for the given feature type.
     *
     * @param featureType the feature type to use for the template
     * @return the preview content
     * @throws IOException in case of an error
     */
    public String preview(SimpleFeatureType featureType) throws IOException {
        CharArrayWriter caw = CharArrayWriterPool.getWriter();
        preview(Collections.emptyMap(), featureType, caw);

        return caw.toString();
    }

    public String features(SimpleFeatureType featureType, SimpleFeature feature) throws IOException {
        CharArrayWriter caw = CharArrayWriterPool.getWriter();

        features(featureType, feature, caw);
        return caw.toString();
    }

    public void features(SimpleFeatureType featureType, SimpleFeature feature, Writer writer) throws IOException {
        execute(feature, featureType, writer, MAPML_FEATURE_FTL);
    }

    /**
     * Generates the head content for the given feature type.
     *
     * @param model the model to use for the template
     * @param featureType the feature type to use for the template
     * @param writer the writer to write the output to
     * @throws IOException in case of an error
     */
    public void head(Map<String, Object> model, SimpleFeatureType featureType, Writer writer) throws IOException {
        execute(model, featureType, writer, MAPML_XML_HEAD_FTL);
    }

    public String featureHead(SimpleFeatureType featureType) throws IOException {
        CharArrayWriter caw = CharArrayWriterPool.getWriter();
        featureHead(featureType, caw);
        return caw.toString();
    }

    public void featureHead(SimpleFeatureType featureType, Writer writer) throws IOException {
        execute(featureType, writer, MAPML_FEATURE_HEAD_FTL);
    }

    /**
     * Generates the head content for the given feature type.
     *
     * @param model the model to use for the template
     * @param featureType the feature type to use for the template
     * @return the head content
     * @throws IOException in case of an error
     */
    public String head(Map<String, Object> model, SimpleFeatureType featureType) throws IOException {
        CharArrayWriter caw = CharArrayWriterPool.getWriter();
        head(model, featureType, caw);

        return caw.toString();
    }

    /*
     * Internal helper method to exceute the template against feature or
     * feature collection.
     */
    private void execute(Map<String, Object> model, SimpleFeatureType featureType, Writer writer, String template)
            throws IOException {

        Template t = lookupTemplate(featureType, template, null);

        try {
            t.process(model, writer);
        } catch (TemplateException e) {
            String msg = "Error occured processing template.";
            throw (IOException) new IOException(msg).initCause(e);
        }
    }

    /*
     * Internal helper method to exceute the template against feature or
     * feature collection.
     */
    private void execute(Feature feature, SimpleFeatureType featureType, Writer writer, String template)
            throws IOException {

        Template t = lookupTemplate(featureType, template, null);

        try {
            t.process(feature, writer);
        } catch (TemplateException e) {
            String msg = "Error occured processing template.";
            throw (IOException) new IOException(msg).initCause(e);
        }
    }

    /*
     * Internal helper method to exceute the template against feature or
     * feature collection.
     */
    private void execute(SimpleFeatureType featureType, Writer writer, String template) throws IOException {

        Template t = lookupTemplate(featureType, template, null);

        try {
            t.process(null, writer);
        } catch (TemplateException e) {
            String msg = "Error occured processing template.";
            throw (IOException) new IOException(msg).initCause(e);
        }
    }

    /**
     * Returns the template for the specified feature type. Looking up templates is pretty expensive, so we cache
     * templates by feture type and template.
     */
    private Template lookupTemplate(SimpleFeatureType featureType, String template, Class<?> lookup)
            throws IOException {

        // lookup the cache first
        TemplateKey key = new TemplateKey(featureType, template);
        Template t = templateCache.get(key);
        if (t != null) return t;

        // otherwise, build a loader and do the lookup
        GeoServerTemplateLoader templateLoader = new GeoServerTemplateLoader(
                lookup != null ? lookup : getClass(), GeoServerExtensions.bean(GeoServerResourceLoader.class));
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        templateLoader.setFeatureType(catalog.getFeatureTypeByName(featureType.getName()));

        // Configuration is not thread safe
        synchronized (templateConfig) {
            templateConfig.setTemplateLoader(templateLoader);
            t = templateConfig.getTemplate(template);
        }
        templateCache.put(key, t);
        return t;
    }

    /** Returns true if the required template is empty or has its default content */
    public boolean isTemplateEmpty(
            SimpleFeatureType featureType, String template, Class<FeatureTemplate> lookup, String defaultContent)
            throws IOException {
        Template t = lookupTemplate(featureType, template, lookup);
        if (t == null) {
            return true;
        }
        // check if the template is empty
        StringWriter sw = new StringWriter();
        t.dump(sw);
        // an empty template canonical form is "0\n".. weird!
        String templateText = sw.toString();
        return "".equals(templateText) || (defaultContent != null && defaultContent.equals(templateText));
    }

    /** Template key class used to cache templates by feature type and template name. */
    private static class TemplateKey {
        SimpleFeatureType type;
        String template;

        /**
         * Template key constructor
         *
         * @param type the feature type
         * @param template the template name
         */
        public TemplateKey(SimpleFeatureType type, String template) {
            super();
            this.type = type;
            this.template = template;
        }

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((template == null) ? 0 : template.hashCode());
            result = PRIME * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final MapMLMapTemplate.TemplateKey other = (MapMLMapTemplate.TemplateKey) obj;
            if (template == null) {
                if (other.template != null) return false;
            } else if (!template.equals(other.template)) return false;
            if (type == null) {
                if (other.type != null) return false;
            } else if (!type.equals(other.type)) return false;
            return true;
        }
    }
}
