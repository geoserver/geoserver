/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.json.GeoJSONFeatureWriter;
import org.geoserver.json.JSONType;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.feature.FeatureCollection;

/**
 * A GetFeatureInfo response handler specialized in producing Json and JsonP data for a GetFeatureInfo request.
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
                    new GeoJSONTemplateManager(FreeMarkerTemplateManager.OutputFormat.JSON, wms, resourceLoader);
    }

    /** @throws Exception if outputFormat is not a valid json mime type */
    public GeoJSONFeatureInfoResponse(final WMS wms, final String outputFormat) {
        super(outputFormat);
        this.wms = wms;
    }

    /**
     * Writes a Json (or Jsonp) response on the passed output stream
     *
     * @see {@link GetFeatureInfoOutputFormat#write(FeatureCollectionType, GetFeatureInfoRequest, OutputStream)}
     */
    @Override
    public void write(FeatureCollectionType features, GetFeatureInfoRequest fInfoReq, OutputStream out)
            throws IOException {
        boolean usedTemplates = false;

        if (templateManager != null) {
            // check before if there are free marker templates to customize response
            @SuppressWarnings("unchecked")
            List<FeatureCollection> collections = features.getFeature();
            usedTemplates = templateManager.write(collections, out);
        }

        if (!usedTemplates) {
            GeoJSONFeatureWriter<FeatureType, Feature> writer = new GeoJSONFeatureWriter<>(wms.getGeoServer()) {
                @Override
                protected boolean isFeatureBounding() {
                    return false; // TODO: what to do here?
                }
            };
            @SuppressWarnings("unchecked")
            List<FeatureCollection<FeatureType, Feature>> collections = features.getFeature();
            boolean jsonp = JSONType.isJsonpMimeType(getContentType());
            writer.write(collections, out, features.getNumberOfFeatures(), jsonp);
        }
    }

    @Override
    public String getCharset() {
        return wms.getGeoServer().getSettings().getCharset();
    }
}
