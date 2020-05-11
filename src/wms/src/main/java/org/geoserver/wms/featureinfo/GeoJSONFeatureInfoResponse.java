/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.io.*;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;

/**
 * A GetFeatureInfo response handler specialized in producing Json and JsonP data for a
 * GetFeatureInfo request.
 *
 * @author Simone Giannecchini, GeoSolutions
 * @author Carlo Cancellieri - GeoSolutions
 */
public class GeoJSONFeatureInfoResponse extends GetFeatureInfoOutputFormat {

    FreeMarkerTemplateManager templateManager;

    private WMS wms;

    /** @throws Exception if outputFormat is not a valid json mime type */
    public GeoJSONFeatureInfoResponse(
            final WMS wms, GeoServerResourceLoader resourceLoader, final String outputFormat) {
        super(outputFormat);
        this.wms = wms;
        if (outputFormat.equals("application/json"))
            this.templateManager =
                    new GeoJSONTemplateManager(
                            FreeMarkerTemplateManager.OutputFormat.JSON, wms, resourceLoader);
    }

    /** @throws Exception if outputFormat is not a valid json mime type */
    public GeoJSONFeatureInfoResponse(final WMS wms, final String outputFormat) {
        super(outputFormat);
        this.wms = wms;
    }

    /**
     * Writes a Json (or Jsonp) response on the passed output stream
     *
     * @see {@link GetFeatureInfoOutputFormat#write(FeatureCollectionType, GetFeatureInfoRequest,
     *     OutputStream)}
     */
    @Override
    public void write(
            FeatureCollectionType features, GetFeatureInfoRequest fInfoReq, OutputStream out)
            throws IOException {
        boolean usedTemplates = false;

        if (templateManager != null)
            // check before if there are free marker templates to customize response
            usedTemplates = templateManager.write(features, fInfoReq, out);

        if (!usedTemplates) {
            GeoJSONGetFeatureResponse format =
                    new GeoJSONGetFeatureResponse(wms.getGeoServer(), getContentType());
            format.write(features, out, null);
        }
    }

    @Override
    public String getCharset() {
        return wms.getGeoServer().getSettings().getCharset();
    }
}
