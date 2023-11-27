/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.graticule;

import java.util.Optional;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;

public class GraticuleModuleStatus extends ModuleStatusImpl implements ModuleStatus {

    @Override
    public String getModule() {
        return "gs-graticule";
    }

    @Override
    public String getName() {
        return "GeoServer Graticule Module";
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
