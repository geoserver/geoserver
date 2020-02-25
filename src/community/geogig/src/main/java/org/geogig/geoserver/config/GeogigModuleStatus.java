/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Properties;
import org.geoserver.platform.ModuleStatus;
import org.locationtech.geogig.porcelain.VersionOp;

/** Spring defined bean publishing geogig module status to the web UI and REST API */
public class GeogigModuleStatus implements ModuleStatus {

    private static final String VERSION;

    private static final String MESSAGE;

    static {
        String version;
        String revision = "Unknown";
        String date = "Unknown";

        Properties props = new Properties();
        try (InputStream in =
                GeogigModuleStatus.class.getResourceAsStream("module_version.properties")) {
            try (InputStreamReader reader = new InputStreamReader(in)) {
                props.load(reader);
            }

            version = props.getProperty("version");
            revision = props.getProperty("build.revision");
            date = props.getProperty("build.date");
        } catch (Exception e) {
            version = "Error obtaining version: " + e.getMessage();
        }

        VERSION = version;

        String geogigInfo = new VersionOp().call().toString();
        MESSAGE =
                String.format(
                        "GeoGig plugin:\n version: %s\n revision: %s\n build date: %s\n\nGeoGig info:\n%s",
                        version, revision, date, geogigInfo);
    }

    public @Override String getModule() {
        return "gs-geogig";
    }

    public @Override Optional<String> getComponent() {
        return Optional.empty();
    }

    public @Override String getName() {
        return "GeoGig GeoServer Plugin";
    }

    public @Override Optional<String> getVersion() {
        return Optional.of(VERSION);
    }

    public @Override boolean isAvailable() {
        return true;
    }

    public @Override boolean isEnabled() {
        return true;
    }

    public @Override Optional<String> getMessage() {
        return Optional.of(MESSAGE);
    }

    public @Override Optional<String> getDocumentation() {
        // not being used as far as I can see
        return Optional.empty();
    }
}
