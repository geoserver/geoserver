/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.crs;

import java.net.URL;
import org.geotools.referencing.factory.epsg.FactoryUsingWKT;

/** Factory defining GSR-specific spatial references, such as 102100. */
public class GSRCustomWKTFactory extends FactoryUsingWKT {

    /**
     * Returns the URL to the property file that contains CRS definitions.
     *
     * @return The URL, or {@code null} if none.
     */
    @Override
    protected URL getDefinitionsURL() {
        // Use the built-in property definitions
        return GSRCustomWKTFactory.class.getResource("user_epsg.properties");
    }
}
