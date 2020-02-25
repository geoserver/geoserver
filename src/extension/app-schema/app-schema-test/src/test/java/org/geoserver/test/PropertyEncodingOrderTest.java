/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.junit.Assert.*;

import org.geoserver.wfs.WFSInfo;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/** @author Xiangtan Lin, CSIRO Information Management and Technology */
public class PropertyEncodingOrderTest extends AbstractAppSchemaTestSupport {

    @Override
    protected PropertyEncodingOrderMockData createTestData() {
        return new PropertyEncodingOrderMockData();
    }

    /**
     * Test the gmsl:Borehole is encoded in the right order, in particular this test is created for
     * an encoding order issue with gsml:indexData according to the schema
     * http://www.geosciml.org/geosciml/2.0/xsd/borehole.xsd
     */
    @Test
    public void testPropertyEncodingOrder_Borehole() throws Exception {
        String path = "wfs?request=GetFeature&version=1.1.0&typename=gsml:Borehole";
        Document doc = getAsDOM(path);
        LOGGER.info("WFS GetFeature&gsml:Borehole:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);

        assertXpathCount(1, "//gsml:Borehole[@gml:id='BOREHOLE.WTB5']", doc);
        Node borehole = doc.getElementsByTagName("gsml:Borehole").item(0);
        assertEquals("gsml:Borehole", borehole.getNodeName());

        // check for gml:id
        assertEquals(
                "BOREHOLE.WTB5", borehole.getAttributes().getNamedItem("gml:id").getNodeValue());

        // gml:name
        Node name = borehole.getFirstChild();
        assertEquals("gml:name", name.getNodeName());
        assertEquals("WTB5 TEST", name.getFirstChild().getNodeValue());

        // sa:sampledFeature
        Node sampledFeature = name.getNextSibling();
        assertEquals("sa:sampledFeature", sampledFeature.getNodeName());

        // sa:shape
        Node shape = sampledFeature.getNextSibling();
        assertEquals("sa:shape", shape.getNodeName());

        Node posList = shape.getFirstChild().getFirstChild();
        assertEquals("gml:posList", posList.getNodeName());
        assertEquals("-28.4139 121.142 -28.4139 121.142", posList.getFirstChild().getNodeValue());

        // gsml:collarLocation
        Node collarLocation = shape.getNextSibling();
        assertEquals("gsml:collarLocation", collarLocation.getNodeName());

        Node boreholeCollar = collarLocation.getFirstChild();
        assertEquals("gsml:BoreholeCollar", boreholeCollar.getNodeName());
        assertEquals(
                "BOREHOLE.COLLAR.WTB5",
                boreholeCollar.getAttributes().getNamedItem("gml:id").getNodeValue());
        assertEquals(
                "-28.4139 121.142",
                boreholeCollar
                        .getFirstChild()
                        .getFirstChild()
                        .getFirstChild()
                        .getFirstChild()
                        .getNodeValue());
        assertEquals(
                "1.0",
                boreholeCollar.getFirstChild().getNextSibling().getFirstChild().getNodeValue());

        // gsml:indexData
        Node indexData = collarLocation.getNextSibling();
        assertEquals("gsml:indexData", indexData.getNodeName());

        Node boreholeDetails = indexData.getFirstChild();
        assertEquals("gsml:BoreholeDetails", boreholeDetails.getNodeName());

        Node operator = boreholeDetails.getFirstChild();
        assertEquals("GSWA", operator.getAttributes().getNamedItem("xlink:title").getNodeValue());

        Node dateOfDrilling = operator.getNextSibling();
        assertEquals("2004-09-17", dateOfDrilling.getFirstChild().getNodeValue());

        Node drillingMethod = dateOfDrilling.getNextSibling();
        assertEquals("diamond core", drillingMethod.getFirstChild().getNodeValue());

        Node startPoint = drillingMethod.getNextSibling();
        assertEquals("natural ground surface", startPoint.getFirstChild().getNodeValue());

        Node inclinationType = startPoint.getNextSibling();
        assertEquals("vertical", inclinationType.getFirstChild().getNodeValue());

        Node coreInterval = inclinationType.getNextSibling();
        assertEquals(
                "106.0",
                coreInterval.getFirstChild().getFirstChild().getFirstChild().getNodeValue());
        assertEquals(
                "249.0",
                coreInterval
                        .getFirstChild()
                        .getFirstChild()
                        .getNextSibling()
                        .getFirstChild()
                        .getNodeValue());

        Node coreCustodian = coreInterval.getNextSibling();
        assertEquals(
                "CSIRONR",
                coreCustodian.getAttributes().getNamedItem("xlink:title").getNodeValue());

        validateGet(path);
    }

    /**
     * Test the gmsl:PlanarOrientation is encoded in the order of aziumth, convention, dip, polarity
     * according to the schema CGI_Value.xsd
     */
    @Test
    public void testPropertyEncodingOrder_PlanarOrientation() throws Exception {
        String path = "wfs?request=GetFeature&version=1.1.0&typename=er:MineralOccurrence";
        Document doc = getAsDOM(path);
        LOGGER.info("WFS GetFeature&er:MineralOccurrence:\n" + prettyString(doc));
        assertXpathCount(1, "//er:MineralOccurrence[@gml:id='er.mineraloccurrence.S0032895']", doc);

        Node feature = doc.getElementsByTagName("er:MineralOccurrence").item(0);
        assertEquals("er:MineralOccurrence", feature.getNodeName());

        // check for gml:id
        assertXpathEvaluatesTo(
                "er.mineraloccurrence.S0032895", "//er:MineralOccurrence/@gml:id", doc);

        Node name = feature.getFirstChild();
        assertEquals("gml:name", name.getNodeName());
        assertXpathEvaluatesTo(
                "Robinson Range - Deposit D",
                "//er:MineralOccurrence[@gml:id='er.mineraloccurrence.S0032895']/gml:name",
                doc);

        // er:planarOrientation
        Node planarOrientation = name.getNextSibling();
        assertEquals("er:planarOrientation", planarOrientation.getNodeName());

        // gsml:CGI_PlanarOrientation
        Node gsml_planarOrientation = planarOrientation.getFirstChild();
        assertEquals("gsml:CGI_PlanarOrientation", gsml_planarOrientation.getNodeName());

        // convention
        Node convention = gsml_planarOrientation.getFirstChild();
        assertEquals("gsml:convention", convention.getNodeName());
        assertXpathEvaluatesTo(
                "strike dip right hand rule",
                "//er:MineralOccurrence[@gml:id='er.mineraloccurrence.S0032895']/er:planarOrientation/gsml:CGI_PlanarOrientation/gsml:convention",
                doc);

        // azimuth
        Node azimuth = convention.getNextSibling();
        assertEquals("gsml:azimuth", azimuth.getNodeName());
        assertXpathEvaluatesTo(
                "50.0",
                "//er:MineralOccurrence[@gml:id='er.mineraloccurrence.S0032895']/er:planarOrientation/gsml:CGI_PlanarOrientation/gsml:azimuth/gsml:CGI_NumericValue/gsml:principalValue",
                doc);

        // dip
        Node dip = azimuth.getNextSibling();
        assertEquals("gsml:dip", dip.getNodeName());
        assertXpathEvaluatesTo(
                "60-80",
                "//er:MineralOccurrence[@gml:id='er.mineraloccurrence.S0032895']/er:planarOrientation/gsml:CGI_PlanarOrientation/gsml:dip/gsml:CGI_TermValue/gsml:value",
                doc);

        // polarity
        Node polarity = dip.getNextSibling();
        assertEquals("gsml:polarity", polarity.getNodeName());
        assertXpathEvaluatesTo(
                "not applicable",
                "//er:MineralOccurrence[@gml:id='er.mineraloccurrence.S0032895']/er:planarOrientation/gsml:CGI_PlanarOrientation/gsml:polarity",
                doc);

        // FIXME: this feature type is not yet complete
        // validateGet(path);
    }

    /**
     * Test elements are encoded in the order as defined in the schema GeologicUnit is tested here
     */
    @Test
    public void testPropertyEncodingOrder_GeologicUnit() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.setEncodeFeatureMember(true);
        getGeoServer().save(wfs);
        String path =
                "wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit&featureid=gu.25699";
        Document doc = getAsDOM(path);
        LOGGER.info(
                "WFS GetFeature&typename=gsml:GeologicUnit&featureid=gu.25699:\n"
                        + prettyString(doc));

        assertEquals(1, doc.getElementsByTagName("gml:featureMember").getLength());
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25699']", doc);

        // GeologicUnit
        Node feature = doc.getElementsByTagName("gsml:GeologicUnit").item(0);
        assertEquals("gsml:GeologicUnit", feature.getNodeName());

        // description
        Node description = feature.getFirstChild();
        assertEquals("gml:description", description.getNodeName());
        assertXpathEvaluatesTo(
                "Olivine basalt, tuff, microgabbro, minor sedimentary rocks",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gml:description",
                doc);

        // name1
        Node name1 = description.getNextSibling();
        assertEquals("gml:name", name1.getNodeName());
        assertXpathEvaluatesTo(
                "Yaugher Volcanic Group",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gml:name[1]",
                doc);

        // name2
        Node name2 = name1.getNextSibling();
        assertEquals("gml:name", name2.getNodeName());
        assertXpathEvaluatesTo("-Py", "//gsml:GeologicUnit[@gml:id='gu.25699']/gml:name[2]", doc);

        // observationMethod
        Node observationMethod = name2.getNextSibling();
        assertEquals("gsml:observationMethod", observationMethod.getNodeName());
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "http://urn.opengis.net",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        // purpose
        Node purpose = observationMethod.getNextSibling();
        assertEquals("gsml:purpose", purpose.getNodeName());
        assertXpathEvaluatesTo(
                "instance", "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:purpose", doc);

        // occurrence
        Node occurrence = purpose.getNextSibling();
        assertEquals("gsml:occurrence", occurrence.getNodeName());
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:occurrence[@xlink:href='urn:cgi:feature:MappedFeature:mf1']",
                doc);

        // geologicUnitType
        Node geologicUnitType = occurrence.getNextSibling();
        assertEquals("gsml:geologicUnitType", geologicUnitType.getNodeName());
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::unknown",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:geologicUnitType/@xlink:href",
                doc);

        // exposureColor
        Node exposureColor = geologicUnitType.getNextSibling();
        assertEquals("gsml:exposureColor", exposureColor.getNodeName());
        assertXpathEvaluatesTo(
                "Blue",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:exposureColor/gsml:CGI_TermValue/gsml:value",
                doc);

        // outcropCharacter
        Node outcropCharacter = exposureColor.getNextSibling();
        assertEquals("gsml:outcropCharacter", outcropCharacter.getNodeName());
        assertXpathEvaluatesTo(
                "x",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:outcropCharacter/gsml:CGI_TermValue/gsml:value",
                doc);

        // composition
        Node composition = outcropCharacter.getNextSibling();
        assertEquals("gsml:composition", composition.getNodeName());

        Node compositionPart = doc.getElementsByTagName("gsml:CompositionPart").item(0);
        assertEquals("gsml:CompositionPart", compositionPart.getNodeName());

        // role
        Node role = compositionPart.getFirstChild();
        assertEquals("gsml:role", role.getNodeName());
        assertXpathEvaluatesTo(
                "fictitious component",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:composition/gsml:CompositionPart/gsml:role",
                doc);

        // lithology
        Node lithology = role.getNextSibling();
        assertEquals("gsml:lithology", lithology.getNodeName());
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:composition/gsml:CompositionPart/gsml:lithology"
                        + "/gsml:ControlledConcept/gsml:vocabulary/@xlink:href",
                doc);

        // proportion
        Node proportion = lithology.getNextSibling();
        assertEquals("gsml:proportion", proportion.getNodeName());
        assertXpathEvaluatesTo(
                "nonexistent",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:composition/gsml:CompositionPart/gsml:proportion"
                        + "/gsml:CGI_TermValue/gsml:value",
                doc);

        validateGet(path);
    }
}
