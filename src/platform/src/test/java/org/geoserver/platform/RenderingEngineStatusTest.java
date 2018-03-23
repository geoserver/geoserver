/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;
 
import static org.junit.Assert.*;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.RenderingEngineStatus;
import org.geotools.util.logging.Logging;
import org.junit.Test;

import sun.java2d.pipe.RenderingEngine;

public class RenderingEngineStatusTest {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.platform");
    private Optional<String> statusMessage;
    private String engine;
    private String provider;
    private String msg;
    private static final String UNKNOWN = "unknown";

    @SuppressWarnings("unchecked")
    @Test
    public void RenderingEngineStatusTest() {
        RenderingEngineStatus res = new RenderingEngineStatus();
        Class<RenderingEngine> renderer;
        try {
            renderer = (Class<RenderingEngine>) sun.java2d.pipe.RenderingEngine.getInstance()
                    .getClass();
        } catch (Throwable e) {
            engine = UNKNOWN;
            provider = UNKNOWN;
            return;
        }
        engine = renderer.getSimpleName();
        statusMessage = res.getMessage();
        Package pkg = renderer.getPackage();
        if (pkg.getName().contains("marlin")) {
            provider = "Marlin";
        } else if (pkg.getName().contains("sun.dc")) {
            provider = "OracleJDK";
        } else if (pkg.getName().contains("sun.java2d")) {
            provider = "OpenJDK";
        } else {
            provider = pkg.getName();
        }
        String config = System.getProperty("sun.java2d.renderer");
        if (config != null) {
             msg = String.format("Java 2D configured with %s.\nProvider: %s\nConfiguration: -Dsun.java2d.renderer=%s", engine,provider,config);
        } else {
             msg = String.format("Java 2D configured with %s.\nProvider: %s\n", engine,provider);
        }
        if (provider.equals("Marlin") || provider.equals("OracleJDK") || provider.equals("OpenJDK")) {
            assertEquals(msg, statusMessage.get());
        } else {
            LOGGER.log(Level.WARNING, "Unknown Java Provider");
        }

        assertEquals(System.getProperty("java.version"), res.getVersion().orElse(null));
    }

}
