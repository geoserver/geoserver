/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.web.GWCSettingsPage;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.web.ComponentInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * This class must be used for implementing new Components which must be added to the {@link ListView} inside {@link GWCSettingsPage}.
 * 
 * @author Nicola Lagomarsini Geosolutions S.A.S.
 * 
 */
public class GWCSettingsPluginPanelInfo extends ComponentInfo<GWCSettingsPluginPanel> implements
        ExtensionPriority {

    private int priority = 50;

    public GWCSettingsPluginPanel getPluginPanel(String id, IModel<GWCConfig> model)
            throws IllegalArgumentException, SecurityException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return getComponentClass().getConstructor(String.class, IModel.class).newInstance(
                "content", model);
    }

    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * Method for setting the priority of the Object. This is used for ordering the various plugin panels.
     * 
     * @param priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * This method creates a pluggable ListView which can store various panels. All the elements must implement the {@link GWCSettingsPluginPanelInfo}
     * class.
     * 
     * @param id
     * @param model
     * @param application
     * @return
     */
    public static ListView createExtensions(String id, final IModel<GWCConfig> model,
            GeoServerApplication application) {
        // List of all the pluggable components
        List<GWCSettingsPluginPanelInfo> panels = application
                .getBeansOfType(GWCSettingsPluginPanelInfo.class);

        return new ListView<GWCSettingsPluginPanelInfo>(id, panels) {

            @Override
            protected void populateItem(ListItem<GWCSettingsPluginPanelInfo> item) {
                // Object stored inside the item
                GWCSettingsPluginPanelInfo info = item.getModelObject();
                // Panel created by the SettingsPluginPanelInfo object
                GWCSettingsPluginPanel panel;
                try {
                    // Panel creation
                    panel = info.getPluginPanel("content", model);
                    // Panel setting inside the ListView
                    item.add(panel);
                } catch (Exception e) {
                    throw new WicketRuntimeException("Failed to create extension panel of "
                            + "type " + info.getComponentClass().getSimpleName(), e);
                }
            }
        };
    }
}