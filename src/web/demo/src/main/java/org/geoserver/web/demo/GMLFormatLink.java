/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import java.util.HashMap;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.config.GeoServer;
import org.geoserver.web.demo.PreviewLayer.GMLOutputParams;
import org.geoserver.web.demo.PreviewLayer.PreviewLayerType;
import org.geoserver.wfs.WFSInfo;

public class GMLFormatLink extends CommonFormatLink {

    private GeoServer geoserver;

    /** GML output params computation may be expensive, results are cached in this map */
    private final transient Map<String, GMLOutputParams> gmlParamsCache =
            new HashMap<String, GMLOutputParams>();

    @Override
    public ExternalLink getFormatLink(PreviewLayer layer) {
        ExternalLink gmlLink =
                new ExternalLink(
                        this.getComponentId(),
                        layer.getGmlLink(gmlParamsCache) + this.getMaxFeatures(),
                        (new StringResourceModel(this.getTitleKey(), (Component) null, null))
                                .getString());
        gmlLink.setVisible(
                layer.getType() == PreviewLayerType.Vector && layer.hasServiceSupport("WFS"));
        return gmlLink;
    }

    /**
     * Generates the maxFeatures element of the WFS request using the value of
     * maxNumberOfFeaturesForPreview. Values <= 0 give no limit.
     *
     * @return "&maxFeatures=${maxNumberOfFeaturesForPreview}" or "" if
     *     maxNumberOfFeaturesForPreview <= 0"
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
