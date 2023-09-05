/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.format;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class KMLFileFormatTest {

    private KMLFileFormat kmlFileFormat;
    static final String DOC_EL = "<kml xmlns=\"http://www.opengis.net/kml/2.2\">";

    @Before
    public void setUp() throws Exception {
        kmlFileFormat = new KMLFileFormat();
    }

    @Test
    public void testParseFeatureTypeNoPlacemarks() throws IOException {
        String kmlInput = DOC_EL + "</kml>";
        try {
            kmlFileFormat.parseFeatureTypes("foo", IOUtils.toInputStream(kmlInput, "UTF-8"));
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
            return;
        }
        Assert.fail("Expected Illegal Argument Exception for no features");
    }

    @Test
    public void testParseFeatureTypeMinimal() throws Exception {
        String kmlInput = DOC_EL + "<Placemark></Placemark></kml>";
        List<SimpleFeatureType> featureTypes =
                kmlFileFormat.parseFeatureTypes("foo", IOUtils.toInputStream(kmlInput, "UTF-8"));
        Assert.assertEquals("Unexpected number of feature types", 1, featureTypes.size());
        SimpleFeatureType featureType = featureTypes.get(0);
        Assert.assertEquals(
                "Unexpected number of feature type attributes",
                10,
                featureType.getAttributeCount());
    }

    @Test
    public void testExtendedUserData() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Placemark>"
                        + "<ExtendedData>"
                        + "<Data name=\"foo\"><value>bar</value></Data>"
                        + "<Data name=\"quux\"><value>morx</value></Data>"
                        + "</ExtendedData>"
                        + "</Placemark></kml>";
        List<SimpleFeatureType> featureTypes =
                kmlFileFormat.parseFeatureTypes("fleem", IOUtils.toInputStream(kmlInput, "UTF-8"));
        Assert.assertEquals("Unexpected number of feature types", 1, featureTypes.size());
        SimpleFeatureType featureType = featureTypes.get(0);
        Assert.assertEquals(
                "Unexpected number of feature type attributes",
                12,
                featureType.getAttributeCount());
        Assert.assertEquals(
                "Invalid attribute descriptor",
                String.class,
                featureType.getDescriptor("foo").getType().getBinding());
        Assert.assertEquals(
                "Invalid attribute descriptor",
                String.class,
                featureType.getDescriptor("quux").getType().getBinding());
    }

    @Test
    public void testReadFeatureWithNameAndDescription() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Placemark><name>foo</name><description>bar</description></Placemark></kml>";
        SimpleFeatureType featureType =
                kmlFileFormat
                        .parseFeatureTypes("foo", IOUtils.toInputStream(kmlInput, "UTF-8"))
                        .get(0);
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                kmlFileFormat.read(featureType, IOUtils.toInputStream(kmlInput, "UTF-8"))) {
            Assert.assertTrue("No features found", reader.hasNext());
            SimpleFeature feature = reader.next();
            Assert.assertNotNull("Expecting feature", feature);
            Assert.assertEquals("Invalid name attribute", "foo", feature.getAttribute("name"));
            Assert.assertEquals(
                    "Invalid description attribute", "bar", feature.getAttribute("description"));
        }
    }

    @Test
    public void testReadFeatureWithUntypedExtendedData() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Placemark>"
                        + "<ExtendedData>"
                        + "<Data name=\"foo\"><value>bar</value></Data>"
                        + "<Data name=\"quux\"><value>morx</value></Data>"
                        + "</ExtendedData>"
                        + "</Placemark></kml>";
        SimpleFeatureType featureType =
                kmlFileFormat
                        .parseFeatureTypes("foo", IOUtils.toInputStream(kmlInput, "UTF-8"))
                        .get(0);
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                kmlFileFormat.read(featureType, IOUtils.toInputStream(kmlInput, "UTF-8"))) {
            Assert.assertTrue("No features found", reader.hasNext());
            SimpleFeature feature = reader.next();
            Assert.assertNotNull("Expecting feature", feature);
            Assert.assertEquals("Invalid ext attr foo", "bar", feature.getAttribute("foo"));
            Assert.assertEquals("Invalid ext attr quux", "morx", feature.getAttribute("quux"));
        }
    }

    @Test
    public void testReadFeatureWithTypedExtendedData() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Schema name=\"myschema\">"
                        + "<SimpleField type=\"int\" name=\"foo\"></SimpleField>"
                        + "</Schema>"
                        + "<Placemark>"
                        + "<ExtendedData>"
                        + "<SchemaData schemaUrl=\"#myschema\">"
                        + "<SimpleData name=\"foo\">42</SimpleData>"
                        + "</SchemaData>"
                        + "</ExtendedData>"
                        + "</Placemark></kml>";
        SimpleFeatureType featureType =
                kmlFileFormat
                        .parseFeatureTypes("foo", IOUtils.toInputStream(kmlInput, "UTF-8"))
                        .get(0);
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                kmlFileFormat.read(featureType, IOUtils.toInputStream(kmlInput, "UTF-8"))) {
            Assert.assertTrue("No features found", reader.hasNext());
            SimpleFeature feature = reader.next();
            Assert.assertNotNull("Expecting feature", feature);
            Assert.assertEquals("Invalid ext attr foo", 42, feature.getAttribute("foo"));
        }
    }

    @Test
    public void testMultipleSchemas() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Schema name=\"schema1\">"
                        + "<SimpleField type=\"int\" name=\"foo\"></SimpleField>"
                        + "</Schema>"
                        + "<Schema name=\"schema2\">"
                        + "<SimpleField type=\"float\" name=\"bar\"></SimpleField>"
                        + "</Schema>"
                        + "<Placemark>"
                        + "<ExtendedData>"
                        + "<SchemaData schemaUrl=\"#schema1\">"
                        + "<SimpleData name=\"foo\">42</SimpleData>"
                        + "</SchemaData>"
                        + "<SchemaData schemaUrl=\"#schema2\">"
                        + "<SimpleData name=\"bar\">4.2</SimpleData>"
                        + "</SchemaData>"
                        + "</ExtendedData>"
                        + "</Placemark></kml>";
        List<SimpleFeatureType> featureTypes =
                kmlFileFormat.parseFeatureTypes(
                        "multiple", IOUtils.toInputStream(kmlInput, "UTF-8"));
        Assert.assertEquals("Unexpected number of feature types", 1, featureTypes.size());
        SimpleFeatureType ft = featureTypes.get(0);

        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                kmlFileFormat.read(ft, IOUtils.toInputStream(kmlInput, "UTF-8"))) {
            SimpleFeature feature1 = reader.next();
            Assert.assertNotNull("Expecting feature", feature1);
            Assert.assertEquals("Invalid ext attr foo", 42, feature1.getAttribute("foo"));
            Assert.assertEquals(
                    "Invalid ext attr bar", 4.2f, (Float) feature1.getAttribute("bar"), 0.01);
        }
    }

    @Test
    public void testTypedAndUntyped() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Schema name=\"myschema\">"
                        + "<SimpleField type=\"int\" name=\"foo\"></SimpleField>"
                        + "</Schema>"
                        + "<Placemark>"
                        + "<ExtendedData>"
                        + "<SchemaData schemaUrl=\"#myschema\">"
                        + "<SimpleData name=\"foo\">42</SimpleData>"
                        + "</SchemaData>"
                        + "<Data name=\"fleem\"><value>bar</value></Data>"
                        + "<Data name=\"quux\"><value>morx</value></Data>"
                        + "</ExtendedData>"
                        + "</Placemark></kml>";
        List<SimpleFeatureType> featureTypes =
                kmlFileFormat.parseFeatureTypes(
                        "typed-and-untyped", IOUtils.toInputStream(kmlInput, "UTF-8"));
        Assert.assertEquals("Unexpected number of feature types", 1, featureTypes.size());
        SimpleFeatureType featureType = featureTypes.get(0);
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                kmlFileFormat.read(featureType, IOUtils.toInputStream(kmlInput, "UTF-8"))) {
            SimpleFeature feature = reader.next();
            Assert.assertNotNull("Expecting feature", feature);
            Assert.assertEquals("Invalid ext attr foo", 42, feature.getAttribute("foo"));
            Assert.assertEquals("bar", feature.getAttribute("fleem"));
            Assert.assertEquals("morx", feature.getAttribute("quux"));
        }
    }

    @Test
    public void testReadCustomSchema() throws Exception {
        String kmlInput =
                DOC_EL
                        + "<Schema name=\"myschema\">"
                        + "<SimpleField type=\"int\" name=\"foo\"></SimpleField>"
                        + "</Schema>"
                        + "<myschema><foo>7</foo></myschema>"
                        + "</kml>";
        List<SimpleFeatureType> featureTypes =
                kmlFileFormat.parseFeatureTypes(
                        "custom-schema", IOUtils.toInputStream(kmlInput, "UTF-8"));
        Assert.assertEquals("Unexpected number of feature types", 1, featureTypes.size());
        SimpleFeatureType featureType = featureTypes.get(0);
        Map<Object, Object> userData = featureType.getUserData();
        @SuppressWarnings("unchecked")
        List<String> schemaNames = (List<String>) userData.get("schemanames");
        Assert.assertEquals(1, schemaNames.size());
        Assert.assertEquals(
                "Did not find expected schema name metadata", "myschema", schemaNames.get(0));
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                kmlFileFormat.read(featureType, IOUtils.toInputStream(kmlInput, "UTF-8"))) {
            SimpleFeature feature = reader.next();
            Assert.assertNotNull("Expecting feature", feature);
            Assert.assertEquals("Invalid ext attr foo", 7, feature.getAttribute("foo"));
        }
    }
}
