/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.HeadContent;
import org.geoserver.mapml.xml.Mapml;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpOutputMessage;

public class MapMLMessageConverterTest {

    private MapMLMessageConverter converter;
    private MapMLEncoder mockEncoder;
    private GeoServer mockGeoServer;
    private GeoServerInfo mockGeoServerInfo;
    private SettingsInfo mockSettings;
    private HttpOutputMessage mockOutputMessage;
    private ByteArrayOutputStream outputStream;

    @Before
    public void setUp() throws Exception {
        converter = new MapMLMessageConverter();

        // Mock the dependencies
        mockEncoder = mock(MapMLEncoder.class);
        mockGeoServer = mock(GeoServer.class);
        mockGeoServerInfo = mock(GeoServerInfo.class);
        mockSettings = mock(SettingsInfo.class);
        mockOutputMessage = mock(HttpOutputMessage.class);
        outputStream = new ByteArrayOutputStream();

        // Set up the mock chain
        when(mockGeoServer.getGlobal()).thenReturn(mockGeoServerInfo);
        when(mockGeoServerInfo.getSettings()).thenReturn(mockSettings);
        when(mockOutputMessage.getBody()).thenReturn(outputStream);

        // Inject mocks using reflection
        java.lang.reflect.Field encoderField = MapMLMessageConverter.class.getDeclaredField("mapMLEncoder");
        encoderField.setAccessible(true);
        encoderField.set(converter, mockEncoder);

        java.lang.reflect.Field geoServerField =
                converter.getClass().getSuperclass().getDeclaredField("geoServer");
        geoServerField.setAccessible(true);
        geoServerField.set(converter, mockGeoServer);
    }

    @Test
    public void testCanWriteMapmlClass() {
        assertTrue(
                "Should be able to write Mapml objects",
                converter.canWrite(Mapml.class, MapMLConstants.MAPML_MEDIA_TYPE));
        assertFalse(
                "Should not be able to write other objects",
                converter.canWrite(String.class, MapMLConstants.MAPML_MEDIA_TYPE));
    }

    @Test
    public void testWriteInternalWithVerboseTrue() throws UnsupportedEncodingException, IOException {
        // Set verbose = true
        when(mockSettings.isVerbose()).thenReturn(true);

        Mapml mapml = createTestMapml();

        converter.writeInternal(mapml, mockOutputMessage);

        // Verify that encode was called with verbose = true
        org.mockito.Mockito.verify(mockEncoder).encode(mapml, outputStream, true);
    }

    @Test
    public void testWriteInternalWithVerboseFalse() throws UnsupportedEncodingException, IOException {
        // Set verbose = false
        when(mockSettings.isVerbose()).thenReturn(false);

        Mapml mapml = createTestMapml();

        converter.writeInternal(mapml, mockOutputMessage);

        // Verify that encode was called with verbose = false
        org.mockito.Mockito.verify(mockEncoder).encode(mapml, outputStream, false);
    }

    @Test
    public void testWriteInternalWithNonMapmlObject() {
        assertThrows(IllegalArgumentException.class, () -> {
            converter.writeInternal("not a mapml object", mockOutputMessage);
        });
    }

    private Mapml createTestMapml() {
        Mapml mapml = new Mapml();
        HeadContent head = new HeadContent();
        head.setTitle("Test");
        mapml.setHead(head);
        mapml.setBody(new BodyContent());
        return mapml;
    }
}
