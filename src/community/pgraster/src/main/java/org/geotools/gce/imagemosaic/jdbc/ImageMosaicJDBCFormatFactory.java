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
package org.geotools.gce.imagemosaic.jdbc;

import java.util.Collections;
import java.util.Map;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;

/**
 * Implementation of the GridCoverageFormat service provider interface for mosaicing of
 * georeferenced images and image pyramids stored in a jdbc database
 *
 * @author mcr
 * @since 2.5
 */
public class ImageMosaicJDBCFormatFactory implements GridFormatFactorySpi {
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
        boolean available = true;

        try {
            Class.forName("javax.media.jai.JAI");
            Class.forName("org.geotools.gce.imagemosaic.jdbc.ImageMosaicJDBCReader");
        } catch (ClassNotFoundException cnf) {
            available = false;
        }

        return available;
    }

    /** @see GridFormatFactorySpi#createFormat(). */
    @Override
    public ImageMosaicJDBCFormat createFormat() {
        return new ImageMosaicJDBCFormat();
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
