/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/** Test the proper encoding of duplicated/repeated features with Ids */
public class GetFeatureNumberMatchedWithoutPrimaryKeyGMLTest
        extends GetFeatureNumberMatchedGMLTest {

    @Override
    protected FeatureGML32MockData createTestData() {
        return new FeatureGML32MockData(false);
    }
}
