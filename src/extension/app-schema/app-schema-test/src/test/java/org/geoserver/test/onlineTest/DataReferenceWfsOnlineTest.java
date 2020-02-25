/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.onlineTest.support.AbstractDataReferenceWfsTest;
import org.geoserver.wfs.WFSInfo;
import org.geotools.appschema.filter.FilterFactoryImplNamespaceAware;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.complex.AppSchemaDataAccessRegistry;
import org.geotools.data.complex.MappingFeatureCollection;
import org.geotools.data.complex.MappingFeatureSource;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.type.Types;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.w3c.dom.Document;

/** @author Victor Tey(CSIRO Earth Science and Resource Engineering) */
public abstract class DataReferenceWfsOnlineTest extends AbstractDataReferenceWfsTest {
    private boolean printDoc = false;

    public DataReferenceWfsOnlineTest() throws Exception {
        super();
    }

    /** URI for om namespace. */
    protected static final String OM_URI = "http://www.opengis.net/om/1.0";

    /** Schema URL for observation and measurements */
    protected static final String OM_SCHEMA_LOCATION_URL =
            "http://schemas.opengis.net/om/1.0.0/observation.xsd";

    /** Test whether GetCapabilities returns wfs:WFS_Capabilities. */
    @Test
    public void testGetCapabilities() {
        Document doc = getAsDOM("wfs?request=GetCapabilities&version=1.1.0");
        if (printDoc) {
            LOGGER.info("WFS =GetCapabilities response:\n" + prettyString(doc));
        }

        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement().getNodeName());
        assertXpathCount(6, "//wfs:FeatureType", doc);

        // make sure non-feature types don't appear in FeatureTypeList

        ArrayList<String> featureTypeNames = new ArrayList<String>(7);
        featureTypeNames.add(evaluate("//wfs:FeatureType[1]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[2]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[3]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[4]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[5]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[6]/wfs:Name", doc));

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

    @Test
    public void testDescribeFeatureType() throws IOException {

        /** gsml:MappedFeature */
        Document doc =
                getAsDOM(
                        "wfs?request=DescribeFeatureType&version=1.1.0&typename=gsml:MappedFeature");
        if (printDoc) {
            LOGGER.info(
                    "WFS DescribeFeatureType, typename=gsml:MappedFeature response:\n"
                            + prettyString(doc));
        }
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        // check target name space is encoded and is correct
        assertXpathEvaluatesTo(AbstractAppSchemaMockData.GSML_URI, "//@targetNamespace", doc);
        // make sure the content is only relevant include
        assertXpathCount(1, "//xsd:include", doc);
        // no import to GML since it's already imported inside the included schema
        // otherwise it's invalid to import twice
        assertXpathCount(0, "//xsd:import", doc);
        // GSML schemaLocation
        assertXpathEvaluatesTo(
                AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL,
                "//xsd:include/@schemaLocation",
                doc);
        // nothing else
        assertXpathCount(0, "//xsd:complexType", doc);
        assertXpathCount(0, "//xsd:element", doc);

        /** gsml:GeologicUnit */
        doc = getAsDOM("wfs?request=DescribeFeatureType&version=1.1.0&typename=gsml:GeologicUnit");
        LOGGER.info(
                "WFS DescribeFeatureType, typename=gsml:GeologicUnit response:\n"
                        + prettyString(doc));
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo(AbstractAppSchemaMockData.GSML_URI, "//@targetNamespace", doc);
        assertXpathCount(1, "//xsd:include", doc);
        assertXpathCount(0, "//xsd:import", doc);
        // GSML schemaLocation
        assertXpathEvaluatesTo(
                AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL,
                "//xsd:include/@schemaLocation",
                doc);
        // nothing else
        assertXpathCount(0, "//xsd:complexType", doc);
        assertXpathCount(0, "//xsd:element", doc);

        doc = getAsDOM("wfs?request=DescribeFeatureType&version=1.1.0&typename=gsml:Contact");
        LOGGER.info(
                "WFS DescribeFeatureType, typename=gsml:Contact response:\n" + prettyString(doc));
        // the namespace om http://www.opengis.net/om/1.0 was added
        // to gsml:Contact on purpose to test the import

        testConsistantSchema(doc);
        doc = null;
        System.gc();
        doc =
                getAsDOM(
                        "wfs?request=DescribeFeatureType&version=1.1.0&typename=gsml:Contact,gsml:MappedFeature");
        if (printDoc) {
            LOGGER.info(
                    "WFS DescribeFeatureType, typename=gsml:Contact,gsml:MappedFeature response:\n"
                            + prettyString(doc));
        }
        testConsistantSchema(doc);
        // TODO without any type specified, gsml:Content om namespace not imported.
        doc = getAsDOM("wfs?request=DescribeFeatureType&version=1.1.0");
        LOGGER.info("WFS DescribeFeatureType, typename=none response:\n" + prettyString(doc));
        // testConsistantSchema(doc); //why didn't inc import?
    }

    private void testConsistantSchema(Document doc) {
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo(AbstractAppSchemaMockData.GSML_URI, "//@targetNamespace", doc);
        assertXpathCount(1, "//xsd:include", doc);
        assertXpathCount(1, "//xsd:import", doc);

        assertXpathEvaluatesTo(DataReferenceWfsOnlineTest.OM_URI, "//xsd:import/@namespace", doc);
        assertXpathEvaluatesTo(
                DataReferenceWfsOnlineTest.OM_SCHEMA_LOCATION_URL,
                "//xsd:import/@schemaLocation",
                doc);
        assertXpathEvaluatesTo(
                AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL,
                "//xsd:include/@schemaLocation",
                doc);
        // nothing else
        assertXpathCount(0, "//xsd:complexType", doc);
        assertXpathCount(0, "//xsd:element", doc);
    }

    /**
     * This will test all mapping associated tests without filters GetFeature, Feature chaining
     * mapping,use of MappingName, GetFeatureValid, including validation
     */
    @Test
    public void testMapping() {
        String path =
                "wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit&featureid=gsml.geologicunit.16777549126932776,gsml.geologicunit.167775491110573732,gsml.geologicunit.16777549126930540";
        Document doc = getAsDOM(path);
        if (printDoc) {
            LOGGER.info(
                    "WFS GetFeature&typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        }
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
        if (printDoc) {
            LOGGER.info(
                    "WFS GetFeature&typename=gsml:ConstituentPart response:\n" + prettyString(doc));
        }
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
    }

    private void checkGU167775491110573732(Document doc) {
        assertXpathEvaluatesTo(
                "typicalNorm",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491110573732']/gsml:purpose",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Lithodemic_IntrusiveUnitRank",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491110573732']/gsml:rank/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "Intrusion [rank]",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491110573732']/gsml:rank",
                doc);
    }

    private void checkGU16777549126932776(Document doc) {
        ArrayList<String> name = new ArrayList<String>();
        name.add(
                evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gml:name[1]",
                        doc));
        name.add(
                evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gml:name[2]",
                        doc));
        assertTrue(name.contains("Castlemaine Group (Oc)"));
        assertTrue(name.contains("urn:cgi:feature:GSV:GeologicUnit:16777549126932776"));

        ArrayList<String> nameCodespace = new ArrayList<String>();
        nameCodespace.add(
                evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gml:name[1]/@codeSpace",
                        doc));
        nameCodespace.add(
                evaluate(
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

        assertXpathEvaluatesTo(
                "typicalNorm",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:purpose",
                doc);

        assertXpathEvaluatesTo(
                "#gsml.geologicevent.16777549126932777",
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
        eventEnvironment.add(
                evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126932777']/gsml:eventEnvironment[1]/gsml:CGI_TermValue/gsml:value",
                        doc));
        eventEnvironment.add(
                evaluate(
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
        eventProcess.add(
                evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126932777']/gsml:eventProcess[1]/gsml:CGI_TermValue/gsml:value",
                        doc));
        eventProcess.add(
                evaluate(
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
        assertXpathEvaluatesTo(
                "Group [lithostratigraphic]",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:rank",
                doc);

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
        name.add(
                evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gml:name[1]",
                        doc));
        name.add(
                evaluate(
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

        assertXpathEvaluatesTo(
                "typicalNorm",
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
                "#gsml.geologicevent.16777549126930541",
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

    /** Check schema location */
    private void checkSchemaLocation(Document doc) {
        String schemaLocation = evaluate("/wfs:FeatureCollection/@xsi:schemaLocation", doc);
        String gsmlLocation =
                AbstractAppSchemaMockData.GSML_URI
                        + " "
                        + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL;
        String wfsLocation =
                org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE
                        + " "
                        + org.geoserver.wfs.xml.v1_1_0.WFS.CANONICAL_SCHEMA_LOCATION;
        if (schemaLocation.startsWith(AbstractAppSchemaMockData.GSML_URI)) {
            // GSML schema location was encoded first
            assertEquals(gsmlLocation + " " + wfsLocation, schemaLocation);
        } else {
            // WFS schema location was encoded first
            assertEquals(wfsLocation + " " + gsmlLocation, schemaLocation);
        }
    }

    /** Test FeatureCollection is encoded with one/many featureMembers element */
    @Test
    public void testEncodeFeatureMember() {

        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        boolean encodeFeatureMember = wfs.isEncodeFeatureMember();
        // test one featureMember element
        wfs.setEncodeFeatureMember(false);
        getGeoServer().save(wfs);

        String path =
                "wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit&featureid=gsml.geologicunit.16777549126932776,gsml.geologicunit.167775491110573732,gsml.geologicunit.16777549126930540";
        Document doc = getAsDOM(path);
        if (printDoc) {
            LOGGER.info(
                    "WFS GetFeature&typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        }

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

        path =
                "wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit&featureid=gsml.geologicunit.16777549126932776,gsml.geologicunit.167775491110573732,gsml.geologicunit.16777549126930540";
        doc = getAsDOM(path);
        if (printDoc) {
            LOGGER.info(
                    "WFS GetFeature&typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        }
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
    @Test
    public void testMinOccur0() {
        String path =
                "wfs?request=GetFeature&version=1.1.0&typename=gsml:ShearDisplacementStructure&featureid=gsml.sheardisplacementstructure.46220,gsml.sheardisplacementstructure.46216";
        Document doc = getAsDOM(path);
        if (printDoc) {
            LOGGER.info(
                    "WFS GetFeature&typename=gsml:ShearDisplacementStructure response:\n"
                            + prettyString(doc));
        }

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

    /** test that geometries are correctly reprojected. */
    @Test
    public void testReprojection() {
        String path =
                "wfs?request=GetFeature&version=1.1.0&srsName=urn:x-ogc:def:crs:EPSG:4283&typename=gsml:MappedFeature&featureid=gsml.mappedfeature.191922";
        Document doc = getAsDOM(path);
        if (printDoc) {
            LOGGER.info(
                    "WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        }
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4283",
                "//gsml:MappedFeature/gsml:shape/gml:MultiSurface/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "2", "//gsml:MappedFeature/gsml:shape/gml:MultiSurface/@srsDimension", doc);

        String expected =
                "-37.391436383333335 144.782547975 -37.39050545 144.78374375833334 -37.38964884166667 144.78460036666667 -37.388678025 144.78522854166667 "
                        + "-37.38776430833333 144.78597093333335 -37.386736383333336 144.7864849 -37.38399524166667 144.788712075 -37.38199649166667 "
                        + "144.79002553333333 -37.38108278333333 144.790767925 -37.3787685 144.7914741 -37.37675986666667 144.79356964166666 "
                        + "-37.377616466666666 144.79436913333333 -37.378701508333336 144.79396939166668 -37.37972944166667 144.79339831666667 "
                        + "-37.380871575 144.79316989166668 -37.38201371666667 144.79311279166666 -37.38315585833333 144.79282725 -37.386011208333336 "
                        + "144.79088560833333 -37.387781525 144.78934370833332 -37.389951591666666 144.78843 -37.39092241666667 144.787801825 "
                        + "-37.39172191666667 144.78694521666668 -37.392178775 144.78586018333334 -37.392235875 144.78471804166668 -37.39172191666667 "
                        + "144.78369011666666 -37.391436383333335 144.782547975";
        String orig =
                evaluate(
                        "//gsml:MappedFeature/gsml:shape/gml:MultiSurface/gml:surfaceMember/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                        doc);
        assertTrue(isEqualGeometry(orig, expected, 5));
        path =
                "wfs?request=GetFeature&version=1.1.0&srsName=EPSG:4283&typename=gsml:MappedFeature&featureid=gsml.mappedfeature.191922";

        doc = getAsDOM(path);
        if (printDoc) {
            LOGGER.info(
                    "WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        }
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature/gsml:shape/gml:MultiSurface/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "2", "//gsml:MappedFeature/gsml:shape/gml:MultiSurface/@srsDimension", doc);

        expected =
                "144.782547975 -37.391436383333335 144.78374375833334 -37.39050545 144.78460036666667 -37.38964884166667 144.78522854166667 "
                        + "-37.388678025 144.78597093333335 -37.38776430833333 144.7864849 -37.386736383333336 144.788712075 -37.38399524166667 "
                        + "144.79002553333333 -37.38199649166667 144.790767925 -37.38108278333333 144.7914741 -37.3787685 144.79356964166666 "
                        + "-37.37675986666667 144.79436913333333 -37.377616466666666 144.79396939166668 -37.378701508333336 144.79339831666667 "
                        + "-37.37972944166667 144.79316989166668 -37.380871575 144.79311279166666 -37.38201371666667 144.79282725 -37.38315585833333 "
                        + "144.79088560833333 -37.386011208333336 144.78934370833332 -37.387781525 144.78843 -37.389951591666666 144.787801825 "
                        + "-37.39092241666667 144.78694521666668 -37.39172191666667 144.78586018333334 -37.392178775 144.78471804166668 -37.392235875 "
                        + "144.78369011666666 -37.39172191666667 144.782547975 -37.391436383333335";
        orig =
                evaluate(
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
        if (origCoordinates.length != expCoordinates.length) return false;

        for (int i = 0; i < origCoordinates.length; i++) {
            BigDecimal origBd = new BigDecimal(origCoordinates[i]);
            BigDecimal expBd = new BigDecimal(expCoordinates[i]);
            origBd = origBd.setScale(leniency, RoundingMode.HALF_UP);
            expBd = expBd.setScale(leniency, RoundingMode.HALF_UP);
            if (!origBd.equals(expBd)) {
                return false;
            }
        }
        return true;
    }

    /** validate the result returned by the given path. */
    @Test
    public void testValidate() {
        String path =
                "wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit&featureid=gsml.geologicunit.16777549126932776,gsml.geologicunit.167775491110573732,gsml.geologicunit.16777549126930540";
        validateGet(path);
    }

    /** Test if we can get gsml.geologicunit.16777549126932776 with a FeatureId fid filter. */
    @Test
    public void testGetFeatureWithFeatureIdFilter() {
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
                        + "            <ogc:FeatureId fid=\"gsml.geologicunit.16777549126932776\"/>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        if (printDoc) {
            LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        }
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        checkGU16777549126932776(doc);
    }

    /** Test if we can get gsml.geologicunit.16777549126932776 with a GmlObjectId gml:id filter. */
    @Test
    public void testGetFeatureWithGmlObjectIdFilter() {
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
                        + "            <ogc:GmlObjectId gml:id=\"gsml.geologicunit.16777549126932776\"/>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        if (printDoc) {
            LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        }
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        checkGU16777549126932776(doc);
    }

    /**
     * Making sure multi-valued attributes in nested features can be queried from the top level.
     * (GEOT-3156)
     */
    @Test
    public void testFilteringNestedMultiValuedAttribute() {
        // test PropertyIsEqual filter
        String xml =
                "<wfs:GetFeature "
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
                        + "            <ogc:PropertyIsEqualTo>"
                        + "                <ogc:Literal>Akuna Mudstone (Oba)</ogc:Literal>"
                        + "                <ogc:PropertyName>gml:name</ogc:PropertyName>"
                        + "            </ogc:PropertyIsEqualTo>"
                        + "        </ogc:Filter>"
                        + "    </wfs:Query> "
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        if (printDoc) {
            LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        }
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo(
                "gsml.geologicunit.167775491107848330", "//gsml:GeologicUnit/@gml:id", doc);

        // like filter
        xml =
                "<wfs:GetFeature "
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
                        + "            </ogc:PropertyIsLike>"
                        + "        </ogc:Filter>"
                        + "    </wfs:Query> "
                        + "</wfs:GetFeature>";

        doc = postAsDOM("wfs", xml);
        if (printDoc) {
            LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        }
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
        xml =
                "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:cdf=\"http://www.opengis.net/cite/data\" "
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
        if (printDoc) {
            LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        }
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

    /** Test if we can get gsml.geologicunit.16777549126930540 using its name. */
    @Test
    public void testGetFeaturePropertyFilter() {
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
                        + "                <ogc:PropertyName>gml:name</ogc:PropertyName>" //
                        + "                <ogc:Literal>Unnamed incised alluvium (Na)</ogc:Literal>" //
                        + "            </ogc:PropertyIsEqualTo>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        if (printDoc) {
            LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        }
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        checkGU16777549126930540(doc);
    }

    /** Test if we can filter on nested client properties */
    @Test
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
        if (printDoc) {
            LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        }
        assertXpathCount(6, "//gsml:GeologicUnit", doc);
        assertXpathCount(
                1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107848330']", doc);
        assertXpathCount(
                1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']", doc);
        assertXpathCount(
                1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']", doc);

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
        if (printDoc) {
            LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        }
        assertXpathCount(17, "//gsml:GeologicUnit", doc);

        assertXpathCount(
                1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107838594']", doc);
        assertXpathCount(
                1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107838781']", doc);
        assertXpathCount(
                1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107838810']", doc);
        assertXpathCount(
                1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107848330']", doc);
        assertXpathCount(
                1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513315832']", doc);
        assertXpathCount(
                1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']", doc);
        assertXpathCount(
                1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']", doc);

        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Proportion",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513315832']/gsml:composition/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "all",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513315832']/gsml:composition/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value",
                doc);
    }

    @Test
    public void testFilterOnPolymorphicFeatures() {
        String xml =
                "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\" xmlns:gsml=\""
                        + AbstractAppSchemaMockData.GSML_URI
                        + "\">"
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
                        + "    </ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        //
        Document doc = postAsDOM("wfs", xml);
        if (printDoc) {
            LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        }

        assertXpathEvaluatesTo(
                "900.0",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491110573732']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:uom:UCUM::SI",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericRange/gsml:upper/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
        assertXpathEvaluatesTo(
                "2700.0",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericRange/gsml:upper/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
    }

    @Test
    public void testResultHitsWithFilter() {
        String path = "wfs?request=GetFeature&version=1.1.0&typename=gsml:Contact&resultType=hits";
        Document doc = getAsDOM(path);
        if (printDoc) {
            LOGGER.info(prettyString(doc));
        }

        assertXpathEvaluatesTo("5", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        // test filter with maxFeature
        String xml =
                "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:cdf=\"http://www.opengis.net/cite/data\" "
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
                        + "    </ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        //
        doc = postAsDOM("wfs", xml);
        if (printDoc) {
            LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        }
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);

        // test filter without maxfeature
        xml =
                "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:cdf=\"http://www.opengis.net/cite/data\" "
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
                        + "    </ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        //
        doc = postAsDOM("wfs", xml);
        if (printDoc) {
            LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        }
        assertXpathEvaluatesTo("5", "/wfs:FeatureCollection/@numberOfFeatures", doc);
    }

    @Test
    public void testGeologicUnit() {
        String path =
                "wfs?request=GetFeature&version=1.1.0&typeName=gsml:GeologicUnit&featureid=gsml.geologicunit.16777549126930540,gsml.geologicunit.167775491107838881,gsml.geologicunit.16777549126931275,gsml.geologicunit.167775491233249211,gsml.geologicunit.1677754911513318041,gsml.geologicunit.167775491107843155,gsml.geologicunit.16777549126932958,gsml.geologicunit.16777549126932676,gsml.geologicunit.16777549126932776,gsml.geologicunit.167775491110573732,gsml.geologicunit.1677754911513320744";
        validateGet(path);
        Document doc = getAsDOM(path);
        if (printDoc) {
            LOGGER.info(prettyString(doc));
        }
        // test number of features
        assertXpathEvaluatesTo("11", "//wfs:FeatureCollection/@numberOfFeatures", doc);
        // tests geologicHistory
        assertXpathEvaluatesTo(
                "urn:cgi:classifier:ICS:StratChart:2008:Pleistocene",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126930541']/gsml:eventAge/gsml:CGI_TermRange/gsml:lower/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifier:ICS:StratChart:2008:Miocene",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126930541']/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "fluvial",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126930541']/gsml:eventEnvironment/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "channelled stream flow",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:geologicHistory/gsml:GeologicEvent[@gml:id='gsml.geologicevent.16777549126930541']/gsml:eventProcess/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:geologicHistory",
                doc);

        // test outcrop character
        assertXpathEvaluatesTo(
                "recessive",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107838881']/gsml:outcropCharacter/gsml:CGI_TermValue/gsml:value",
                doc);

        // test bodymorphology
        assertXpathEvaluatesTo(
                "dyke",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126931275']/gsml:bodyMorphology/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Morphology",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126931275']/gsml:bodyMorphology/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        // test composition
        assertXpathCount(
                2,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491233249211']/gsml:composition/gsml:CompositionPart",
                doc);
        assertXpathCount(
                2,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491233249211']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107855939']/gsml:part",
                doc);
        assertXpathEvaluatesTo(
                "indurated",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491233249211']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107855939']/gsml:consolidationDegree/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:ConsolidationDegree",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491233249211']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107855939']/gsml:consolidationDegree/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "indurated",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491233249211']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107855964']/gsml:consolidationDegree/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:ConsolidationDegree",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491233249211']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107855964']/gsml:consolidationDegree/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "indurated",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513318041']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107856083']/gsml:consolidationDegree/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:ConsolidationDegree",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513318041']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107856083']/gsml:consolidationDegree/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifier:CGI:SimpleLithology:2008:granodiorite",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513318041']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107856083']/gsml:lithology/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "all",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513318041']/gsml:composition/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Proportion",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513318041']/gsml:composition/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathCount(
                3,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107843155']/gsml:composition/gsml:CompositionPart",
                doc);
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107843155']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107858185']",
                doc);
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107843155']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107858173']",
                doc);
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107843155']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107858161']",
                doc);

        assertXpathEvaluatesTo(
                "urn:cgi:classifier:CGI:SimpleLithology:2008:shale",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107843155']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107858185']/gsml:lithology/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifier:CGI:SimpleLithology:2008:sandstone",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107843155']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107858173']/gsml:lithology/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifier:CGI:SimpleLithology:2008:mudstone",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107843155']/gsml:composition/gsml:CompositionPart/gsml:material/gsml:RockMaterial[@gml:id='gsml.rockmaterial.167775491107858161']/gsml:lithology/@xlink:href",
                doc);
        // test parts
        assertXpathCount(
                4,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932958']/gsml:part",
                doc);
        assertXpathEvaluatesTo(
                "http://urn.opengis.net/",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932958']/gsml:part/gsml:GeologicUnitPart/gsml:proportion/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        // test density
        assertXpathEvaluatesTo(
                "2.707",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932676']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:density/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:uom:UCUM:g.cm-3",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932676']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:density/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
        assertXpathEvaluatesTo(
                "2.114",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:density/gsml:CGI_NumericRange/gsml:lower/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "2.955",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932776']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:density/gsml:CGI_NumericRange/gsml:upper/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        // test magnetic susceptibility
        assertXpathEvaluatesTo(
                "900.0",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491110573732']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:uom:UCUM::SI",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491110573732']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
        assertXpathEvaluatesTo(
                "55.0",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513320744']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericRange/gsml:lower/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "230.0",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513320744']/gsml:physicalProperty/gsml:PhysicalDescription/gsml:magneticSusceptibility/gsml:CGI_NumericRange/gsml:upper/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        // test the rest of geologic unit
        assertXpathCount(
                2,
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107838881']/gml:name",
                doc);

        ArrayList<String> ls = new ArrayList<String>();
        ls.add(
                evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107838881']/gml:name[1]",
                        doc));
        ls.add(
                evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107838881']/gml:name[2]",
                        doc));
        assertTrue(ls.contains("Silverband Formation (Sks)"));
        assertTrue(ls.contains("urn:cgi:feature:GSV:GeologicUnit:167775491107838881"));
        ls.clear();
        ls.add(
                evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107838881']/gml:name[1]/@codeSpace",
                        doc));
        ls.add(
                evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107838881']/gml:name[2]/@codeSpace",
                        doc));
        assertTrue(ls.contains("http://www.dpi.vic.gov.au/earth-resources"));
        assertTrue(ls.contains("http://www.ietf.org/rfc/rfc2141"));

        assertXpathEvaluatesTo(
                "Intrusion [rank]",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513318041']/gsml:rank",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Lithodemic_IntrusiveUnitRank",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.1677754911513318041']/gsml:rank/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "typicalNorm",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126932676']/gsml:purpose",
                doc);
    }

    @Test
    public void testContact() {
        String path =
                "wfs?request=GetFeature&version=1.1.0&typeName=gsml:Contact&featureid=gsml.contact.46233,gsml.contact.46235";
        validateGet(path);
        Document doc = getAsDOM(path);
        if (printDoc) {
            LOGGER.info(prettyString(doc));
        }

        assertXpathEvaluatesTo("2", "//wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(
                153,
                "//gsml:Contact[@gml:id='gsml.contact.46233']/gsml:occurrence/gsml:MappedFeature",
                doc);
        assertXpathCount(
                1,
                "//gsml:Contact[@gml:id='gsml.contact.46233']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "http://urn.opengis.net/",
                "//gsml:Contact[@gml:id='gsml.contact.46233']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo(
                "urn:cgi:feature:GSV:Contact:46233",
                "//gsml:Contact[@gml:id='gsml.contact.46233']/gml:name",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifier:CGI:ContactType:200811:igneous_intrusive_contact",
                "//gsml:Contact[@gml:id='gsml.contact.46233']/gsml:contactType/@xlink:href",
                doc);

        assertXpathCount(
                3,
                "//gsml:Contact[@gml:id='gsml.contact.46235']/gsml:occurrence/gsml:MappedFeature",
                doc);
        assertXpathCount(
                1,
                "//gsml:Contact[@gml:id='gsml.contact.46235']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "http://urn.opengis.net/",
                "//gsml:Contact[@gml:id='gsml.contact.46235']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo(
                "urn:cgi:feature:GSV:Contact:46235",
                "//gsml:Contact[@gml:id='gsml.contact.46235']/gml:name",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifier:CGI:ContactType:200811:igneous_phase_contact",
                "//gsml:Contact[@gml:id='gsml.contact.46235']/gsml:contactType/@xlink:href",
                doc);
        assertXpathCount(
                1,
                "//gsml:Contact[@gml:id='gsml.contact.46235']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.182366']",
                doc);
        assertXpathCount(
                1,
                "//gsml:Contact[@gml:id='gsml.contact.46235']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.185193']",
                doc);
        assertXpathCount(
                1,
                "//gsml:Contact[@gml:id='gsml.contact.46235']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.185608']",
                doc);
    }

    @Test
    public void testShearDisplacement() {
        String path =
                "wfs?request=GetFeature&version=1.1.0&typeName=gsml:ShearDisplacementStructure&featureid=gsml.sheardisplacementstructure.46179,gsml.sheardisplacementstructure.46181,gsml.sheardisplacementstructure.46188,gsml.sheardisplacementstructure.46199,gsml.sheardisplacementstructure.46216";
        validateGet(path);
        Document doc = getAsDOM(path);
        if (printDoc) {
            LOGGER.info(prettyString(doc));
        }
        assertXpathEvaluatesTo("5", "//wfs:FeatureCollection/@numberOfFeatures", doc);

        assertXpathCount(
                12,
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46179']/gsml:occurrence/gsml:MappedFeature",
                doc);
        assertXpathCount(
                2,
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46181']/gsml:occurrence/gsml:MappedFeature",
                doc);
        assertXpathCount(
                1,
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46188']/gsml:occurrence/gsml:MappedFeature",
                doc);
        assertXpathCount(
                4,
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46199']/gsml:occurrence/gsml:MappedFeature",
                doc);
        assertXpathCount(
                1,
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46216']/gsml:occurrence/gsml:MappedFeature",
                doc);

        assertXpathEvaluatesTo(
                "urn:cgi:classifier:ICS:StratChart:2008:Holocene",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46179']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000003']/gsml:eventAge/gsml:CGI_TermRange/gsml:lower/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:ICS:StratChart:2008",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46179']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000003']/gsml:eventAge/gsml:CGI_TermRange/gsml:lower/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "reverse",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46179']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000003']/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:movementSense/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Provisional:MovementSense",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46179']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000003']/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:movementSense/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "dip-slip",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46179']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000003']/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:movementType/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Provisional:MovementType",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46179']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000003']/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:movementType/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "SSE",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46179']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000003']/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:hangingWallDirection/gsml:CGI_LinearOrientation/gsml:descriptiveOrientation/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:GmlCompassPointEnumeration",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46179']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000003']/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:hangingWallDirection/gsml:CGI_LinearOrientation/gsml:descriptiveOrientation/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathCount(
                1,
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46181']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.185969']",
                doc);
        assertXpathCount(
                1,
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46181']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.186045']",
                doc);

        assertXpathCount(
                1,
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46181']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000008']",
                doc);
        assertXpathCount(
                1,
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46181']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000007']",
                doc);

        assertXpathCount(
                10,
                "//gsml:ShearDisplacementStructure/gsml:geologicHistory/gsml:DisplacementEvent",
                doc);
        assertXpathEvaluatesTo(
                "typicalNorm",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46216']/gsml:purpose",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:CGI:EarthNaturalSurface",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46216']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.185911']/gsml:samplingFrame/@xlink:href",
                doc);
        assertXpathCount(
                2,
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46188']/gml:name",
                doc);

        ArrayList<String> ls = new ArrayList<String>();
        ls.add(
                evaluate(
                        "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46188']/gml:name[1]",
                        doc));
        ls.add(
                evaluate(
                        "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46188']/gml:name[2]",
                        doc));
        assertTrue(ls.contains("Castle Cove Fault"));
        assertTrue(ls.contains("urn:cgi:feature:GSV:ShearDisplacementStructure:46188"));

        ls.clear();
        ls.add(
                evaluate(
                        "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46188']/gml:name[1]/@codeSpace",
                        doc));
        ls.add(
                evaluate(
                        "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46188']/gml:name[2]/@codeSpace",
                        doc));
        assertTrue(ls.contains("http://www.dpi.vic.gov.au/earth-resources"));
        assertTrue(ls.contains("http://www.ietf.org/rfc/rfc2141"));

        assertXpathEvaluatesTo(
                "S",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46199']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000037']/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:hangingWallDirection/gsml:CGI_LinearOrientation/gsml:descriptiveOrientation/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:GmlCompassPointEnumeration",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46199']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000037']/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:hangingWallDirection/gsml:CGI_LinearOrientation/gsml:descriptiveOrientation/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "normal",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46199']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000037']/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:movementSense/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Provisional:MovementSense",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46199']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000037']/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:movementSense/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        assertXpathEvaluatesTo(
                "dip-slip",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46199']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000037']/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:movementType/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:Provisional:MovementType",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46199']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000037']/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:movementType/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifier:ICS:StratChart:2008:Permian",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46199']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000037']/gsml:eventAge/gsml:CGI_TermRange/gsml:lower/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:ICS:StratChart:2008",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46199']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000037']/gsml:eventAge/gsml:CGI_TermRange/gsml:lower/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifier:ICS:StratChart:2008:Permian",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46199']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000037']/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:ICS:StratChart:2008",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46199']/gsml:geologicHistory/gsml:DisplacementEvent[@gml:id='gsml.displacementevent.1000037']/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
    }

    @Test
    public void testMappedFeature() {
        String path =
                "wfs?request=GetFeature&version=1.1.0&typeName=gsml:MappedFeature&featureid=gsml.mappedfeature.195201,gsml.mappedfeature.192654,gsml.mappedfeature.191921,gsml.mappedfeature.179239,gsml.mappedfeature.185969,gsml.mappedfeature.186037,gsml.mappedfeature.185817,gsml.mappedfeature.185911,gsml.mappedfeature.178855,gsml.mappedfeature.185608";
        validateGet(path);
        Document doc = getAsDOM(path);
        if (printDoc) {
            LOGGER.info(prettyString(doc));
        }
        assertXpathEvaluatesTo("10", "//wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(10, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo(
                "previous mapping",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.179239']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:InterpretationMethod",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.179239']/gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "approximate",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.185608']/gsml:positionalAccuracy/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:classifierScheme:GSV:PositionalAccuracy",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.185608']/gsml:positionalAccuracy/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:CGI:EarthNaturalSurface",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.185911']/gsml:samplingFrame/@xlink:href",
                doc);

        ArrayList<String> specs = new ArrayList<String>();

        for (int i = 1; i <= 10; i++) {
            specs.add(
                    this.evaluate(
                            "/wfs:FeatureCollection/gml:featureMember["
                                    + i
                                    + "]/gsml:MappedFeature/gsml:specification/@xlink:href",
                            doc));
        }
        int[] countType = new int[3];
        for (String spec : specs) {
            if (spec.contains("ShearDisplacementStructure")) {
                countType[0]++;
            }
            if (spec.contains("GeologicUnit")) {
                countType[1]++;
            }
            if (spec.contains("Contact")) {
                countType[2]++;
            }
        }
        assertEquals(5, countType[0]);
        assertEquals(3, countType[1]);
        assertEquals(2, countType[2]);

        String expected =
                "-38.410785700000325 143.86545265833303 -38.40925703333365 143.86857949166634";
        String actual =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.179239']/gsml:shape/gml:MultiCurve/gml:curveMember/gml:LineString/gml:posList",
                        doc);
        assertTrue(this.isEqualGeometry(actual, expected, 5));

        expected = "-38.139133550000324 144.2364237333331 -38.13991570000029 144.2415325499997";
        actual =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.185969']/gsml:shape/gml:MultiCurve/gml:curveMember/gml:LineString/gml:posList",
                        doc);
        assertTrue(this.isEqualGeometry(actual, expected, 5));
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.192654']/gsml:shape/gml:MultiSurface/@srsDimension",
                doc);
        expected =
                "-37.469102166666666 143.76702998333334 -37.470454841666665 143.76797201666668 -37.4716163 "
                        + "143.76817698333332 -37.472777758333336 143.7679037 -37.473939216666665 143.76776705833333 -37.475100675 143.76797201666668 "
                        + "-37.475715566666665 143.76899683333335 -37.476877025 143.76927011666666 -37.478038483333336 143.76933844166666 "
                        + "-37.4790633 143.76988500833335 -37.480224766666666 143.76974836666668 -37.480293083333336 "
                        + "143.76858690833333 -37.480088125 143.76742545 -37.478516733333336 143.76571741666666 "
                        + "-37.479336591666666 143.76489756666666 -37.47954155 143.76257465 -37.480429725 143.76332618333333 "
                        + "-37.481591183333336 143.7633945 -37.482684325 143.76291625 -37.483367533333336 143.760729975 "
                        + "-37.4835725 143.75956851666666 -37.48364083333333 143.75840705833335 -37.483504175 143.7572456 "
                        + "-37.482889291666666 143.75622078333333 -37.48213775833334 143.75533260833333 -37.481864475 "
                        + "143.75417114166666 -37.4822744 143.751848225 -37.4822744 143.75068676666666 -37.48213775833334 "
                        + "143.74952530833335 -37.479814833333336 143.749252025 -37.479609875 143.74809055833333 -37.47954155 "
                        + "143.7469291 -37.47865338333333 143.74617756666666 -37.477560241666666 143.74583596666668 "
                        + "-37.47721863333334 143.744742825 -37.47721863333334 143.74358136666666 -37.47694535 143.74241990833335 "
                        + "-37.476193808333335 143.740233625 -37.476193808333335 143.73907216666666 -37.476398775 "
                        + "143.73791070833335 -37.4774236 143.73579275 -37.476877025 143.73476793333333 -37.4764671 143.7336748 "
                        + "-37.475783891666666 143.7327183 -37.474827391666665 143.730532025 -37.474622425 143.72937056666666 "
                        + "-37.474554108333336 143.72820910833335 -37.474622425 143.7265694 -37.473460966666664 143.7265694 "
                        + "-37.47236784166667 143.726979325 -37.471547975 143.727799175 -37.470386516666665 143.727799175 "
                        + "-37.469293375 143.72738925 -37.4684052 143.72663771666666 -37.468131916666664 143.72547625833334 "
                        + "-37.467312066666665 143.72445143333334 -37.466355566666664 143.72410983333333 -37.465262433333336 143.72438313333333 "
                        + "-37.464784183333336 143.72547625833334 -37.464647541666665 143.72663771666666 -37.464784183333336 143.727799175 -37.465262433333336 "
                        + "143.72889231666667 -37.465809 143.72991713333334 -37.466492208333335 143.73087363333335 -37.467380383333335 143.73169348333334 "
                        + "-37.469498341666664 143.734426325 -37.4703182 143.73531450833335 -37.470933083333335 143.736339325 -37.472299508333336 "
                        + "143.73825231666666 -37.473051041666665 143.73914049166666 -37.474075858333336 143.74125845 -37.474417466666665 "
                        + "143.74235158333335 -37.474485783333336 143.74351304166666 -37.474759075 143.74467450833333 -37.47544228333334 "
                        + "143.745631 -37.475783891666666 143.74672414166668 -37.47598885 143.7478856 -37.4764671 143.74897873333333 "
                        + "-37.476535425 143.75246311666666 -37.476125491666664 143.75355625833333 -37.4755106 143.754581075 -37.473597608333336 "
                        + "143.75594749166666 -37.472777758333336 143.75676735 -37.472299508333336 143.75786048333333 -37.47236784166667 "
                        + "143.76018340833332 -37.47209455 143.76134486666666 -37.469293375 143.76517085 -37.469020091666664 143.76633230833335 -37.469102166666666 143.76702998333334";
        actual =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.192654']/gsml:shape/gml:MultiSurface/gml:surfaceMember/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                        doc);
        assertTrue(this.isEqualGeometry(actual, expected, 5));
    }

    @Test
    public void testPredicates() {
        // predicates currently only works with complex post-filters used in joining
        if (AppSchemaDataAccessConfigurator.isJoining()) {

            // test with slash in predicate
            String xml = //
                    "<wfs:GetFeature " //
                            + "service=\"WFS\" " //
                            + "version=\"1.1.0\" " //
                            + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                            + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                            + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                            + "xmlns:gsml=\""
                            + AbstractAppSchemaMockData.GSML_URI
                            + "\" " //
                            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                            + "xsi:schemaLocation=\"" //
                            + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd " //
                            + AbstractAppSchemaMockData.GSML_URI
                            + " "
                            + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
                            + "\""
                            + ">" //
                            + "    <wfs:Query typeName=\"gsml:GeologicUnit\">" //
                            + "        <ogc:Filter>" //
                            + "            <ogc:PropertyIsEqualTo>" //
                            + "                <ogc:PropertyName>gsml:geologicHistory/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:lower/gsml:CGI_TermValue[gsml:value/@codeSpace='urn:cgi:classifierScheme:ICS:StratChart:2008']/gsml:value</ogc:PropertyName>" //
                            + "                <ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Devonian</ogc:Literal>" //
                            + "            </ogc:PropertyIsEqualTo>" //
                            + "        </ogc:Filter>" //
                            + "    </wfs:Query> " //
                            + "</wfs:GetFeature>";
            validate(xml);
            Document doc = postAsDOM("wfs", xml);
            if (printDoc) {
                LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
            }

            assertXpathCount(3, "//gsml:GeologicUnit", doc);
            assertXpathCount(
                    1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.167775491107843155']", doc);
            assertXpathCount(
                    1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126941436']", doc);
            assertXpathCount(
                    1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126942588']", doc);

            // test that it does *not* give the same result when changing the predicate
            xml = //
                    "<wfs:GetFeature " //
                            + "service=\"WFS\" " //
                            + "version=\"1.1.0\" " //
                            + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                            + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                            + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                            + "xmlns:gsml=\""
                            + AbstractAppSchemaMockData.GSML_URI
                            + "\" " //
                            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                            + "xsi:schemaLocation=\"" //
                            + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd " //
                            + AbstractAppSchemaMockData.GSML_URI
                            + " "
                            + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
                            + "\""
                            + ">" //
                            + "    <wfs:Query typeName=\"gsml:GeologicUnit\">" //
                            + "        <ogc:Filter>" //
                            + "            <ogc:PropertyIsEqualTo>" //
                            + "                <ogc:PropertyName>gsml:geologicHistory/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:lower/gsml:CGI_TermValue[gsml:value/@codeSpace='urn:cgi:classifierScheme:ICS:StratChart:2008z']/gsml:value</ogc:PropertyName>" //
                            + "                <ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Devonian</ogc:Literal>" //
                            + "            </ogc:PropertyIsEqualTo>" //
                            + "        </ogc:Filter>" //
                            + "    </wfs:Query> " //
                            + "</wfs:GetFeature>";
            validate(xml);
            doc = postAsDOM("wfs", xml);
            if (printDoc) {
                LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
            }

            assertXpathCount(0, "//gsml:GeologicUnit", doc);

            // tests use of predicate in one step path + also test an X-path function
            xml = //
                    "<wfs:GetFeature " //
                            + "service=\"WFS\" " //
                            + "version=\"1.1.0\" " //
                            + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                            + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                            + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                            + "xmlns:gsml=\""
                            + AbstractAppSchemaMockData.GSML_URI
                            + "\" " //
                            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                            + "xsi:schemaLocation=\"" //
                            + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd " //
                            + AbstractAppSchemaMockData.GSML_URI
                            + " "
                            + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
                            + "\""
                            + ">" //
                            + "    <wfs:Query typeName=\"gsml:GeologicUnit\">" //
                            + "        <ogc:Filter>" //
                            + "            <ogc:PropertyIsEqualTo>" //
                            + "                <ogc:PropertyName>gml:name[substring(@codeSpace,3)='tp://www.dpi.vic.gov.au/earth-resources']</ogc:PropertyName>" //
                            + "                <ogc:Literal>Nelson Creek Granite (G191)</ogc:Literal>" //
                            + "            </ogc:PropertyIsEqualTo>" //
                            + "        </ogc:Filter>" //
                            + "    </wfs:Query> " //
                            + "</wfs:GetFeature>";
            validate(xml);
            doc = postAsDOM("wfs", xml);
            if (printDoc) {
                LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
            }

            assertXpathCount(1, "//gsml:GeologicUnit", doc);
            assertXpathCount(
                    1, "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126942588']", doc);

            // test that it does *not* give the same result when changing the predicate
            xml = //
                    "<wfs:GetFeature " //
                            + "service=\"WFS\" " //
                            + "version=\"1.1.0\" " //
                            + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                            + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                            + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                            + "xmlns:gsml=\""
                            + AbstractAppSchemaMockData.GSML_URI
                            + "\" " //
                            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                            + "xsi:schemaLocation=\"" //
                            + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd " //
                            + AbstractAppSchemaMockData.GSML_URI
                            + " "
                            + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
                            + "\""
                            + ">" //
                            + "    <wfs:Query typeName=\"gsml:GeologicUnit\">" //
                            + "        <ogc:Filter>" //
                            + "            <ogc:PropertyIsEqualTo>" //
                            + "                <ogc:PropertyName>gml:name[substring(@codeSpace,2)='tp://www.dpi.vic.gov.au/earth-resources']</ogc:PropertyName>" //
                            + "                <ogc:Literal>Nelson Creek Granite (G191)</ogc:Literal>" //
                            + "            </ogc:PropertyIsEqualTo>" //
                            + "        </ogc:Filter>" //
                            + "    </wfs:Query> " //
                            + "</wfs:GetFeature>";
            validate(xml);
            doc = postAsDOM("wfs", xml);
            if (printDoc) {
                LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
            }

            assertXpathCount(0, "//gsml:GeologicUnit", doc);
        }
    }

    @Test
    public void testReprojectionInFeatureChaining() {

        String path =
                "wfs?request=GetFeature&srsName=urn:x-ogc:def:crs:EPSG:4326&version=1.1.0&typeName=gsml:GeologicUnit&featureid=gsml.geologicunit.16777549126930540";
        Document doc = getAsDOM(path);
        if (printDoc) {
            LOGGER.info(
                    "WFS GetFeature&typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        }

        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.191322']/gsml:shape/gml:MultiSurface/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.191322']/gsml:shape/gml:MultiSurface/@srsDimension",
                doc);

        String expected =
                "-36.91391191948047 144.0478089083333 -36.91535066114876 144.04748426666666 -36.915274969482006 144.04633634166666 -36.91504654448175"
                        + " 144.0451942 -36.91510365281515 144.04405205833336 -36.91510365281515 144.04290991666667 -36.91476101114809 144.04211040833334"
                        + " -36.91418048614744 144.04222736666668 -36.91336111114652 144.04308674166666 -36.912202486145226 144.04339155 -36.91174298614471"
                        + " 144.04447269166667 -36.91133763614426 144.04556750833333 -36.91071870281022 144.04663162500003 -36.910000569476075 144.047540275"
                        + " -36.90946055280882 144.048551125 -36.90943138614211 144.04873191666667 -36.909494336142174"
                        + " 144.04969045833334 -36.9095378778089 144.04975733333333 -36.910296377809736 144.050369725"
                        + " -36.91135182781094 144.04991965833332 -36.91220002781188 144.04912606666667 -36.913342494479835"
                        + " 144.04889391666663 -36.91391191948047 144.0478089083333";
        String orig =
                evaluate(
                        "//gsml:GeologicUnit[@gml:id='gsml.geologicunit.16777549126930540']/gsml:occurrence/gsml:MappedFeature[@gml:id='gsml.mappedfeature.191322']/gsml:shape/gml:MultiSurface/gml:surfaceMember/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                        doc);
        assertTrue(isEqualGeometry(orig, expected, 5));
    }

    @Test
    public void testFilteringSplit() throws Exception {
        if (AppSchemaDataAccessConfigurator.isJoining()) {
            // this is non joining test
            return;
        }
        FeatureSource<FeatureType, Feature> featureSource;
        try {
            Name gu = Types.typeName("urn:cgi:xmlns:CGI:GeoSciML:2.0", "GeologicUnit");
            featureSource = this.getFeatureSource(gu);
        } catch (IOException ioe) {
            featureSource = null;
        }
        assertNotNull(featureSource);
        List<Filter> filterList = new ArrayList<Filter>();
        FilterFactory2 ff =
                new FilterFactoryImplNamespaceAware(
                        ((MappingFeatureSource) featureSource).getMapping().getNamespaces());
        Expression property =
                ff.property(
                        "gsml:geologicHistory/gsml:GeologicEvent/gsml:eventEnvironment/gsml:CGI_TermValue/gsml:value");
        Filter filter = ff.like(property, "*mid-crustal*");

        filterList.add(filter);
        property = ff.property("gsml:purpose");
        filter = ff.like(property, "*ypical*");
        filterList.add(filter);
        Filter andFilter = ff.and(filterList);

        FeatureCollection<FeatureType, Feature> filteredResults =
                featureSource.getFeatures(andFilter);
        FeatureIterator<Feature> iterator = filteredResults.features();

        assertTrue(filteredResults instanceof MappingFeatureCollection);

        MappingFeatureCollection mfc = ((MappingFeatureCollection) filteredResults);
        Filter afterSplit = mfc.getQuery().getFilter();
        // this tests that after the split, only he LikeFilterImpl exist on the query as a pre
        // filter
        assertTrue(afterSplit instanceof org.geotools.filter.LikeFilterImpl);
        // Below ensures that the right filter exist on the query for pre processing on the
        // database. The LikeFilter with the nested attribute should be post processed as at that
        // current point in time the property has not been mapped and is unable to be filtered on
        // the database whereas gsml:purpose is straight forward and can be pre processed on the
        // database
        assertTrue(
                "*ypical*".equals(((org.geotools.filter.LikeFilterImpl) afterSplit).getLiteral()));
        ArrayList<String> ids = new ArrayList<String>();
        while (iterator.hasNext()) {
            ids.add(iterator.next().getIdentifier().toString());
        }
        assertEquals(3, ids.size());
        assertTrue(ids.contains("gsml.geologicunit.1677754911513315832"));
        assertTrue(ids.contains("gsml.geologicunit.16777549126941084"));
        assertTrue(ids.contains("gsml.geologicunit.16777549126942588"));
    }

    public FeatureSource<FeatureType, Feature> getFeatureSource(Name feature) throws IOException {
        DataAccess<FeatureType, Feature> mfDataAccess =
                AppSchemaDataAccessRegistry.getDataAccess(feature);
        return (FeatureSource) mfDataAccess.getFeatureSource(feature);
    }
}
