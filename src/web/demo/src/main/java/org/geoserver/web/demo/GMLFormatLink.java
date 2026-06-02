/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import java.util.HashMap;
import java.util.Map;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.config.GeoServer;
import org.geoserver.web.PreviewLink;
import org.geoserver.web.demo.PreviewLayer.GMLOutputParams;
import org.geoserver.web.demo.PreviewLayer.PreviewLayerType;
import org.geoserver.wfs.WFSInfo;

public class GMLFormatLink extends CommonFormatLink {

    private GeoServer geoserver;

    /** GML output params computation may be expensive, results are cached in this map */
    private final transient Map<String, GMLOutputParams> gmlParamsCache = new HashMap<>();

    @Override
    public PreviewLink getFormatLink(PreviewLayer layer) {
        if (layer.getType() != PreviewLayerType.Vector || !layer.hasServiceSupport("WFS")) return null;
        String label = new StringResourceModel(this.getTitleKey(), null, null).getString();
        return new PreviewLink(label, layer.getGmlLink(gmlParamsCache) + this.getMaxFeatures(), label);
    }

    /**
     * Generates the maxFeatures element of the WFS request using the value of maxNumberOfFeaturesForPreview. Values <=
     * 0 give no limit.
     *
     * @return "&maxFeatures=${maxNumberOfFeaturesForPreview}" or "" if maxNumberOfFeaturesForPreview <= 0"
     */
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
