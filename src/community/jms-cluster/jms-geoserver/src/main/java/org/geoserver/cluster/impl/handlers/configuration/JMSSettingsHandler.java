/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.configuration;

import com.thoughtworks.xstream.XStream;
import java.util.logging.Level;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cluster.events.ToggleSwitch;
import org.geoserver.cluster.impl.events.configuration.JMSSettingsModifyEvent;
import org.geoserver.cluster.impl.utils.BeanUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;

/** */
public class JMSSettingsHandler extends JMSConfigurationHandler<JMSSettingsModifyEvent> {

    private final GeoServer geoServer;
    private final ToggleSwitch producer;

    public JMSSettingsHandler(GeoServer geo, XStream xstream, Class clazz, ToggleSwitch producer) {
        super(xstream, clazz);
        this.geoServer = geo;
        this.producer = producer;
    }

    @Override
    protected void omitFields(final XStream xstream) {
        xstream.omitField(GeoServer.class, "geoServer");
    }

    @Override
    public boolean synchronize(JMSSettingsModifyEvent event) throws Exception {
        if (event == null) {
            throw new NullPointerException("Incoming event is NULL.");
        }
        try {
            // disable the message producer to avoid recursion
            producer.disable();
            // let's see which type of event we have and handle it
            switch (event.getEventType()) {
                case MODIFIED:
                    handleModifiedSettings(event);
                    break;
                case ADDED:
                    handleAddedSettings(event);
                    break;
                case REMOVED:
                    handleRemovedSettings(event);
                    break;
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Error handling settings event.", exception);
            throw exception;
        } finally {
            // enabling the events producer again
            producer.enable();
        }
        return true;
    }

    private void handleModifiedSettings(JMSSettingsModifyEvent event) {
        // let's extract some useful information from the event
        WorkspaceInfo workspace = event.getSource().getWorkspace();
        // settings are global or specific to a certain workspace
        SettingsInfo settingsInfo =
                workspace == null ? geoServer.getSettings() : geoServer.getSettings(workspace);
        // if not settings were found this means that a user just deleted this workspace
        // or deleted this workspace settings on this GeoServer instance or that a previously
        // synchronization problem happened
        if (settingsInfo == null) {
            throw new IllegalArgumentException(
                    String.format(
                            "No settings for workspace '%s' found on this instance.",
                            workspace.getName()));
        }
        // well let's update our settings updating only the modified properties
        try {
            BeanUtils.smartUpdate(settingsInfo, event.getPropertyNames(), event.getNewValues());
        } catch (Exception exception) {
            String message =
                    workspace == null
                            ? "Error updating GeoServer global settings."
                            : "Error updating workspace '%s' settings.";
            throw new RuntimeException(String.format(message, workspace), exception);
        }
        // save the updated settings
        geoServer.save(settingsInfo);
    }

    private void handleAddedSettings(JMSSettingsModifyEvent event) {
        // we only need to save the new settings, if the workspace associated
        // with this settings doesn't exists or this settings already exists
        // GeoServer will complain about it with a proper exception
        geoServer.add(event.getSource());
    }

    private void handleRemovedSettings(JMSSettingsModifyEvent event) {
        // we only need to remove the new settings
        geoServer.remove(event.getSource());
    }
}
