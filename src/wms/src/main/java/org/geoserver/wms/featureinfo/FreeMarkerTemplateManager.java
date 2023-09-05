/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.geoserver.wms.featureinfo.FreemarkerStaticsAccessRule.fromPattern;

import freemarker.cache.NullCacheStorage;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ServiceException;
import org.geoserver.template.DirectTemplateFeatureCollectionFactory;
import org.geoserver.template.FeatureWrapper;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.template.TemplateUtils;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.featureinfo.FreemarkerStaticsAccessRule.RuleItem;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.logging.Logging;

/**
 * Abstract class to manage free marker templates used to customize getFeatureInfo output format. It
 * provides methods to retrieve templates and write the output processing them.
 */
public abstract class FreeMarkerTemplateManager {

    /** Config key determining the restrictions for accessing static members */
    static final String KEY_STATIC_MEMBER_ACCESS = "org.geoserver.htmlTemplates.staticMemberAccess";

    public enum OutputFormat {
        JSON("application/json"),
        HTML("text/html");

        OutputFormat(String format) {
            this.format = format;
        }

        private String format;

        String getFormat() {
            return format;
        }
    }

    private static Configuration templateConfig;

    private static DirectTemplateFeatureCollectionFactory tfcFactory =
            new DirectTemplateFeatureCollectionFactory();

    private static Logger logger = Logging.getLogger(FreeMarkerTemplateManager.class);
    private static FreemarkerStaticsAccessRule staticsAccessRule;

    /** Initializes the {@link #staticsAccessRule}. */
    static void initStaticsAccessRule() {
        String tmpAccessPattern = GeoServerExtensions.getProperty(KEY_STATIC_MEMBER_ACCESS);
        FreemarkerStaticsAccessRule tmpRule = fromPattern(tmpAccessPattern);
        logger.fine("Initializing with " + tmpRule);
        for (RuleItem tmpItem : tmpRule.getAllowedItems()) {
            if (tmpItem.isNumberedAlias()) {
                logger.warning(
                        "Granting access to static members of "
                                + tmpItem.getClassName()
                                + " using the variable name "
                                + tmpItem.getAlias()
                                + " to keep names unique.");
            } else if (logger.isLoggable(Level.FINER)) {
                logger.finer(
                        "Granting access to static members of "
                                + tmpItem.getClassName()
                                + " using the variable name "
                                + tmpItem.getAlias()
                                + ".");
            }
        }
        staticsAccessRule = tmpRule;
    }

    static {
        // initialize the template engine, this is static to maintain a cache
        // over instantiations of kml writer
        initStaticsAccessRule();
        templateConfig = TemplateUtils.getSafeConfiguration();
        templateConfig.setObjectWrapper(
                new FeatureWrapper(tfcFactory) {

                    @Override
                    public TemplateModel wrap(Object object) throws TemplateModelException {
                        if (object instanceof FeatureCollection) {
                            SimpleHash map = (SimpleHash) super.wrap(object);
                            map.put("request", Dispatcher.REQUEST.get().getKvp());
                            map.put("environment", new EnvironmentVariablesTemplateModel());
                            map.put("Math", getStaticModel("java.lang.Math"));
                            map.put(
                                    "geoJSON",
                                    getStaticModel(
                                            "org.geoserver.wms.featureinfo.GeoJSONTemplateManager"));
                            addConfiguredStatics(map);
                            return map;
                        }
                        return super.wrap(object);
                    }

                    private void addConfiguredStatics(SimpleHash aMap)
                            throws TemplateModelException {
                        if (staticsAccessRule.isUnrestricted()) {
                            aMap.put("statics", getStaticModels());
                        } else if (staticsAccessRule.getAllowedItems().isEmpty()) {
                            for (RuleItem tmpItem : staticsAccessRule.getAllowedItems()) {
                                aMap.put(tmpItem.getAlias(), tmpItem.getClassName());
                            }
                        }
                    }

                    private TemplateHashModel getStaticModel(String path)
                            throws TemplateModelException {
                        return (TemplateHashModel) getStaticModels().get(path);
                    }
                });
        // as we want to look up different templates for each resource, the templates cannot
        // be cached by name. Freemarker used to clear the cache when setting the loader,
        // but does not do that anymore since
        // https://github.com/apache/freemarker/commit/fc9eba51492c3cd4da3547ba15b95c7db9b3d237
        // because we use the same loader, we just re-configure it to point to a different resource
        templateConfig.setCacheStorage(new NullCacheStorage());

        templateConfig.setDefaultEncoding("UTF-8");
    }

    private GeoServerResourceLoader resourceLoader;

    protected WMS wms;

    private GeoServerTemplateLoader templateLoader;

    private OutputFormat format;

    public FreeMarkerTemplateManager(
            OutputFormat format, final WMS wms, GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.wms = wms;
        this.format = format;
    }

    /** Writes the features to the output */
    public boolean write(
            FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {
        // setup the writer
        final Charset charSet = wms.getCharSet();
        final OutputStreamWriter osw = new OutputStreamWriter(out, charSet);

        try {
            // if there is only one feature type loaded, we allow for header/footer customization,
            // otherwise we stick with the default ones for html, or for those
            // in the template directory for JSON
            @SuppressWarnings("unchecked")
            List<FeatureCollection> collections = results.getFeature();

            ResourceInfo ri = null;
            if (collections.size() == 1) {
                ri = wms.getResourceInfo(FeatureCollectionDecorator.getName(collections.get(0)));
            }
            // ri can be null if the type is the result of a rendering transformation
            Template header;
            Template footer;
            if (ri != null) {
                header = getTemplate(ri, charSet, "header");
                footer = getTemplate(ri, charSet, "footer");
            } else {
                header = getTemplate(null, charSet, "header");
                footer = getTemplate(null, charSet, "footer");
            }
            if (!templatesExist(header, footer, collections)) return false;

            processTemplate("header", null, header, osw);

            handleContent(collections, osw, request);

            // if a template footer was loaded (ie, there were only one feature
            // collection), process it
            if (footer != null) processTemplate("footer", null, footer, osw);

            osw.flush();
            return true;

        } finally {
            cleanup();
        }
    }

    public void cleanup() {
        // close any open iterators
        tfcFactory.purge();
    }

    /**
     * Processes the given template for the given FeatureCollection.
     *
     * @param templateType Type of template for display in error message if required (content,
     *     footer, header)
     * @param fc
     * @param template
     * @param osw
     * @throws IOException
     */
    protected void processTemplate(
            String templateType, FeatureCollection fc, Template template, OutputStreamWriter osw)
            throws IOException {
        try {
            template.process(fc, osw);
        } catch (TemplateException e) {
            String msg =
                    "Error occurred processing " + templateType + " template " + template.getName();
            if (fc != null) {
                Name name = FeatureCollectionDecorator.getName(fc);
                msg += " for featureType " + (name == null ? null : name.getLocalPart());
            }
            throw (IOException) new IOException(msg).initCause(e);
        }
    }

    protected Template getContentTemplate(FeatureCollection fc, Charset charset)
            throws IOException {
        Template content = null;
        if (fc != null && fc.size() > 0) {
            ResourceInfo ri = wms.getResourceInfo(FeatureCollectionDecorator.getName(fc));
            if (!(fc.getSchema() instanceof SimpleFeatureType)) {
                // if there is a specific template for complex features, use that.
                content = getTemplate(ri, charset, "complex_content");
            }
            if (content == null) {
                content = getTemplate(ri, charset, "content");
            }
        }
        return content;
    }

    protected Template getTemplate(ResourceInfo ri, Charset charset, String name)
            throws IOException {
        String templateName = getTemplateFileName(name);
        return getTemplate(ri, templateName, charset);
    }

    private Template getTemplate(ResourceInfo ri, String templateFileName, Charset charset)
            throws IOException {

        synchronized (templateConfig) {
            // setup template subsystem
            if (templateLoader == null) {
                templateLoader = new GeoServerTemplateLoader(getClass(), resourceLoader);
            }
            templateLoader.setResource(ri);
            templateConfig.setTemplateLoader(templateLoader);
            templateConfig.unsetOutputFormat();
            if (format.equals(OutputFormat.HTML) && wms.isAutoEscapeTemplateValues()) {
                templateConfig.setOutputFormat(HTMLOutputFormat.INSTANCE);
            }
            Template t = null;
            try {
                t = templateConfig.getTemplate(templateFileName);
            } catch (FileNotFoundException ex) {
                // throws exception just for text/html that completely rely on templates
                if (format.equals(OutputFormat.HTML)) throw ex;
            }

            return t;
        }
    }

    /**
     * Get the expected template file name by appending to the requested one a string matching the
     * output format
     */
    protected abstract String getTemplateFileName(String filename);

    /** Check the needed files exists according to the output format */
    protected abstract boolean templatesExist(
            Template header, Template footer, List<FeatureCollection> collections)
            throws IOException;

    protected abstract void handleContent(
            List<FeatureCollection> collections,
            OutputStreamWriter osw,
            GetFeatureInfoRequest request)
            throws IOException;

    public void setTemplateLoader(GeoServerTemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
    }
}
