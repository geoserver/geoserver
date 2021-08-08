/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.vsi;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.geosolutions.imageio.plugins.vrt.VRTImageReaderSpi;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for VSIFormatFactory class
 *
 * @author Matthew Northcott <matthewnorthcott@catalyst.net.nz>
 */
public class VSIFormatFactoryTest extends GeoServerTestSupport {

    @Test
    public void testAvailableWhenClassAvailable() {
        VSIFormatFactory factory = new VSIFormatFactory();

        VRTImageReaderSpi mockVrtImageReaderSpi = mock(VRTImageReaderSpi.class);
        when(mockVrtImageReaderSpi.isAvailable()).thenReturn(true);
        ReflectionTestUtils.setField(factory, "vrtImageReaderSpi", mockVrtImageReaderSpi);

        assertTrue(factory.isAvailable());
    }

    @Test
    public void testAvailableWhenClassNotAvailable() {
        VSIFormatFactory factory = new VSIFormatFactory();

        VRTImageReaderSpi mockVrtImageReaderSpi = mock(VRTImageReaderSpi.class);
        when(mockVrtImageReaderSpi.isAvailable()).thenReturn(false);
        ReflectionTestUtils.setField(factory, "vrtImageReaderSpi", mockVrtImageReaderSpi);

        assertFalse(factory.isAvailable());
    }

    @Test
    public void testCreateFormat() {
        // Check there are no exceptions
        VSIFormatFactory factory = new VSIFormatFactory();
        assertTrue(factory.createFormat() instanceof VSIFormat);
    }
}
