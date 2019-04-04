/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wms;

import java.util.List;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geowebcache.io.codec.ImageDecoderContainer;
import org.geowebcache.io.codec.ImageDecoderImpl;
import org.geowebcache.io.codec.ImageIOInitializer;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 * Test class ensuring that the GWC beans stored in the geowebcache-wmsservice-context.xml file are
 * correctly loaded.
 *
 * @author Nicola Lagomarsini geosolutions
 */
public class TestFullWMSBeans extends GeoServerSystemTestSupport {

    @Test
    public void testBeanSelection() {
        // Selection of the test application context
        ApplicationContext context = applicationContext;
        // Check that the initializer is present
        Object obj = context.getBean("ioInitializer");
        Assert.assertNotNull(obj);
        Assert.assertTrue(obj instanceof ImageIOInitializer);

        // Ensure that the excluded spis are present
        ImageIOInitializer init = (ImageIOInitializer) obj;
        List<String> excluded = init.getExcludedSpis();
        Assert.assertNotNull(excluded);
        Assert.assertTrue(excluded.size() > 0);

        // Ensure that a decoder is present
        Object obj2 = context.getBean("TIFFDecoder");
        Assert.assertNotNull(obj2);
        Assert.assertTrue(obj2 instanceof ImageDecoderImpl);

        // Test if the container has been created
        ImageDecoderContainer container = context.getBean(ImageDecoderContainer.class);
        Assert.assertNotNull(container);
    }
}
