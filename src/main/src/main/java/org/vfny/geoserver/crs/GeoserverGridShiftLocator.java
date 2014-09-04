/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import java.io.File;
import java.net.URL;

import org.geotools.data.DataUtilities;
import org.geotools.factory.AbstractFactory;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.factory.gridshift.GridShiftLocator;
import org.opengis.metadata.citation.Citation;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * Provides a hook to locate grid shift files, such as NTv1, NTv2 and NADCON ones.
 * 
 * @author Andrea Aime - Geosolutions
 * @author Oscar Fonts - geomati.co
 */
public class GeoserverGridShiftLocator extends AbstractFactory implements GridShiftLocator {

    public GeoserverGridShiftLocator() {
        // higher priority than the default locator
        super(NORMAL_PRIORITY + 10);
    }

    @Override
    public Citation getVendor() {
        // one day we could roll a GeoServer citation, but for just one use it's too much work
        return Citations.GEOTOOLS;
    }

    /**
     * Locate the specified grid file.
     * 
     * It will look in GEOSERVER_DATA_DIR/user_projections
     * 
     * @param grid the grid name/location
     * @return the fully resolved URL of the grid or null, if the resource cannot be located.
     */
    @Override
    public URL locateGrid(String grid) {
        if (grid == null)
            return null;

        File gridfile = new File(GeoserverDataDirectory.getGeoserverDataDirectory(),
                "user_projections/" + grid);

        if (gridfile.exists()) {
            return DataUtilities.fileToURL(gridfile);
        } else {
            return null;
        }
    }
}
