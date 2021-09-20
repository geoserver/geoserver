/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * Mock data for testing Multiple Geometries in app-schema
 *
 * @author Niels Charlier
 */
public class MultiGeometryMockData extends AbstractAppSchemaMockData {

    @Override
    public void addContent() {
        putNamespace("ex", "http://example.com");
        addFeatureType(
                "ex",
                "geomContainer",
                "MultiGeometry.xml",
                "MultiGeometry.properties",
                "NestedGeometry.xsd");
    }
}
