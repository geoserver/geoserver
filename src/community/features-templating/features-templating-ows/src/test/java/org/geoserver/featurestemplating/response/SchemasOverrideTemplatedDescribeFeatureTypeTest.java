package org.geoserver.featurestemplating.response;
/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.junit.Assert;
import org.junit.Test;

public class SchemasOverrideTemplatedDescribeFeatureTypeTest extends SchemaComplexTestSupport {

    private static final String SCHEMA_OVERRIDE_FILE = "schemaTemplateOverride.xsd";
    private static final String MF_GML3 = "MappedFeatureGML31";

    @Override
    public void onSetUp(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo mappedFeature = catalog.getFeatureTypeByName("gsml", "MappedFeature");
        // GML
        setUpSchemaOverride(
                null, null, SupportedFormat.GML, SCHEMA_OVERRIDE_FILE, MF_GML3, ".xml", "gsml", mappedFeature);
    }

    @Test
    public void testGmlSchemaOverride() throws Exception {
        String schemaStr =
                getAsString("wfs?service=WFS&request=DescribeFeatureType&version=1.1.0&typename=gsml:MappedFeature");
        Assert.assertTrue(
                schemaStr.contains(
                        "<import namespace=\"http://www.opengis.net/gml\" schemaLocation=\"http://localhost:8080/geoserver/www/gml/3.1.1/base/gml.xsd\"/>"));
    }
}
