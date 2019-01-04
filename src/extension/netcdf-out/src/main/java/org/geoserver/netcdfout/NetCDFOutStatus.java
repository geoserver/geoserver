/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.netcdfout;

import java.lang.reflect.Method;
import java.util.Optional;
import org.geoserver.platform.ModuleStatus;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;
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

    public String Nc4Version() {
        try {
            // This reflection code is to deal with JNA being an optional jar.
            // Its the same as;
            // Nc4prototypes nc4 = (Nc4prototypes) Native.loadLibrary("netcdf",
            // Nc4prototypes.class);
            // return nc4.nc_inq_libvers();
            Class jnaNativeClass = Class.forName("com.sun.jna.Native");
            Method loadLibraryMethod =
                    jnaNativeClass.getMethod("loadLibrary", String.class, Class.class);
            Object nc4 = loadLibraryMethod.invoke(null, "netcdf", Nc4prototypes.class);

            Method nc_inq_libversMethod = Nc4prototypes.class.getMethod("nc_inq_libvers");
            String version = (String) nc_inq_libversMethod.invoke(nc4);

            return version;
        } catch (Exception e) {
            return "unavailable (" + e.getClass() + ":" + e.getMessage() + ")";
        }
    }

    @Override
    public Optional<String> getMessage() {
        String message = "NETCDF-4 Binary Available: " + NetCDFUtilities.isNC4CAvailable();
        message += "\nNc4prototypes Version: " + GeoTools.getVersion(Nc4prototypes.class);
        if (NetCDFUtilities.isNC4CAvailable()) {
            message += "\nc_inq_libvers: " + Nc4Version();
        }
        return Optional.ofNullable(message);
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.ofNullable("");
    }
}
