/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.ServiceException;
import org.geoserver.template.DirectTemplateFeatureCollectionFactory;
import org.geoserver.template.FeatureWrapper;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.template.TemplateUtils;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

/**
 * Produces a FeatureInfo response in HTML. Relies on {@link AbstractFeatureInfoResponse} and the
 * feature delegate to do most of the work, just implements an HTML based writeTo method.
 *
 * @author James Macgill, PSU
 * @author Andrea Aime, TOPP
 * @version $Id$
 */
public class HTMLFeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {

    private static final String FORMAT = "text/html";

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
                            return map;
                        }
                        return super.wrap(object);
                    }
                });
    }

    GeoServerTemplateLoader templateLoader;

    private WMS wms;

    public HTMLFeatureInfoOutputFormat(final WMS wms) {
        super(FORMAT);
        this.wms = wms;
    }

    /**
     * Writes the image to the client.
     *
     * @param out The output stream to write to.
     * @throws org.vfny.geoserver.ServiceException For problems with geoserver
     * @throws java.io.IOException For problems writing the output.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void write(
            FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {
        // setup the writer
        final Charset charSet = wms.getCharSet();
        final OutputStreamWriter osw = new OutputStreamWriter(out, charSet);

        try {
            // if there is only one feature type loaded, we allow for header/footer customization,
            // otherwise we stick with the generic ones
            Template header = null;
            Template footer = null;
            List<FeatureCollection> collections = results.getFeature();
            if (collections.size() == 1) {
                header =
                        getTemplate(
                                FeatureCollectionDecorator.getName(collections.get(0)),
                                "header.ftl",
                                charSet);
                footer =
                        getTemplate(
                                FeatureCollectionDecorator.getName(collections.get(0)),
                                "footer.ftl",
                                charSet);
            } else {
                // load the default ones
                header = getTemplate(null, "header.ftl", charSet);
                footer = getTemplate(null, "footer.ftl", charSet);
            }

            try {
                header.process(null, osw);
            } catch (TemplateException e) {
                String msg = "Error occured processing header template.";
                throw (IOException) new IOException(msg).initCause(e);
            }

            // process content template for all feature collections found
            for (int i = 0; i < collections.size(); i++) {
                FeatureCollection fc = collections.get(i);
                if (fc != null && fc.size() > 0) {
                    Template content = null;
                    if (!(fc.getSchema() instanceof SimpleFeatureType)) {
                        // if there is a specific template for complex features, use that.
                        content =
                                getTemplate(
                                        FeatureCollectionDecorator.getName(fc),
                                        "complex_content.ftl",
                                        charSet);
                    }
                    if (content == null) {
                        content =
                                getTemplate(
                                        FeatureCollectionDecorator.getName(fc),
                                        "content.ftl",
                                        charSet);
                    }
                    try {
                        content.process(fc, osw);
                    } catch (TemplateException e) {
                        String msg =
                                "Error occured processing content template "
                                        + content.getName()
                                        + " for "
                                        + request.getQueryLayers().get(i).getName();
                        throw (IOException) new IOException(msg).initCause(e);
                    }
                }
            }

            // if a template footer was loaded (ie, there were only one feature
            // collection), process it
            if (footer != null) {
                try {
                    footer.process(null, osw);
                } catch (TemplateException e) {
                    String msg = "Error occured processing footer template.";
                    throw (IOException) new IOException(msg).initCause(e);
                }
            }
            osw.flush();
        } finally {

            // close any open iterators
            tfcFactory.purge();
        }
    }

    /**
     * Uses a {@link GeoServerTemplateLoader TemplateLoader} too look up for the template file named
     * <code>templateFilename</code> for the given <code>featureType</code>.
     *
     * @param name the name of the featureType to look the template for In case you want to load the
     *     default template you can leave this argument null
     * @param templateFileName the name of the template to look for
     * @param charset the encoding to apply to the resulting {@link Template}
     * @return the template named <code>templateFileName</code>
     * @throws IOException if the template can't be loaded
     */
    Template getTemplate(Name name, String templateFileName, Charset charset) throws IOException {
        ResourceInfo ri = null;
        if (name != null) {
            ri = wms.getResourceInfo(name);
            // ri can be null if the type is the result of a rendering transformation
        }

        synchronized (templateConfig) {
            // setup template subsystem
            if (templateLoader == null) {
                templateLoader = new GeoServerTemplateLoader(getClass());
            }
            templateLoader.setResource(ri);
            templateConfig.setTemplateLoader(templateLoader);
            Template t = templateConfig.getTemplate(templateFileName);
            t.setEncoding(charset.name());
            return t;
        }
    }

    @Override
    public String getCharset() {
        return wms.getGeoServer().getSettings().getCharset();
    }
}
