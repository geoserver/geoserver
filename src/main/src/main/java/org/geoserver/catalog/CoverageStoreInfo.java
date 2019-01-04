/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.util.ProgressListener;

/**
 * A raster or coverage based store.
 *
 * @author Justin Deoliveira, The Open Planning project
 */
public interface CoverageStoreInfo extends StoreInfo {

    /**
     * The coverage store url.
     *
     * @uml.property name="url"
     */
    String getURL();

    /**
     * Sets the coverage store url.
     *
     * @uml.property name="url"
     */
    void setURL(String url);

    /** The grid format. */
    AbstractGridFormat getFormat();

    GridCoverageReader getGridCoverageReader(ProgressListener listener, Hints hints)
            throws IOException;

    /** Returns the coverage resource from the store with the given name. */
    // CoverageResource getResource(String name, ProgressListener listener);

    /** Returns the coverage resources provided by the store. */
    // Iterator<CoverageResource> getResources(ProgressListener monitor)
    //    throws IOException;
}
