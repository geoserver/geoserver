/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.settings;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.geoserver.config.SettingsInfo;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.web.ComponentInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.admin.GlobalSettingsPage;
import org.geoserver.web.data.workspace.WorkspaceEditPage;

/**
 * This class must be used for implementing new Components which must be added to the {@link
 * ListView}s inside the {@link WorkspaceEditPage} and {@link GlobalSettingsPage}.
 *
 * @author Nicola Lagomarsini Geosolutions S.A.S.
 */
public class SettingsPluginPanelInfo extends ComponentInfo<SettingsPluginPanel>
        implements ExtensionPriority {
    private static final long serialVersionUID = 3630664243092125954L;
    private int priority = 50;

    public SettingsPluginPanel getPluginPanel(String id, IModel<SettingsInfo> model)
            throws IllegalArgumentException, SecurityException, InstantiationException,
                    IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return getComponentClass()
                .getConstructor(String.class, IModel.class)
                .newInstance("content", model);
    }

    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * Method for setting the priority of the Object. This is used for ordering the various plugin
     * panels.
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * This method creates a pluggable ListView which can store various panels. All the elements
     * must implement the {@link SettingsPluginPanelInfo} class.
     */
    public static ListView<SettingsPluginPanelInfo> createExtensions(
            String id, final IModel<SettingsInfo> model, GeoServerApplication application) {
        // List of all the pluggable components
        List<SettingsPluginPanelInfo> panels =
                application.getBeansOfType(SettingsPluginPanelInfo.class);

        return new ListView<SettingsPluginPanelInfo>(id, panels) {

            /** */
            private static final long serialVersionUID = 3967381810650109343L;

            @Override
            protected void populateItem(ListItem<SettingsPluginPanelInfo> item) {
                // Object stored inside the item
                SettingsPluginPanelInfo info = item.getModelObject();
                // Panel created by the SettingsPluginPanelInfo object
                SettingsPluginPanel panel;
                try {
                    // Panel creation
                    panel = info.getPluginPanel("content", model);
                    // Panel setting inside the ListView
                    item.add(panel);
                } catch (Exception e) {
                    throw new WicketRuntimeException(
                            "Failed to create extension panel of "
                                    + "type "
                                    + info.getComponentClass().getSimpleName(),
                            e);
                }
            }
        };
    }
}
