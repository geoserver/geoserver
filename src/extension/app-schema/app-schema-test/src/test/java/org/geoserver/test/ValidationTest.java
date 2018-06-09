/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Validation testing with GeoServer
 *
 * @author Victor Tey, CSIRO Exploration and Mining
 */
public class ValidationTest extends AbstractAppSchemaTestSupport {

    @Override
    protected ValidationTestMockData createTestData() {
        return new ValidationTestMockData();
    }

    /** Test that when minOccur=0 the validation should let it pass */
    @Test
    public void testAttributeMinOccur0() {
        Document doc = null;
        doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit");
        LOGGER.info("WFS GetFeature&typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.1']/gml:name", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.2']/gml:name", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.3']/gml:name", doc);

        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.1']/gsml:bodyMorphology/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "myBody1",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.1']/gsml:bodyMorphology/gsml:CGI_TermValue/gsml:value[@codeSpace='myBodyCodespace1']",
                doc);
        assertXpathEvaluatesTo(
                "compositionName",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.1']/gsml:composition/gsml:CompositionPart/gsml:lithology[1]/gsml:ControlledConcept/gml:name",
                doc);
        assertXpathEvaluatesTo(
                "myBody1",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.1']/gsml:composition/gsml:CompositionPart/gsml:lithology[2]/gsml:ControlledConcept/gml:name",
                doc);
        assertXpathEvaluatesTo(
                "myBody1",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.1']/gsml:rank[@codeSpace='myBodyCodespace1']",
                doc);

        assertXpathCount(
                0,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.2']/gsml:bodyMorphology/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "compositionName",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.2']/gsml:composition/gsml:CompositionPart/gsml:lithology[1]/gsml:ControlledConcept/gml:name",
                doc);
        assertXpathCount(
                0,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.2']/gsml:composition/gsml:CompositionPart/gsml:lithology[2]/gsml:ControlledConcept/gml:name",
                doc);
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.2']/gsml:rank[@codeSpace='myBodyCodespace2']",
                doc);

        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.3']/gsml:bodyMorphology/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "myBody3",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.3']/gsml:bodyMorphology/gsml:CGI_TermValue/gsml:value[@codeSpace='myBodyCodespace3']",
                doc);
        assertXpathEvaluatesTo(
                "compositionName",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.3']/gsml:composition/gsml:CompositionPart/gsml:lithology[1]/gsml:ControlledConcept/gml:name",
                doc);
        assertXpathEvaluatesTo(
                "myBody3",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.3']/gsml:composition/gsml:CompositionPart/gsml:lithology[2]/gsml:ControlledConcept/gml:name",
                doc);

        assertXpathEvaluatesTo(
                "myBody3",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.gu.3']/gsml:rank[@codeSpace='myBodyCodespace3']",
                doc);
    }

    @Test
    public void testSimpleContentInteger() {
        Document doc = null;
        doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=er:Commodity");
        LOGGER.info("WFS GetFeature&typename=er:Commodity response:\n" + prettyString(doc));
        assertXpathCount(1, "//er:Commodity[@gml:id='er.commodity.gu.1']/gml:name", doc);
        assertXpathCount(1, "//er:Commodity[@gml:id='er.commodity.gu.1']/er:commodityRank", doc);
        assertXpathEvaluatesTo(
                "myName1", "//er:Commodity[@gml:id='er.commodity.gu.1']/gml:name", doc);
        assertXpathEvaluatesTo(
                "1", "//er:Commodity[@gml:id='er.commodity.gu.1']/er:commodityRank", doc);

        assertXpathCount(1, "//er:Commodity[@gml:id='er.commodity.gu.2']/gml:name", doc);
        assertXpathCount(0, "//er:Commodity[@gml:id='er.commodity.gu.2']/er:commodityRank", doc);
        assertXpathEvaluatesTo(
                "myName2", "//er:Commodity[@gml:id='er.commodity.gu.2']/gml:name", doc);

        assertXpathCount(1, "//er:Commodity[@gml:id='er.commodity.gu.3']/gml:name", doc);
        assertXpathCount(1, "//er:Commodity[@gml:id='er.commodity.gu.3']/er:commodityRank", doc);
        assertXpathEvaluatesTo(
                "myName3", "//er:Commodity[@gml:id='er.commodity.gu.3']/gml:name", doc);
        assertXpathEvaluatesTo(
                "3", "//er:Commodity[@gml:id='er.commodity.gu.3']/er:commodityRank", doc);
    }

    /** Test minOccur=1 and the attribute should always be encoded even when empty. */
    @Test
    public void testAttributeMinOccur1() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info(
                "WFS GetFeature&typename=gsml:gsml:MappedFeature response:\n" + prettyString(doc));
        assertXpathCount(3, "//gsml:MappedFeature", doc);

        // with minOccur = 1 and null value, an empty tag would be encoded
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.1']", doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.1']/gsml:observationMethod",
                doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.1']/gsml:observationMethod/gsml:CGI_TermValue",
                doc);

        // the rest should be encoded as normal
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.2']", doc);
        assertXpathEvaluatesTo(
                "observation2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.2']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value",
                doc);

        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.3']", doc);
        assertXpathEvaluatesTo(
                "observation3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.gu.3']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value",
                doc);
    }
}
