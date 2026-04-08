/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.tiffspy;

import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;

/** Extension to GeoTIFF format allowing to spy read parameters */
public class GeoTIFFSpyFormatFactory implements GridFormatFactorySpi {

    @Override
    public AbstractGridFormat createFormat() {
        return new GeoTIFFSpyFormat();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
