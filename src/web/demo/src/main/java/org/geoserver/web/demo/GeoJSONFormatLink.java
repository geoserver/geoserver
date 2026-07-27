/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.model.StringResourceModel;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.PreviewLink;
import org.geoserver.web.demo.PreviewLayer.PreviewLayerType;
import org.geoserver.wfs.WFSInfo;

public class GeoJSONFormatLink extends CommonFormatLink {

    private static final String GEOJSON_OUTPUT_FORMAT = "application/json";

    private GeoServer geoserver;

    @Override
    public PreviewLink getFormatLink(PreviewLayer layer) {
        if (layer.getType() != PreviewLayerType.Vector || !layer.hasServiceSupport("WFS")) return null;
        String label = new StringResourceModel(this.getTitleKey(), null, null).getString();
        String href = layer.buildWfsLink()
                + "&outputFormat="
                + ResponseUtils.urlEncode(GEOJSON_OUTPUT_FORMAT)
                + getMaxFeatures();
        return new PreviewLink(label, href, label);
    }

    private String getMaxFeatures() {
        WFSInfo service = geoserver.getService(WFSInfo.class);
        if (service.getMaxNumberOfFeaturesForPreview() > 0) {
            return "&maxFeatures=" + service.getMaxNumberOfFeaturesForPreview();
        }
        return "";
    }

    public void setGeoserver(GeoServer geoserver) {
        this.geoserver = geoserver;
    }
}
