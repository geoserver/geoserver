/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * Mock data used to test exception thrown when encoding of secondary (transitively imported) namespace without the
 * secondary namespace declared
 *
 * @author Victor Tey, CSIRO Earth Science and Resource Engineering
 */
public class SecondaryNamespaceExceptionMockData extends AbstractAppSchemaMockData {

    /** Prefix for ex namespace. */
    protected static final String EX_PREFIX = "ex";

    /** URI for ex namespace. */
    protected static final String EX_URI = "http://example.com";

    /** Prefix for sa namespace. */
    protected static final String SA_PREFIX = "sa";

    /** URI for sa namespace. */
    protected static final String SA_URI = "http://www.opengis.net/sampling/1.0";

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        putNamespace(EX_PREFIX, EX_URI);
        removeNamespace(SA_PREFIX);
        addFeatureType(
                EX_PREFIX,
                "ShapeContent",
                "SecondaryNamespace.xml",
                "SecondaryNamespacePropertyfile.properties",
                "secondaryNamespace.xsd");
    }
}
