/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.w3c.dom.Document;

import junit.framework.Test;

/**
 * Tests whether Property Selection is properly applied on complex features
 * 
 * @author Niels Charlier, Curtin University of Technology
 * 
 */
public class PropertySelectionTest extends AbstractAppSchemaWfsTestSupport {

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        return new OneTimeTestSetup(new PropertySelectionTest());
    }

    @Override
    protected NamespaceTestData buildTestData() {
        return new PropertySelectionMockData();
    }
    
    /**
     * Test GetFeature with Property Selection.
     */
    public void testGetFeature() {
    	Document doc = getAsDOM("wfs?request=GetFeature&typeName=gsml:MappedFeature&propertyname=gml:description");
        LOGGER.info("wfs?request=GetFeature&typeName=gsml:MappedFeature&propertyname=gml:description Response:\n" + prettyString(doc));

        // using custom IDs - this is being tested too

        // check if requested property is present
        assertXpathCount(1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:description", doc);

        // check if required property is present
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape",
                doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:positionalAccuracy/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit",
                doc);

        // check if non-requested property is not present
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:metadata",
                doc);
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name", doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit/gml:name",
                doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit/gml:description",
                doc);

    }

    /**
     * Test Property Selection with Feature Chaining.
     */
    public void testGetFeatureFeatureChaining() {
        Document doc = getAsDOM("wfs?request=GetFeature&typename=gsml:MappedFeature&propertyname=gsml:specification/gsml:GeologicUnit/gml:description");
        LOGGER.info("wfs?request=GetFeature&typename=gsml:MappedFeature&propertyname=gsml:specification/gsml:GeologicUnit/gml:description response:\n"
                + prettyString(doc));

        // check if requested property is present
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit/gml:description",
                doc);

        // check if required property is present
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape",
                doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:positionalAccuracy/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit",
                doc);

        // check if non-requested property is not present
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:metadata",
                doc);
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name", doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit/gml:name",
                doc);
        assertXpathCount(0,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:description", doc);

    }

    /**
     * Test GetFeature with Property Selection, using client properties.
     */
    public void testGetFeatureClientProperty() {
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=gsml:MappedFeature&propertyname=gsml:metadata");
        LOGGER.info("wfs?request=GetFeature&typeName=gsml:MappedFeature&propertyname=gsml:metadata response:\n" + prettyString(doc));

        // test client property works
        assertXpathEvaluatesTo("zzzgu.25699",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:metadata/@xlink:href",
                doc);
    }

    /**
     * Test GetFeature with Property Selection, with an invalid column name.
     */
    public void testGetFeatureInvalidName() {
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=gsml:MappedFeature&propertyname=gml:name");
        LOGGER.info("wfs?request=GetFeature&typeName=gsml:MappedFeature&propertyname=gml:name response:\n" + prettyString(doc));

        // test exception refering to missing column
        assertTrue(evaluate("//ows:ExceptionText", doc)
                .endsWith("No value for xpath: DOESNT_EXIST"));

    }

    /**
     * Test Posting GetFeature
     */
    public void testPostGetFeature() {
        String xml = "<wfs:GetFeature " //
                + "service=\"WFS\" " //
                + "version=\"1.1.0\" " //
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:gsml=\""
                + AbstractAppSchemaMockData.GSML_URI
                + "\" " //
                + ">" //
                + "<wfs:Query typeName=\"gsml:MappedFeature\">"
                + "<ogc:PropertyName>gml:description</ogc:PropertyName> "
                + "<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gml:name</ogc:PropertyName> "
                + "</wfs:Query>" + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);

        LOGGER.info("WFS GetFeature POST response:\n" + prettyString(doc));

        // check if requested property is present
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit/gml:name",
                doc);
        assertXpathCount(1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:description", doc);

        // check if required property is present
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape",
                doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:positionalAccuracy/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit",
                doc);

        // check if non-requested property is not present
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:metadata",
                doc);
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name", doc);

    }

    /**
     * Test GetFeature with Property Selection, with properties names with same name but different
     * namespace.
     */
    public void testSameNameDiffNamespace1() {
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=ex:MyTestFeature&propertyname=ex:name");
        LOGGER.info("wfs?request=GetFeature&typeName=ex:MyTestFeature&propertyname=ex:name response:\n" + prettyString(doc));

        assertXpathCount(1, "//ex:MyTestFeature[@gml:id='f1']/ex:name", doc);
        assertXpathCount(0, "//ex:MyTestFeature[@gml:id='f1']/gml:name", doc);
    }

    /**
     * Test GetFeature with Property Selection, with properties names with same name but different
     * namespace.
     */
    public void testSameNameDiffNamespace2() {
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=ex:MyTestFeature&propertyname=gml:name");
        LOGGER.info("wfs?request=GetFeature&typeName=ex:MyTestFeature&propertyname=gml:name response:\n" + prettyString(doc));

        assertXpathCount(1, "//ex:MyTestFeature[@gml:id='f1']/gml:name", doc);
        assertXpathCount(0, "//ex:MyTestFeature[@gml:id='f1']/ex:name", doc);
    }

    /**
     * Test GetFeature with Property Selection, with an invalid column name.
     */
    public void testSameNameDiffNamespace3() {
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=ex:MyTestFeature");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));

    }

}
