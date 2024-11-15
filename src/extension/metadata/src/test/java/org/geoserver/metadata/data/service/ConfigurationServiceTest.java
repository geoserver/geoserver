/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;
import org.geoserver.metadata.AbstractMetadataTest;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeConfiguration;
import org.geoserver.metadata.data.dto.GeonetworkMappingConfiguration;
import org.geoserver.metadata.data.dto.MetadataConfiguration;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test yaml parsing.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class ConfigurationServiceTest extends AbstractMetadataTest {

    @Autowired private ConfigurationService yamlService;

    @Test
    public void testFileRegistry() throws IOException {
        MetadataConfiguration configuration = yamlService.getMetadataConfiguration();
        assertNotNull(configuration);
        assertEquals(16, configuration.getAttributes().size());
        assertEquals(3, configuration.getGeonetworks().size());
        assertEquals(5, configuration.getTypes().size());

        // test csv's were imported
        assertEquals(3, configuration.findAttribute("source").getValues().size());
        assertEquals(3, configuration.findAttribute("target").getValues().size());

        assertEquals(
                "identifier-single",
                findAttribute(configuration.getAttributes(), "identifier-single").getLabel());
        assertEquals(
                "identifier-single",
                findAttribute(configuration.getAttributes(), "identifier-single").getLabel());
        assertEquals(
                "dropdown-field",
                findAttribute(configuration.getAttributes(), "dropdown-field").getLabel());
        assertEquals(
                "refsystem as list",
                findAttribute(configuration.getAttributes(), "refsystem-as-list").getLabel());

        List<AttributeConfiguration> complexAttributes =
                configuration.findType("referencesystem").getAttributes();
        assertEquals("Code", findAttribute(complexAttributes, "code").getLabel());
    }

    @Test
    public void testFeatureCatalog() {
        AttributeTypeConfiguration featureAtt =
                yamlService.getMetadataConfiguration().findType("featureAttribute");
        assertNotNull(featureAtt);
        AttributeConfiguration attType = featureAtt.findAttribute("type");
        assertNotNull(attType);
        assertEquals(7, attType.getValues().size());
    }

    @Test
    public void testGeonetworkMappingRegistry() throws IOException {
        GeonetworkMappingConfiguration configuration =
                yamlService.getGeonetworkMappingConfiguration();
        assertNotNull(configuration);
        assertEquals(10, configuration.getGeonetworkmapping().size());
        assertEquals(2, configuration.getObjectmapping().size());
    }

    private AttributeConfiguration findAttribute(
            List<AttributeConfiguration> configurations, String key) {
        for (AttributeConfiguration attribute : configurations) {
            if (attribute.getKey().equals(key)) {
                return attribute;
            }
        }
        return null;
    }
}
