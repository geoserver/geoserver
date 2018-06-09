/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * Mock data for testing SF0 in app-schema
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class BoreholeViewMockData extends AbstractAppSchemaMockData {

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        putNamespace("gsmlp", "http://xmlns.geosciml.org/geosciml-portrayal/2.0");
        addFeatureType(
                "gsmlp",
                "BoreholeView",
                "BoreholeView.xml",
                "Gsml32Borehole.properties",
                "geosciml-portrayal.xsd");
    }
}
