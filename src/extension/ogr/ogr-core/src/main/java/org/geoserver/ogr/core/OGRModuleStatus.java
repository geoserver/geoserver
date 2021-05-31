/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogr.core;

import java.util.Optional;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;

public class OGRModuleStatus extends ModuleStatusImpl implements ModuleStatus {

    @Override
    public String getModule() {
        return "gs-ogr-core";
    }

    @Override
    public String getName() {
        return "OGR Core Module";
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
    public Optional<String> getDocumentation() {
        return Optional.ofNullable("");
    }
}
