/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.configuration;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cluster.impl.events.configuration.JMSEventType;
import org.geoserver.cluster.impl.events.configuration.JMSSettingsModifyEvent;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JmsWorkspaceHandlerTest extends GeoServerSystemTestSupport {

    private WorkspaceInfo testWorkspace;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:TestContext.xml");
    }

    @Before
    public void setup() {
        // create a test workspace
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("jms-test-workspace");
        workspace.setName("jms-test-workspace");
        getCatalog().add(workspace);
        testWorkspace = workspace;
    }

    @After
    public void clean() {
        // remove test workspace
        getCatalog().remove(getCatalog().getWorkspace("jms-test-workspace"));
    }

    @Test
    public void testSettingsSimpleCrud() throws Exception {
        // settings events handler
        JMSSettingsHandler handler = createHandler();
        // create a new settings
        handler.synchronize(createNewSettingsEvent("settings1", "settings1"));
        checkSettingsExists("settings1");
        // update settings
        handler.synchronize(createModifySettingsEvent("settings2"));
        checkSettingsExists("settings2");
        // delete settings
        handler.synchronize(createRemoveSettings());
        assertThat(getGeoServer().getSettings(testWorkspace), nullValue());
    }

    private void checkSettingsExists(String settingsTile) {
        SettingsInfo settingsInfo = getGeoServer().getSettings(testWorkspace);
        assertThat(settingsInfo, notNullValue());
        assertThat(settingsInfo.getTitle(), is(settingsTile));
    }

    private JMSSettingsModifyEvent createNewSettingsEvent(String settingsId, String settingsTitle) {
        // our settings information
        SettingsInfoImpl settingsInfo = new SettingsInfoImpl();
        settingsInfo.setId(settingsId);
        settingsInfo.setTitle(settingsTitle);
        settingsInfo.setWorkspace(testWorkspace);
        // create jms settings modify event
        return new JMSSettingsModifyEvent(settingsInfo, JMSEventType.ADDED);
    }

    private JMSSettingsModifyEvent createModifySettingsEvent(String newSettingsTitle) {
        // settings information
        SettingsInfo settingsInfo = getGeoServer().getSettings(testWorkspace);
        String oldSettingsTitle = settingsInfo.getTitle();
        settingsInfo.setTitle(newSettingsTitle);
        // create jms settings modify event
        return new JMSSettingsModifyEvent(
                settingsInfo,
                Collections.singletonList("title"),
                Collections.singletonList(oldSettingsTitle),
                Collections.singletonList(newSettingsTitle),
                JMSEventType.MODIFIED);
    }

    private JMSSettingsModifyEvent createRemoveSettings() {
        // our settings information
        SettingsInfo settingsInfo = getGeoServer().getSettings(testWorkspace);
        // create jms settings modify event
        return new JMSSettingsModifyEvent(settingsInfo, JMSEventType.REMOVED);
    }

    private JMSSettingsHandler createHandler() {
        JMSSettingsHandlerSPI handlerSpi = GeoServerExtensions.bean(JMSSettingsHandlerSPI.class);
        return (JMSSettingsHandler) handlerSpi.createHandler();
    }
}
