/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import java.util.Map.Entry;
import java.util.Optional;

public class SystemPropertyStatus implements ModuleStatus {

    @Override
    public String getModule() {
        return "system-properties";
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.ofNullable(System.getProperty("java.runtime.name"));
    }

    @Override
    public String getName() {
        return "System Properties";
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.ofNullable(System.getProperty("java.runtime.version"));
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
        StringBuffer result = new StringBuffer();
        for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
            result.append(entry.getKey().toString() + "=" + entry.getValue().toString() + "\n");
        }
        return Optional.ofNullable(result.toString());
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.empty();
    }
}
