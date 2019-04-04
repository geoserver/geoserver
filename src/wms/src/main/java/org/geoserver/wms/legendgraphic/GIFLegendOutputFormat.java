/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.BufferedImage;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicOutputFormat;
import org.geoserver.wms.GetLegendGraphicRequest;

/**
 * Producer of legend graphics in image/gif format.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GIFLegendOutputFormat implements GetLegendGraphicOutputFormat {

    static final String MIME_TYPE = "image/gif";

    /**
     * @return a {@link BufferedImageLegendGraphic}
     * @see GetLegendGraphicOutputFormat#produceLegendGraphic(GetLegendGraphicRequest)
     * @see BufferedImageLegendGraphicBuilder
     */
    public LegendGraphic produceLegendGraphic(GetLegendGraphicRequest request)
            throws ServiceException {
        LegendGraphicBuilder builder = new BufferedImageLegendGraphicBuilder();
        BufferedImage legendGraphic = (BufferedImage) builder.buildLegendGraphic(request);
        LegendGraphic legend = new BufferedImageLegendGraphic(legendGraphic);
        return legend;
    }

    /**
     * @return {@code "image/gif"}
     * @see GetLegendGraphicOutputFormat#getContentType()
     */
    public String getContentType() {
        return MIME_TYPE;
    }
}
