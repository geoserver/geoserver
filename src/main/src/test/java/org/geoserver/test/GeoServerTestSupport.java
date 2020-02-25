/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;

/**
 * Base test class for GeoServer unit tests.
 *
 * <p>Deriving from this test class provides the test case with preconfigured geoserver and catalog
 * objects.
 *
 * <p>This test case provides a spring application context which loads the application contexts from
 * all modules on the classpath.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class GeoServerTestSupport extends GeoServerAbstractTestSupport {

    @Override
    public MockData buildTestData() throws Exception {
        // create the data directory
        MockData dataDirectory = new MockData();
        populateDataDirectory(dataDirectory);
        return dataDirectory;
    }

    public MockData getTestData() {
        return (MockData) super.getTestData();
    }

    /**
     * Adds the desired type and coverages to the data directory. This method adds all well known
     * data types, subclasses may add their extra ones or decide to avoid the standar ones and build
     * a custom list calling {@link MockData#addPropertiesType(QName, java.net.URL, java.net.URL)}
     * and {@link MockData#addCoverage(QName, InputStream, String)}
     */
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        // set up the data directory
        dataDirectory.addWellKnownTypes(MockData.TYPENAMES);
    }

    /**
     * Sets up a template in a feature type directory.
     *
     * @param featureTypeName The name of the feature type.
     * @param template The name of the template.
     * @param body The content of the template.
     */
    protected void setupTemplate(QName featureTypeName, String template, String body)
            throws IOException {

        getTestData()
                .copyToFeatureTypeDirectory(
                        new ByteArrayInputStream(body.getBytes()), featureTypeName, template);
    }
}
