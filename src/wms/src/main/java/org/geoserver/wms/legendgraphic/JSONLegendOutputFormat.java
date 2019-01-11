/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import net.sf.json.JSONObject;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphic;
import org.geoserver.wms.GetLegendGraphicOutputFormat;
import org.geoserver.wms.GetLegendGraphicRequest;

/**
 * JSON output format for the WMS {@link GetLegendGraphic} operation.
 *
 * @author Gabriel Roldan
 * @author Justin Deoliveira
 * @author Ian Turton
 */
public class JSONLegendOutputFormat implements GetLegendGraphicOutputFormat {

    public static final String MIME_TYPE = "application/json";

    /**
     * Creates a new JAI based legend producer for creating <code>outputFormat</code> type images.
     */
    public JSONLegendOutputFormat() {
        //
    }

    /**
     * Builds and returns a {@link JSONLegendGraphic} appropriate to be encoded as JSON
     *
     * @see GetLegendGraphicOutputFormat#produceLegendGraphic(GetLegendGraphicRequest)
     */
    public JSONLegendGraphic produceLegendGraphic(GetLegendGraphicRequest request)
            throws ServiceException {
        LegendGraphicBuilder builder = new JSONLegendGraphicBuilder();
        JSONObject legendGraphic = (JSONObject) builder.buildLegendGraphic(request);
        JSONLegendGraphic legend = new JSONLegendGraphic(legendGraphic);
        return legend;
    }

    /**
     * @return {@code "image/png"}
     * @see org.geoserver.wms.GetLegendGraphicOutputFormat#getContentType()
     */
    public String getContentType() throws IllegalStateException {
        return MIME_TYPE;
    }
}
