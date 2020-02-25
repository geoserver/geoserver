/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geoserver.platform.ServiceException;

/**
 * Provides the skeleton for producers of a legend image, as required by the GetLegendGraphic WMS
 * request.
 *
 * <p>Implementations are meant to be state-less and to support a single image output format, which
 * is declared in {@link #getContentType()}.
 *
 * <p>For a given GetLegendGraphicOutputFormat to be found at runtime, it is only needed that a
 * Spring bean is declared in the GeoServer application context.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public interface GetLegendGraphicOutputFormat {
    /**
     * Asks this legend graphic producer to create a graphic for the GetLegenGraphic request
     * parameters held in <code>request</code>
     *
     * @param request the "parsed" request, where "parsed" means that it's properties are already
     *     validated so this method must not take care of verifying the requested layer exists and
     *     the like.
     * @return a representation of the produced legend graphic
     * @throws ServiceException something goes wrong
     */
    Object produceLegendGraphic(GetLegendGraphicRequest request) throws ServiceException;

    /**
     * Returns the MIME type of the content supported by this format
     *
     * @return the output format
     * @throws java.lang.IllegalStateException if this method is called before {@linkplain
     *     #produceLegendGraphic(GetLegendGraphicRequest)}.
     */
    String getContentType();
}
