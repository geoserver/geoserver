/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.tiffspy;

import java.io.IOException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.util.factory.Hints;
import org.opengis.parameter.GeneralParameterValue;

/** Extension to GeoTIFF reader allowing to spy read parameters */
public class GeoTIFFSpyReader extends GeoTiffReader {

    public static GeneralParameterValue[] getLastParams() {
        return LAST_PARAMS;
    }

    static GeneralParameterValue[] LAST_PARAMS;

    public GeoTIFFSpyReader(Object input) throws DataSourceException {
        super(input);
    }

    public GeoTIFFSpyReader(Object input, Hints uHints) throws DataSourceException {
        super(input, uHints);
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] params) throws IOException {
        LAST_PARAMS = params;
        return super.read(params);
    }
}
