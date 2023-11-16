/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.tiffspy;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

/** Extension to GeoTIFF format allowing to spy read parameters */
public class GeoTIFFSpyFormat extends GeoTiffFormat {

    /** Can be used to enable/disable this format during tests */
    public static boolean ENABLED = false;

    static final Logger LOGGER = Logging.getLogger(GeoTIFFSpyFormat.class);
    public static final String NAME = "GeoTIFFSpy";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean accepts(Object source) {
        return ENABLED && super.accepts(source);
    }

    @Override
    public boolean accepts(Object o, Hints hints) {
        return ENABLED && super.accepts(o);
    }

    @Override
    public GeoTiffReader getReader(Object source) {
        try {
            return new GeoTIFFSpyReader(source);
        } catch (DataSourceException e) {
            LOGGER.log(Level.INFO, "Failed to create GeoTIFFSpyReader", e);
            return null;
        }
    }

    @Override
    public GeoTiffReader getReader(Object source, Hints hints) {
        try {
            return new GeoTIFFSpyReader(source, hints);
        } catch (DataSourceException e) {
            LOGGER.log(Level.INFO, "Failed to create GeoTIFFSpyReader", e);
            return null;
        }
    }
}
