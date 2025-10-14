/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.List;

/**
 * Adapter for ConfigurationListener.
 *
 * <p>Configuration listeners can implement this class to pick and choose the events they wish to handle.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ConfigurationListenerAdapter implements ConfigurationListener {

    @Override
    public void handleGlobalChange(
            GeoServerInfo global, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {}

    @Override
    public void handlePostGlobalChange(GeoServerInfo global) {}

    @Override
    public void handleSettingsAdded(SettingsInfo settings) {}

    @Override
    public void handleSettingsModified(
            SettingsInfo settings, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {}

    @Override
    public void handleSettingsPostModified(SettingsInfo settings) {}

    @Override
    public void handleSettingsRemoved(SettingsInfo settings) {}

    @Override
    public void handleLoggingChange(
            LoggingInfo logging, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {}

    @Override
    public void handlePostLoggingChange(LoggingInfo logging) {}

    @Override
    public void handlePostServiceChange(ServiceInfo service) {}

    @Override
    public void handleServiceChange(
            ServiceInfo service, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {}

    @Override
    public void handleServiceRemove(ServiceInfo service) {}

    @Override
    public void reloaded() {}
}
