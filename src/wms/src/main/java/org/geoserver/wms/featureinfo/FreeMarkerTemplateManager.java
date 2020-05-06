/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import freemarker.template.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ServiceException;
import org.geoserver.template.DirectTemplateFeatureCollectionFactory;
import org.geoserver.template.FeatureWrapper;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.template.TemplateUtils;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Class to manage free marker templates used to customize getFeatureInfo output format. It takes
 * care to load templates according to the output format and writing the result.
 */
public class FreeMarkerTemplateManager {

    private static Configuration templateConfig;

    private static DirectTemplateFeatureCollectionFactory tfcFactory =
            new DirectTemplateFeatureCollectionFactory();

    static {
        // initialize the template engine, this is static to maintain a cache
        // over instantiations of kml writer
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
                            return map;
                        }
                        return super.wrap(object);
                    }

                    private TemplateHashModel getStaticModel(String path)
                            throws TemplateModelException {
                        return (TemplateHashModel) getStaticModels().get(path);
                    }
                });
    }

    private String format;

    private GeoServerResourceLoader resourceLoader;

    private WMS wms;

    private GeoServerTemplateLoader templateLoader;

    public FreeMarkerTemplateManager(
            String format, final WMS wms, GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.wms = wms;
        this.format = format;
    }

    /**
     * Write FeatureCollectionType to the output
     *
     * @param results
     * @param request
     * @param out
     * @throws ServiceException
     * @throws IOException
     */
    public void write(
            FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {
        // setup the writer
        final Charset charSet = wms.getCharSet();
        final OutputStreamWriter osw = new OutputStreamWriter(out, charSet);

        try {
            // if there is only one feature type loaded, we allow for header/footer customization,
            // otherwise we stick with the generic ones
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

            processStaticTemplate("header", header, osw);

            // process content template for all feature collections found
            for (int i = 0; i < collections.size(); i++) {
                FeatureCollection fc = collections.get(i);
                Template content = getContentTemplate(fc, charSet);
                String typeName = request.getQueryLayers().get(i).getName();
                processDynamicTemplate(typeName, fc, content, osw);
            }

            // if a template footer was loaded (ie, there were only one feature
            // collection), process it
            if (footer != null) processStaticTemplate("footer", footer, osw);
            osw.flush();

        } finally {
            // close any open iterators
            tfcFactory.purge();
        }
    }

    /**
     * Write a FeatureCollectionType to the output, checking that header.ftl, content.ftl and
     * footer.ftl are all present
     *
     * @param results
     * @param request
     * @param out
     * @return false if the three templates are not all present true if they are and output got
     *     written.
     * @throws ServiceException
     * @throws IOException
     */
    public boolean writeWithNullCheck(
            FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {
        // setup the writer
        final Charset charSet = wms.getCharSet();
        final OutputStreamWriter osw = new OutputStreamWriter(out, charSet);

        try {
            // if there is only one feature type loaded, we allow for header/footer customization,
            // otherwise we stick with the generic ones
            List<FeatureCollection> collections = results.getFeature();
            ResourceInfo ri = null;
            if (collections.size() == 1) {
                ri = wms.getResourceInfo(FeatureCollectionDecorator.getName(collections.get(0)));
            }
            // ri can be null if the type is the result of a rendering transformation
            Template header;
            Template footer;

            header = getTemplate(ri, charSet, "header");
            if (header == null) return false;

            footer = getTemplate(ri, charSet, "footer");
            if (footer == null) return false;

            int collSize = collections.size();
            Template[] contentTemplates = new Template[collSize];
            for (int i = 0; i < collSize; i++) {
                FeatureCollection fc = collections.get(i);

                Template content = getContentTemplate(fc, charSet);
                if (content == null) return false;

                contentTemplates[i] = content;
            }
            processStaticTemplate("header", header, osw);

            // process content template for all feature collections found

            for (int i = 0; i < collSize; i++) {
                FeatureCollection fc = collections.get(i);
                String typeName = request.getQueryLayers().get(i).getName();
                processDynamicTemplate(typeName, fc, contentTemplates[i], osw);
            }

            // if a template footer was loaded (ie, there were only one feature
            // collection), process it
            if (footer != null) processStaticTemplate("footer", footer, osw);
            osw.flush();

        } finally {
            // close any open iterators
            tfcFactory.purge();
        }

        return true;
    }

    /**
     * Process a template with dynamic content
     *
     * @param typeName
     * @param fc
     * @param template
     * @param osw
     * @throws IOException
     */
    private void processDynamicTemplate(
            String typeName, FeatureCollection fc, Template template, OutputStreamWriter osw)
            throws IOException {
        try {
            template.process(fc, osw);
        } catch (TemplateException e) {
            String msg =
                    "Error occurred processing content template "
                            + template.getName()
                            + " for "
                            + typeName;
            throw (IOException) new IOException(msg).initCause(e);
        }
    }

    /**
     * Process template with static content
     *
     * @param name
     * @param template
     * @param osw
     * @throws IOException
     */
    private void processStaticTemplate(String name, Template template, OutputStreamWriter osw)
            throws IOException {
        try {
            template.process(null, osw);
        } catch (TemplateException e) {
            String msg = "Error occured processing " + name + " template.";
            throw (IOException) new IOException(msg).initCause(e);
        }
    }

    private Template getContentTemplate(FeatureCollection fc, Charset charset) throws IOException {
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

    private Template getTemplate(ResourceInfo ri, Charset charset, String name) throws IOException {
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
            Template t = null;
            try {
                t = templateConfig.getTemplate(templateFileName);
            } catch (FileNotFoundException ex) {
                // throws exception just for text/html that completely rely on templates
                if (format.equals("text/html")) throw ex;
            }
            if (t != null) t.setEncoding(charset.name());

            return t;
        }
    }

    /**
     * Get the expected template file name by appending to the requested one a string matching the
     * output format
     *
     * @param filename
     * @return the complete filename according to the output format
     */
    private String getTemplateFileName(String filename) {
        if (format.equals(JSONType.json)) return filename + "_json" + ".ftl";
        else return filename + ".ftl";
    }

    public void setTemplateLoader(GeoServerTemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
    }
}
