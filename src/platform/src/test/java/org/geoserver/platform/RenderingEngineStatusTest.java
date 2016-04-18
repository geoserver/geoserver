package org.geoserver.platform;
 
import static org.junit.Assert.*;

import java.util.Optional;

import org.geoserver.platform.RenderingEngineStatus;
import org.junit.Test;

public class RenderingEngineStatusTest {

    private Optional<String> statusMessage;
    
    @Test
    public void RenderingEngineStatusTest() {
        RenderingEngineStatus res = new RenderingEngineStatus();
        statusMessage = res.getMessage();
        assertEquals("Java 2D configured with DuctusRenderingEngine.\nProvider: OracleJDK\n", statusMessage.get());
    }

}
