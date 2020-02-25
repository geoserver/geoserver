/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geoserver.ows.Response;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.legendgraphic.BufferedImageLegendGraphic;

/**
 * WMS GetLegendGraphic operation default implementation.
 *
 * @author Gabriel Roldan
 */
public class GetLegendGraphic {

    private final WMS wms;

    public GetLegendGraphic(final WMS wms) {
        this.wms = wms;
    }

    /**
     * Produces a representation of the map's legend graphic given by the {@code request} by means
     * of a {@link GetLegendGraphicOutputFormat}.
     *
     * <p>Note there's no subscribed return type for the produced legend graphic, the only requisite
     * for the whole OWS operation to succeed is that there exist a {@link Response} object (in the
     * application context) that can handle the returned object.
     *
     * @return an Object representing the produced legend graphic
     * @see WMSExtensions#findLegendGraphicFormat
     * @see BufferedImageLegendGraphic
     */
    public Object run(final GetLegendGraphicRequest request) throws ServiceException {

        final String outputFormat = request.getFormat();
        final GetLegendGraphicOutputFormat format = wms.getLegendGraphicOutputFormat(outputFormat);
        if (format == null) {
            throw new ServiceException(
                    "There is no support for creating legends in " + outputFormat + " format",
                    "InvalidFormat");
        }
        Object legend = format.produceLegendGraphic(request);
        return legend;
    }
}
