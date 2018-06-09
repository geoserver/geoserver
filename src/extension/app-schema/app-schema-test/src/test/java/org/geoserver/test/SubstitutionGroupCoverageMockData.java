/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * Mock data for testing substitution groups {@link SubstitutionGroupCoverageWfsTest}
 *
 * @author Aaron Braeckel (National Center for Atmospheric Research)
 */
public class SubstitutionGroupCoverageMockData extends AbstractAppSchemaMockData {

    /** Prefix for namespace. */
    protected static final String NAMESPACE_PREFIX = "test";

    /** URI for namespace. */
    protected static final String URI = "http://www.geotools.org/test";

    public SubstitutionGroupCoverageMockData() {
        super(GML32_NAMESPACES);
    }

    /** @see AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        putNamespace(NAMESPACE_PREFIX, URI);
        addFeatureType(
                NAMESPACE_PREFIX,
                "DiscreteCoverage",
                "subgrp.xml",
                "subgrpcoverage.properties",
                "subgrp.xsd");
    }
}
