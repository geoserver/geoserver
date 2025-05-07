/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.util.IOUtils;
import org.junit.Assert;
import org.junit.Test;

/** Tests for the DescribeFeatureType response when using the schemas override. */
public class SchemasOverrideDescribeFeatureTypeTest extends SchemaComplexTestSupport {

    private static final String SCHEMA_OVERRIDE_FILE = "schemaOverride.xsd";
    private static final String MF_GML3 = "MappedFeatureGML31";
    private static final String JSON_SCHEMA_OVERRIDE_FILE = "schemaOverride.json";
    private static final String JSON_SCHEMA_OVERRIDE_NAME = "MappedFeatureGEOJSON31";

    @Override
    public void onSetUp(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo mappedFeature = catalog.getFeatureTypeByName("gsml", "MappedFeature");
        String templateMappedFeature = "MappedFeatureGML31.xml";
        // GML
        setUpSchemaOverride(
                null, null, SupportedFormat.GML, SCHEMA_OVERRIDE_FILE, MF_GML3, ".xml", "gsml", mappedFeature);
        // JSON
        setUpSchemaOverride(
                null,
                null,
                SupportedFormat.GEOJSON,
                JSON_SCHEMA_OVERRIDE_FILE,
                JSON_SCHEMA_OVERRIDE_NAME,
                ".json",
                "gsml",
                mappedFeature);
    }

    @Test
    public void testGmlSchemaOverride() throws Exception {
        String schemaStr =
                getAsString("wfs?service=WFS&request=DescribeFeatureType&version=1.1.0&typename=gsml:MappedFeature");
        String expectedContent = IOUtils.toString(this.getClass().getResourceAsStream(SCHEMA_OVERRIDE_FILE));
        Assert.assertEquals(expectedContent.trim(), schemaStr.trim());
    }

    @Test
    public void testJsonSchemaOverride() throws Exception {
        String schemaStr =
                getAsString("wfs?service=WFS&request=DescribeFeatureType&version=2.0.0&typenames=gsml:MappedFeature"
                        + "&outputformat=application/json");
        String expectedContent = IOUtils.toString(this.getClass().getResourceAsStream(JSON_SCHEMA_OVERRIDE_FILE));
        Assert.assertEquals(expectedContent.trim(), schemaStr.trim());
    }
}
