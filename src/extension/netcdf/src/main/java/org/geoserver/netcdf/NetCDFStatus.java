/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.netcdf;

import java.util.Optional;
import org.geoserver.platform.ModuleStatus;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;
import ucar.nc2.jni.netcdf.Nc4prototypes;

public class NetCDFStatus implements ModuleStatus {

    @Override
    public String getModule() {
        return "gs-netcdf";
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.ofNullable("GridCoverageMultiDimReader");
    }

    @Override
    public String getName() {
        return "NetCDF Coverage format";
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
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Optional<String> getMessage() {
        String message = "NETCDF-4 Binary Available: " + NetCDFUtilities.isNC4CAvailable();
        message += "\nNc4prototypes Version: " + GeoTools.getVersion(Nc4prototypes.class);
        return Optional.ofNullable(message);
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.ofNullable("");
    }
}
