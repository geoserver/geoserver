/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage;

import java.io.IOException;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.GridCoverageReaderCallback;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourcePool;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.factory.Hints;

/**
 * 
 * Implementation of GridCoverageReaderCallback extension point to return wrapping GridCoverageReaders
 * 
 * @see {@link GridCoverageReaderCallback}
 * 
 */
public class CachingGridCoverageReaderCallback implements GridCoverageReaderCallback {
    
    public final static String COVERAGETILELAYERINFO_KEY = "coverageTileLayerInfo.key";

    private GridCoveragesCache gridCoveragesCache;

    public GridCoveragesCache getGridCoveragesCache() {
        return gridCoveragesCache;
    }

    public void setGridCoveragesCache(GridCoveragesCache gridCoveragesCache) {
        this.gridCoveragesCache = gridCoveragesCache;
    }

    @Override
    public boolean canHandle(CoverageInfo info) {
        boolean canHandle = false;
        MetadataMap metadata = info.getMetadata();
        if (metadata != null && metadata.containsKey(COVERAGETILELAYERINFO_KEY)) {
            canHandle = true;
        }
        return canHandle;
    }

    @Override
    public GridCoverage2DReader wrapGridCoverageReader(ResourcePool pool, CoverageInfo info,
            String coverageName, Hints hints) throws IOException {
        return CachingGridCoverage2DReader.wrap(pool, gridCoveragesCache, info, coverageName, hints);
    }

}
