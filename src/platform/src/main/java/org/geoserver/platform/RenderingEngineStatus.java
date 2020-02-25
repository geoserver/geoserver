/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.util.Optional;

/** @author Morgan Thompson - Boundless */
public class RenderingEngineStatus implements ModuleStatus {

    private static final String DEFAULT = "PLATFORM DEFAULT";

    private String provider;

    public RenderingEngineStatus() {
        this.provider = System.getProperty("sun.java2d.renderer", DEFAULT);
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
        msg.append("Java 2D renderer configured with: ");

        msg.append(provider);

        return Optional.of(msg.toString());
    }

    @Override
    public Optional<String> getDocumentation() {
        // TODO Auto-generated method stub
        return Optional.empty();
    }
}
