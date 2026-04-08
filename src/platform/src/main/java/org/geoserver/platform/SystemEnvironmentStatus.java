/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/** Report system environment details to server status page. */
public class SystemEnvironmentStatus implements ModuleStatus {

    private static final Logger LOGGER = Logging.getLogger(SystemEnvironmentStatus.class);

    /**
     * Name of the environment variable that turns on the details (listing of all environment variables) for this
     * module. "false" = don't show, "true" = show all the environment variables on the web interface.
     */
    public static final String SystemEnvironmentStatusEnabledEnvironmentVar =
            "GEOSERVER_MODULE_SYSTEM_ENVIRONMENT_STATUS_ENABLED";

    @Override
    public String getModule() {
        return "system-environment";
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.ofNullable(System.getProperty("os.name"));
    }

    @Override
    public String getName() {
        return "System Environment";
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.ofNullable(System.getProperty("os.version"));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /** For Testing - this can be mocked to change environment variables. */
    String getEnvironmentVariable(String envVar) {
        return System.getenv(envVar);
    }

    /**
     * returns true if the message (list of variables) should be shown
     *
     * <p>Uses environment variable SystemPropertyStatusEnabledEnvironmentVar
     * ("GEOSERVER_MODULE_SYSTEM_ENVIRONMENT_STATUS_ENABLED") not defined -> false (default) bad value -> false
     * (default)
     */
    public boolean isShow() {
        String val = getEnvironmentVariable(SystemEnvironmentStatusEnabledEnvironmentVar);
        if (val == null) {
            return false; // not defined -> default
        }
        if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) {
            return val.equalsIgnoreCase("true");
        }
        LOGGER.log(
                Level.WARNING,
                "environment variable '%s' should be 'true' or 'false', but was '%s'"
                        .formatted(SystemEnvironmentStatusEnabledEnvironmentVar, val));
        return false; // bad value -> default
    }

    @Override
    public Optional<String> getMessage() {
        if (!isShow()) {
            String message =
                    "Environment variables hidden for security reasons.  Set the environment variable '%s' to 'true' to see them."
                            .formatted(SystemEnvironmentStatusEnabledEnvironmentVar);
            return Optional.of(message);
        }

        StringBuffer result = new StringBuffer();
        for (Entry<String, String> entry : System.getenv().entrySet()) {
            result.append(entry.getKey().toString() + "=" + entry.getValue().toString() + "\n");
        }
        return Optional.ofNullable(result.toString());
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.empty();
    }

    @Override
    public Category getCategory() {
        return Category.CORE;
    }
}
