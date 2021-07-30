/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.util.Map;
import org.apache.commons.collections4.map.HashedMap;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.feature.type.FeatureType;

/**
 * A grid coverage wrapper that allows to carry around a user map like {@link
 * FeatureType#getUserData()} does
 */
public class MetaGridCoverage2D extends GridCoverage2D {

    Map<Object, Object> userData = new HashedMap<>();

    public MetaGridCoverage2D(GridCoverage2D coverage) {
        super(coverage.getName().toString(), coverage);
    }

    public Map<Object, Object> getUserData() {
        return userData;
    }
}
