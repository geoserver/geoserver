/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.resource.GridCoverageReaderResource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.util.ImageUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ArcGridPPIOTest {

    GridCoverage2D coverage;
    GridCoverageReaderResource resource;

    WPSResourceManager resources;
    ArcGridPPIO ppio;

    @Before
    public void prepare() {
        resources = mock(WPSResourceManager.class);
        ppio = new ArcGridPPIO(resources);
    }

    @After
    public void cleanup() {
        if (coverage != null) {
            ImageUtilities.disposeImage(coverage.getRenderedImage());
        }
        if (resource != null) {
            resource.delete();
        }
    }

    @Test
    public void testDecodeValidStream() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("arcGrid.asc")) {
            doAnswer(
                            inv -> {
                                resource = inv.getArgument(0, GridCoverageReaderResource.class);
                                return null;
                            })
                    .when(resources)
                    .addResource(any(GridCoverageReaderResource.class));
            Object result = ppio.decode(is);
            assertThat(result, instanceOf(GridCoverage2D.class));
            coverage = (GridCoverage2D) result;
            verify(resources).addResource(any(GridCoverageReaderResource.class));
        }
    }

    @Test
    public void testDecodeValidString() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("arcGrid.asc")) {
            String string = IOUtils.toString(is, StandardCharsets.UTF_8);
            doAnswer(
                            inv -> {
                                resource = inv.getArgument(0, GridCoverageReaderResource.class);
                                return null;
                            })
                    .when(resources)
                    .addResource(any(GridCoverageReaderResource.class));
            Object result = ppio.decode(string);
            assertThat(result, instanceOf(GridCoverage2D.class));
            coverage = (GridCoverage2D) result;
            verify(resources).addResource(any(GridCoverageReaderResource.class));
        }
    }

    @Test
    public void testDecodeInvalidStream() throws Exception {
        try (InputStream is = SystemTestData.class.getResourceAsStream("tazbm.tiff")) {
            WPSException exception = assertThrows(WPSException.class, () -> ppio.decode(is));
            assertEquals("Could not read application/arcgrid coverage", exception.getMessage());
            verify(resources, never()).addResource(any());
        }
    }

    @Test
    public void testDecodeInvalidString() throws Exception {
        WPSException exception = assertThrows(WPSException.class, () -> ppio.decode("foo"));
        assertEquals("Could not read application/arcgrid coverage", exception.getMessage());
        verify(resources, never()).addResource(any());
    }
}
