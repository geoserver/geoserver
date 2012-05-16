/*
 * Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import junit.framework.Test;

import org.w3c.dom.Document;

/**
 * Test feature chaining with simple content type, e.g. for gml:name. 
 * 
 * @author Rini Angreani, CSIRO Earth Science and Resource Engineering
 */
public class SimpleAttributeFeatureChainWfsTest extends AbstractAppSchemaWfsTestSupport {

    /**
     * Read-only test so can use one-time setup.
     */
    public static Test suite() {
        return new OneTimeTestSetup(new SimpleAttributeFeatureChainWfsTest());
    }

    @Override
    protected NamespaceTestData buildTestData() {
        return new SimpleAttributeFeatureChainMockData();
    }

    /**
     * Test that feature chaining for gml:name works.
     */
    public void testGetFeature() {
        String path = "wfs?request=GetFeature&version=1.1.0&typeName=gsml:MappedFeature";
        Document doc = getAsDOM(path);
        LOGGER.info("MappedFeature with name feature chained Response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("4", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(4, "//gsml:MappedFeature", doc);

        // mf1
        assertXpathCount(4, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name", doc);
        // gml:name with values coming from the main table
        assertXpathEvaluatesTo("GUNTHORPE FORMATION",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[1]", doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("nameone 1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[2]", doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("some uri 1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[2]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("nametwo 1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[3]", doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo("some:uri:mf1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[3]/@codeSpace",
                doc);
        // gml:name as xlink:href from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("some:uri:1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[4]/@xlink:href",
                doc);

        // mf2: extra values from denormalised tables
        checkMf2(doc);

        // mf3
        checkMf3(doc);

        // mf4
        assertXpathCount(4, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name", doc);
        // gml:name with values coming from the main table
        assertXpathEvaluatesTo("MURRADUC BASALT",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[1]", doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("nameone 5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[2]", doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("some uri 5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[2]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("nametwo 5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[3]", doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo("some:uri:mf4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[3]/@codeSpace",
                doc);
        // gml:name as xlink:href from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("some:uri:5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[4]/@xlink:href",
                doc);
    }

    /**
     * Test that filtering feature chained values works.
     */
    public void testAttributeFilter() {
        // filter by name
        String xml = //
        "<wfs:GetFeature " //
                + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                + ">" //
                + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                + "        <ogc:Filter>" //
                + "            <ogc:PropertyIsEqualTo>" //
                + "                <ogc:PropertyName>gml:name</ogc:PropertyName>" //
                + "                <ogc:Literal>nametwo 4</ogc:Literal>" //
                + "            </ogc:PropertyIsEqualTo>" //
                + "        </ogc:Filter>" //
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf3(doc);

        xml = //
        "<wfs:GetFeature " //
                + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                + ">" //
                + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                + "        <ogc:Filter>" //
                + "            <ogc:PropertyIsEqualTo>" //
                + "                <ogc:PropertyName>gml:name</ogc:PropertyName>" //
                + "                <ogc:Literal>nametwo 3</ogc:Literal>" //
                + "            </ogc:PropertyIsEqualTo>" //
                + "        </ogc:Filter>" //
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));

        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf2(doc);

        xml = //
        "<wfs:GetFeature " //
                + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                + ">" //
                + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                + "        <ogc:Filter>" //
                + "            <ogc:PropertyIsEqualTo>" //
                + "                <ogc:PropertyName>gml:name</ogc:PropertyName>" //
                + "                <ogc:Literal>nametwo 2</ogc:Literal>" //
                + "            </ogc:PropertyIsEqualTo>" //
                + "        </ogc:Filter>" //
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));

        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf2(doc);
    }

    /**
     * Test filtering client properties.
     */
    public void testClientPropertiesFilter() {
        // filter by codespace coming from parent table
        String xml = //
        "<wfs:GetFeature " //
                + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES
                + ">" //
                + "    <wfs:Query typeName=\"gsml:MappedFeature\">" 
                + "        <ogc:Filter>" 
                + "            <ogc:PropertyIsEqualTo>" 
                + "                <ogc:PropertyName>gml:name/@codeSpace</ogc:PropertyName>"
                + "                <ogc:Literal>some:uri:mf3</ogc:Literal>"
                + "            </ogc:PropertyIsEqualTo>"
                + "        </ogc:Filter>"
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf3(doc);

        // filter by codespace coming from chained feature
        xml = //
        "<wfs:GetFeature " //
                + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                + ">"
                + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
                + "        <ogc:Filter>"
                + "            <ogc:PropertyIsEqualTo>"
                + "                <ogc:PropertyName>gml:name/@codeSpace</ogc:PropertyName>"
                + "                <ogc:Literal>some uri 4</ogc:Literal>"
                + "            </ogc:PropertyIsEqualTo>"
                + "        </ogc:Filter>"
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf3(doc);

        // filter by xlink:href coming from chained feature
        xml = //
        "<wfs:GetFeature " //
                + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                + ">"
                + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
                + "        <ogc:Filter>"
                + "            <ogc:PropertyIsEqualTo>"
                + "                <ogc:PropertyName>gml:name/@xlink:href</ogc:PropertyName>"
                + "                <ogc:Literal>some:uri:4</ogc:Literal>"
                + "            </ogc:PropertyIsEqualTo>"
                + "        </ogc:Filter>"
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf3(doc);
    }

    private void checkMf2(Document doc) {
        // mf2: extra values from denormalised tables
        assertXpathCount(7, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name", doc);
        // gml:name with values coming from the main table
        assertXpathEvaluatesTo("MERCIA MUDSTONE GROUP",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[1]", doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("nameone 2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[2]", doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("some uri 2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[2]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("nameone 3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[3]", doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("some uri 3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[3]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("nametwo 2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[4]", doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo("some:uri:mf2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[4]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("nametwo 3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[5]", doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo("some:uri:mf2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[5]/@codeSpace",
                doc);
        // gml:name as xlink:href from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("some:uri:2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[6]/@xlink:href",
                doc);
        assertXpathEvaluatesTo("some:uri:3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[7]/@xlink:href",
                doc);

    }

    private void checkMf3(Document doc) {
        assertXpathCount(4, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name", doc);
        // gml:name with values coming from the main table
        assertXpathEvaluatesTo("CLIFTON FORMATION",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[1]", doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("nameone 4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[2]", doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("some uri 4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[2]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("nametwo 4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[3]", doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo("some:uri:mf3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[3]/@codeSpace",
                doc);
        // gml:name as xlink:href from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("some:uri:4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[4]/@xlink:href",
                doc);
    }

}
