/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfsv;

import org.geoserver.catalog.FeatureTypeInfo;

public class VersionedDescribeResults {
    private FeatureTypeInfo[] featureTypeInfo;

    private boolean versioned;

    public VersionedDescribeResults(FeatureTypeInfo[] featureTypeInfo,
            boolean versioned) {
        this.featureTypeInfo = featureTypeInfo;
        this.versioned = versioned;
    }

    public FeatureTypeInfo[] getFeatureTypeInfo() {
        return featureTypeInfo;
    }

    public boolean isVersioned() {
        return versioned;
    }

}
