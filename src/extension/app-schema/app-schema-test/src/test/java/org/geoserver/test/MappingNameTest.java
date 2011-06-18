/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.w3c.dom.Document;

import junit.framework.Test;

/**
 * Tests whether the use of special characters in a mapping name can be used for feature chaining
 * (using quotes in LinkElement).
 * 
 * @author Niels Charlier, Curtin University of Technology
 * 
 */

public class MappingNameTest extends AbstractAppSchemaWfsTestSupport {

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        return new OneTimeTestSetup(new MappingNameTest());
    }

    @Override
    protected NamespaceTestData buildTestData() {
        return new MappingNameMockData();
    }

    /**
     * Test whether GetFeature returns a FeatureCollection with the right content based on the
     * feature chaining.
     */
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=gsml:MappedFeature");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//wfs:FeatureCollection", doc);

        assertXpathEvaluatesTo("4", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(4, "//gsml:MappedFeature", doc);

        assertXpathEvaluatesTo(
                "value01",
                "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value[@codeSpace='codespace01']",
                doc);

        assertXpathEvaluatesTo(
                "value02",
                "/wfs:FeatureCollection/gml:featureMember//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value[@codeSpace='codespace02']",
                doc);

        assertXpathEvaluatesTo(
                "value02",
                "/wfs:FeatureCollection/gml:featureMember//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value[@codeSpace='codespace02']",
                doc);

        assertXpathEvaluatesTo(
                "value03",
                "/wfs:FeatureCollection/gml:featureMember//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value[@codeSpace='codespace03']",
                doc);

    }

}
