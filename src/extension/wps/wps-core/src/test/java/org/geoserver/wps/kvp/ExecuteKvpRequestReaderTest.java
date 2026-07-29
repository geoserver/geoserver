/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.opengis.ows11.BoundingBoxType;
import net.opengis.wps10.InputType;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.BoundingBoxPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geotools.api.data.Parameter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;

public class ExecuteKvpRequestReaderTest {

    private ExecuteKvpRequestReader reader;
    private Map<String, Parameter<?>> mockInputParams;
    private Parameter<?> mockParameter;

    @Before
    public void setUp() {
        reader = new ExecuteKvpRequestReader();

        // Mock the Spring context dependency
        ApplicationContext mockContext = mock(ApplicationContext.class);
        reader.setApplicationContext(mockContext);

        // Setup a generic process input parameter map
        mockInputParams = new HashMap<>();
        mockParameter = mock(Parameter.class);
        mockInputParams.put("aoi", mockParameter);
    }

    @Test
    public void testParseBoundingBoxValidInput() {
        try (MockedStatic<ProcessParameterIO> mockedPpioStatic = mockStatic(ProcessParameterIO.class)) {
            BoundingBoxPPIO mockPpio = mock(BoundingBoxPPIO.class);
            mockedPpioStatic
                    .when(() -> ProcessParameterIO.findAll(eq(mockParameter), any()))
                    .thenReturn(List.of(mockPpio));

            // A clean, standard Bounding Box KVP string (minX, minY, maxX, maxY, EPSG)
            String inputString = "aoi=-105.36,39.82,-105.16,39.96,EPSG:4326";

            List<InputType> inputs = reader.parseDataInputs(mockInputParams, inputString);

            assertNotNull(inputs);
            assertFalse(inputs.isEmpty());

            InputType aoiInput = inputs.get(0);
            assertEquals("aoi", aoiInput.getIdentifier().getValue());

            BoundingBoxType bbox = aoiInput.getData().getBoundingBoxData();
            assertNotNull(bbox);

            // Verify coordinate bounds layout
            assertEquals(39.82, (Double) bbox.getLowerCorner().get(1), 0.001);
            assertEquals(-105.36, (Double) bbox.getLowerCorner().get(0), 0.001);
            assertEquals(39.96, (Double) bbox.getUpperCorner().get(1), 0.001);
            assertEquals(-105.16, (Double) bbox.getUpperCorner().get(0), 0.001);
        }
    }

    @Test
    public void testParseBoundingBoxMalformedCoordinatesThrowsException() {
        try (MockedStatic<ProcessParameterIO> mockedPpioStatic = mockStatic(ProcessParameterIO.class)) {
            BoundingBoxPPIO mockPpio = mock(BoundingBoxPPIO.class);
            mockedPpioStatic
                    .when(() -> ProcessParameterIO.findAll(eq(mockParameter), any()))
                    .thenReturn(List.of(mockPpio));

            // Broken payload structure to verify the exception handling path
            String brokenInputString = "aoi=39.82,invalidCoord,39.96,-105.16,EPSG:4326";

            try {
                reader.parseDataInputs(mockInputParams, brokenInputString);
                fail("Expected a WPSException due to unparseable values.");
            } catch (WPSException e) {
                assertTrue(e.getMessage().contains("Failed to parse the bounding box"));
            }
        }
    }
}
