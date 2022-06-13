/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import java.io.IOException;
import java.util.List;
import org.geoserver.metadata.AbstractMetadataTest;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.GeonetworkMappingConfiguration;
import org.geoserver.metadata.data.dto.MetadataConfiguration;
import org.junit.Assert;
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
        Assert.assertNotNull(configuration);
        Assert.assertEquals(15, configuration.getAttributes().size());
        Assert.assertEquals(3, configuration.getGeonetworks().size());
        Assert.assertEquals(5, configuration.getTypes().size());

        // test csv's were imported
        Assert.assertEquals(3, configuration.findAttribute("source").getValues().size());
        Assert.assertEquals(3, configuration.findAttribute("target").getValues().size());

        Assert.assertEquals(
                "identifier-single",
                findAttribute(configuration.getAttributes(), "identifier-single").getLabel());
        Assert.assertEquals(
                "identifier-single",
                findAttribute(configuration.getAttributes(), "identifier-single").getLabel());
        Assert.assertEquals(
                "dropdown-field",
                findAttribute(configuration.getAttributes(), "dropdown-field").getLabel());
        Assert.assertEquals(
                "refsystem as list",
                findAttribute(configuration.getAttributes(), "refsystem-as-list").getLabel());

        List<AttributeConfiguration> complexAttributes =
                configuration.findType("referencesystem").getAttributes();
        Assert.assertEquals("Code", findAttribute(complexAttributes, "code").getLabel());
    }

    @Test
    public void testGeonetworkMappingRegistry() throws IOException {
        GeonetworkMappingConfiguration configuration =
                yamlService.getGeonetworkMappingConfiguration();
        Assert.assertNotNull(configuration);
        Assert.assertEquals(10, configuration.getGeonetworkmapping().size());
        Assert.assertEquals(2, configuration.getObjectmapping().size());
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
