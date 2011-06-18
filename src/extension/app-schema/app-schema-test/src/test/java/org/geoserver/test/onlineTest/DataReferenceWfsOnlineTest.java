/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.onlineTest.support.AbstractDataReferenceWfsTest;
import org.geoserver.wfs.WFSInfo;
import org.w3c.dom.Document;

public abstract class DataReferenceWfsOnlineTest extends AbstractDataReferenceWfsTest {

    public DataReferenceWfsOnlineTest() throws Exception {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * URI for om namespace.
     */
    protected static final String OM_URI = "http://www.opengis.net/om/1.0";

    /**
     * Schema URL for observation and measurements
     */
    protected static final String OM_SCHEMA_LOCATION_URL = "http://schemas.opengis.net/om/1.0.0/observation.xsd";

    /**
     * Test whether GetCapabilities returns wfs:WFS_Capabilities.
     */
    public void testGetCapabilities() {
        Document doc = getAsDOM("wfs?request=GetCapabilities");
        LOGGER.info("WFS =GetCapabilities response:\n" + prettyString(doc));

        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement().getNodeName());
        assertXpathCount(7, "//wfs:FeatureType", doc);

        // make sure non-feature types don't appear in FeatureTypeList

        ArrayList<String> featureTypeNames = new ArrayList<String>(7);
        featureTypeNames.add(evaluate("//wfs:FeatureType[1]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[2]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[3]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[4]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[5]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[6]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[7]/wfs:Name", doc));

        // gsml:CompositionPart
        assertTrue(featureTypeNames.contains("gsml:CompositionPart"));
        // gsml:Contact
        assertTrue(featureTypeNames.contains("gsml:Contact"));
        // gsml:DisplacementEvent
        assertTrue(featureTypeNames.contains("gsml:DisplacementEvent"));
        // gsml:GeologicEvent
        assertTrue(featureTypeNames.contains("gsml:GeologicEvent"));
        // gsml:GeologicUnit
        assertTrue(featureTypeNames.contains("gsml:GeologicUnit"));
        // gsml:MappedFeature
        assertTrue(featureTypeNames.contains("gsml:MappedFeature"));
        // gsml:ShearDisplacementStructure
        assertTrue(featureTypeNames.contains("gsml:ShearDisplacementStructure"));
    }

    public void testDescribeFeatureType() throws IOException {

        /**
         * gsml:MappedFeature
         */
        Document doc = getAsDOM("wfs?request=DescribeFeatureType&typename=gsml:MappedFeature");
        LOGGER.info("WFS DescribeFeatureType, typename=gsml:MappedFeature response:\n"
                + prettyString(doc));
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        // check target name space is encoded and is correct
        assertXpathEvaluatesTo(AbstractAppSchemaMockData.GSML_URI, "//@targetNamespace", doc);
        // make sure the content is only relevant include
        assertXpathCount(1, "//xsd:include", doc);
        // no import to GML since it's already imported inside the included schema
        // otherwise it's invalid to import twice
        assertXpathCount(0, "//xsd:import", doc);
        // GSML schemaLocation
        assertXpathEvaluatesTo(AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL,
                "//xsd:include/@schemaLocation", doc);
        // nothing else
        assertXpathCount(0, "//xsd:complexType", doc);
        assertXpathCount(0, "//xsd:element", doc);

        /**
         * gsml:GeologicUnit
         */
        doc = getAsDOM("wfs?request=DescribeFeatureType&typename=gsml:GeologicUnit");
        LOGGER.info("WFS DescribeFeatureType, typename=gsml:GeologicUnit response:\n"
                + prettyString(doc));
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo(AbstractAppSchemaMockData.GSML_URI, "//@targetNamespace", doc);
        assertXpathCount(1, "//xsd:include", doc);
        assertXpathCount(0, "//xsd:import", doc);
        // GSML schemaLocation
        assertXpathEvaluatesTo(AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL,
                "//xsd:include/@schemaLocation", doc);
        // nothing else
        assertXpathCount(0, "//xsd:complexType", doc);
        assertXpathCount(0, "//xsd:element", doc);

        doc = getAsDOM("wfs?request=DescribeFeatureType&typename=gsml:Contact");
        LOGGER.info("WFS DescribeFeatureType, typename=gsml:Contact response:\n"
                + prettyString(doc));
        // the namespace om http://www.opengis.net/om/1.0 was added
        // to gsml:Contact on purpose to test the import

        testConsistantSchema(doc);
        doc = null;
        System.gc();
        doc = getAsDOM("wfs?request=DescribeFeatureType&typename=gsml:Contact,gsml:MappedFeature");
        LOGGER.info("WFS DescribeFeatureType, typename=gsml:Contact,gsml:MappedFeature response:\n"
                + prettyString(doc));
        testConsistantSchema(doc);
        // TODO without any type specified, gsml:Content om namespace not imported.
        doc = getAsDOM("wfs?request=DescribeFeatureType");
        LOGGER.info("WFS DescribeFeatureType, typename=none response:\n" + prettyString(doc));
        // testConsistantSchema(doc); //why didn't inc import?
    }

    private void testConsistantSchema(Document doc) {
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo(AbstractAppSchemaMockData.GSML_URI, "//@targetNamespace", doc);
        assertXpathCount(1, "//xsd:include", doc);
        assertXpathCount(1, "//xsd:import", doc);

        assertXpathEvaluatesTo(DataReferenceWfsOnlineTest.OM_URI, "//xsd:import/@namespace", doc);
        assertXpathEvaluatesTo(DataReferenceWfsOnlineTest.OM_SCHEMA_LOCATION_URL,
                "//xsd:import/@schemaLocation", doc);
        assertXpathEvaluatesTo(AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL,
                "//xsd:include/@schemaLocation", doc);
        // nothing else
        assertXpathCount(0, "//xsd:complexType", doc);
        assertXpathCount(0, "//xsd:element", doc);

    }

    /**
     * This will test all mapping associated tests without filters GetFeature, Feature chaining
     * mapping,use of MappingName, GetFeatureValid, including validation
     */
    public void testMapping() {
        String path = "wfs?request=GetFeature&typename=gsml:GeologicUnit&featureid=gsml.geologicunit.16777549126932776,gsml.geologicunit.167775491110573732,gsml.geologicunit.16777549126930540";
        Document doc = getAsDOM(path);
        LOGGER.info("WFS GetFeature&typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        this.checkSchemaLocation(doc);
        // test if_then_else along with Inline mapping,includedTypes, MappingName, eg,
        // if_then_else(isNull(density_value02),'Density_Value02_is_Null','Density_Value02_Not_Null'
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491110573732']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericRange/gsml:lower/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericRange/gsml:upper/gsml:CGI_NumericValue/gsml:principalValue",
                doc);

        // test feature chaining and xlink working
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.191322']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:CGI:EarthNaturalSurface",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.191322']/gsml:samplingFrame/@xlink:href",
                doc);

        // test multiple occurrence
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.191922']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:CGI:EarthNaturalSurface",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.191922']/gsml:samplingFrame/@xlink:href",
                doc);

        // test that geologicUnit is correctly encoded

        checkGU167775491110573732(doc);
        checkGU16777549126930540(doc);
        checkGU16777549126932776(doc);

        // test that non-feature type should return nothing/exception
        doc = getAsDOM("wfs?request=GetFeature&typename=gsml:ConstituentPart");
        LOGGER.info("WFS GetFeature&typename=gsml:ConstituentPart response:\n" + prettyString(doc));
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
    }

    private void checkGU167775491110573732(Document doc) {
        assertXpathEvaluatesTo("typicalNorm",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491110573732']/gsml:purpose",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Lithodemic_IntrusiveUnitRank",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491110573732']/gsml:rank/@codeSpace",
                doc);
        assertXpathEvaluatesTo("Intrusion [rank]",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491110573732']/gsml:rank",
                doc);

    }

    private void checkGU16777549126932776(Document doc) {
        ArrayList<String> name = new ArrayList<String>();
        name.add(evaluate(
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gml:name[1]",
                doc));
        name.add(evaluate(
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gml:name[2]",
                doc));
        assertTrue(name.contains("Castlemaine Group (Oc)"));
        assertTrue(name.contains("urn:cgi:feature:GSV:GeologicUnit:16777549126932776"));

        ArrayList<String> nameCodespace = new ArrayList<String>();
        nameCodespace
                .add(evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gml:name[1]/@codeSpace",
                        doc));
        nameCodespace
                .add(evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gml:name[2]/@codeSpace",
                        doc));
        assertTrue(nameCodespace.contains("http://www.dpi.vic.gov.au/earth-resources"));
        assertTrue(nameCodespace.contains("http://www.ietf.org/rfc/rfc2141"));

        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "http://urn.opengis.net/",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo("typicalNorm",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:purpose",
                doc);

        assertXpathEvaluatesTo(
                "#gsml.geologicevent.16777549126932776",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:preferredAge/@xlink:href",
                doc);

        assertXpathEvaluatesTo(
                "urn:cgi:classifier:ICS:StratChart:2008:MiddleOrdovician",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126932777']/gsml:eventAge/gsml:CGI_TermRange/gsml:lower/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:ICS:StratChart:2008",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126932777']/gsml:eventAge/gsml:CGI_TermRange/gsml:lower/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo(
                "urn:cgi:classifier:ICS:StratChart:2008:Cambrian",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126932777']/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:ICS:StratChart:2008",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126932777']/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        // test eventEnvironment with no regards to its order
        ArrayList<String> eventEnvironment = new ArrayList<String>();
        eventEnvironment
                .add(evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126932777']/gsml:eventEnvironment[1]/gsml:CGI_TermValue/gsml:value",
                        doc));
        eventEnvironment
                .add(evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126932777']/gsml:eventEnvironment[2]/gsml:CGI_TermValue/gsml:value",
                        doc));
        assertTrue(eventEnvironment.contains("hemipelagic"));
        assertTrue(eventEnvironment.contains("submarine fan"));

        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:MarineEnvironments",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126932777']/gsml:eventEnvironment[1]/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        // test eventProcess with no regards to its order
        ArrayList<String> eventProcess = new ArrayList<String>();
        eventProcess
                .add(evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126932777']/gsml:eventProcess[1]/gsml:CGI_TermValue/gsml:value",
                        doc));
        eventProcess
                .add(evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126932777']/gsml:eventProcess[2]/gsml:CGI_TermValue/gsml:value",
                        doc));
        assertTrue(eventProcess.contains("water [process]"));
        assertTrue(eventProcess.contains("turbidity current"));

        assertXpathEvaluatesTo(
                "urn:cgi:classifier:CGI:GeologicUnitType:200811:lithostratigraphic_unit",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:geologicUnitType/@xlink:href",
                doc);

        // assertXpathEvaluatesTo("",
        // "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/",doc);
        assertXpathEvaluatesTo("Group [lithostratigraphic]",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:rank", doc);

        assertXpathEvaluatesTo(
                "typicalNorm",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491110575003']/gsml:purpose",
                doc);

        assertXpathEvaluatesTo(
                "indurated",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491110575003']/gsml:consolidationDegree/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:ConsolidationDegree",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491110575003']/gsml:consolidationDegree/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifier:CGI:SimpleLithology:2008:sandstone",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491110575003']/gsml:lithology/@xlink:href",
                doc);

        assertXpathEvaluatesTo(
                "unit part",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:part/gsml:GeologicUnitPart/gsml:role",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Provisional:GeologicalUnitPart",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:part/gsml:GeologicUnitPart/gsml:role/@codeSpace",
                doc);

        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:part/gsml:GeologicUnitPart/gsml:proportion/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "http://urn.opengis.net/",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:part/gsml:GeologicUnitPart/gsml:proportion/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo(
                "http://urn.resolver.url/?uri=urn:cgi:feature:GSV:GeologicUnit:16777549126932767",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:part/gsml:GeologicUnitPart/gsml:containedUnit/@xlink:href",
                doc);

        assertXpathEvaluatesTo(
                "2.114",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:density/gsml:CGI_NumericRange/gsml:lower/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:uom:UCUM:g.cm-3",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:density/gsml:CGI_NumericRange/gsml:lower/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);

        assertXpathEvaluatesTo(
                "2.955",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:density/gsml:CGI_NumericRange/gsml:upper/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:uom:UCUM:g.cm-3",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:density/gsml:CGI_NumericRange/gsml:upper/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);

        assertXpathEvaluatesTo(
                "0.0",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericRange/gsml:lower/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:uom:UCUM::SI",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericRange/gsml:lower/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);

        assertXpathEvaluatesTo(
                "2700.0",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericRange/gsml:upper/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:uom:UCUM::SI",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericRange/gsml:upper/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
    }

    private void checkGU16777549126930540(Document doc) {

        ArrayList<String> name = new ArrayList<String>();
        name.add(evaluate(
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gml:name[1]",
                doc));
        name.add(evaluate(
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gml:name[2]",
                doc));
        assertTrue(name.contains("urn:cgi:feature:GSV:GeologicUnit:16777549126930540"));
        assertTrue(name.contains("Unnamed incised alluvium (Na)"));

        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "http://urn.opengis.net/",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo("typicalNorm",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:purpose",
                doc);

        assertXpathEvaluatesTo(
                "previous mapping",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.195237']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:InterpretationMethod",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.195237']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::missing",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.195237']/gsml:positionalAccuracy/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "http://urn.opengis.net/",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.195237']/gsml:positionalAccuracy/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo(
                "urn:cgi:feature:CGI:EarthNaturalSurface",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.195237']/gsml:samplingFrame/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:GSV:GeologicUnit:16777549126930540",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.195237']/gsml:specification/@xlink:href",
                doc);

        assertXpathEvaluatesTo(
                "#gsml.geologicevent.16777549126930540",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:preferredAge/@xlink:href",
                doc);

        assertXpathEvaluatesTo(
                "urn:cgi:classifier:ICS:StratChart:2008:Pleistocene",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126930541']/gsml:eventAge/gsml:CGI_TermRange/gsml:lower/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:ICS:StratChart:2008",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126930541']/gsml:eventAge/gsml:CGI_TermRange/gsml:lower/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo(
                "urn:cgi:classifier:ICS:StratChart:2008:Miocene",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126930541']/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:ICS:StratChart:2008",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126930541']/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo(
                "fluvial",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126930541']/gsml:eventEnvironment/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:TerrestrialEnvironments",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126930541']/gsml:eventEnvironment/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo(
                "channelled stream flow",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126930541']/gsml:eventProcess/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Process",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126930541']/gsml:eventProcess/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo(
                "typicalNorm",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491110573565']/gsml:purpose",
                doc);

        assertXpathEvaluatesTo(
                "consolidated",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491110573565']/gsml:consolidationDegree/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:ConsolidationDegree",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491110573565']/gsml:consolidationDegree/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo(
                "urn:cgi:classifier:CGI:SimpleLithology:2008:gravel",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491110573565']/gsml:lithology/@xlink:href",
                doc);

    }

    /**
     * Check schema location
     * 
     * @param doc
     */
    private void checkSchemaLocation(Document doc) {
        String schemaLocation = evaluate("/wfs:FeatureCollection/@xsi:schemaLocation", doc);
        String gsmlLocation = AbstractAppSchemaMockData.GSML_URI + " "
                + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL;
        String wfsLocation = org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE + " "
                + org.geoserver.wfs.xml.v1_1_0.WFS.CANONICAL_SCHEMA_LOCATION;
        if (schemaLocation.startsWith(AbstractAppSchemaMockData.GSML_URI)) {
            // GSML schema location was encoded first
            assertEquals(gsmlLocation + " " + wfsLocation, schemaLocation);
        } else {
            // WFS schema location was encoded first
            assertEquals(wfsLocation + " " + gsmlLocation, schemaLocation);
        }
    }

    /**
     * Test FeatureCollection is encoded with one/many featureMembers element
     * 
     * @throws Exception
     */
    public void testEncodeFeatureMember() {

        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        boolean encodeFeatureMember = wfs.isEncodeFeatureMember();
        // test one featureMember element
        wfs.setEncodeFeatureMember(false);
        getGeoServer().save(wfs);

        String path = "wfs?request=GetFeature&typename=gsml:GeologicUnit&featureid=gsml.geologicunit.16777549126932776,gsml.geologicunit.167775491110573732,gsml.geologicunit.16777549126930540";
        Document doc = getAsDOM(path);
        LOGGER.info("WFS GetFeature&typename=gsml:GeologicUnit response:\n" + prettyString(doc));

        checkSchemaLocation(doc);

        assertXpathEvaluatesTo("3", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(3, "//gsml:GeologicUnit", doc);

        assertEquals(0, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(1, doc.getElementsByTagName("gml:featureMembers").getLength());

        checkGU167775491110573732(doc);
        checkGU16777549126930540(doc);
        checkGU16777549126932776(doc);
        // test many featureMember
        wfs.setEncodeFeatureMember(true);
        getGeoServer().save(wfs);

        path = "wfs?request=GetFeature&typename=gsml:GeologicUnit&featureid=gsml.geologicunit.16777549126932776,gsml.geologicunit.167775491110573732,gsml.geologicunit.16777549126930540";
        doc = getAsDOM(path);
        LOGGER.info("WFS GetFeature&typename=gsml:GeologicUnit response:\n" + prettyString(doc));

        checkSchemaLocation(doc);

        assertXpathEvaluatesTo("3", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(3, "//gsml:GeologicUnit", doc);

        assertEquals(3, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(0, doc.getElementsByTagName("gml:featureMembers").getLength());

        checkGU167775491110573732(doc);
        checkGU16777549126930540(doc);
        checkGU16777549126932776(doc);

        wfs = getGeoServer().getService(WFSInfo.class);
        wfs.setEncodeFeatureMember(encodeFeatureMember);
        getGeoServer().save(wfs);
    }

    /**
     * Where there is null and the schema is minOccur=0, test that element is ommitted instead of
     * throwing an exception.
     */
    public void testMinOccur0() {
        String path = "wfs?request=GetFeature&typename=gsml:ShearDisplacementStructure&featureid=gsml.sheardisplacementstructure.46220,gsml.sheardisplacementstructure.46216";
        Document doc = getAsDOM(path);
        LOGGER.info("WFS GetFeature&typename=gsml:ShearDisplacementStructure response:\n"
                + prettyString(doc));

        assertXpathEvaluatesTo(
                "E",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46216']/gsml:geologicHistory/gsml:DisplacementEvent/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:hangingWallDirection/gsml:CGI_LinearOrientation/gsml:descriptiveOrientation/gsml:CGI_TermValue/gsml:value",
                doc);

        assertXpathCount(
                0,
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46220']/gsml:geologicHistory/gsml:DisplacementEvent/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:hangingWallDirection/gsml:CGI_LinearOrientation/gsml:descriptiveOrientation/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:NullValues",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46220']/gsml:geologicHistory/gsml:DisplacementEvent/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:movementSense/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
    }

    /**
     * test that geometries are correctly reprojected.
     */
    public void testReprojection() {
        String path = "wfs?request=GetFeature&srsName=urn:x-ogc:def:crs:EPSG:4283&typename=gsml:MappedFeature&featureid=gsml.mappedfeature.191922";
        Document doc = getAsDOM(path);
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));

        assertXpathEvaluatesTo("urn:x-ogc:def:crs:EPSG:4283",
                "//gsml:MappedFeature/gsml:shape/gml:MultiSurface/@srsName", doc);
        assertXpathEvaluatesTo("2",
                "//gsml:MappedFeature/gsml:shape/gml:MultiSurface/@srsDimension", doc);

        String expected = "-37.391436383333335 144.782547975 -37.39050545 144.78374375833334 -37.38964884166667 144.78460036666667 -37.388678025 144.78522854166667 "
                + "-37.38776430833333 144.78597093333335 -37.386736383333336 144.7864849 -37.38399524166667 144.788712075 -37.38199649166667 "
                + "144.79002553333333 -37.38108278333333 144.790767925 -37.3787685 144.7914741 -37.37675986666667 144.79356964166666 "
                + "-37.377616466666666 144.79436913333333 -37.378701508333336 144.79396939166668 -37.37972944166667 144.79339831666667 "
                + "-37.380871575 144.79316989166668 -37.38201371666667 144.79311279166666 -37.38315585833333 144.79282725 -37.386011208333336 "
                + "144.79088560833333 -37.387781525 144.78934370833332 -37.389951591666666 144.78843 -37.39092241666667 144.787801825 "
                + "-37.39172191666667 144.78694521666668 -37.392178775 144.78586018333334 -37.392235875 144.78471804166668 -37.39172191666667 "
                + "144.78369011666666 -37.391436383333335 144.782547975";
        String orig = evaluate(
                "//gsml:MappedFeature/gsml:shape/gml:MultiSurface/gml:surfaceMember/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertTrue(isEqualGeometry(orig, expected, 5));
        path = "wfs?request=GetFeature&srsName=EPSG:4283&typename=gsml:MappedFeature&featureid=gsml.mappedfeature.191922";

        doc = getAsDOM(path);
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));

        assertXpathEvaluatesTo("http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature/gsml:shape/gml:MultiSurface/@srsName", doc);
        assertXpathEvaluatesTo("2",
                "//gsml:MappedFeature/gsml:shape/gml:MultiSurface/@srsDimension", doc);

        expected = "144.782547975 -37.391436383333335 144.78374375833334 -37.39050545 144.78460036666667 -37.38964884166667 144.78522854166667 "
                + "-37.388678025 144.78597093333335 -37.38776430833333 144.7864849 -37.386736383333336 144.788712075 -37.38399524166667 "
                + "144.79002553333333 -37.38199649166667 144.790767925 -37.38108278333333 144.7914741 -37.3787685 144.79356964166666 "
                + "-37.37675986666667 144.79436913333333 -37.377616466666666 144.79396939166668 -37.378701508333336 144.79339831666667 "
                + "-37.37972944166667 144.79316989166668 -37.380871575 144.79311279166666 -37.38201371666667 144.79282725 -37.38315585833333 "
                + "144.79088560833333 -37.386011208333336 144.78934370833332 -37.387781525 144.78843 -37.389951591666666 144.787801825 "
                + "-37.39092241666667 144.78694521666668 -37.39172191666667 144.78586018333334 -37.392178775 144.78471804166668 -37.392235875 "
                + "144.78369011666666 -37.39172191666667 144.782547975 -37.391436383333335";
        orig = evaluate(
                "//gsml:MappedFeature/gsml:shape/gml:MultiSurface/gml:surfaceMember/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertTrue(isEqualGeometry(orig, expected, 5));
        // TODO Reprojection on GeologicUnit seem buggy. VT to come back and fix it.
    }

    // compares whether a list of coordinates are the same with certain level of leniency. eg
    // -37.391436383333335 -37.3914363833333 are the same
    // if we round it off to 10 decimal places
    private boolean isEqualGeometry(String orig, String expected, int leniency) {
        String[] origCoordinates = orig.split(" ");
        String[] expCoordinates = expected.split(" ");
        if (origCoordinates.length != expCoordinates.length)
            return false;

        for (int i = 0; i < origCoordinates.length; i++) {
            BigDecimal origBd = new BigDecimal(origCoordinates[i]);
            BigDecimal expBd = new BigDecimal(expCoordinates[i]);
            origBd = origBd.setScale(leniency, BigDecimal.ROUND_HALF_UP);
            expBd = expBd.setScale(leniency, BigDecimal.ROUND_HALF_UP);
            if (!origBd.equals(expBd)) {
                return false;
            }

        }
        return true;
    }

    /**
     * validate the result returned by the given path.
     */
    public void testValidate() {
        String path = "wfs?request=GetFeature&typename=gsml:GeologicUnit&featureid=gsml.geologicunit.16777549126932776,gsml.geologicunit.167775491110573732,gsml.geologicunit.16777549126930540";
        validateGet(path);
    }

    /**
     * Test if we can get gsml.geologicunit.16777549126932776 with a FeatureId fid filter.
     */
    public void testGetFeatureWithFeatureIdFilter() {
        String xml = //
        "<wfs:GetFeature " //
                + "service=\"WFS\" " //
                + "version=\"1.1.0\" " //
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                + "xmlns:gsml=\"" + AbstractAppSchemaMockData.GSML_URI + "\" " //
                + ">" //
                + "    <wfs:Query typeName=\"gsml:GeologicUnit\">" //
                + "        <ogc:Filter>" //
                + "            <ogc:FeatureId fid=\"gsml.geologicunit.16777549126932776\"/>" //
                + "        </ogc:Filter>" //
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        checkGU16777549126932776(doc);
    }

    /**
     * Test if we can get gsml.geologicunit.16777549126932776 with a GmlObjectId gml:id filter.
     */
    public void testGetFeatureWithGmlObjectIdFilter() {
        String xml = //
        "<wfs:GetFeature " //
                + "service=\"WFS\" " //
                + "version=\"1.1.0\" " //
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                + "xmlns:gsml=\"" + AbstractAppSchemaMockData.GSML_URI + "\" " //
                + ">" //
                + "    <wfs:Query typeName=\"gsml:GeologicUnit\">" //
                + "        <ogc:Filter>" //
                + "            <ogc:GmlObjectId gml:id=\"gsml.geologicunit.16777549126932776\"/>" //
                + "        </ogc:Filter>" //
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        checkGU16777549126932776(doc);
    }

    /**
     * Making sure multi-valued attributes in nested features can be queried from the top level.
     * (GEOT-3156)
     */
    public void testFilteringNestedMultiValuedAttribute() {
        // test PropertyIsEqual filter
        String xml = "<wfs:GetFeature " + "service=\"WFS\" " + "version=\"1.1.0\" "
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\" " + "xmlns:gsml=\""
                + AbstractAppSchemaMockData.GSML_URI + "\" " + ">"
                + "    <wfs:Query typeName=\"gsml:GeologicUnit\">" + "        <ogc:Filter>"
                + "            <ogc:PropertyIsEqualTo>"
                + "                <ogc:Literal>Akuna Mudstone (Oba)</ogc:Literal>"
                + "                <ogc:PropertyName>gml:name</ogc:PropertyName>"
                + "            </ogc:PropertyIsEqualTo>" + "        </ogc:Filter>"
                + "    </wfs:Query> " + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo("gsml.geologicunit.167775491107848330",
                "//gsml:GeologicUnit/@gml:id", doc);

        // like filter
        xml = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.1.0\" "
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\" "
                + "xmlns:gsml=\""
                + AbstractAppSchemaMockData.GSML_URI
                + "\" "
                + ">"
                + "    <wfs:Query typeName=\"gsml:GeologicUnit\">"
                + "        <ogc:Filter>"
                + "            <ogc:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">>"
                + "                <ogc:PropertyName>gml:name</ogc:PropertyName>"
                + "                <ogc:Literal>*Creek Granite*</ogc:Literal>"
                + "            </ogc:PropertyIsLike>" + "        </ogc:Filter>"
                + "    </wfs:Query> " + "</wfs:GetFeature>";

        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        // there should be 9:
        // - mf1/gu.25699
        // - mf2/gu.25678
        // - mf3/gu.25678
        assertXpathEvaluatesTo("9", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(9, "//gsml:GeologicUnit", doc);

        ArrayList<String> ids = new ArrayList<String>();
        for (int i = 1; i <= 9; i++) {
            ids.add(evaluate("(//gsml:GeologicUnit)[" + i + "]/@gml:id", doc));
        }

        assertTrue(ids.contains("gsml.geologicunit.1677754911513315832"));
        assertTrue(ids.contains("gsml.geologicunit.1677754911513318069"));
        assertTrue(ids.contains("gsml.geologicunit.1677754911513318192"));
        assertTrue(ids.contains("gsml.geologicunit.16777549126941084"));
        assertTrue(ids.contains("gsml.geologicunit.16777549126941436"));
        assertTrue(ids.contains("gsml.geologicunit.16777549126942189"));
        assertTrue(ids.contains("gsml.geologicunit.16777549126942227"));
        assertTrue(ids.contains("gsml.geologicunit.16777549126942588"));
        assertTrue(ids.contains("gsml.geologicunit.16777549126942855"));

        // test combined filter
        xml = "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\" xmlns:gsml=\""
                + AbstractAppSchemaMockData.GSML_URI
                + "\">"
                + "<wfs:Query typeName=\"gsml:GeologicUnit\">"
                + "    <ogc:Filter>"
                + "        <ogc:And>"
                + "            <ogc:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">>"
                + "                <ogc:Literal>*Creek Granite*</ogc:Literal>"
                + "                <ogc:PropertyName>gml:name</ogc:PropertyName>"
                + "            </ogc:PropertyIsLike>"
                + "                <ogc:Not>"
                + "                    <ogc:PropertyIsEqualTo>"
                + "                        <ogc:Literal>mid-crustal</ogc:Literal>"
                + "                        <ogc:PropertyName>gsml:geologicHistory/gsml:GeologicEvent/gsml:eventEnvironment/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>"
                + "                    </ogc:PropertyIsEqualTo>"
                + "                </ogc:Not>"
                + "        </ogc:And>"
                + "    </ogc:Filter>"
                + "</wfs:Query> "
                + "</wfs:GetFeature>";
        //
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("6", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(6, "//gsml:GeologicUnit", doc);

        ids.clear();

        for (int i = 1; i <= 6; i++) {
            ids.add(evaluate("(//gsml:GeologicUnit)[" + i + "]/@gml:id", doc));
        }

        assertTrue(ids.contains("gsml.geologicunit.1677754911513318069"));
        assertTrue(ids.contains("gsml.geologicunit.1677754911513318192"));
        assertTrue(ids.contains("gsml.geologicunit.16777549126941436"));
        assertTrue(ids.contains("gsml.geologicunit.16777549126942189"));
        assertTrue(ids.contains("gsml.geologicunit.16777549126942227"));
        assertTrue(ids.contains("gsml.geologicunit.16777549126942855"));

        //       
    }

    /**
     * Test if we can get gsml.geologicunit.16777549126930540 using its name.
     */
    public void testGetFeaturePropertyFilter() {
        String xml = //
        "<wfs:GetFeature " //
                + "service=\"WFS\" " //
                + "version=\"1.1.0\" " //
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                + "xmlns:gsml=\"" + AbstractAppSchemaMockData.GSML_URI + "\" " //
                + ">" //
                + "    <wfs:Query typeName=\"gsml:GeologicUnit\">" //
                + "        <ogc:Filter>" //
                + "            <ogc:PropertyIsEqualTo>" //
                + "                <ogc:PropertyName>gml:name</ogc:PropertyName>" //
                + "                <ogc:Literal>Unnamed incised alluvium (Na)</ogc:Literal>" //
                + "            </ogc:PropertyIsEqualTo>" //
                + "        </ogc:Filter>" //
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        checkGU16777549126930540(doc);

    }

    /**
     * Test if we can get gsml.geologicunit.16777549126930540 using its name.
     */
    public void testFilterOnNestedAttribute() {
        String xml = //
        "<wfs:GetFeature " //
                + "service=\"WFS\" " //
                + "version=\"1.1.0\" " //
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                + "xmlns:gsml=\""
                + AbstractAppSchemaMockData.GSML_URI
                + "\" " //
                + ">" //
                + "    <wfs:Query typeName=\"gsml:GeologicUnit\">" //
                + "        <ogc:Filter>" //
                + "            <ogc:PropertyIsEqualTo>" //
                + "                <ogc:PropertyName>gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>" //
                + "                <ogc:Literal>significant</ogc:Literal>" //
                + "            </ogc:PropertyIsEqualTo>" //
                + "        </ogc:Filter>" //
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(3, "//gsml:GeologicUnit", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107848330']",
                doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']",
                doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']",
                doc);

        assertXpathEvaluatesTo(
                "significant",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:composition/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Proportion",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:composition/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        this.checkGU16777549126930540(doc);
        this.checkGU16777549126932776(doc);

        xml = //
        "<wfs:GetFeature " //
                + "service=\"WFS\" " //
                + "version=\"1.1.0\" " //
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                + "xmlns:gsml=\""
                + AbstractAppSchemaMockData.GSML_URI
                + "\" " //
                + ">" //
                + "    <wfs:Query typeName=\"gsml:GeologicUnit\">" //
                + "        <ogc:Filter>" //
                + "            <ogc:PropertyIsEqualTo>" //
                + "                <ogc:PropertyName>gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>" //
                + "                <ogc:Literal>urn:cgi:classifierScheme:GSV:Proportion</ogc:Literal>" //
                + "            </ogc:PropertyIsEqualTo>" //
                + "        </ogc:Filter>" //
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(7, "//gsml:GeologicUnit", doc);

        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107838594']",
                doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107838781']",
                doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107838810']",
                doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107848330']",
                doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513315832']",
                doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']",
                doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']",
                doc);

        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Proportion",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513315832']/gsml:composition/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "all",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513315832']/gsml:composition/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value",
                doc);
    }

    public void testResultHitsWithFilter() {
        String path = "wfs?request=GetFeature&typename=gsml:Contact&resultType=hits";
        Document doc = getAsDOM(path);
        LOGGER.info(prettyString(doc));

        assertXpathEvaluatesTo("5", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        // test filter with maxFeature
        String xml = "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\" xmlns:gsml=\""
                + AbstractAppSchemaMockData.GSML_URI
                + "\" resultType=\"hits\" maxFeatures=\"1\">"
                + "<wfs:Query typeName=\"gsml:GeologicUnit\">"
                + "    <ogc:Filter>"
                + "        <ogc:Or>"
                + "            <ogc:PropertyIsEqualTo>"
                + "                <ogc:Literal>900.0</ogc:Literal>"
                + "                <ogc:PropertyName>gsml:GeologicUnit/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericValue/gsml:principalValue</ogc:PropertyName>"
                + "            </ogc:PropertyIsEqualTo>"
                + "            <ogc:PropertyIsEqualTo>"
                + "                 <ogc:Literal>urn:ogc:def:uom:UCUM::SI</ogc:Literal>"
                + "                 <ogc:PropertyName>gsml:GeologicUnit/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericRange/gsml:upper/gsml:CGI_NumericValue/gsml:principalValue/@uom</ogc:PropertyName>"
                + "            </ogc:PropertyIsEqualTo>"
                + "        </ogc:Or>"
                + "    </ogc:Filter>" + "</wfs:Query> " + "</wfs:GetFeature>";
        //
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);

        // test filter without maxfeature
        xml = "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\" xmlns:gsml=\""
                + AbstractAppSchemaMockData.GSML_URI
                + "\" resultType=\"hits\">"
                + "<wfs:Query typeName=\"gsml:GeologicUnit\">"
                + "    <ogc:Filter>"
                + "        <ogc:Or>"
                + "            <ogc:PropertyIsEqualTo>"
                + "                <ogc:Literal>900.0</ogc:Literal>"
                + "                <ogc:PropertyName>gsml:GeologicUnit/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericValue/gsml:principalValue</ogc:PropertyName>"
                + "            </ogc:PropertyIsEqualTo>"
                + "            <ogc:PropertyIsEqualTo>"
                + "                 <ogc:Literal>urn:ogc:def:uom:UCUM::SI</ogc:Literal>"
                + "                 <ogc:PropertyName>gsml:GeologicUnit/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericRange/gsml:upper/gsml:CGI_NumericValue/gsml:principalValue/@uom</ogc:PropertyName>"
                + "            </ogc:PropertyIsEqualTo>"
                + "        </ogc:Or>"
                + "    </ogc:Filter>" + "</wfs:Query> " + "</wfs:GetFeature>";
        //
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
    }

    public void testNoPrimaryKey() {
        String path = "wfs?request=GetFeature&typename=gsml:ShearDisplacementStructure&featureid=gsml.sheardisplacementstructure.46216";
        Document doc = getAsDOM(path);
        LOGGER.info(prettyString(doc));
        assertXpathCount(
                2,
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46216']/gsml:geologicHistory/gsml:DisplacementEvent",
                doc);

        String xml = "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\" xmlns:gsml=\""
                + AbstractAppSchemaMockData.GSML_URI
                + "\">"
                + "<wfs:Query typeName=\"gsml:ShearDisplacementStructure\">"
                + "    <ogc:Filter>"
                + "          <ogc:PropertyIsEqualTo>"
                + "              <ogc:Literal>E</ogc:Literal>"
                + "              <ogc:PropertyName>gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46216']/gsml:geologicHistory/gsml:DisplacementEvent/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:hangingWallDirection/gsml:CGI_LinearOrientation/gsml:descriptiveOrientation/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>"
                + "          </ogc:PropertyIsEqualTo>"
                + "    </ogc:Filter>"
                + "</wfs:Query> "
                + "</wfs:GetFeature>";

        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo(
                "E",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46216']/gsml:geologicHistory/gsml:DisplacementEvent/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:hangingWallDirection/gsml:CGI_LinearOrientation/gsml:descriptiveOrientation/gsml:CGI_TermValue/gsml:value",
                doc);
    }
}
