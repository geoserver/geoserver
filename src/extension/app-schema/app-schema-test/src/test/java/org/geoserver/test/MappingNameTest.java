/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests whether the use of special characters in a mapping name can be used for feature chaining
 * (using quotes in LinkElement).
 *
 * @author Niels Charlier, Curtin University of Technology
 */
public class MappingNameTest extends AbstractAppSchemaTestSupport {

    @Override
    protected MappingNameMockData createTestData() {
        return new MappingNameMockData();
    }

    /**
     * Test whether GetFeature returns a FeatureCollection with the right content based on the
     * feature chaining.
     */
    @Test
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typeName=gsml:MappedFeature");
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
