/*
 * Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import junit.framework.Test;

import org.w3c.dom.Document;

/**
 * Test encoding of secondary (transitively imported) namespace.
 * 
 * @author Jacqui Githaiga, Curtin University of Technology
 */
public class SecondaryNamespaceTest extends AbstractAppSchemaWfsTestSupport {

    /**
     * Read-only test so can use one-time setup.
     */
    public static Test suite() {
        return new OneTimeTestSetup(new SecondaryNamespaceTest());
    }

    @Override
    protected NamespaceTestData buildTestData() {
        return new SecondaryNamespaceMockData();
    }

    /**
     * Test encoding of sa namespace.
     */
    public void testNamespaces() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typeName=ex:ShapeContent");
        LOGGER.info("Secondary Namespace Response:\n" + prettyString(doc));
        assertXpathCount(2, "//sa:shape", doc);
        assertXpathEvaluatesTo("420502.00109583 399739.99000583 420502.00109583 399739.99000583",
                "//ex:ShapeContent[@gml:id='sa.1']/sa:shape/gml:LineString/gml:posList", doc);
    }

}
