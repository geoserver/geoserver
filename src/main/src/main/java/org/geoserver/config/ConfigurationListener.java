/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.List;

/** @author Justin Deoliveira, The Open Planning Project */
public interface ConfigurationListener {

    /**
     * Handles a change to the global configuration.
     *
     * @param global The global config object.
     * @param propertyNames The names of the properties that were changed.
     * @param oldValues The old values for the properties that were changed.
     * @param newValues The new values for the properties that were changed.
     */
    void handleGlobalChange(
            GeoServerInfo global,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues);

    /**
     * Handles the event fired post change to global configuration.
     *
     * @param global The global config object.
     */
    void handlePostGlobalChange(GeoServerInfo global);

    /**
     * Handles the event fired when a settings configuration is added.
     *
     * @param settings The settings.
     */
    void handleSettingsAdded(SettingsInfo settings);

    /**
     * Handles the event fired when a settings configuration is changed.
     *
     * @param settings The settings.
     * @param propertyNames The names of the properties that were changed.
     * @param oldValues The old values for the properties that were changed.
     * @param newValues The new values for the properties that were changed.
     */
    void handleSettingsModified(
            SettingsInfo settings,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues);

    /**
     * Handles the event fired post change to a settings configuration.
     *
     * @param settings The settings.
     */
    void handleSettingsPostModified(SettingsInfo settings);

    /**
     * Handles the event fired when a settings configuration is removed.
     *
     * @param settings The settings.
     */
    void handleSettingsRemoved(SettingsInfo settings);

    /**
     * Handles a change to the logging configuration.
     *
     * @param logging The logging config object.
     * @param propertyNames The names of the properties that were changed.
     * @param oldValues The old values for the properties that were changed.
     * @param newValues The new values for the properties that were changed.
     */
    void handleLoggingChange(
            LoggingInfo logging,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues);

    /** Handles the event fired post change to logging configuration. */
    void handlePostLoggingChange(LoggingInfo logging);

    /**
     * Handles a change to a service configuration.
     *
     * @param service The service config object.
     * @param propertyNames The names of the properties that were changed.
     * @param oldValues The old values for the properties that were changed.
     * @param newValues The new values for the properties that were changed.
     */
    void handleServiceChange(
            ServiceInfo service,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues);

    /**
     * Handles the event fired post change to service configuration.
     *
     * @param service The service config object.
     */
    void handlePostServiceChange(ServiceInfo service);

    /**
     * Handles the event fired when a service configuration is removed.
     *
     * @param service The service config object.
     */
    void handleServiceRemove(ServiceInfo service);

    /** A callback notifying when GeoServer configuration has been reloaded. */
    void reloaded();
}
