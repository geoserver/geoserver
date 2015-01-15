/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;

import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.factory.Hints;

/**
 * Extension point to provide GridCoverage.
 * 
 * The extension point is used as follows:
 * 
 * <pre>
 * {@code
 * CoverageInfo info; //CoverageInfo instance
 * gridCoverageInitializers = GeoServerExtensions.extensions(GridCoverageReaderCallback.class);
 * for(GridCoverageReaderCallback gcc : gridCoverageInitializers){
 *      if(gcc.canHandle(info)){
 *          ;
 *      }
 * }
 * </pre>
 * 
 * @author Daniele Romagnoli GeoSolutions 
 * @author Nicola Lagomarsini Geosolutions
 */
public interface GridCoverageReaderCallback {

    /**
     * Checks if this initializer can handle the specified resource handle
     */
    boolean canHandle(CoverageInfo info);

    GridCoverage2DReader wrapGridCoverageReader(ResourcePool pool, 
            CoverageInfo coverageInfo,
            String coverageName,
            Hints hints) throws IOException;
}
