/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * An integration test for the GetMapOutputFormat implementations
 *
 * @author Gabriel Roldan (TOPP)
 */
public class GetMapOutputFormatTest extends WMSTestSupport {

    @Test
    public void testGetOutputFormatNames() {
        List<GetMapOutputFormat> producers = WMSExtensions.findMapProducers(applicationContext);
        for (GetMapOutputFormat producer : producers) {
            Set<String> outputFormats = producer.getOutputFormatNames();
            assertNotNull(outputFormats);
            assertFalse(outputFormats.isEmpty());
            for (String oformat : outputFormats) {
                assertNotNull(oformat);
            }
        }
    }

    @Test
    public void testGetOutputFormat() {
        List<GetMapOutputFormat> producers = WMSExtensions.findMapProducers(applicationContext);
        for (GetMapOutputFormat producer : producers) {
            assertNotNull(producer.getMimeType());
        }
    }

    @Test
    public void testSetOutputFormat() {

        List<GetMapOutputFormat> producers = WMSExtensions.findMapProducers(applicationContext);

        for (GetMapOutputFormat producer : producers) {
            assertNotNull(producer.getMimeType());
            assertNotNull(producer.getOutputFormatNames());
            assertFalse(producer.getOutputFormatNames().isEmpty());
        }
    }
}
