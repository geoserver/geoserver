/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.vsi;

import it.geosolutions.imageio.plugins.vrt.VRTImageReaderSpi;
import org.apache.log4j.Logger;
import org.geotools.coverageio.BaseGridFormatFactorySPI;

/**
 * Format factory for VSI virtual file systems
 *
 * @author Matthew Northcott <matthewnorthcott@catalyst.net.nz>
 */
public final class VSIFormatFactory extends BaseGridFormatFactorySPI {

    private static final Logger LOGGER = Logger.getLogger(VSIFormatFactory.class.getName());

    private VRTImageReaderSpi vrtImageReaderSpi;

    /** Constructor */
    public VSIFormatFactory() {
        vrtImageReaderSpi = new VRTImageReaderSpi();
    }

    /**
     * Return true if all necessary dependencies are met in order to use this resource type
     *
     * @return true if all necessary dependencies are met in order to use this resource type
     */
    @Override
    public boolean isAvailable() {
        boolean available = false;

        try {
            Class.forName("it.geosolutions.imageio.plugins.vrt.VRTImageReaderSpi");
            available = vrtImageReaderSpi.isAvailable();
        } catch (ClassNotFoundException cnf) {
        }

        LOGGER.debug(
                available
                        ? "VSIFormatFactory is available."
                        : "VSIFormatFactory is not available.");

        return available;
    }

    /**
     * Create and return the corrsponding format object of this class
     *
     * @return VSIFormat object for this data resource type
     */
    @Override
    public VSIFormat createFormat() {
        return new VSIFormat();
    }
}
