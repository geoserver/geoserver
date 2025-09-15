/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wms;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.geowebcache.io.codec.ImageDecoderContainer;
import org.geowebcache.io.codec.ImageDecoderImpl;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 * Test class ensuring that the GWC beans stored in the geowebcache-wmsservice-context.xml file are correctly loaded.
 *
 * @author Nicola Lagomarsini geosolutions
 */
public class TestFullWMSBeans extends GeoServerSystemTestSupport {

    @Test
    public void testBeanSelection() {
        // Selection of the test application context
        @SuppressWarnings("PMD.CloseResource")
        ApplicationContext context = applicationContext;

        // Ensure that a decoder is present
        Object obj2 = context.getBean("TIFFDecoder");
        Assert.assertNotNull(obj2);
        Assert.assertTrue(obj2 instanceof ImageDecoderImpl);

        // Test if the container has been created
        ImageDecoderContainer container = context.getBean(ImageDecoderContainer.class);
        Assert.assertNotNull(container);
    }
}
