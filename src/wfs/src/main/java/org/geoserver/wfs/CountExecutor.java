/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;

/**
 * A class executing a feature count, but also able to return a pre-computed one. Used as an
 * accessory to compute the numberMatched attribute for WFS 2.0, and running the actual counts only
 * when strictly necessary
 *
 * @author Andrea Aime - GeoSolutions
 */
class CountExecutor {

    private static final int COUNT_UNSET = -1;

    FeatureSource source;

    Query query;

    int providedCount = COUNT_UNSET;

    public CountExecutor(FeatureSource source, Query query) {
        this.source = source;
        this.query = query;
    }

    public CountExecutor(int providedCount) {
        this.providedCount = providedCount;
    }

    public int getCount() throws IOException {
        if (isCountSet()) {
            return providedCount;
        } else {
            // make sure we get a count by getting a feature colleciton
            // FeatureSource.getCount(...) can return -1
            return source.getFeatures(query).size();
        }
    }

    public boolean isCountSet() {
        return providedCount != COUNT_UNSET;
    }
}
