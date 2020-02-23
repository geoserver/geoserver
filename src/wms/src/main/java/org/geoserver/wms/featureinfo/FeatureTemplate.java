/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.template.FeatureWrapper;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.template.TemplateUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Executes a template for a feature.
 *
 * <p>Usage:
 *
 * <pre>
 * <code>
 * Feature feature = ...  //some feature
 * Writer writer = ...    //some writer
 *
 * FeatureTemplate template = new FeatureTemplate();
 *
 *  //title
 * template.title( feature );
 *
 *  //description
 * template.description( feature );
 * </code>
 * </pre>
 *
 * For performance reasons the template lookups will be cached, so it's advised to use the same
 * FeatureTemplate object in a loop that encodes various features, but not to cache it for a long
 * time (static reference). Moreover, FeatureTemplate is not thread safe, so instantiate one for
 * each thread.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @author Andrea Aime, TOPP
 */
public class FeatureTemplate {
    /** The template configuration used for placemark descriptions */
    static Configuration templateConfig;

    static {
        // initialize the template engine, this is static to maintain a cache
        // over instantiations of kml writer
        templateConfig = TemplateUtils.getSafeConfiguration();
        templateConfig.setObjectWrapper(new FeatureWrapper());

        // set the default output formats for dates
        templateConfig.setDateFormat("MM/dd/yyyy");
        templateConfig.setDateTimeFormat("MM/dd/yyyy HH:mm:ss");
        templateConfig.setTimeFormat("HH:mm:ss");

        // set the default locale to be US and the
        // TODO: this may be somethign we want to configure/change
        templateConfig.setLocale(Locale.US);
        templateConfig.setNumberFormat("0.###########");
    }

    /** The pattern used by DATETIME_FORMAT */
    public static String DATE_FORMAT_PATTERN = "MM/dd/yy";

    /** The pattern used by DATETIME_FORMAT */
    public static String DATETIME_FORMAT_PATTERN = "MM/dd/yy HH:mm:ss";

    /** The pattern used by DATETIME_FORMAT */
    public static String TIME_FORMAT_PATTERN = "HH:mm:ss";

    /** Template cache used to avoid paying the cost of template lookup for each feature */
    Map templateCache = new HashMap();

    /**
     * Cached writer used for plain conversion from Feature to String. Improves performance
     * significantly compared to an OutputStreamWriter over a ByteOutputStream.
     */
    CharArrayWriter caw = new CharArrayWriter();

    /**
     * Executes the title template for a feature writing the results to an output stream.
     *
     * <p>This method is convenience for: <code>
     * description( feature, new OutputStreamWriter( output ) );
     * </code>
     *
     * @param feature The feature to execute the template against.
     * @param output The output to write the result of the template to.
     * @throws IOException Any errors that occur during execution of the template.
     */
    public void title(SimpleFeature feature, OutputStream output) throws IOException {
        title(feature, new OutputStreamWriter(output, Charset.forName("UTF-8")));
    }

    /**
     * Executes the link template for a feature writing the results to an output stream.
     *
     * <p>This method is convenience for: <code>
     * link( feature, new OutputStreamWriter( output ) );
     * </code>
     *
     * @param feature The feature to execute the template against.
     * @param output The output to write the result of the template to.
     * @throws IOException Any errors that occur during execution of the template.
     */
    public void link(SimpleFeature feature, OutputStream output) throws IOException {
        link(feature, new OutputStreamWriter(output, Charset.forName("UTF-8")));
    }

    /**
     * Executes the description template for a feature writing the results to an output stream.
     *
     * <p>This method is convenience for: <code>
     * description( feature, new OutputStreamWriter( output ) );
     * </code>
     *
     * @param feature The feature to execute the template against.
     * @param output The output to write the result of the template to.
     * @throws IOException Any errors that occur during execution of the template.
     */
    public void description(SimpleFeature feature, OutputStream output) throws IOException {
        description(feature, new OutputStreamWriter(output, Charset.forName("UTF-8")));
    }

    /**
     * Executes the title template for a feature writing the results to a writer.
     *
     * @param feature The feature to execute the template against.
     * @param writer The writer to write the template output to.
     * @throws IOException Any errors that occur during execution of the template.
     */
    public void title(SimpleFeature feature, Writer writer) throws IOException {
        execute(feature, feature.getFeatureType(), writer, "title.ftl", null);
    }

    /**
     * Executes the link template for a feature writing the results to a writer.
     *
     * @param feature The feature to execute the template against.
     * @param writer The writer to write the template output to.
     * @throws IOException Any errors that occur during execution of the template.
     */
    public void link(SimpleFeature feature, Writer writer) throws IOException {
        execute(feature, feature.getFeatureType(), writer, "link.ftl", null);
    }

    /**
     * Executes the description template for a feature writing the results to a writer.
     *
     * @param feature The feature to execute the template against.
     * @param writer The writer to write the template output to.
     * @throws IOException Any errors that occur during execution of the template.
     */
    public void description(SimpleFeature feature, Writer writer) throws IOException {
        execute(feature, feature.getFeatureType(), writer, "description.ftl", null);
    }

    /**
     * Executes the title template for a feature returning the result as a string.
     *
     * @param feature The feature to execute the template against.
     * @throws IOException Any errors that occur during execution of the template.
     */
    public String title(SimpleFeature feature) throws IOException {
        caw.reset();
        title(feature, caw);

        return caw.toString();
    }

    /**
     * Executes the link template for a feature returning the result as a string.
     *
     * @param feature The feature to execute the template against.
     * @throws IOException Any errors that occur during execution of the template.
     */
    public String link(SimpleFeature feature) throws IOException {
        caw.reset();
        link(feature, caw);

        return caw.toString();
    }

    /**
     * Executes the description template for a feature returning the result as a string.
     *
     * @param feature The feature to execute the template against.
     * @throws IOException Any errors that occur during execution of the template.
     */
    public String description(SimpleFeature feature) throws IOException {
        caw.reset();
        description(feature, caw);

        return caw.toString();
    }

    /**
     * Executes a template for the feature writing the results to a writer.
     *
     * <p>The template to execute is secified via the <tt>template</tt>, and <tt>lookup</tt>
     * parameters. The <tt>lookup</tt> is used to specify the class from which <tt>template</tt>
     * shoould be loaded relative to in teh case where the user has not specified an override in the
     * data directory.
     *
     * @param feature The feature to execute the template against.
     * @param writer The writer for output.
     * @param template The template name.
     * @param lookup The class to lookup the template relative to.
     */
    public void template(SimpleFeature feature, Writer writer, String template, Class lookup)
            throws IOException {
        execute(feature, feature.getFeatureType(), writer, template, lookup);
    }

    /**
     * Executes a template for the feature writing the results to an output stream.
     *
     * <p>The template to execute is secified via the <tt>template</tt>, and <tt>lookup</tt>
     * parameters. The <tt>lookup</tt> is used to specify the class from which <tt>template</tt>
     * shoould be loaded relative to in teh case where the user has not specified an override in the
     * data directory.
     *
     * @param feature The feature to execute the template against.
     * @param output The output.
     * @param template The template name.
     * @param lookup The class to lookup the template relative to.
     */
    public void template(SimpleFeature feature, OutputStream output, String template, Class lookup)
            throws IOException {
        template(feature, new OutputStreamWriter(output), template, lookup);
    }

    /**
     * Executes a template for the feature returning the result as a string.
     *
     * <p>The template to execute is secified via the <tt>template</tt>, and <tt>lookup</tt>
     * parameters. The <tt>lookup</tt> is used to specify the class from which <tt>template</tt>
     * shoould be loaded relative to in teh case where the user has not specified an override in the
     * data directory.
     *
     * @param feature The feature to execute the template against.
     * @param template The template name.
     * @param lookup The class to lookup the template relative to.
     */
    public String template(SimpleFeature feature, String template, Class lookup)
            throws IOException {
        caw.reset();
        template(feature, caw, template, lookup);
        return caw.toString();
    }

    /*
     * Internal helper method to exceute the template against feature or
     * feature collection.
     */
    private void execute(
            Object feature,
            SimpleFeatureType featureType,
            Writer writer,
            String template,
            Class lookup)
            throws IOException {
        Template t = null;

        t = lookupTemplate(featureType, template, lookup);

        try {
            t.process(feature, writer);
        } catch (TemplateException e) {
            String msg = "Error occured processing template.";
            throw (IOException) new IOException(msg).initCause(e);
        }
    }

    /**
     * Returns the template for the specified feature type. Looking up templates is pretty
     * expensive, so we cache templates by feture type and template.
     */
    private Template lookupTemplate(SimpleFeatureType featureType, String template, Class lookup)
            throws IOException {
        Template t;

        // lookup the cache first
        TemplateKey key = new TemplateKey(featureType, template);
        t = (Template) templateCache.get(key);
        if (t != null) return t;

        // otherwise, build a loader and do the lookup
        GeoServerTemplateLoader templateLoader =
                new GeoServerTemplateLoader(
                        lookup != null ? lookup : getClass(),
                        GeoServerExtensions.bean(GeoServerResourceLoader.class));
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        templateLoader.setFeatureType(catalog.getFeatureTypeByName(featureType.getName()));

        // Configuration is not thread safe
        synchronized (templateConfig) {
            templateConfig.setTemplateLoader(templateLoader);
            t = templateConfig.getTemplate(template);
            t.setEncoding("UTF-8");
        }
        templateCache.put(key, t);
        return t;
    }

    /** Returns true if the required template is empty or has its default content */
    public boolean isTemplateEmpty(
            SimpleFeatureType featureType,
            String template,
            Class<FeatureTemplate> lookup,
            String defaultContent)
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
        return "".equals(templateText)
                || (defaultContent != null && defaultContent.equals(templateText));
    }

    private static class TemplateKey {
        SimpleFeatureType type;
        String template;

        public TemplateKey(SimpleFeatureType type, String template) {
            super();
            this.type = type;
            this.template = template;
        }

        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((template == null) ? 0 : template.hashCode());
            result = PRIME * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final TemplateKey other = (TemplateKey) obj;
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
