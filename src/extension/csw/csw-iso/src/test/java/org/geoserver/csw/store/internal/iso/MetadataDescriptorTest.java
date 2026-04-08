/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal.iso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geoserver.csw.records.QueryablesMapping;
import org.geoserver.csw.records.iso.MetaDataDescriptor;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class MetadataDescriptorTest extends GeoServerSystemTestSupport {

    @Test
    public void testCreateDefaultQueryablesMapping() {
        MetaDataDescriptor mdDescriptor = applicationContext.getBean(MetaDataDescriptor.class);
        QueryablesMapping qMapping = mdDescriptor.getQueryablesMapping(null);
        assertNotNull(qMapping);
        assertEquals(
                "identificationInfo.MD_DataIdentification.extent.EX_Extent.geographicElement.EX_GeographicBoundingBox",
                qMapping.getBoundingBoxPropertyName());
    }
}
