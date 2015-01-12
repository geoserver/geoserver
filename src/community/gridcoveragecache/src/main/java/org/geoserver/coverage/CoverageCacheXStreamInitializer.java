/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geoserver.coverage.layer.CoverageTileLayerInfo;
import org.geoserver.coverage.layer.CoverageTileLayerInfoImpl;

/**
 * 
 * Implementation of XStreamPersisterInitializer extension point to serialize {@link CoverageTileLayerInfo}
 *
 */
public class CoverageCacheXStreamInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType("coverageTileLayerInfo",CoverageTileLayerInfoImpl.class);
    }
}
