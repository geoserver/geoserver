/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.vsi;

import it.geosolutions.imageio.plugins.vrt.VRTImageReaderSpi;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverageio.gdal.BaseGDALGridCoverage2DReader;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Reads a virtual file system supported by GDAL and create a {@link GridCoverage2D} from the data.
 *
 * @author Matthew Northcott <matthewnorthcott@catalyst.net.nz>
 */
public final class VSIReader extends BaseGDALGridCoverage2DReader implements GridCoverageReader {
    private static final Logger LOGGER = Logger.getLogger(VSIReader.class.getName());
    private static final String WORLDFILE_EXT = "";

    /**
     * Creates a new instance of a {@link GridCoverageReader}.
     *
     * @param input Source file for which we want to build an {@link VSIReader}.
     * @throws IOException
     */
    public VSIReader(VRTFile input) throws IOException {
        this(input, null);
    }

    /**
     * Creates a new instance of a {@link GridCoverageReader}.
     *
     * @param input Source file for which we want to build an {@link VSIReader}.
     * @param hints Hints to be used by this reader throughout its life.
     * @throws IOException
     */
    public VSIReader(VRTFile input, Hints hints) throws IOException {
        super(input.getFile(), hints, WORLDFILE_EXT, new VRTImageReaderSpi());

        final CoordinateReferenceSystem tempCrs = input.getCRS();

        if (tempCrs != null) {
            crs = tempCrs;
            originalEnvelope.setCoordinateReferenceSystem(tempCrs);
        }
    }

    /**
     * Return the corresponding format object for this resource
     *
     * @return an instance of the VSIFormat object that corresponds to this resource
     */
    @Override
    public Format getFormat() {
        return new VSIFormat();
    }
}
