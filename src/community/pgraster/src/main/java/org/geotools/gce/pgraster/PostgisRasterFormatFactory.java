/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.gce.pgraster;

import java.util.Collections;
import java.util.Map;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;

/**
 * Implementation of the {@link GridFormatFactorySpi} service provider interface for mosaicing of
 * georeferenced images and image pyramids stored in postgis-raster.
 *
 * @author mcr
 * @since 2.5
 */
public class PostgisRasterFormatFactory implements GridFormatFactorySpi {

    private static Boolean available;

    /**
     * Tells me if this plugin will work on not given the actual installation.
     *
     * <p>Dependecies are mostly from JAI and ImageIO so if they are installed you should not have
     * many problems.
     *
     * @return False if something's missing, true otherwise.
     */
    @Override
    public boolean isAvailable() {
        if (null == available) {
            try {
                Class.forName("javax.media.jai.JAI");
                Class.forName("org.postgresql.Driver");
                available = true;
            } catch (ClassNotFoundException cnf) {
                available = false;
            }
        }

        return available;
    }

    /** @see GridFormatFactorySpi#createFormat(). */
    @Override
    public PostgisRasterFormat createFormat() {
        return new PostgisRasterFormat();
    }

    /**
     * Returns the implementation hints. The default implementation returns an empty map.
     *
     * @return An empty map.
     */
    @Override
    public Map<java.awt.RenderingHints.Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }
}
