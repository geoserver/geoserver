/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.netcdfout;

import java.util.Optional;

import org.geoserver.platform.ModuleStatus;
import org.geotools.factory.GeoTools;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.util.Version;

import ucar.nc2.jni.netcdf.Nc4prototypes;

public class NetCDFOutStatus implements ModuleStatus {

    @Override
    public String getModule() {
        return "gs-netcdf-out";
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.ofNullable("GridCoverage2DWriter");
    }

    @Override
    public String getName() {
        return "WCS NetCDF output Module";
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
