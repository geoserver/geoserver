/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.BufferedImage;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphic;
import org.geoserver.wms.GetLegendGraphicOutputFormat;
import org.geoserver.wms.GetLegendGraphicRequest;

/**
 * JAI based output format for the WMS {@link GetLegendGraphic} operation that creates legend
 * graphics to be encoded as {@code image/jpeg}.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class JPEGLegendOutputFormat implements GetLegendGraphicOutputFormat {

    public static final String MIME_TYPE = "image/jpeg";

    public JPEGLegendOutputFormat() {
        //
    }

    /**
     * Builds a JPEG {@link BufferedImageLegendGraphic}
     *
     * @return a {@link BufferedImageLegendGraphic} holding a legend image appropriate to be encoded
     *     as JPEG
     * @see GetLegendGraphicOutputFormat#produceLegendGraphic(GetLegendGraphicRequest)
     */
    public LegendGraphic produceLegendGraphic(GetLegendGraphicRequest request)
            throws ServiceException {

        request.setTransparent(false);

        LegendGraphicBuilder builder = new BufferedImageLegendGraphicBuilder();
        BufferedImage legendGraphic = (BufferedImage) builder.buildLegendGraphic(request);
        LegendGraphic legend = new BufferedImageLegendGraphic(legendGraphic);
        return legend;
    }

    /**
     * @return {@code image/jpeg}
     * @see GetLegendGraphicOutputFormat#getContentType()
     */
    public String getContentType() throws IllegalStateException {
        return MIME_TYPE;
    }
}
