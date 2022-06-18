/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import static org.junit.Assert.assertNotNull;

import org.geoserver.metadata.AbstractMetadataTest;
import org.geoserver.metadata.data.service.impl.MetadataConstants;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class GeneratorServiceTest extends AbstractMetadataTest {

    @Autowired private GeneratorService generatorService;

    @Test
    public void testGeneratorService() {
        assertNotNull(
                generatorService.findGeneratorByType(MetadataConstants.FEATURE_ATTRIBUTE_TYPENAME));

        assertNotNull(generatorService.findGeneratorByType(MetadataConstants.DOMAIN_TYPENAME));
    }
}
