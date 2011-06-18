/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.template.FeatureWrapper;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Produces a FeatureInfo response in HTML. Relies on {@link AbstractFeatureInfoResponse} and the
 * feature delegate to do most of the work, just implements an HTML based writeTo method.
 * 
 * <p>
 * In the future James suggested that we allow some sort of template system, so that one can control
 * the formatting of the html output, since now we just hard code some minimal header stuff. See
 * http://jira.codehaus.org/browse/GEOS-196
 * </p>
 * 
 * @author James Macgill, PSU
 * @author Andrea Aime, TOPP
 * @version $Id$
 */
public class HTMLFeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {

    private static final String FORMAT = "text/html";

    private static Configuration templateConfig;

    static {
        // initialize the template engine, this is static to maintain a cache
        // over instantiations of kml writer
        templateConfig = new Configuration();
        templateConfig.setObjectWrapper(new FeatureWrapper());
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
     * @param out
     *            The output stream to write to.
     * 
     * @throws org.vfny.geoserver.ServiceException
     *             For problems with geoserver
     * @throws java.io.IOException
     *             For problems writing the output.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void write(FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {
        // setup the writer
        final Charset charSet = wms.getCharSet();
        final OutputStreamWriter osw = new OutputStreamWriter(out, charSet);

        // if there is only one feature type loaded, we allow for header/footer customization,
        // otherwise we stick with the generic ones
        Template header = null;
        Template footer = null;
        List<SimpleFeatureCollection> collections = results.getFeature();
        if (collections.size() == 1) {
            SimpleFeatureCollection collection = (SimpleFeatureCollection) collections.get(0);
            SimpleFeatureType templateFeatureType = collection.getSchema();
            header = getTemplate(templateFeatureType, "header.ftl", charSet);
            footer = getTemplate(templateFeatureType, "footer.ftl", charSet);
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
        for (Iterator it = collections.iterator(); it.hasNext();) {
            SimpleFeatureCollection fc = (SimpleFeatureCollection) it.next();
            if (fc.size() > 0) {
                SimpleFeatureType ft = fc.getSchema();
                Template content = getTemplate(ft, "content.ftl", charSet);
                try {
                    content.process(fc, osw);
                } catch (TemplateException e) {
                    String msg = "Error occured processing content template " + content.getName()
                            + " for " + ft.getTypeName();
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
    }

    /**
     * Uses a {@link GeoServerTemplateLoader TemplateLoader} too look up for the template file named
     * <code>templateFilename</code> for the given <code>featureType</code>.
     * 
     * @param featureType
     *            the featureType to look the template for, may well correspond to an actually
     *            registered feature type or to a wrapper feature type used to adapt the result of
     *            the feature info request over a raster coverage. In case you want to load the
     *            default template you can leave this argument null
     * @param templateFileName
     *            the name of the template to look for
     * @param charset
     *            the encoding to apply to the resulting {@link Template}
     * @return the template named <code>templateFileName</code>
     * @throws IOException
     *             if the template can't be loaded
     */
    Template getTemplate(SimpleFeatureType featureType, String templateFileName, Charset charset)
            throws IOException {
        // setup template subsystem
        if (templateLoader == null) {
            templateLoader = new GeoServerTemplateLoader(getClass());
        }

        if (featureType != null) {
            final Name name = featureType.getName();

            ResourceInfo ri = wms.getResourceInfo(name);
            if (ri != null) {
                templateLoader.setResource(ri);
            } else {
                throw new IllegalArgumentException("Can't find neither a FeatureType nor "
                        + "a CoverageInfo or WMSLayerInfo named " + name);
            }                        
        }

        synchronized (templateConfig) {
            templateConfig.setTemplateLoader(templateLoader);
            Template t = templateConfig.getTemplate(templateFileName);
            t.setEncoding(charset.name());
            return t;
        }
    }
}
