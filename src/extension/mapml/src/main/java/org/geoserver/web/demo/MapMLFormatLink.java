/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;

import java.util.Map;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.mapml.MapMLConstants;
import org.geoserver.mapml.tcrs.TiledCRSConstants;
import org.geoserver.mapml.tcrs.TiledCRSParams;
import org.geoserver.wms.GetMapRequest;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

public class MapMLFormatLink extends CommonFormatLink {

    @Override
    public ExternalLink getFormatLink(PreviewLayer layer) {
        String link = layer.getWmsLink(this::customizeRequest);

        ExternalLink olLink =
                new ExternalLink(
                        this.getComponentId(),
                        link,
                        (new StringResourceModel(this.getTitleKey(), null, null)).getString());
        olLink.setVisible(layer.hasServiceSupport("WMS"));
        return olLink;
    }

    /** Customize the request to use the MapML format and a native MapML CRS if possible */
    void customizeRequest(GetMapRequest request, Map<String, String> params) {
        // set the format
        params.put("format", MapMLConstants.MAPML_HTML_MIME_TYPE);

        // check if we can use a native MapML CRS, otherwise fall back to WGS84 to
        // have something that can display anyways
        TiledCRSConstants.tiledCRSDefinitions.values().stream()
                .filter(tcrs -> matches(request, tcrs))
                .findFirst()
                .ifPresentOrElse(
                        tcrs -> params.put("srs", tcrs.getSRSName()),
                        () -> {
                            params.put("srs", "MapML:WGS84");
                            params.put("bbox", getWGS84Bounds(request));
                        });
    }

    /** Check if the request CRS matches the given TiledCRSParams */
    private static boolean matches(GetMapRequest request, TiledCRSParams tcrs) {
        try {
            return CRS.equalsIgnoreMetadata(CRS.decode(tcrs.getSRSName()), request.getCrs());
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    /** Get the WGS84 bounds of the request */
    private static String getWGS84Bounds(GetMapRequest request) {
        try {
            ReferencedEnvelope re =
                    new ReferencedEnvelope(request.getBbox(), CRS.decode(request.getSRS()))
                            .transform(WGS84, true);
            return re.getMinX() + "," + re.getMinY() + "," + re.getMaxX() + "," + re.getMaxY();
        } catch (TransformException | FactoryException e) {
            throw new RuntimeException(e);
        }
    }
}
