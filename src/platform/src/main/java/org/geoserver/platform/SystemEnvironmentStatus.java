/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;

public class SystemEnvironmentStatus implements ModuleStatus {

    @Override
    public String getModule() {
        return "system-environment";
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.ofNullable("system-environment");
    }

    @Override
    public String getName() {
        return "system-environment";
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.ofNullable(null);
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
        for (Iterator<Entry<String, String>> it = System.getenv().entrySet().iterator();
                it.hasNext(); ) {
            Entry<String, String> entry = it.next();
            result.append(entry.getKey().toString() + "=" + entry.getValue().toString() + "\n");
        }
        return Optional.ofNullable(result.toString());
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.empty();
    }
}
