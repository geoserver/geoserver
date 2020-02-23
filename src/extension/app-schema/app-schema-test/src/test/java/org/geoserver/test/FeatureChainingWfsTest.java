/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.geoserver.test.AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL;
import static org.geoserver.test.AbstractAppSchemaMockData.GSML_URI;
import static org.geoserver.test.FeatureChainingMockData.EX_URI;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geotools.appschema.filter.FilterFactoryImplNamespaceAware;
import org.geotools.appschema.jdbc.NestedFilterToSQL;
import org.geotools.data.FeatureSource;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.AppSchemaDataAccessRegistry;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.complex.filter.ComplexFilterSplitter;
import org.geotools.data.jdbc.FilterToSQLException;
import org.geotools.data.util.NullProgressListener;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.util.URLs;
import org.junit.Test;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsLike;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * WFS GetFeature to test integration of {@link AppSchemaDataAccess} with GeoServer.
 *
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 * @author Rini Angreani, CSIRO Earth Science and Resource Engineering
 * @author Xiangtan Lin, CSIRO Information Management and Technology
 */
public class FeatureChainingWfsTest extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureChainingMockData createTestData() {
        return new FeatureChainingMockData();
    }

    public static final String GETFEATURE_ATTRIBUTES =
            "service=\"WFS\" " //
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
                    + "\""; // end of schemaLocation

    /** Return the root of the test fixture data directory. */
    private File getDataDir() {
        return getTestData().getDataDirectoryRoot();
    }

    /** Return first ex schema file. */
    private File getExSchemaOne() {
        return findFile("featureTypes/ex_FirstParentFeature/simpleContent.xsd", getDataDir());
    }

    /** Return first ex schema location. */
    private String getExSchemaOneLocation() {
        return URLs.fileToUrl(getExSchemaOne()).toString();
    }

    /** Return second ex schema file. */
    private File getExSchemaTwo() {
        return findFile("featureTypes/ex_SecondParentFeature/simpleContent.xsd", getDataDir());
    }

    /** Return second ex schema location. */
    private String getExSchemaTwoLocation() {
        return URLs.fileToUrl(getExSchemaTwo()).toString();
    }

    /** Return third ex schema file. */
    private File getExSchemaThree() {
        return findFile("featureTypes/ex_ParentFeature/NonValidNestedGML.xsd", getDataDir());
    }

    /** Return third ex schema location. */
    private String getExSchemaThreeLocation() {
        return URLs.fileToUrl(getExSchemaThree()).toString();
    }

    /** Test that ex schemas are found and the files exist. */
    @Test
    public void testExSchemas() {
        assertNotNull(getExSchemaOne());
        assertTrue(getExSchemaOne().exists());
        assertNotNull(getExSchemaTwo());
        assertTrue(getExSchemaTwo().exists());
    }

    /** Test whether GetCapabilities returns wfs:WFS_Capabilities. */
    @Test
    public void testGetCapabilities() {
        Document doc = getAsDOM("wfs?request=GetCapabilities&version=1.1.0");
        LOGGER.info("WFS GetCapabilities response:\n" + prettyString(doc));
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement().getNodeName());

        // check wfs schema location is canonical
        String schemaLocation = evaluate("wfs:WFS_Capabilities/@xsi:schemaLocation", doc);
        String location = "http://www.opengis.net/wfs " + WFS.CANONICAL_SCHEMA_LOCATION;
        assertEquals(location, schemaLocation);

        // make sure non-feature types don't appear in FeatureTypeList
        assertXpathCount(6, "//wfs:FeatureType", doc);
        ArrayList<String> featureTypeNames = new ArrayList<String>(6);
        featureTypeNames.add(evaluate("//wfs:FeatureType[1]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[2]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[3]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[4]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[5]/wfs:Name", doc));
        featureTypeNames.add(evaluate("//wfs:FeatureType[6]/wfs:Name", doc));
        // Mapped Feture
        assertTrue(featureTypeNames.contains("gsml:MappedFeature"));
        // Geologic Unit
        assertTrue(featureTypeNames.contains("gsml:GeologicUnit"));
        // FirstParentFeature
        assertTrue(featureTypeNames.contains("ex:FirstParentFeature"));
        // SecondParentFeature
        assertTrue(featureTypeNames.contains("ex:SecondParentFeature"));
        // ParentFeature
        assertTrue(featureTypeNames.contains("ex:ParentFeature"));
        // om:Observation
        assertTrue(featureTypeNames.contains("om:Observation"));
    }

    /** Test DescribeFeatureType response for gsml:MappedFeature. */
    @Test
    public void testDescribeFeatureTypeMappedFeature() {
        Document doc =
                getAsDOM(
                        "wfs?request=DescribeFeatureType&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info(
                "WFS DescribeFeatureType, typename=gsml:MappedFeature response:\n"
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
        assertXpathEvaluatesTo(
                AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL,
                "//xsd:include/@schemaLocation",
                doc);
        // nothing else
        assertXpathCount(0, "//xsd:complexType", doc);
        assertXpathCount(0, "//xsd:element", doc);
    }

    /** Test DescribeFeatureType response for gsml:GeologicUnit. */
    @Test
    public void testDescribeFeatureTypeGeologicUnit() {
        Document doc =
                getAsDOM(
                        "wfs?request=DescribeFeatureType&version=1.1.0&typename=gsml:GeologicUnit");
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
    }

    /**
     * Test DescribeFeatureType response for ex:FirstParentFeature and ex:SecondParentFeature, which
     * have two schemas in the same namespace.
     */
    @Test
    public void testDescribeFeatureTypeTwoSchemasSameNamespace() {
        Document doc =
                getAsDOM(
                        "wfs?request=DescribeFeatureType&version=1.1.0&typeName=ex:FirstParentFeature,ex:SecondParentFeature");
        LOGGER.info(
                "WFS DescribeFeatureType, typename=ex:FirstParentFeature,"
                        + "ex:SecondParentFeature response:\n"
                        + prettyString(doc));
        assertXpathEvaluatesTo(FeatureChainingMockData.EX_URI, "//@targetNamespace", doc);
        assertXpathCount(1, "//xsd:include", doc);
        assertXpathCount(0, "//xsd:import", doc);
        // EX include
        String schemaLocation = evaluate("//xsd:include/@schemaLocation", doc);
        if (!schemaLocation.equals(getExSchemaOneLocation())) {
            assertEquals(getExSchemaTwoLocation(), schemaLocation);
        }
        // nothing else
        assertXpathCount(0, "//xsd:complexType", doc);
        assertXpathCount(0, "//xsd:element", doc);
    }

    /**
     * Test DescribeFeatureType response for om:Observation, which has 2 schemaUris specified in the
     * mapping file. Both must appear.
     */
    @Test
    public void testDescribeFeatureTypeObservation() {
        Document doc =
                getAsDOM("wfs?request=DescribeFeatureType&version=1.1.0&typename=om:Observation");
        LOGGER.info(
                "WFS DescribeFeatureType, typename=om:Observation response:\n" + prettyString(doc));
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo(FeatureChainingMockData.OM_URI, "//@targetNamespace", doc);
        assertXpathCount(1, "//xsd:include", doc);
        assertXpathCount(1, "//xsd:import", doc);
        // GSML schemaLocation as xsd:import because the namespace is different
        assertXpathEvaluatesTo(AbstractAppSchemaMockData.GSML_URI, "//xsd:import/@namespace", doc);
        assertXpathEvaluatesTo(
                AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL,
                "//xsd:import/@schemaLocation",
                doc);
        // OM schemaLocation as xsd:include
        assertXpathEvaluatesTo(
                FeatureChainingMockData.OM_SCHEMA_LOCATION_URL,
                "//xsd:include/@schemaLocation",
                doc);
        // nothing else
        assertXpathCount(0, "//xsd:complexType", doc);
        assertXpathCount(0, "//xsd:element", doc);
    }

    /** Test DescribeFeatureType response for mixed namespaces. */
    @Test
    public void testDescribeFeatureTypeMixedNamespaces() {
        Document doc =
                getAsDOM(
                        "wfs?request=DescribeFeatureType&version=1.1.0&typeName=gsml:MappedFeature,ex:FirstParentFeature");
        LOGGER.info(
                "WFS DescribeFeatureType, typename=gsml:MappedFeature,ex:FirstParentFeature response:\n"
                        + prettyString(doc));
        checkGsmlExDescribeFeatureType(doc);
    }

    /** Test DescribeFeatureType response for many types. */
    @Test
    public void testDescribeFeatureTypeManyTypes() {
        Document doc =
                getAsDOM(
                        "wfs?request=DescribeFeatureType&version=1.1.0&typeName=gsml:MappedFeature,gsml:GeologicUnit,ex:FirstParentFeature,ex:SecondParentFeature");
        LOGGER.info(
                "WFS DescribeFeatureType, typename=gsml:MappedFeature,gsml:GeologicUnit,ex:FirstParentFeature,ex:SecondParentFeature response:\n"
                        + prettyString(doc));
        checkGsmlExDescribeFeatureType(doc);
    }

    private void checkGsmlExDescribeFeatureType(Document doc) {
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo(FeatureChainingMockData.GSML_URI, "//@targetNamespace", doc);
        assertXpathCount(1, "//xsd:include", doc);
        assertXpathCount(1, "//xsd:import", doc);
        // gsml schemaLocation as xsd:include
        assertXpathEvaluatesTo(
                FeatureChainingMockData.GSML_SCHEMA_LOCATION_URL,
                "//xsd:include/@schemaLocation",
                doc);
        // ex schemaLocation as xsd:import because the namespace is different
        // Only one ex schema can be imported.
        // The other ex schema is ignored (a warning is logged).
        // The reason for this is that the effect of importing multiple schemas with
        // the same target namespace is undefined, so FeatureTypeSchemaBuilder does
        // not do it.
        assertXpathEvaluatesTo(FeatureChainingMockData.EX_URI, "//xsd:import/@namespace", doc);
        String schemaLocation = evaluate("//xsd:import/@schemaLocation", doc);
        assertTrue(
                schemaLocation.equals(getExSchemaOneLocation())
                        || schemaLocation.equals(getExSchemaTwoLocation()));
        // nothing else
        assertXpathCount(0, "//xsd:complexType", doc);
        assertXpathCount(0, "//xsd:element", doc);
    }

    /** Test DescribeFeatureType response when no types are specified. */
    @Test
    public void testDescribeFeatureTypeNoTypes() {
        Document doc = getAsDOM("wfs?request=DescribeFeatureType&version=1.1.0");
        LOGGER.info("WFS DescribeFeatureType response:\n" + prettyString(doc));
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        String targetNamespace = evaluate("//@targetNamespace", doc);
        assertFalse(targetNamespace.isEmpty());
        int numberOfImports = getMatchingNodes("//xsd:import", doc).getLength();
        int numberOfIncludes = getMatchingNodes("//xsd:include", doc).getLength();

        ArrayList<String> namespaces = new ArrayList<String>();
        namespaces.add(AbstractAppSchemaMockData.GSML_URI);
        namespaces.add(FeatureChainingMockData.OM_URI);
        namespaces.add(FeatureChainingMockData.EX_URI);
        // targetNamespace depends on load order which is platform dependent
        if (targetNamespace.equals(FeatureChainingMockData.EX_URI)) {
            assertEquals(2, numberOfImports);
            assertEquals(3, numberOfIncludes);
            @SuppressWarnings("serial")
            Set<String> expectedExSchemaLocations =
                    new HashSet<String>() {
                        {
                            add(getExSchemaOneLocation());
                            add(getExSchemaTwoLocation());
                            add(getExSchemaThreeLocation());
                        }
                    };
            // ensure expected schemaLocations are distinct
            assertEquals(numberOfIncludes, expectedExSchemaLocations.size());
            // check that found schemaLocations are as expected
            Set<String> foundExSchemaLocations = new HashSet<String>();
            for (int i = 1; i <= numberOfIncludes; i++) {
                foundExSchemaLocations.add(
                        evaluate("//xsd:include[" + i + "]/@schemaLocation", doc));
            }
            assertEquals(expectedExSchemaLocations, foundExSchemaLocations);
            // ensure that this namespace is not used for imports in later asserts
            namespaces.remove(FeatureChainingMockData.EX_URI);
        } else {
            // If the targetNamespace is not the ex namespace, only one ex schema can be imported.
            // The other ex schema is ignored (a warning is logged).
            // The reason for this is that the effect of importing multiple schemas with
            // the same target namespace is undefined, so FeatureTypeSchemaBuilder does
            // not do it.
            assertEquals(2, numberOfImports);
            assertEquals(1, numberOfIncludes);

            String schemaLocation = "//xsd:include[" + 1 + "]/@schemaLocation";
            if (targetNamespace.equals(AbstractAppSchemaMockData.GSML_URI)) {
                // gsml include
                assertXpathEvaluatesTo(
                        AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL, schemaLocation, doc);
                namespaces.remove(AbstractAppSchemaMockData.GSML_URI);
            } else {
                // om include
                assertEquals(FeatureChainingMockData.OM_URI, targetNamespace);
                assertXpathEvaluatesTo(
                        FeatureChainingMockData.OM_SCHEMA_LOCATION_URL, schemaLocation, doc);
                namespaces.remove(FeatureChainingMockData.OM_URI);
            }
        }

        // order is unimportant, and could change, so we don't test the order
        for (int i = 1; i <= numberOfImports; i++) {
            String namespace = evaluate("//xsd:import[" + i + "]/@namespace", doc);
            String schemaLocation = "//xsd:import[" + i + "]/@schemaLocation";
            if (namespace.equals(AbstractAppSchemaMockData.GSML_URI)) {
                // gsml import
                assertXpathEvaluatesTo(
                        AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL, schemaLocation, doc);
                namespaces.remove(AbstractAppSchemaMockData.GSML_URI);
            } else if (namespace.equals(FeatureChainingMockData.EX_URI)) {
                // ex import
                String loc = evaluate(schemaLocation, doc);
                assertTrue(
                        loc.equals(getExSchemaOneLocation())
                                || loc.equals(getExSchemaTwoLocation())
                                || loc.equals(getExSchemaThreeLocation()));
                namespaces.remove(FeatureChainingMockData.EX_URI);
            } else {
                // om import
                assertEquals(FeatureChainingMockData.OM_URI, namespace);
                assertXpathEvaluatesTo(
                        FeatureChainingMockData.OM_SCHEMA_LOCATION_URL, schemaLocation, doc);
                namespaces.remove(FeatureChainingMockData.OM_URI);
            }
        }
        // ensure there are no unexpected references
        assertTrue(namespaces.isEmpty());
        // nothing else
        assertXpathCount(0, "//xsd:complexType", doc);
        assertXpathCount(0, "//xsd:element", doc);
    }

    /**
     * Tests that WFS schema is never imported in a DescribeFeatureType response.
     *
     * <p><strong>Remarks:</strong> this test only targets WFS 2.0, as 1.1.0 is sufficiently covered
     * by other tests.
     */
    @Test
    public void testDescribeFeatureTypeNoWfsSchemaImport() {
        // one feature type --> schema is included, no imports
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typenames=gsml:GeologicUnit");
        assertXpathCount(0, "//xsd:import", doc);
        assertXpathCount(1, "//xsd:include", doc);
        assertImportNotExists(doc, WFS.NAMESPACE);
        assertIncludeExists(doc, GSML_SCHEMA_LOCATION_URL);

        // two feature types, same schema / namespace --> schema is included once, no imports
        doc =
                getAsDOM(
                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typenames=gsml:GeologicUnit,gsml:MappedFeature");
        assertXpathCount(0, "//xsd:import", doc);
        assertXpathCount(0, "//xsd:import[@namespace='" + WFS.NAMESPACE + "']", doc);
        assertImportNotExists(doc, WFS.NAMESPACE);
        assertIncludeExists(doc, GSML_SCHEMA_LOCATION_URL);

        // two feature types, different schemas / namespaces --> first schema is included, second is
        // imported
        doc =
                getAsDOM(
                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typenames=gsml:GeologicUnit,ex:FirstParentFeature");
        // target namespace is set to the namespace of the first requested feature type
        assertXpathEvaluatesTo(GSML_URI, "//@targetNamespace", doc);
        assertXpathCount(1, "//xsd:import", doc);
        assertXpathCount(1, "//xsd:include", doc);
        assertImportNotExists(doc, WFS.NAMESPACE);
        // the schema of the feature type whose namespace is equal to the target namespace is
        // included
        assertIncludeExists(doc, GSML_SCHEMA_LOCATION_URL);
        // the other schema is imported
        assertImportExists(doc, EX_URI);
        assertXpathEvaluatesTo(getExSchemaOneLocation(), "//xsd:import/@schemaLocation", doc);

        // same thing, reverse order of the requested types
        doc =
                getAsDOM(
                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typenames=ex:FirstParentFeature,gsml:GeologicUnit");
        // target namespace is set to the namespace of the first requested feature type
        assertXpathEvaluatesTo(EX_URI, "//@targetNamespace", doc);
        assertXpathCount(1, "//xsd:import", doc);
        assertXpathCount(1, "//xsd:include", doc);
        assertImportNotExists(doc, WFS.NAMESPACE);
        // the schema of the feature type whose namespace is equal to the target namespace is
        // included
        assertIncludeExists(doc, getExSchemaOneLocation());
        // the other schema is imported
        assertImportExists(doc, GSML_URI);
        assertXpathEvaluatesTo(GSML_SCHEMA_LOCATION_URL, "//xsd:import/@schemaLocation", doc);
    }

    /** Test whether GetFeature returns wfs:FeatureCollection. */
    @Test
    public void testGetFeatureGML() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        // non-feature type should return nothing/exception
        doc = getAsDOM("wfs?request=GetFeature&typename=gsml:CompositionPart");
        LOGGER.info(
                "WFS GetFeature&typename=gsml:CompositionPart response, exception expected:\n"
                        + prettyString(doc));
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testGetFeatureJSON() throws Exception {
        JSON json =
                getAsJSON(
                        "wfs?request=GetFeature&version=1.1"
                                + ".0&typename=gsml:GeologicUnit&outputFormat=application/json&featureId=gu.25678");
        print(json);
        JSONObject properties = getFeaturePropertiesById(json, "gu.25678");
        assertNotNull(properties);
        // make sure these are not encoded as GeoJSON features even if they are GeoTools Feature
        // objects
        JSONArray colors = properties.getJSONArray("exposureColor");
        assertNotNull(colors);
        JSONObject color = colors.getJSONObject(0);
        // no top level feature elements
        assertFalse(color.has("type"));
        assertFalse(color.has("geometry"));
        assertFalse(color.has("properties"));
        // but value and codespace right in instead
        color = color.getJSONObject("value");
        assertThat(color.getString("value"), anyOf(is("Blue"), is("Yellow")));
        assertThat(color.getString("@codeSpace"), is("some:uri"));
    }

    @Test
    public void testGetFeatureValid() {
        String path = "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature";
        String newline = System.getProperty("line.separator");
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :" + newline + prettyString(doc));
        validateGet(path);
    }

    /**
     * GeologicUnit mapping has mappingName specified, to override targetElementName when feature
     * chained to MappedFeature. This is to test that querying GeologicUnit as top level feature
     * still works, when its real type name is specified in the query.
     */
    @Test
    public void testGetFeatureWithMappingName() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit");
        LOGGER.info("WFS GetFeature&typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("3", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(3, "//gsml:GeologicUnit", doc);
    }

    /**
     * Test nesting features of complex types with simple content. Previously the nested features
     * attributes weren't encoded, so this is to ensure that this works. This also tests that a
     * feature type can have multiple FEATURE_LINK to be referred by different types.
     */
    @Test
    public void testComplexTypeWithSimpleContentGML() {
        Document doc =
                getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=ex:FirstParentFeature");
        LOGGER.info(
                "WFS GetFeature&typename=ex:FirstParentFeature response:\n" + prettyString(doc));
        assertXpathCount(5, "//ex:FirstParentFeature", doc);

        // cc.1
        assertXpathCount(2, "//ex:FirstParentFeature[@gml:id='cc.1']/ex:nestedFeature", doc);
        assertXpathEvaluatesTo(
                "string_one",
                "//ex:FirstParentFeature[@gml:id='cc.1']/ex:nestedFeature[1]/ex:SimpleContent/ex:someAttribute",
                doc);
        assertXpathEvaluatesTo(
                "string_two",
                "//ex:FirstParentFeature[@gml:id='cc.1']/ex:nestedFeature[2]/ex:SimpleContent/ex:someAttribute",
                doc);
        assertXpathCount(
                0,
                "//ex:FirstParentFeature[@gml:id='cc.1']/ex:nestedFeature[2]/ex:SimpleContent/FEATURE_LINK",
                doc);
        // cc.2
        assertXpathCount(0, "//ex:FirstParentFeature[@gml:id='cc.2']/ex:nestedFeature", doc);

        doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=ex:SecondParentFeature");
        LOGGER.info(
                "WFS GetFeature&typename=ex:SecondParentFeature response:\n" + prettyString(doc));
        assertXpathCount(5, "//ex:SecondParentFeature", doc);

        // cc.1
        assertXpathCount(0, "//ex:SecondParentFeature[@gml:id='cc.1']/ex:nestedFeature", doc);
        // cc.2
        assertXpathCount(3, "//ex:SecondParentFeature[@gml:id='cc.2']/ex:nestedFeature", doc);
        assertXpathEvaluatesTo(
                "string_one",
                "//ex:SecondParentFeature[@gml:id='cc.2']/ex:nestedFeature[1]/ex:SimpleContent/ex:someAttribute",
                doc);
        assertXpathEvaluatesTo(
                "string_two",
                "//ex:SecondParentFeature[@gml:id='cc.2']/ex:nestedFeature[2]/ex:SimpleContent/ex:someAttribute",
                doc);
        assertXpathEvaluatesTo(
                "string_three",
                "//ex:SecondParentFeature[@gml:id='cc.2']/ex:nestedFeature[3]/ex:SimpleContent/ex:someAttribute",
                doc);
    }

    @Test
    public void testComplexTypeWithSimpleContentJSON() throws Exception {
        testJsonRequest("ex:FirstParentFeature", "/test-data/FirstParentFeature.json");
    }

    /** Test content of GetFeature response. */
    @Test
    public void testGetFeatureContent() throws Exception {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("4", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(4, "//gsml:MappedFeature", doc);

        checkSchemaLocation(doc);

        // mf1
        {
            String id = "mf1";
            assertXpathEvaluatesTo(id, "(//gsml:MappedFeature)[1]/@gml:id", doc);
            checkMf1Content(id, doc);
        }

        // mf2
        {
            String id = "mf2";
            assertXpathEvaluatesTo(id, "(//gsml:MappedFeature)[2]/@gml:id", doc);
            checkMf2Content(id, doc);
        }

        // mf3
        {
            String id = "mf3";
            assertXpathEvaluatesTo(id, "(//gsml:MappedFeature)[3]/@gml:id", doc);
            checkMf3Content(id, doc);
        }

        // mf4
        {
            String id = "mf4";
            assertXpathEvaluatesTo(id, "(//gsml:MappedFeature)[4]/@gml:id", doc);
            checkMf4Content(id, doc);
        }

        // check for duplicate gml:id
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25678']", doc);
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

    /** Check mf1 content are encoded correctly */
    private void checkMf1Content(String id, Document doc) {
        assertXpathEvaluatesTo(
                "GUNTHORPE FORMATION", "//gsml:MappedFeature[@gml:id='" + id + "']/gml:name", doc);
        // positionalAccuracy
        assertXpathEvaluatesTo(
                "200.0",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:uom:UCUM:m",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
        // shape
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.2 52.6 -1.2 52.6 -1.1 52.5 -1.1 52.5 -1.2",
                "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape//gml:posList",
                doc);
        // specification gu.25699
        assertXpathEvaluatesTo(
                "gu.25699",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
        // description
        assertXpathEvaluatesTo(
                "Olivine basalt, tuff, microgabbro, minor sedimentary rocks",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:description",
                doc);
        // name
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:name",
                doc);
        assertXpathEvaluatesTo(
                "Yaugher Volcanic Group",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:name[@codeSpace='urn:ietf:rfc:2141']",
                doc);
        // multi-valued leaf attributes that are feature chained come in random order
        // when joining is used
        List<String> names = new ArrayList<String>();
        names.add("Yaugher Volcanic Group");
        names.add("-Py");
        String name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='"
                                + id
                                + "']/gsml:specification/gsml:GeologicUnit/gml:name[1]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='"
                                + id
                                + "']/gsml:specification/gsml:GeologicUnit/gml:name[2]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        assertTrue(names.isEmpty());

        // feature link shouldn't appear as it's not in the schema
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/FEATURE_LINK",
                doc);
        // occurrence
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence",
                doc);
        assertXpathEvaluatesTo(
                "",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence[1]",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf1",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence/@xlink:href",
                doc);
        // exposureColor
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:exposureColor",
                doc);
        assertXpathEvaluatesTo(
                "Blue",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:exposureColor"
                        + "/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "some:uri",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:exposureColor/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        // feature link shouldn't appear as it's not in the schema
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:exposureColor"
                        + "/gsml:CGI_TermValue/FEATURE_LINK",
                doc);
        // outcropCharacter
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:outcropCharacter",
                doc);
        assertXpathEvaluatesTo(
                "x",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:outcropCharacter"
                        + "/gsml:CGI_TermValue/gsml:value",
                doc);
        // feature link shouldn't appear as it's not in the schema
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:outcropCharacter"
                        + "/gsml:CGI_TermValue/FEATURE_LINK",
                doc);
        // composition
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:composition",
                doc);
        assertXpathEvaluatesTo(
                "nonexistent",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition"
                        + "/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "fictitious component",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition"
                        + "/gsml:CompositionPart/gsml:role",
                doc);
        // feature link shouldn't appear as it's not in the schema
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition"
                        + "/gsml:CompositionPart/gsml:role/FEATURE_LINK",
                doc);
        // lithology
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology",
                doc);
        // feature link shouldn't appear as it's not in the schema
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition"
                        + "/gsml:CompositionPart/gsml:lithology/FEATURE_LINK",
                doc);
    }

    /** Check mf2 content are encoded correctly */
    private void checkMf2Content(String id, Document doc) {
        assertXpathEvaluatesTo(
                "MERCIA MUDSTONE GROUP",
                "//gsml:MappedFeature[@gml:id='" + id + "']/gml:name",
                doc);
        // positionalAccuracy
        assertXpathEvaluatesTo(
                "100.0",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:uom:UCUM:m",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
        // shape
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.3 52.6 -1.3 52.6 -1.2 52.5 -1.2 52.5 -1.3",
                "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape//gml:posList",
                doc);
        // gu.25678
        assertXpathEvaluatesTo(
                "gu.25678",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
        // name
        assertXpathCount(
                3,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:name",
                doc);
        // multi-valued leaf attributes that are feature chained come in random order
        // when joining is used
        HashMap<String, String> names = new HashMap<String, String>();
        names.put("Yaugher Volcanic Group 1", "urn:ietf:rfc:2141");
        names.put("Yaugher Volcanic Group 2", "urn:ietf:rfc:2141");
        names.put("-Py", "");
        String name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='"
                                + id
                                + "']/gsml:specification/gsml:GeologicUnit/gml:name[1]",
                        doc);
        assertTrue(names.containsKey(name));
        assertXpathEvaluatesTo(
                names.get(name),
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gml:name[1]/@codeSpace",
                doc);
        names.remove(name);

        name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='"
                                + id
                                + "']/gsml:specification/gsml:GeologicUnit/gml:name[2]",
                        doc);
        assertTrue(names.containsKey(name));
        assertXpathEvaluatesTo(
                names.get(name),
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gml:name[2]/@codeSpace",
                doc);
        names.remove(name);

        name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='"
                                + id
                                + "']/gsml:specification/gsml:GeologicUnit/gml:name[3]",
                        doc);
        assertTrue(names.containsKey(name));
        assertXpathEvaluatesTo(
                names.get(name),
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gml:name[3]/@codeSpace",
                doc);
        names.remove(name);
        assertTrue(names.isEmpty());

        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/FEATURE_LINK",
                doc);
        // occurrence
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence",
                doc);
        assertXpathEvaluatesTo(
                "",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence[1]",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf2",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence[1]/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence[2]",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf3",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence[2]/@xlink:href",
                doc);
        // description
        assertXpathEvaluatesTo(
                "Olivine basalt, tuff, microgabbro, minor sedimentary rocks",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:description",
                doc);
        // exposureColor
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:exposureColor",
                doc);
        assertXpathEvaluatesTo(
                "Blue",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:exposureColor[1]"
                        + "/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "some:uri",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:exposureColor[1]/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "Yellow",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:exposureColor[2]"
                        + "/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "some:uri",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:exposureColor[2]/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:exposureColor"
                        + "/gsml:CGI_TermValue/FEATURE_LINK",
                doc);
        // outcropCharacter
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:outcropCharacter",
                doc);
        assertXpathEvaluatesTo(
                "x",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:outcropCharacter[1]"
                        + "/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "y",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:outcropCharacter[2]"
                        + "/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:outcropCharacter"
                        + "/gsml:CGI_TermValue/FEATURE_LINK",
                doc);
        // composition
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:composition",
                doc);
        assertXpathEvaluatesTo(
                "significant",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition[1]"
                        + "/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "interbedded component",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit[@gml:id='gu.25678']/gsml:composition[1]"
                        + "/gsml:CompositionPart/gsml:role",
                doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition[1]"
                        + "/gsml:CompositionPart/gsml:role/FEATURE_LINK",
                doc);
        assertXpathEvaluatesTo(
                "minor",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition[2]"
                        + "/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "interbedded component",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition[2]"
                        + "/gsml:CompositionPart/gsml:role",
                doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition[2]"
                        + "/gsml:CompositionPart/gsml:role/FEATURE_LINK",
                doc);

        // lithology
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology",
                doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition"
                        + "/gsml:CompositionPart/gsml:lithology/FEATURE_LINK",
                doc);
    }

    /** Check mf3 content are encoded correctly */
    private void checkMf3Content(String id, Document doc) {
        assertXpathEvaluatesTo(
                "CLIFTON FORMATION", "//gsml:MappedFeature[@gml:id='" + id + "']/gml:name", doc);
        // positionalAccuracy
        assertXpathEvaluatesTo(
                "150.0",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:uom:UCUM:m",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
        // shape
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.2 52.6 -1.2 52.6 -1.1 52.5 -1.1 52.5 -1.2",
                "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape//gml:posList",
                doc);
        // gu.25678
        assertXpathEvaluatesTo(
                "#gu.25678",
                "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:specification/@xlink:href",
                doc);
        // make sure nothing else is encoded
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:specification/gsml:GeologicUnit",
                doc);
    }

    /** Check mf4 content are encoded correctly */
    private void checkMf4Content(String id, Document doc) {
        assertXpathEvaluatesTo(
                "MURRADUC BASALT", "//gsml:MappedFeature[@gml:id='" + id + "']/gml:name", doc);
        // positionalAccuracy
        assertXpathEvaluatesTo(
                "120.0",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:uom:UCUM:m",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue/@uom",
                doc);
        // shape
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.3 52.6 -1.3 52.6 -1.2 52.5 -1.2 52.5 -1.3",
                "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape//gml:posList",
                doc);
        // gu.25682
        assertXpathEvaluatesTo(
                "gu.25682",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
        // description
        assertXpathEvaluatesTo(
                "Olivine basalt",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gml:description",
                doc);
        // name
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:name",
                doc);
        assertXpathEvaluatesTo(
                "New Group",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:name[@codeSpace='urn:ietf:rfc:2141']",
                doc);
        List<String> names = new ArrayList<String>();
        names.add("New Group");
        names.add("-Xy");
        String name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='"
                                + id
                                + "']/gsml:specification/gsml:GeologicUnit/gml:name[1]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='"
                                + id
                                + "']/gsml:specification/gsml:GeologicUnit/gml:name[2]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        assertTrue(names.isEmpty());

        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/FEATURE_LINK",
                doc);
        // occurrence
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence",
                doc);
        assertXpathEvaluatesTo(
                "",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence[1]",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf4",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence/@xlink:href",
                doc);
        // exposureColor
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:exposureColor",
                doc);
        assertXpathEvaluatesTo(
                "some:uri",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:exposureColor/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "Red",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:exposureColor"
                        + "/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:exposureColor"
                        + "/gsml:CGI_TermValue/FEATURE_LINK",
                doc);
        // outcropCharacter
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:outcropCharacter",
                doc);
        assertXpathEvaluatesTo(
                "z",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:outcropCharacter"
                        + "/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:outcropCharacter"
                        + "/gsml:CGI_TermValue/FEATURE_LINK",
                doc);
        // composition
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:composition",
                doc);
        assertXpathEvaluatesTo(
                "significant",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition"
                        + "/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "interbedded component",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition"
                        + "/gsml:CompositionPart/gsml:role",
                doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition"
                        + "/gsml:CompositionPart/gsml:role/FEATURE_LINK",
                doc);
        // lithology
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology",
                doc);
        // lithology:1
        assertXpathEvaluatesTo(
                "cc.1",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology[1]"
                        + "/gsml:ControlledConcept/@gml:id",
                doc);
        assertXpathCount(
                3,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology[1]"
                        + "/gsml:ControlledConcept/gml:name",
                doc);
        // Order depends on what the database returns in case of joining queries
        names = new ArrayList<String>();
        names.add("name_a");
        names.add("name_b");
        names.add("name_c");
        name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='"
                                + id
                                + "']/gsml:specification/gsml:GeologicUnit"
                                + "/gsml:composition/gsml:CompositionPart/gsml:lithology[1]"
                                + "/gsml:ControlledConcept/gml:name[1]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='"
                                + id
                                + "']/gsml:specification/gsml:GeologicUnit"
                                + "/gsml:composition/gsml:CompositionPart/gsml:lithology[1]"
                                + "/gsml:ControlledConcept/gml:name[2]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='"
                                + id
                                + "']/gsml:specification/gsml:GeologicUnit"
                                + "/gsml:composition/gsml:CompositionPart/gsml:lithology[1]"
                                + "/gsml:ControlledConcept/gml:name[3]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition"
                        + "/gsml:CompositionPart/gsml:lithology[1]/FEATURE_LINK",
                doc);
        // lithology:2
        assertXpathEvaluatesTo(
                "cc.2",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology[2]/"
                        + "/gsml:ControlledConcept/@gml:id",
                doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology[2]"
                        + "/gsml:ControlledConcept/gml:name",
                doc);
        assertXpathEvaluatesTo(
                "name_2",
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit"
                        + "/gsml:composition/gsml:CompositionPart/gsml:lithology[2]"
                        + "/gsml:ControlledConcept/gml:name",
                doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='"
                        + id
                        + "']/gsml:specification/gsml:GeologicUnit/gsml:composition"
                        + "/gsml:CompositionPart/gsml:lithology[2]/FEATURE_LINK",
                doc);
    }

    /** Implementation for tests expected to get mf4 only. */
    private void checkGetMf4Only(String xml) {
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        // mf4
        {
            String id = "mf4";
            assertXpathEvaluatesTo(id, "//gsml:MappedFeature[1]/@gml:id", doc);
            assertXpathEvaluatesTo(
                    "MURRADUC BASALT", "//gsml:MappedFeature[@gml:id='" + id + "']/gml:name", doc);
            // gu.25682
            assertXpathEvaluatesTo(
                    "gu.25682",
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification/gsml:GeologicUnit/@gml:id",
                    doc);
        }
    }

    /** Test if we can get mf4 by its name. */
    @Test
    public void testGetFeaturePropertyFilter() {
        String xml = //
                "<wfs:GetFeature " //
                        + GETFEATURE_ATTRIBUTES //
                        + ">" //
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                        + "        <ogc:Filter>" //
                        + "            <ogc:PropertyIsEqualTo>" //
                        + "                <ogc:PropertyName>gml:name</ogc:PropertyName>" //
                        + "                <ogc:Literal>MURRADUC BASALT</ogc:Literal>" //
                        + "            </ogc:PropertyIsEqualTo>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        validate(xml);
        checkGetMf4Only(xml);
    }

    /** Test if we can get mf4 with a FeatureId fid filter. */
    @Test
    public void testGetFeatureWithFeatureIdFilter() {
        String xml = //
                "<wfs:GetFeature " //
                        + GETFEATURE_ATTRIBUTES //
                        + ">" //
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                        + "        <ogc:Filter>" //
                        + "            <ogc:FeatureId fid=\"mf4\"/>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        validate(xml);
        checkGetMf4Only(xml);
    }

    /** Test if we can get mf4 with a GmlObjectId gml:id filter. */
    @Test
    public void testGetFeatureWithGmlObjectIdFilter() {
        String xml = //
                "<wfs:GetFeature " //
                        + GETFEATURE_ATTRIBUTES //
                        + ">" //
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                        + "        <ogc:Filter>" //
                        + "            <ogc:GmlObjectId gml:id=\"mf4\"/>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        validate(xml);
        checkGetMf4Only(xml);
    }

    /**
     * Test anyType as complex attributes, and placeholder type (e.g AnyOrReference) which contains
     * <any/> element.
     */
    @Test
    public void testAnyTypeAndAnyElementGML() {
        final String OBSERVATION_ID_PREFIX = "observation:";
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=om:Observation");
        LOGGER.info("WFS GetFeature&typename=om:Observation response:\n" + prettyString(doc));

        assertXpathEvaluatesTo("4", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(4, "//om:Observation", doc);

        String id = "mf1";
        assertXpathEvaluatesTo(OBSERVATION_ID_PREFIX + id, "(//om:Observation)[1]/@gml:id", doc);
        // om:metadata
        assertXpathEvaluatesTo(
                "651.0",
                "(//om:Observation)[1]/om:metadata/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        // om:resultQuality
        Node resultQuality = doc.getElementsByTagName("om:resultQuality").item(0);
        Node geologicUnit = resultQuality.getFirstChild();
        assertEquals(
                "gu.25699", geologicUnit.getAttributes().getNamedItem("gml:id").getNodeValue());
        // om:result
        assertXpathEvaluatesTo("", "(//om:Observation)[1]/om:result/text()", doc);
        assertXpathEvaluatesTo(
                id, "(//om:Observation)[1]/om:result/gsml:MappedFeature/@gml:id", doc);

        id = "mf2";
        assertXpathEvaluatesTo(OBSERVATION_ID_PREFIX + id, "(//om:Observation)[2]/@gml:id", doc);
        // om:metadata
        assertXpathEvaluatesTo(
                "269.0",
                "(//om:Observation)[2]/om:metadata/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        // om:resultQuality
        resultQuality = doc.getElementsByTagName("om:resultQuality").item(1);
        geologicUnit = resultQuality.getFirstChild();
        assertEquals(
                "gu.25678", geologicUnit.getAttributes().getNamedItem("gml:id").getNodeValue());
        // om:result
        assertXpathEvaluatesTo("", "(//om:Observation)[2]/om:result/text()", doc);
        assertXpathEvaluatesTo(
                id, "(//om:Observation)[2]/om:result/gsml:MappedFeature/@gml:id", doc);

        id = "mf3";
        assertXpathEvaluatesTo(OBSERVATION_ID_PREFIX + id, "(//om:Observation)[3]/@gml:id", doc);
        // om:metadata
        assertXpathEvaluatesTo(
                "123.0",
                "(//om:Observation)[3]/om:metadata/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        // om:resultQuality
        resultQuality = doc.getElementsByTagName("om:resultQuality").item(2);
        assertEquals(
                "#gu.25678",
                resultQuality.getAttributes().getNamedItem("xlink:href").getNodeValue());
        // om:result
        assertXpathEvaluatesTo("", "(//om:Observation)[3]/om:result/text()", doc);
        assertXpathEvaluatesTo(
                id, "(//om:Observation)[3]/om:result/gsml:MappedFeature/@gml:id", doc);

        id = "mf4";
        assertXpathEvaluatesTo(OBSERVATION_ID_PREFIX + id, "(//om:Observation)[4]/@gml:id", doc);
        // om:metadata
        assertXpathEvaluatesTo(
                "456.0",
                "(//om:Observation)[4]/om:metadata/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
        // om:resultQuality
        resultQuality = doc.getElementsByTagName("om:resultQuality").item(3);
        geologicUnit = resultQuality.getFirstChild();
        assertEquals(
                "gu.25682", geologicUnit.getAttributes().getNamedItem("gml:id").getNodeValue());
        // om:result
        assertXpathEvaluatesTo("", "(//om:Observation)[4]/om:result/text()", doc);
        assertXpathEvaluatesTo(
                id, "(//om:Observation)[4]/om:result/gsml:MappedFeature/@gml:id", doc);
    }

    /** Making sure attributes that are encoded as xlink:href can still be queried in filters. */
    @Test
    public void testFilteringXlinkHref() {
        String xml = //
                "<wfs:GetFeature " //
                        + GETFEATURE_ATTRIBUTES //
                        + ">" //
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                        + "        <ogc:Filter>" //
                        + "            <ogc:PropertyIsEqualTo>" //
                        + "                <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gml:name</ogc:PropertyName>" //
                        + "                <ogc:Literal>Yaugher Volcanic Group</ogc:Literal>" //
                        + "            </ogc:PropertyIsEqualTo>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        validate(xml);
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        // there should be 1:
        // - mf1/gu.25699
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf1", "//gsml:MappedFeature/@gml:id", doc);
        // TODO: This test case no longer serves its purpose. It should be changed to below, but it
        // fails at the moment.
        //      String xml = //
        //      "<wfs:GetFeature " //
        //              + GETFEATURE_ATTRIBUTES //
        //              + ">" //
        //              + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
        //              + "        <ogc:Filter>" //
        //              + "            <ogc:PropertyIsEqualTo>" //
        //              + "
        // <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gml:name</ogc:PropertyName>" //
        //              + "                <ogc:Literal>Yaugher Volcanic Group 2</ogc:Literal>" //
        //              + "            </ogc:PropertyIsEqualTo>" //
        //              + "        </ogc:Filter>" //
        //              + "    </wfs:Query> " //
        //              + "</wfs:GetFeature>";
        //      validate(xml);
        //      Document doc = postAsDOM("wfs", xml);
        //      LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        //      assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        //      // there should be 2:
        //      // - mf2/gu.25678
        //      // - mf3/gu.25678
        //      assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        //      assertXpathCount(2, "//gsml:MappedFeature", doc);
        //      assertXpathEvaluatesTo("mf2", "//gsml:MappedFeature[1]/@gml:id", doc);
        //      assertXpathEvaluatesTo("mf3", "//gsml:MappedFeature[2]/@gml:id", doc);
    }

    /**
     * Making sure multi-valued attributes in nested features can be queried from the top level.
     * (GEOT-3156)
     */
    @Test
    public void testFilteringNestedMultiValuedAttribute() {
        // PropertyIsEqual
        String xml =
                "<wfs:GetFeature "
                        + GETFEATURE_ATTRIBUTES
                        + ">"
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
                        + "        <ogc:Filter>"
                        + "            <ogc:PropertyIsEqualTo>"
                        + "                <ogc:Literal>Yaugher Volcanic Group 2</ogc:Literal>"
                        + "                <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gml:name</ogc:PropertyName>"
                        + "            </ogc:PropertyIsEqualTo>"
                        + "        </ogc:Filter>"
                        + "    </wfs:Query> "
                        + "</wfs:GetFeature>";
        validate(xml);
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        // there should be 2:
        // - mf2/gu.25678
        // - mf3/gu.25678
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(2, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf2", "(//gsml:MappedFeature)[1]/@gml:id", doc);
        assertXpathEvaluatesTo("mf3", "(//gsml:MappedFeature)[2]/@gml:id", doc);

        // PropertyIsLike
        xml = //
                "<wfs:GetFeature "
                        + GETFEATURE_ATTRIBUTES
                        + ">"
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
                        + "        <ogc:Filter>"
                        + "            <ogc:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
                        + "                <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gml:name</ogc:PropertyName>"
                        + "                <ogc:Literal>Yaugher Volcanic Group*</ogc:Literal>"
                        + "            </ogc:PropertyIsLike>"
                        + "        </ogc:Filter>"
                        + "    </wfs:Query> "
                        + "</wfs:GetFeature>";
        validate(xml);
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        // there should be 3:
        // - mf1/gu.25699
        // - mf2/gu.25678
        // - mf3/gu.25678
        assertXpathEvaluatesTo("3", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(3, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf1", "(//gsml:MappedFeature)[1]/@gml:id", doc);
        assertXpathEvaluatesTo("mf2", "(//gsml:MappedFeature)[2]/@gml:id", doc);
        assertXpathEvaluatesTo("mf3", "(//gsml:MappedFeature)[3]/@gml:id", doc);
    }

    /**
     * Similar to above test case but using AND as a wrapper for 2 filters involving nested
     * attributes.
     */
    @Test
    public void testFilterAnd() {
        String xml =
                "<wfs:GetFeature "
                        + GETFEATURE_ATTRIBUTES
                        + ">"
                        + "<wfs:Query typeName=\"gsml:MappedFeature\">"
                        + "    <ogc:Filter>"
                        + "        <ogc:And>"
                        + "            <ogc:PropertyIsEqualTo>"
                        + "                <ogc:Literal>significant</ogc:Literal>"
                        + "                <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>"
                        + "            </ogc:PropertyIsEqualTo>"
                        + "            <ogc:PropertyIsEqualTo>"
                        + "                 <ogc:Literal>New Group</ogc:Literal>"
                        + "                 <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gml:name</ogc:PropertyName>"
                        + "            </ogc:PropertyIsEqualTo>"
                        + "        </ogc:And>"
                        + "    </ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        validate(xml);
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf4", "//gsml:MappedFeature/@gml:id", doc);
    }

    /** Test that denormalized data reports the correct number of features */
    @Test
    public void testDenormalisedFeaturesCount() {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit&maxFeatures=3");
        LOGGER.info(
                "WFS GetFeature&typename=gsml:GeologicUnit&maxFeatures=3 response:\n"
                        + prettyString(doc));
        assertXpathCount(3, "//gsml:GeologicUnit", doc);

        // check that we get features we're expecting
        String id = "gu.25678";
        assertXpathEvaluatesTo(id, "(//gsml:GeologicUnit)[1]/@gml:id", doc);

        id = "gu.25682";
        assertXpathEvaluatesTo(id, "(//gsml:GeologicUnit)[2]/@gml:id", doc);

        id = "gu.25699";
        assertXpathEvaluatesTo(id, "(//gsml:GeologicUnit)[3]/@gml:id", doc);
    }

    /** Test FeatureCollection is encoded with multiple featureMember elements */
    @Test
    public void testEncodeFeatureMember() throws Exception {
        // change fixture settings (must restore this at end)
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        boolean encodeFeatureMember = wfs.isEncodeFeatureMember();
        wfs.setEncodeFeatureMember(true);
        getGeoServer().save(wfs);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature,gsml:GeologicUnit");
        LOGGER.info(
                "WFS GetFeature&typename=gsml:MappedFeature,gsml:GeologicUnit response:\n"
                        + prettyString(doc));

        checkSchemaLocation(doc);

        assertXpathEvaluatesTo("7", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(4, "//gsml:MappedFeature", doc);

        assertEquals(7, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(0, doc.getElementsByTagName("gml:featureMembers").getLength());

        // mf1
        {
            String id = "mf1";
            checkMf1Content(id, doc);
        }

        // mf2
        {
            String id = "mf2";
            checkMf2Content(id, doc);
        }

        // mf3
        {
            String id = "mf3";
            checkMf3Content(id, doc);
        }

        // mf4
        {
            String id = "mf4";
            checkMf4Content(id, doc);
        }

        // check for duplicate gml:id
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25699']", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25678']", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25682']", doc);

        // test for xlink:href is encoded within featureMember
        assertXpathCount(1, "//gml:featureMember[@xlink:href='#gu.25699']", doc);
        assertXpathCount(1, "//gml:featureMember[@xlink:href='#gu.25678']", doc);
        assertXpathCount(1, "//gml:featureMember[@xlink:href='#gu.25682']", doc);

        // restore fixture settings
        wfs = getGeoServer().getService(WFSInfo.class);
        wfs.setEncodeFeatureMember(encodeFeatureMember);
        getGeoServer().save(wfs);
    }

    /** Test FeatureCollection is encoded with one featureMembers element */
    @Test
    public void testEncodeFeatureMembersGML() throws Exception {
        // change fixture settings (must restore this at end)
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        boolean encodeFeatureMember = wfs.isEncodeFeatureMember();
        wfs.setEncodeFeatureMember(false);
        getGeoServer().save(wfs);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature,gsml:GeologicUnit");
        LOGGER.info(
                "WFS GetFeature&typename=gsml:MappedFeature,gsml:GeologicUnit response:\n"
                        + prettyString(doc));

        checkSchemaLocation(doc);

        assertXpathEvaluatesTo("7", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(4, "//gsml:MappedFeature", doc);

        assertEquals(1, doc.getElementsByTagName("gml:featureMembers").getLength());
        assertEquals(0, doc.getElementsByTagName("gml:featureMember").getLength());

        // mf1
        {
            String id = "mf1";
            checkMf1Content(id, doc);
        }

        // mf2
        {
            String id = "mf2";
            checkMf2Content(id, doc);
        }

        // mf3
        {
            String id = "mf3";
            checkMf3Content(id, doc);
        }

        // mf4
        {
            String id = "mf4";
            checkMf4Content(id, doc);
        }

        // check for duplicate gml:id
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25699']", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25678']", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25682']", doc);

        // check for xlink:href if encoded within GeologicUnit itself
        // note that this can never be schema-valid, but the best that the
        // encoder can do when configured to use featureMembers.
        assertXpathCount(1, "//gsml:GeologicUnit[@xlink:href='#gu.25699']", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@xlink:href='#gu.25678']", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@xlink:href='#gu.25682']", doc);

        // restore fixture settings
        wfs = getGeoServer().getService(WFSInfo.class);
        wfs.setEncodeFeatureMember(encodeFeatureMember);
        getGeoServer().save(wfs);
    }

    @Test
    public void testNestedFilterEncoding() throws FilterToSQLException, IOException {
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName("gsml", "MappedFeature");
        FeatureSource fs = ftInfo.getFeatureSource(new NullProgressListener(), null);
        AppSchemaDataAccess da = (AppSchemaDataAccess) fs.getDataStore();
        FeatureTypeMapping rootMapping = da.getMappingByNameOrElement(ftInfo.getQualifiedName());

        // make sure nested filters encoding is enabled, otherwise skip test
        assumeTrue(shouldTestNestedFiltersEncoding(rootMapping));

        JDBCDataStore store = (JDBCDataStore) rootMapping.getSource().getDataStore();
        NestedFilterToSQL nestedFilterToSQL = createNestedFilterEncoder(rootMapping);

        FilterFactoryImplNamespaceAware ff = new FilterFactoryImplNamespaceAware();
        ff.setNamepaceContext(rootMapping.getNamespaces());

        /*
         * test combined filters on nested attributes
         */
        And and =
                ff.and(
                        ff.equals(
                                ff.property("gsml:specification/gsml:GeologicUnit/gml:name"),
                                ff.literal("New Group")),
                        ff.equals(
                                ff.property(
                                        "gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value"),
                                ff.literal("significant")));

        // Each filter involves a single nested attribute --> can be encoded
        ComplexFilterSplitter splitter =
                new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
        splitter.visit(and, null);
        Filter preFilter = splitter.getFilterPre();
        Filter postFilter = splitter.getFilterPost();

        assertEquals(and, preFilter);
        assertEquals(Filter.INCLUDE, postFilter);

        // filter must be "unrolled" (i.e. reverse mapped) first
        Filter unrolled = AppSchemaDataAccess.unrollFilter(and, rootMapping);

        // Filter is nested
        assertTrue(NestedFilterToSQL.isNestedFilter(unrolled));

        String encodedFilter = nestedFilterToSQL.encodeToString(unrolled);

        assertTrue(encodedFilter.matches("^\\(EXISTS.*AND EXISTS.*\\)$"));
        assertContainsFeatures(fs.getFeatures(and), "mf4");

        /*
         * test like filter on nested attribute
         */
        PropertyIsLike like =
                ff.like(
                        ff.property("gsml:specification/gsml:GeologicUnit/gml:description"),
                        "*sedimentary*");

        // Filter involving single nested attribute --> can be encoded
        ComplexFilterSplitter splitterLike =
                new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
        splitterLike.visit(like, null);
        preFilter = splitterLike.getFilterPre();
        postFilter = splitterLike.getFilterPost();

        assertEquals(like, preFilter);
        assertEquals(Filter.INCLUDE, postFilter);

        // filter must be "unrolled" (i.e. reverse mapped) first
        unrolled = AppSchemaDataAccess.unrollFilter(like, rootMapping);

        // Filter is nested
        assertTrue(NestedFilterToSQL.isNestedFilter(unrolled));

        encodedFilter = nestedFilterToSQL.encodeToString(unrolled);

        // this is the generated query in PostGIS, but the test limits to check the presence of the
        // EXISTS keyword, as the actual SQL is dependent on the underlying database
        // EXISTS (SELECT "chain_link_1"."PKEY"
        //      FROM "appschematest"."GEOLOGICUNIT" "chain_link_1"
        //      WHERE UPPER("chain_link_1"."TEXTDESCRIPTION") LIKE '%SEDIMENTARY%' AND
        //            "appschematest"."MAPPEDFEATUREPROPERTYFILE"."GEOLOGIC_UNIT_ID" =
        // "chain_link_1"."GML_ID")
        assertTrue(encodedFilter.contains("EXISTS"));
        assertContainsFeatures(fs.getFeatures(like), "mf1", "mf2", "mf3");
    }

    @Test
    public void testNestedFilterEncodingDisabled() throws IOException, FilterToSQLException {
        // disable nested filters encoding during the test
        AppSchemaDataAccessRegistry.getAppSchemaProperties()
                .setProperty(
                        AppSchemaDataAccessConfigurator.PROPERTY_ENCODE_NESTED_FILTERS, "false");
        try {
            assertFalse(AppSchemaDataAccessConfigurator.shouldEncodeNestedFilters());

            FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName("gsml", "MappedFeature");
            FeatureSource fs = ftInfo.getFeatureSource(new NullProgressListener(), null);
            AppSchemaDataAccess da = (AppSchemaDataAccess) fs.getDataStore();
            FeatureTypeMapping rootMapping =
                    da.getMappingByNameOrElement(ftInfo.getQualifiedName());

            // skip test if it's not running against a database
            assumeTrue(rootMapping.getSource().getDataStore() instanceof JDBCDataStore);

            JDBCDataStore store = (JDBCDataStore) rootMapping.getSource().getDataStore();
            NestedFilterToSQL nestedFilterToSQL = createNestedFilterEncoder(rootMapping);

            FilterFactoryImplNamespaceAware ff = new FilterFactoryImplNamespaceAware();
            ff.setNamepaceContext(rootMapping.getNamespaces());

            /*
             * test the same like filter tested in method testNestedFilterEncoding
             */
            PropertyIsLike like =
                    ff.like(
                            ff.property("gsml:specification/gsml:GeologicUnit/gml:description"),
                            "*sedimentary*");

            // Encoding of filters involving nested attributes is disabled --> CANNOT be encoded
            ComplexFilterSplitter splitterLike =
                    new ComplexFilterSplitter(store.getFilterCapabilities(), rootMapping);
            splitterLike.visit(like, null);
            Filter preFilter = splitterLike.getFilterPre();
            Filter postFilter = splitterLike.getFilterPost();

            assertEquals(Filter.INCLUDE, preFilter);
            assertEquals(like, postFilter);

            // filter must be "unrolled" (i.e. reverse mapped) first
            Filter unrolled = AppSchemaDataAccess.unrollFilter(like, rootMapping);

            // Filter is nested
            assertTrue(NestedFilterToSQL.isNestedFilter(unrolled));

            assertContainsFeatures(fs.getFeatures(like), "mf1", "mf2", "mf3");
        } finally {
            // reset default
            AppSchemaDataAccessRegistry.getAppSchemaProperties()
                    .setProperty(
                            AppSchemaDataAccessConfigurator.PROPERTY_ENCODE_NESTED_FILTERS, "true");
        }
    }

    /**
     * Test the encoding of nested complex features that are mapped to GML complex types that don't
     * respect the GML object-property model.
     */
    @Test
    public void testNonValidNestedGML() throws Exception {
        // get the complex features encoded in GML
        Document result =
                getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=ex:ParentFeature");
        // checking that we have the correct number of elements
        assertXpathCount(3, "//ex:ParentFeature", result);
        assertXpathCount(9, "//ex:ParentFeature/ex:nestedFeature", result);
        assertXpathCount(9, "//ex:ParentFeature/ex:nestedFeature/ex:nestedValue", result);
        // checking the content of the first feature
        assertXpathCount(
                1,
                "//ex:ParentFeature[@gml:id='sc.1']/ex:parentValue[text()='string_one']",
                result);
        assertXpathCount(
                1,
                "//ex:ParentFeature[@gml:id='sc.1']/ex:nestedFeature/ex:nestedValue[text()='1GRAV']",
                result);
        assertXpathCount(
                1,
                "//ex:ParentFeature[@gml:id='sc.1']/ex:nestedFeature/ex:nestedValue[text()='1TILL']",
                result);
        assertXpathCount(
                1,
                "//ex:ParentFeature[@gml:id='sc.1']/ex:nestedFeature/ex:nestedValue[text()='6ALLU']",
                result);
        // checking the content of the second feature
        assertXpathCount(
                1,
                "//ex:ParentFeature[@gml:id='sc.2']/ex:parentValue[text()='string_two']",
                result);
        assertXpathCount(
                1,
                "//ex:ParentFeature[@gml:id='sc.2']/ex:nestedFeature/ex:nestedValue[text()='1GRAV']",
                result);
        assertXpathCount(
                1,
                "//ex:ParentFeature[@gml:id='sc.2']/ex:nestedFeature/ex:nestedValue[text()='1TILL']",
                result);
        assertXpathCount(
                1,
                "//ex:ParentFeature[@gml:id='sc.2']/ex:nestedFeature/ex:nestedValue[text()='6ALLU']",
                result);
        // checking the content of the third feature
        assertXpathCount(
                1,
                "//ex:ParentFeature[@gml:id='sc.3']/ex:parentValue[text()='string_three']",
                result);
        assertXpathCount(
                1,
                "//ex:ParentFeature[@gml:id='sc.3']/ex:nestedFeature/ex:nestedValue[text()='1GRAV']",
                result);
        assertXpathCount(
                1,
                "//ex:ParentFeature[@gml:id='sc.3']/ex:nestedFeature/ex:nestedValue[text()='1TILL']",
                result);
        assertXpathCount(
                1,
                "//ex:ParentFeature[@gml:id='sc.3']/ex:nestedFeature/ex:nestedValue[text()='6ALLU']",
                result);
    }

    @Test
    public void testNonValidNestedJSON() throws Exception {
        testJsonRequest("ex:ParentFeature", "/test-data/ParentFeature.json");
    }

    private void testJsonRequest(String featureType, String expectResultPath) throws Exception {
        // get the complex features encoded in JSON
        String request =
                String.format(
                        "wfs?request=GetFeature&version=1.1.0&typename=%s&outputFormat=application/json",
                        featureType);
        JSON result = getAsJSON(request);
        // check that we got a valida JSON object
        assertThat(result, instanceOf(JSONObject.class));
        JSONObject resultJson = (JSONObject) result;
        // check the returned JSON againts the expected result
        JSONObject expectedJson = readJsonObject(expectResultPath);

        // the timestamp attribute cannot be the same, remove it
        resultJson.remove("timeStamp");

        assertThat(resultJson, is(expectedJson));
    }

    /** Helper method that just reads a JSON object from a resource file. */
    private JSONObject readJsonObject(String resourcePath) throws Exception {
        // read the JSON file content
        InputStream input = this.getClass().getResourceAsStream(resourcePath);
        assertThat(input, notNullValue());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(input, output);
        // parse the JSON file
        String jsonText = new String(output.toByteArray());
        return JSONObject.fromObject(jsonText);
    }

    private void assertImportExists(Document doc, String ns) {
        assertXpathCount(1, "//xsd:import[@namespace='" + ns + "']", doc);
    }

    private void assertImportNotExists(Document doc, String ns) {
        assertXpathCount(0, "//xsd:import[@namespace='" + ns + "']", doc);
    }

    private void assertIncludeExists(Document doc, String schemaLocation) {
        assertXpathCount(1, "//xsd:include[@schemaLocation='" + schemaLocation + "']", doc);
    }
}
