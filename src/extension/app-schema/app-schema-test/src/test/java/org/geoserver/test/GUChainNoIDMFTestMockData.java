/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * Mock data for testing validation with GeoServer.
 *
 * @author Victor Tey, CSIRO Exploration and Mining
 */
public class GUChainNoIDMFTestMockData extends AbstractAppSchemaMockData {

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        addFeatureType(
                GSML_PREFIX, "GeologicUnit", "GUInLineNoIDMF.xml", "GeologicUnit.properties");
    }
}
