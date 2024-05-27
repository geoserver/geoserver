/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import freemarker.template.Template;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;

/**
 * The {@code HTMLTemplateManager} class extends the {@code FreeMarkerTemplateManager} class to
 * provide functionality for applying Freemarker templates to generate HTML output for WMS (Web Map
 * Service) and potentially WFS (Web Feature Service) responses.
 */
public final class HTMLTemplateManager extends FreeMarkerTemplateManager {

    public HTMLTemplateManager(
            OutputFormat format, WMS wms, GeoServerResourceLoader resourceLoader) {
        super(format, wms, resourceLoader);
    }

    @Override
    protected boolean templatesExist(
            Template header, Template footer, List<FeatureCollection> collections) {
        return true;
    }

    @Override
    protected void handleContent(List<FeatureCollection> collections, OutputStreamWriter osw)
            throws IOException {
        for (FeatureCollection fc : collections) {
            Template content = getContentTemplate(fc, wms.getCharSet());
            processTemplate("content", fc, content, osw);
        }
    }

    @Override
    protected String getTemplateFileName(String filename) {
        return filename + ".ftl";
    }
}
