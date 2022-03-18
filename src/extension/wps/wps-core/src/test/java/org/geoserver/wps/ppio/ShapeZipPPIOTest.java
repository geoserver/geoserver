/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import org.geoserver.util.ZipTestUtil;
import org.geoserver.wps.resource.ShapefileResource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.store.ContentFeatureCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ShapeZipPPIOTest {

    ShapefileResource resource;

    WPSResourceManager resources;
    ShapeZipPPIO ppio;

    @Before
    public void prepare() {
        resources = mock(WPSResourceManager.class);
        ppio = new ShapeZipPPIO(resources, null, null, null);
    }

    @After
    public void cleanup() throws Exception {
        if (resource != null) {
            resource.delete();
        }
    }

    @Test
    public void testDecodeValid() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("empty-shapefile.zip")) {
            doAnswer(
                            inv -> {
                                resource = inv.getArgument(0, ShapefileResource.class);
                                return null;
                            })
                    .when(resources)
                    .addResource(any(ShapefileResource.class));
            Object result = ppio.decode(is);
            assertThat(result, instanceOf(ContentFeatureCollection.class));
            verify(resources).addResource(any(ShapefileResource.class));
        }
    }

    @Test
    public void testDecodeNoShapefiles() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("invalid.zip")) {
            IOException exception = assertThrows(IOException.class, () -> ppio.decode(input));
            assertEquals(
                    "Could not find any file with .shp extension in the zip file",
                    exception.getMessage());
            verify(resources, never()).addResource(any());
        }
    }

    @Test
    public void testDecodeBadEntryName() throws Exception {
        try (InputStream input = ZipTestUtil.getZipSlipInput()) {
            IOException exception = assertThrows(IOException.class, () -> ppio.decode(input));
            assertThat(
                    exception.getMessage(), startsWith("Entry is outside of the target directory"));
            verify(resources, never()).addResource(any());
        }
    }
}
