/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.grib;

import java.util.Optional;
import org.geoserver.platform.ModuleStatus;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;

public class GribStatus implements ModuleStatus {

    @Override
    public String getModule() {
        return "gs-grib";
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.ofNullable("GridCoverageMultiDimReader");
    }

    @Override
    public String getName() {
        return "GRIB Coverage Format";
    }

    @Override
    public Optional<String> getVersion() {
        Version v = GeoTools.getVersion(NetCDFUtilities.class);
        if (v == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(v.toString());
    }

    @Override
    public boolean isAvailable() {
        return NetCDFUtilities.isGribAvailable();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Optional<String> getMessage() {
        String message = "NetCDFUtilities.isGribAvailable: " + NetCDFUtilities.isGribAvailable();
        return Optional.ofNullable(message);
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.ofNullable("");
    }
}
