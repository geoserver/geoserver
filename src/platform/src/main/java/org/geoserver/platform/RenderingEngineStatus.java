/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.util.Optional;
import sun.java2d.pipe.RenderingEngine;

/**
 * @author Morgan Thompson - Boundless
 */
public class RenderingEngineStatus implements ModuleStatus {

    private static final String UNKNOWN = "unknown";

    private String engine;
    private String provider;

    @SuppressWarnings("unchecked")
    public RenderingEngineStatus() {

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
    }

    @Override
    public String getModule() {
        return "jvm";
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.ofNullable("java2d");
    }

    @Override
    public String getName() {
        return "Rendering Engine";
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.ofNullable(System.getProperty("java.version"));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Optional<String> getMessage() {
        StringBuilder msg = new StringBuilder();
        msg.append("Java 2D configured with ");
        msg.append(engine);
        msg.append(".\n");

        msg.append("Provider: ");
        msg.append(provider);
        msg.append("\n");

        String config = System.getProperty("sun.java2d.renderer");

        if (config != null) {
            msg.append("Configuration: -Dsun.java2d.renderer=");
            msg.append(config);
        }
        return Optional.of(msg.toString());
    }

    @Override
    public Optional<String> getDocumentation() {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

}
