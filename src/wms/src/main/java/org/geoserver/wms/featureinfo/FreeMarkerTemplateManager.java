/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import freemarker.template.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
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
import org.geoserver.wfs.json.GeoJSONBuilder;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Class to manage free marker templates used to customize getFeatureInfo output format. It takes
 * care to load templates according to the output format and writing the result.
 */
public class FreeMarkerTemplateManager {

    enum OutputFormat {
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

    private GeoServerResourceLoader resourceLoader;

    private WMS wms;

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

            // process content template for all feature collections found
            switch (format) {
                case JSON:
                    handleJSONContent(collections, osw, request);
                    break;
                default:
                    handleHTMLContent(collections, osw, request);
                    break;
            }

            // if a template footer was loaded (ie, there were only one feature
            // collection), process it
            if (footer != null) processTemplate("footer", null, footer, osw);

            osw.flush();
            return true;

        } finally {
            // close any open iterators
            tfcFactory.purge();
        }
    }

    private void processTemplate(
            String name, FeatureCollection fc, Template template, OutputStreamWriter osw)
            throws IOException {
        try {
            template.process(fc, osw);
        } catch (TemplateException e) {
            String msg = null;
            if (fc == null) {
                msg = "Error occured processing " + name + " template.";
            } else {
                msg =
                        "Error occurred processing content template "
                                + template.getName()
                                + " for "
                                + name;
            }
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
                if (format.equals(OutputFormat.HTML)) throw ex;
            }

            if (t != null) t.setEncoding(charset.name());

            return t;
        }
    }

    /**
     * Get the expected template file name by appending to the requested one a string matching the
     * output format
     */
    private String getTemplateFileName(String filename) {
        switch (format) {
            case JSON:
                filename += "_json.ftl";
                break;
            default:
                filename += ".ftl";
                break;
        }
        return filename;
    }

    /** Check the needed files according to the output format */
    private boolean templatesExist(
            Template header, Template footer, List<FeatureCollection> collections)
            throws IOException {
        switch (format) {
            case JSON:
                return allTemplatesExist(header, footer, collections);
            default:
                return true;
        }
    }

    /**
     * Checking if header, content and footer exists for at least one FeatureType among those at
     * stake.
     */
    private boolean allTemplatesExist(
            Template header, Template footer, List<FeatureCollection> collections)
            throws IOException {
        int collSize = collections.size();
        if (header == null || footer == null) return false;
        else {
            for (int i = 0; i < collSize; i++) {
                FeatureCollection fc = collections.get(i);
                Template content = getContentTemplate(fc, wms.getCharSet());
                if (content != null) return true;
            }
        }
        return false;
    }

    public void setTemplateLoader(GeoServerTemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
    }

    private void handleHTMLContent(
            List<FeatureCollection> collections,
            OutputStreamWriter osw,
            GetFeatureInfoRequest request)
            throws IOException {
        for (int i = 0; i < collections.size(); i++) {
            FeatureCollection fc = collections.get(i);
            Template content = getContentTemplate(fc, wms.getCharSet());
            String typeName = request.getQueryLayers().get(i).getName();
            processTemplate(typeName, fc, content, osw);
        }
    }

    private void handleJSONContent(
            List<FeatureCollection> collections,
            OutputStreamWriter osw,
            GetFeatureInfoRequest request)
            throws IOException {

        for (int i = 0; i < collections.size(); i++) {
            FeatureCollection fc = collections.get(i);
            Template content = getContentTemplate(fc, wms.getCharSet());
            if (i > 0) {
                // appending a comma between json object representation
                // of a feature
                osw.write(',');
            }
            if (content == null) {
                handleJSONWithoutTemplate(fc, osw);
            } else {
                String typeName = request.getQueryLayers().get(i).getName();
                processTemplate(typeName, fc, content, osw);
            }
        }
    }

    /** Write a FeatureCollection using normal GeoJSON encoding */
    private void handleJSONWithoutTemplate(FeatureCollection collection, OutputStreamWriter osw)
            throws IOException {
        GeoJSONGetFeatureResponse format =
                new GeoJSONGetFeatureResponse(wms.getGeoServer(), OutputFormat.JSON.getFormat());
        boolean isComplex = collection.getSchema() instanceof SimpleFeatureType;
        Writer outWriter = new BufferedWriter(osw);
        final GeoJSONBuilder jsonWriter = new GeoJSONBuilder(outWriter);
        format.writeFeatures(Arrays.asList(collection), null, isComplex, jsonWriter);
        outWriter.flush();
    }
}
