/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.*;

import org.junit.Test;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class WFSReprojectionUtilTest {

    @Test
    public void testNullCheck() {
        // used to throw an NPE if the feature type was null
        CoordinateReferenceSystem crs =
                WFSReprojectionUtil.getDeclaredCrs((FeatureType) null, "1.1.0");
        assertNull(crs);
    }
}
