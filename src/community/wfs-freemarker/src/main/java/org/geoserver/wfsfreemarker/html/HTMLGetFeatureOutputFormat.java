/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfsfreemarker.html;

import static org.geoserver.wms.featureinfo.FreeMarkerTemplateManager.OutputFormat.HTML;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.response.ComplexFeatureAwareFormat;
import org.geoserver.wms.WMS;
import org.geoserver.wms.featureinfo.FreeMarkerTemplateManager;
import org.geoserver.wms.featureinfo.HTMLTemplateManager;
import org.geotools.feature.FeatureCollection;

/**
 * A GetFeature response handler specialized in producing HTML data for a GetFeature request through the
 * {@link FreeMarkerTemplateManager}.
 */
public class HTMLGetFeatureOutputFormat extends WFSGetFeatureOutputFormat implements ComplexFeatureAwareFormat {

    private final HTMLTemplateManager templateManager;

    public HTMLGetFeatureOutputFormat(GeoServer gs, final WMS wms, GeoServerResourceLoader resourceLoader) {
        super(gs, HTML.getFormat());
        this.templateManager = new HTMLTemplateManager(HTML, wms, resourceLoader);
    }

    /** capabilities output format string. */
    @Override
    public String getCapabilitiesElementName() {
        return "HTML";
    }

    /** Returns the mime type */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return HTML.getFormat();
    }

    @Override
    protected void write(FeatureCollectionResponse featureCollection, OutputStream output, Operation operation)
            throws IOException {
        List<FeatureCollection> resultsList = featureCollection.getFeature();
        templateManager.write(resultsList, output);
    }

    @Override
    public String getCharset(Operation operation) {
        return gs.getGlobal().getSettings().getCharset();
    }

    @Override
    protected String getExtension(FeatureCollectionResponse response) {
        return "html";
    }
}
