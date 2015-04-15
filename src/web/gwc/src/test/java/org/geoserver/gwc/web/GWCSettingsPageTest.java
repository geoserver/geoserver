/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Sets.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.gwc.ConfigurableLockProvider;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.web.plugin.DefaultCachingPluginPanel;
import org.geoserver.gwc.web.plugin.GWCSettingsPluginPanel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.locks.MemoryLockProvider;
import org.geowebcache.locks.NIOLockProvider;
import org.junit.Before;
import org.junit.Test;

public class GWCSettingsPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testPageLoad() {
        GWCSettingsPage page = new GWCSettingsPage();

        tester.startPage(page);
        tester.assertRenderedPage(GWCSettingsPage.class);

        // print(page, true, true);
    }
    
    @Before
    public void cleanup() throws IOException {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setLockProviderName(null);
        gwc.saveConfig(config);
    }

    @Test
    public void testEditDirectWMSIntegration() {
        GWC gwc = GWC.get();
        boolean directWMSIntegrationEnabled = gwc.getConfig().isDirectWMSIntegrationEnabled();
        testEditCheckboxOption("form:gwcServicesPanel:enableWMSIntegration",
                "gwcServicesPanel:enableWMSIntegration", directWMSIntegrationEnabled);

        assertEquals(!directWMSIntegrationEnabled, gwc.getConfig().isDirectWMSIntegrationEnabled());
    }

    @Test
    public void testEditEnableWMSC() {
        GWC gwc = GWC.get();
        boolean enabled = gwc.getConfig().isWMSCEnabled();
        testEditCheckboxOption("form:gwcServicesPanel:enableWMSC", "gwcServicesPanel:enableWMSC",
                enabled);
        assertEquals(!enabled, gwc.getConfig().isWMSCEnabled());
    }

    @Test public void testEditEnableWMTS() {
        GWC gwc = GWC.get();
        boolean enabled = gwc.getConfig().isWMTSEnabled();
        testEditCheckboxOption("form:gwcServicesPanel:enableWMTS", "gwcServicesPanel:enableWMTS",
                enabled);
        assertEquals(!enabled, gwc.getConfig().isWMTSEnabled());
    }

    @Test
    public void testEditEnableTMS() {
        GWC gwc = GWC.get();
        boolean enabled = gwc.getConfig().isTMSEnabled();
        testEditCheckboxOption("form:gwcServicesPanel:enableTMS", "gwcServicesPanel:enableTMS",
                enabled);
        assertEquals(!enabled, gwc.getConfig().isTMSEnabled());
    }

    @Test
    public void testEnableCacheLayersByDefault() throws Exception {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setCacheLayersByDefault(false);
        gwc.saveConfig(config);
        assertFalse(gwc.getConfig().isCacheLayersByDefault());

        testEditCheckboxOption("form:cachingOptionsPanel:cacheLayersByDefault",
                "cachingOptionsPanel:cacheLayersByDefault", false);

        assertTrue(gwc.getConfig().isCacheLayersByDefault());
    }

    @Test
    public void testDisableCacheLayersByDefault() throws Exception {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setCacheLayersByDefault(true);
        gwc.saveConfig(config);
        assertTrue(gwc.getConfig().isCacheLayersByDefault());

        testEditCheckboxOption("form:cachingOptionsPanel:cacheLayersByDefault",
                "cachingOptionsPanel:cacheLayersByDefault", true);

        assertFalse(gwc.getConfig().isCacheLayersByDefault());
    }

    @Test
    public void testEnableAutoCacheStyles() throws Exception {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setCacheLayersByDefault(true);
        config.setCacheNonDefaultStyles(false);
        gwc.saveConfig(config);
        assertFalse(gwc.getConfig().isCacheNonDefaultStyles());

        testEditCheckboxOption("form:cachingOptionsPanel:container:configs:cacheNonDefaultStyles",
                "cachingOptionsPanel:container:configs:cacheNonDefaultStyles", false);

        assertTrue(gwc.getConfig().isCacheNonDefaultStyles());
    }

    @Test
    public void testDisableAutoCacheStyles() throws Exception {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setCacheLayersByDefault(true);
        config.setCacheNonDefaultStyles(true);
        gwc.saveConfig(config);
        assertTrue(gwc.getConfig().isCacheNonDefaultStyles());

        testEditCheckboxOption("form:cachingOptionsPanel:container:configs:cacheNonDefaultStyles",
                "cachingOptionsPanel:container:configs:cacheNonDefaultStyles", true);

        assertFalse(gwc.getConfig().isCacheNonDefaultStyles());
    }

    @Test
    public void testSetDefaultCacheFormats() throws Exception {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setCacheLayersByDefault(true);
        gwc.saveConfig(config);

        GWCSettingsPage page = new GWCSettingsPage();
        tester.startPage(page);
        // print(page, true, true);
        tester.assertRenderedPage(GWCSettingsPage.class);

        final List<String> formats = newArrayList("image/png", "image/png8", "image/jpeg",
                "image/gif");

        tester.assertListView(
                "form:cachingOptionsPanel:container:configs:vectorFormatsGroup:vectorFromats",
                formats);
        tester.assertListView(
                "form:cachingOptionsPanel:container:configs:rasterFormatsGroup:rasterFromats",
                formats);
        tester.assertListView(
                "form:cachingOptionsPanel:container:configs:otherFormatsGroup:otherFromats",
                formats);

        FormTester form = tester.newFormTester("form");
        final boolean replace = true;// tell selectMultiple to first set all options to false
        form.selectMultiple("cachingOptionsPanel:container:configs:vectorFormatsGroup", new int[] {
                1, 3 }, replace);
        form.selectMultiple("cachingOptionsPanel:container:configs:rasterFormatsGroup", new int[] {
                1, 3 }, replace);
        form.selectMultiple("cachingOptionsPanel:container:configs:otherFormatsGroup", new int[] {
                1, 3 }, replace);
        // print(page, true, true);
        form.submit("submit");

        tester.assertRenderedPage(GeoServerHomePage.class);

        Set<String> expected = newHashSet(formats.get(1), formats.get(3));
        assertEquals(expected, gwc.getConfig().getDefaultVectorCacheFormats());
        assertEquals(expected, gwc.getConfig().getDefaultCoverageCacheFormats());
        assertEquals(expected, gwc.getConfig().getDefaultOtherCacheFormats());
    }

    private void testEditCheckboxOption(final String pagePath, final String formPath,
            final Boolean initialValue) {

        GWCSettingsPage page = new GWCSettingsPage();

        tester.startPage(page);
        // print(page, true, true);
        tester.assertRenderedPage(GWCSettingsPage.class);

        assertNotNull(pagePath, tester.getComponentFromLastRenderedPage(pagePath));
        tester.assertModelValue(pagePath, initialValue);

        FormTester form = tester.newFormTester("form");
        form.setValue(formPath, !initialValue.booleanValue());
        form.submit("submit");

        tester.assertRenderedPage(GeoServerHomePage.class);
    }
    
    @Test
    public void testEditLockProvider() {
        GWC gwc = GWC.get();
        ConfigurableLockProvider lockProvider = (ConfigurableLockProvider) gwc.getLockProvider();
        assertTrue(lockProvider.getDelegate() instanceof MemoryLockProvider);
        
        GWCSettingsPage page = new GWCSettingsPage();
        tester.startPage(page);
        tester.assertRenderedPage(GWCSettingsPage.class);
        
        // determine in a future proof way which item contains nioLock
        DropDownChoice lockDropDown = (DropDownChoice) tester.getComponentFromLastRenderedPage("form:cachingOptionsPanel:container:configs:lockProvider");
        List choices = lockDropDown.getChoices();
        int nioLockIndex = -1;
        for (int i = 0; i < choices.size(); i++) {
            if("nioLock".equals(choices.get(i))) {
                nioLockIndex = i;
                break;
            }
        }
        assertTrue(nioLockIndex >= 0);
        
        FormTester form = tester.newFormTester("form");
        form.select("cachingOptionsPanel:container:configs:lockProvider", nioLockIndex);
        form.submit("submit");
        
        tester.assertNoErrorMessage();
        
        // check the lock provider has been changed
        lockProvider = (ConfigurableLockProvider) gwc.getLockProvider();
        assertTrue(lockProvider.getDelegate() instanceof NIOLockProvider);
    }

    @Test
    public void testNewDefaultGridSet() throws IOException {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setCacheLayersByDefault(true);
        gwc.saveConfig(config);
        // Creation of a new page to test
        GWCSettingsPage page = new GWCSettingsPage();
        // Start the page
        tester.startPage(page);
        // Ensure the page is correctly rendered
        tester.assertRenderedPage(GWCSettingsPage.class);
        // Ensure the component cachedGridsets belongs to the DefaultGridsetsEditor class
        tester.assertComponent("form:cachingOptionsPanel:container:configs:cachedGridsets",
                DefaultGridsetsEditor.class);
        // Get the available GridSets
        DropDownChoice<String> availableItems = (DropDownChoice<String>) tester
                .getComponentFromLastRenderedPage("form:cachingOptionsPanel:container:configs:cachedGridsets:availableGridsets");
        // Ensure the component is present
        assertNotNull(availableItems);
        // Get the first item
        String item = availableItems.getChoices().get(0);
        // Ensure the item is not null
        assertNotNull(item);
        // Ensure the item is GlobalCRS84Pixel
        assertTrue(item.equalsIgnoreCase("GlobalCRS84Pixel"));

        // Selection of the form tests
        FormTester form = tester.newFormTester("form");
        form.select("cachingOptionsPanel:container:configs:cachedGridsets:availableGridsets", 0);
        tester.executeAjaxEvent(
                "form:cachingOptionsPanel:container:configs:cachedGridsets:addGridset", "onclick");
        // Check that the page is correctly rendered
        tester.assertRenderedPage(GWCSettingsPage.class);
        // Save the changes
        form.submit("submit");
        // Check no exception has been thrown
        tester.assertNoErrorMessage();
        // Restart the page
        tester.startPage(page);
        // Ensure the page is correctly rendered
        tester.assertRenderedPage(GWCSettingsPage.class);
        // Get the list of available elements
        availableItems = (DropDownChoice<String>) tester
                .getComponentFromLastRenderedPage("form:cachingOptionsPanel:container:configs:cachedGridsets:availableGridsets");
        // Ensure that the one used above is no more present
        assertFalse(availableItems.getChoices().contains(item));
    }

    @Test
    public void testExtensions() {
        // Create a new instance of the GWCSettingsPage
        GWCSettingsPage page = new GWCSettingsPage();
        tester.startPage(page);
        tester.assertRenderedPage(GWCSettingsPage.class);

        // Ensure the extension point is defined
        tester.assertComponent("form:extensions", ListView.class);

        // Get the list
        ListView view = (ListView) tester.getComponentFromLastRenderedPage("form:extensions");

        // Ensure at least the default component is present
        List list = view.getList();
        assertTrue(list.size() > 0);

        Iterator it = view.iterator();
        boolean defaultComponentFound = false;
        while (it.hasNext()) {
            ListItem item = (ListItem) it.next();
            GWCSettingsPluginPanel component = (GWCSettingsPluginPanel) item.get("content");
            if (component instanceof DefaultCachingPluginPanel) {
                defaultComponentFound = true;
                break;
            }
        }
        assertTrue(defaultComponentFound);
    }
}
