/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.vsi;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.apache.commons.io.FilenameUtils;
import org.gdal.gdal.Dataset;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for VRTFile class
 *
 * @author Matthew Northcott <matthewnorthcott@catalyst.net.nz>
 */
public class VRTFileTest extends GeoServerTestSupport {

    private VSITestHelper helper = new VSITestHelper();

    @Test
    public void testConstructorCreatesCorrectVRTFile() throws IOException, URISyntaxException {
        helper.setVSIPropertiesToResource(helper.PROPERTIES_VALID);

        final VRTFile vrt = spy(new VRTFile(helper.TIFF_LOCATION, helper.mockStoreInfo()));
        final File file = (File) ReflectionTestUtils.getField(vrt, "vrt");
        final String path = file.getAbsolutePath();
        final String extension = FilenameUtils.getExtension(path);
        final String expectedPath =
                Paths.get("workspaces", helper.WORKSPACE_NAME, helper.STORE_NAME, file.getName())
                        .toString();

        assertEquals("vrt", extension.toLowerCase());
        assertTrue(path.endsWith(expectedPath));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstuctorWithInvalidLocation() throws IOException {
        new VRTFile(helper.INVALID_LOCATION, helper.mockStoreInfo());
    }

    @Test
    public void testGenerate() throws IOException {
        final File file = mock(File.class);
        final VRTFile vrt = spy(new VRTFile(helper.TIFF_LOCATION, helper.mockStoreInfo()));
        final Dataset dataset = mock(Dataset.class);
        final String path = ((File) ReflectionTestUtils.getField(vrt, "vrt")).getAbsolutePath();

        when(file.exists()).thenReturn(true);
        when(file.getAbsolutePath()).thenReturn(path);
        doReturn(dataset).when(vrt).open(anyString());
        doReturn(null).when(vrt).saveDatasetToVRT(dataset);

        ReflectionTestUtils.setField(vrt, "vrt", file);

        vrt.generate();

        verify(vrt, times(1)).saveDatasetToVRT(any());
        verify(vrt, times(1)).open(anyString());
    }

    @Test
    public void testGetFileWhenExists() throws IOException {
        final VRTFile vrt = spy(new VRTFile(helper.TIFF_LOCATION, helper.mockStoreInfo()));

        // Pretend vrtFile exists
        final File file = mock(File.class);
        when(file.exists()).thenReturn(true);
        ReflectionTestUtils.setField(vrt, "vrt", file);

        // Check getter returns the property we just set
        assertEquals(file, vrt.getFile());

        // Verify we're not regenerating the VRT file
        verify(vrt, times(0)).generate();
        verify(vrt, times(0)).open(anyString());
    }

    @Test
    public void testGetFileWhenDoesNotExist() throws IOException {
        final File file = mock(File.class);
        final VRTFile vrt = spy(new VRTFile(helper.TIFF_LOCATION, helper.mockStoreInfo()));
        final Dataset dataset = mock(Dataset.class);
        final String path = ((File) ReflectionTestUtils.getField(vrt, "vrt")).getAbsolutePath();

        when(file.exists()).thenReturn(false);
        when(file.getAbsolutePath()).thenReturn(path);
        doReturn(dataset).when(vrt).open(anyString());
        doReturn(null).when(vrt).saveDatasetToVRT(dataset);
        doReturn(true).when(vrt).exists();

        ReflectionTestUtils.setField(vrt, "vrt", file);

        assertEquals(file, vrt.getFile());

        verify(vrt, times(1)).generate();
        verify(vrt, times(1)).saveDatasetToVRT(any());
        verify(vrt, times(1)).open(anyString());
    }
}
