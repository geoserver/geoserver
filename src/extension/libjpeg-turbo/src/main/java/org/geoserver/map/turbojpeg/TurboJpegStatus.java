/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.map.turbojpeg;

import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegUtilities;
import java.util.Optional;
import org.geoserver.platform.ModuleStatus;
import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;

/** @author David Blasby - Boundless */
public class TurboJpegStatus implements ModuleStatus {

    @Override
    public String getModule() {
        return "libjpeg-turbo";
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.ofNullable("RenderedImageMapResponse");
    }

    @Override
    public String getName() {
        return "GeoServer libjpeg-turbo Module";
    }

    @Override
    public Optional<String> getVersion() {
        Version v = GeoTools.getVersion(TurboJpegStatus.class);
        if (v == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(v.toString());
    }

    @Override
    public boolean isAvailable() {
        return TurboJpegUtilities.isTurboJpegAvailable();
    }

    @Override
    public boolean isEnabled() {
        return !TurboJPEGMapResponse.isDisabled();
    }

    @Override
    public Optional<String> getMessage() {
        String message = "JNI LibJPEGTurbo Wrapper Version: " + getJniWrapperJarVersion();
        if (!isAvailable()) {
            message += "\njava.library.path: " + System.getProperty("java.library.path", "");
        }
        return Optional.ofNullable(message);
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.ofNullable("");
    }

    public String getJniWrapperJarVersion() {
        if (isAvailable()) {
            return GeoTools.getVersion(org.libjpegturbo.turbojpeg.TJ.class).toString();
        } else {
            return "unavailable";
        }
    }
}
