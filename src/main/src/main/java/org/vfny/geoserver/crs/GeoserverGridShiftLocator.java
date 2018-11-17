/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import java.net.URL;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.factory.gridshift.GridShiftLocator;
import org.geotools.util.URLs;
import org.geotools.util.factory.AbstractFactory;
import org.opengis.metadata.citation.Citation;

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
     * <p>It will look in GEOSERVER_DATA_DIR/user_projections
     *
     * @param grid the grid name/location
     * @return the fully resolved URL of the grid or null, if the resource cannot be located.
     */
    @Override
    public URL locateGrid(String grid) {
        if (grid == null) return null;

        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        if (loader == null) {
            return null; // must be test case still loading
        }
        Resource gridfile = loader.get("user_projections/" + grid);

        if (gridfile.getType() == Type.RESOURCE) {
            return URLs.fileToUrl(gridfile.file());
        } else {
            return null;
        }
    }
}
