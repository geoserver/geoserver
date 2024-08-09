/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class GeoVolumesProviderTest {

    @Before
    public void setUp() throws Exception {
        // mock information needed to build self-links
        APIRequestInfo mocked = niceMock(APIRequestInfo.class);
        expect(mocked.getProducibleMediaTypes(GeoVolumes.class, true))
                .andReturn(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML))
                .anyTimes();
        expect(mocked.getBaseURL()).andReturn("http://localhost:8080/geoserver").anyTimes();
        expect(mocked.isFormatRequested(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON))
                .andReturn(true)
                .anyTimes();
        replay(mocked);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestContextHolder.getRequestAttributes()
                .setAttribute(APIRequestInfo.KEY, mocked, RequestAttributes.SCOPE_REQUEST);
    }

    @Test
    public void testEmptyDirectory() throws IOException {
        Resource resource = Files.asResource(new File("./target/empty-geovolumes-dir"));
        GeoVolumesProvider provider = new GeoVolumesProvider(resource);

        // the directory has been created
        assertEquals(Resource.Type.DIRECTORY, resource.getType());

        // the file is missing, but it can still create a valid GeoVolumes object
        // (links are added on dynamic update, based on the current request)
        GeoVolumes geoVolumes = provider.getGeoVolumes();
        assertNotNull(geoVolumes);
        assertThat(geoVolumes.getCollections(), empty());
        assertThat(geoVolumes.getLinks(), empty());
    }

    @Test
    public void testParseSample() throws IOException {
        Resource resource = Files.asResource(new File("./src/test/resources"));
        GeoVolumesProvider provider = new GeoVolumesProvider(resource);

        // the directory has been created
        assertEquals(Resource.Type.DIRECTORY, resource.getType());

        // the file is missing, but it can still create a valid GeoVolumes object
        GeoVolumes geoVolumes = provider.getGeoVolumes();
        assertNotNull(geoVolumes);

        // first collection
        List<GeoVolume> collections = geoVolumes.getCollections();
        assertEquals(2, collections.size());

        // New York
        GeoVolume ny = collections.get(0);
        assertEquals("NewYork", ny.getTitle());
        assertEquals("NewYork", ny.getId());
        assertEquals("All Supported 3D Containers for the city of NewYork", ny.getDescription());
        assertEquals("NewYork/3dtiles/tileset.json", ny.getContent().get(0).getHref());
        assertEquals(
                "https://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/Buildings_NewYork_17/SceneServer/layers/0/",
                ny.getContent().get(1).getHref());
        double[] nyExpected = {
            -74.01900887327089,
            40.700475291581974,
            -11.892070104139751,
            -73.9068954348699,
            40.880256294183646,
            547.7591871983744
        };
        assertArrayEquals(nyExpected, ny.getExtent().getSpatialExtents().getBbox().get(0), 1e-9);

        // Stuttgart
        GeoVolume stuttgart = collections.get(1);
        assertEquals("Stuttgart", stuttgart.getTitle());
        assertEquals("Stuttgart", stuttgart.getId());
        assertEquals(
                "All Supported 3D Containers for the city of Stuttgart LoD 1 from OSM with Textures",
                stuttgart.getDescription());
        double[] stgExpected = {9.161434, 48.771841, -10, 9.183426, 48.786318, 550};
        assertArrayEquals(
                stgExpected, stuttgart.getExtent().getSpatialExtents().getBbox().get(0), 1e-9);
    }
}
