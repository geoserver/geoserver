/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import freemarker.template.Template;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;

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

    private FreeMarkerTemplateManager templateManager;

    private WMS wms;

    public HTMLFeatureInfoOutputFormat(final WMS wms, GeoServerResourceLoader resourceLoader) {
        super(FORMAT);
        this.wms = wms;
        this.templateManager =
                new HTMLTemplateManager(
                        FreeMarkerTemplateManager.OutputFormat.HTML, wms, resourceLoader);
    }

    /**
     * Writes the image to the client.
     *
     * @param out The output stream to write to.
     * @throws ServiceException For problems with geoserver
     * @throws java.io.IOException For problems writing the output.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void write(
            FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {
        templateManager.write(results, request, out);
    }

    @Override
    public String getCharset() {
        return wms.getGeoServer().getSettings().getCharset();
    }

    public FreeMarkerTemplateManager getTemplateManager() {
        return templateManager;
    }

    /** */
    private final class HTMLTemplateManager extends FreeMarkerTemplateManager {

        public HTMLTemplateManager(
                OutputFormat format, WMS wms, GeoServerResourceLoader resourceLoader) {
            super(format, wms, resourceLoader);
        }

        @Override
        protected boolean templatesExist(
                Template header, Template footer, List<FeatureCollection> collections)
                throws IOException {
            return true;
        }

        @Override
        protected void handleContent(
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

        @Override
        protected String getTemplateFileName(String filename) {
            return filename + ".ftl";
        }
    }
}
