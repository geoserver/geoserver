/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.Result;
import org.geoserver.catalog.PublishedType;
import org.geoserver.gwc.ConfigurableLockProvider;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.util.DimensionWarning.WarningType;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.locks.MemoryLockProvider;
import org.geowebcache.locks.NIOLockProvider;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration;
import org.geowebcache.storage.blobstore.memory.guava.GuavaCacheProvider;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class GWCSettingsPageTest extends GeoServerWicketTestSupport {

    @Before
    public void loginBefore() {
        super.login();
    }

    @Before
    public void cleanupBogusVectorFormat() throws IOException {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.getDefaultVectorCacheFormats().remove("foo/bar");
        gwc.saveConfig(config);
    }

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
        config.setCacheLayersByDefault(true);
        config.setLockProviderName(null);
        config.setInnerCachingEnabled(false);
        gwc.saveConfig(config);
    }

    @Test
    public void testEditDirectWMSIntegration() {
        GWC gwc = GWC.get();
        boolean directWMSIntegrationEnabled = gwc.getConfig().isDirectWMSIntegrationEnabled();
        testEditCheckboxOption(
                "form:gwcServicesPanel:enableWMSIntegration",
                "gwcServicesPanel:enableWMSIntegration",
                directWMSIntegrationEnabled);

        assertEquals(!directWMSIntegrationEnabled, gwc.getConfig().isDirectWMSIntegrationEnabled());
    }

    @Test
    public void testEditRequireTiledParameter() {
        GWC gwc = GWC.get();
        boolean enabled = gwc.getConfig().isRequireTiledParameter();
        testEditCheckboxOption(
                "form:gwcServicesPanel:requireTiledParameter",
                "gwcServicesPanel:requireTiledParameter",
                enabled);
        assertEquals(!enabled, gwc.getConfig().isRequireTiledParameter());
    }

    @Test
    public void testRequireTiledParameterDefaultsTrue() {
        GWC gwc = GWC.get();
        boolean enabled = gwc.getConfig().isRequireTiledParameter();
        assertTrue(enabled);
        GWCSettingsPage page = new GWCSettingsPage();

        tester.startPage(page);
        // print(page, true, true);
        tester.assertRenderedPage(GWCSettingsPage.class);

        assertNotNull(
                "form:gwcServicesPanel:requireTiledParameter",
                tester.getComponentFromLastRenderedPage(
                        "form:gwcServicesPanel:requireTiledParameter"));
        tester.assertModelValue("form:gwcServicesPanel:requireTiledParameter", true);
    }

    @Test
    public void testEditEnableWMSC() {
        GWC gwc = GWC.get();
        boolean enabled = gwc.getConfig().isWMSCEnabled();
        testEditCheckboxOption(
                "form:gwcServicesPanel:enableWMSC", "gwcServicesPanel:enableWMSC", enabled);
        assertEquals(!enabled, gwc.getConfig().isWMSCEnabled());
    }

    @Test
    public void testEnableCacheLayersByDefault() throws Exception {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setCacheLayersByDefault(false);
        gwc.saveConfig(config);
        assertFalse(gwc.getConfig().isCacheLayersByDefault());

        testEditCheckboxOption(
                "form:cachingOptionsPanel:cacheLayersByDefault",
                "cachingOptionsPanel:cacheLayersByDefault",
                false);

        assertTrue(gwc.getConfig().isCacheLayersByDefault());
    }

    @Test
    public void testDisableCacheLayersByDefault() throws Exception {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setCacheLayersByDefault(true);
        gwc.saveConfig(config);
        assertTrue(gwc.getConfig().isCacheLayersByDefault());

        testEditCheckboxOption(
                "form:cachingOptionsPanel:cacheLayersByDefault",
                "cachingOptionsPanel:cacheLayersByDefault",
                true);

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

        testEditCheckboxOption(
                "form:cachingOptionsPanel:container:configs:cacheNonDefaultStyles",
                "cachingOptionsPanel:container:configs:cacheNonDefaultStyles",
                false);

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

        testEditCheckboxOption(
                "form:cachingOptionsPanel:container:configs:cacheNonDefaultStyles",
                "cachingOptionsPanel:container:configs:cacheNonDefaultStyles",
                true);

        assertFalse(gwc.getConfig().isCacheNonDefaultStyles());
    }

    @Test
    public void testSetDefaultCacheFormats() throws Exception {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setCacheLayersByDefault(true);
        config.getDefaultVectorCacheFormats().add("foo/bar");
        gwc.saveConfig(config);

        GWCSettingsPage page = new GWCSettingsPage();
        tester.startPage(page);
        // print(page, true, true);
        tester.assertRenderedPage(GWCSettingsPage.class);

        final List<String> vectorFormats =
                new ArrayList<>(GWC.getAdvertisedCachedFormats(PublishedType.VECTOR));
        vectorFormats.add("foo/bar");
        final List<String> rasterFormats =
                new ArrayList<>(GWC.getAdvertisedCachedFormats(PublishedType.RASTER));
        final List<String> groupFormats =
                new ArrayList<>(GWC.getAdvertisedCachedFormats(PublishedType.GROUP));

        tester.assertListView(
                "form:cachingOptionsPanel:container:configs:vectorFormatsGroup:vectorFromats",
                vectorFormats);
        tester.assertListView(
                "form:cachingOptionsPanel:container:configs:rasterFormatsGroup:rasterFromats",
                rasterFormats);
        tester.assertListView(
                "form:cachingOptionsPanel:container:configs:otherFormatsGroup:otherFromats",
                groupFormats);

        FormTester form = tester.newFormTester("form");
        final boolean replace = true; // tell selectMultiple to first set all options to false
        form.selectMultiple(
                "cachingOptionsPanel:container:configs:vectorFormatsGroup",
                new int[] {1, 3},
                replace);
        form.selectMultiple(
                "cachingOptionsPanel:container:configs:rasterFormatsGroup",
                new int[] {1, 3},
                replace);
        form.selectMultiple(
                "cachingOptionsPanel:container:configs:otherFormatsGroup",
                new int[] {1, 3},
                replace);
        // print(page, true, true);
        form.submit("submit");

        tester.assertRenderedPage(GeoServerHomePage.class);

        assertEquals(
                newHashSet(vectorFormats.get(1), vectorFormats.get(3)),
                gwc.getConfig().getDefaultVectorCacheFormats());
        assertEquals(
                newHashSet(rasterFormats.get(1), rasterFormats.get(3)),
                gwc.getConfig().getDefaultCoverageCacheFormats());
        assertEquals(
                newHashSet(groupFormats.get(1), groupFormats.get(3)),
                gwc.getConfig().getDefaultOtherCacheFormats());
    }

    private void testEditCheckboxOption(
            final String pagePath, final String formPath, final Boolean initialValue) {

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

    @SuppressWarnings("unchecked")
    @Test
    public void testEditLockProvider() {
        GWC gwc = GWC.get();
        ConfigurableLockProvider lockProvider = (ConfigurableLockProvider) gwc.getLockProvider();
        assertTrue(lockProvider.getDelegate() instanceof MemoryLockProvider);

        GWCSettingsPage page = new GWCSettingsPage();
        tester.startPage(page);
        tester.assertRenderedPage(GWCSettingsPage.class);

        // determine in a future proof way which item contains nioLock
        DropDownChoice<String> lockDropDown =
                (DropDownChoice<String>)
                        tester.getComponentFromLastRenderedPage(
                                "form:cachingOptionsPanel:container:configs:lockProvider");
        List<String> choices = (List<String>) lockDropDown.getChoices();
        int nioLockIndex = -1;
        for (int i = 0; i < choices.size(); i++) {
            if ("nioLock".equals(choices.get(i))) {
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

    @SuppressWarnings("unchecked")
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
        tester.assertComponent(
                "form:cachingOptionsPanel:container:configs:cachedGridsets",
                DefaultGridsetsEditor.class);
        // Get the available GridSets
        DropDownChoice<String> availableItems =
                (DropDownChoice<String>)
                        tester.getComponentFromLastRenderedPage(
                                "form:cachingOptionsPanel:container:configs:cachedGridsets:availableGridsets");
        // Ensure the component is present
        assertNotNull(availableItems);
        // Get the first item
        String item = availableItems.getChoices().get(0);
        // Ensure the item is not null
        assertNotNull(item);
        // Ensure the item is CanadianNAD83_LCC
        assertEquals("CanadianNAD83_LCC", item);

        // Selection of the form tests
        FormTester form = tester.newFormTester("form", false);
        form.select("cachingOptionsPanel:container:configs:cachedGridsets:availableGridsets", 0);
        tester.executeAjaxEvent(
                "form:cachingOptionsPanel:container:configs:cachedGridsets:addGridset", "click");
        // Check that the page is correctly rendered
        tester.assertRenderedPage(GWCSettingsPage.class);
        // Save the changes
        form = tester.newFormTester("form", false);
        form.select("cachingOptionsPanel:container:configs:cachedGridsets:availableGridsets", 0);
        form.submit("submit");
        // Check no exception has been thrown
        tester.assertNoErrorMessage();
        // Restart the page
        tester.startPage(page);
        // Ensure the page is correctly rendered
        tester.assertRenderedPage(GWCSettingsPage.class);
        // Get the list of available elements
        availableItems =
                (DropDownChoice<String>)
                        tester.getComponentFromLastRenderedPage(
                                "form:cachingOptionsPanel:container:configs:cachedGridsets:availableGridsets");
        // Ensure that the one used above is no more present
        assertFalse(availableItems.getChoices().contains(item));
    }

    @Test
    public void testEnableDisableInnerCaching() throws Exception {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        // Creation of a new page to test
        GWCSettingsPage page = new GWCSettingsPage();
        // Start the page
        tester.startPage(page);
        // Ensure the page is correctly rendered
        tester.assertRenderedPage(GWCSettingsPage.class);
        // Ensure the component blobstores belongs to the BlobStorePanel class
        tester.assertComponent(
                "form:cachingOptionsPanel:container:configs:blobstores",
                InMemoryBlobStorePanel.class);

        // Selection of the form tests
        FormTester form = tester.newFormTester("form");
        form.setValue("cachingOptionsPanel:container:configs:blobstores:innerCachingEnabled", true);
        // Check that the page is correctly rendered
        tester.assertRenderedPage(GWCSettingsPage.class);
        // Save the changes
        form.submit("submit");
        // Check no exception has been thrown
        tester.assertNoErrorMessage();
        // Check the GWCConfig
        config = gwc.getConfig();
        assertTrue(config.isInnerCachingEnabled());

        // Start the page
        tester.startPage(new GWCSettingsPage());
        // Ensure the page is correctly rendered
        tester.assertRenderedPage(GWCSettingsPage.class);

        // Check if the Cache Provider is GuavaCacheProvider
        tester.assertComponent(
                "form:cachingOptionsPanel:container:configs:blobstores:container:caches",
                DropDownChoice.class);
        @SuppressWarnings("unchecked")
        DropDownChoice<String> choice =
                (DropDownChoice<String>)
                        tester.getComponentFromLastRenderedPage(
                                "form:cachingOptionsPanel:container:configs:blobstores:container:caches");
        assertTrue(
                choice.getChoices().get(0).equalsIgnoreCase(GuavaCacheProvider.class.toString()));

        // Ensure that the other fields are enabled
        Component comp1 =
                tester.getComponentFromLastRenderedPage(
                        "form:cachingOptionsPanel:container:configs:blobstores:container:cacheConfContainer:hardMemoryLimit");
        Component comp2 =
                tester.getComponentFromLastRenderedPage(
                        "form:cachingOptionsPanel:container:configs:blobstores:container:cacheConfContainer:concurrencyLevel");

        assertTrue(comp1.isEnabled());
        assertTrue(comp2.isEnabled());

        // Selection of the form tests
        form = tester.newFormTester("form");
        form.setValue(
                "cachingOptionsPanel:container:configs:blobstores:container:persistenceEnabled",
                true);
        form.setValue(
                "cachingOptionsPanel:container:configs:blobstores:container:cacheConfContainer:hardMemoryLimit",
                1 + "");
        form.setValue(
                "cachingOptionsPanel:container:configs:blobstores:container:cacheConfContainer:concurrencyLevel",
                1 + "");
        // Check that the page is correctly rendered
        tester.assertRenderedPage(GWCSettingsPage.class);
        // Save the changes
        form.submit("submit");
        // Check no exception has been thrown
        tester.assertNoErrorMessage();
        // Check the GWCConfig
        config = gwc.getConfig();
        assertTrue(config.isPersistenceEnabled());
        assertEquals(
                config.getCacheConfigurations()
                        .get(GuavaCacheProvider.class.toString())
                        .getConcurrencyLevel(),
                1);
        assertEquals(
                config.getCacheConfigurations()
                        .get(GuavaCacheProvider.class.toString())
                        .getHardMemoryLimit(),
                1);

        // Start the page
        tester.startPage(new GWCSettingsPage());
        // Ensure the page is correctly rendered
        tester.assertRenderedPage(GWCSettingsPage.class);

        // Selection of the form tests
        form = tester.newFormTester("form");
        form.setValue(
                "cachingOptionsPanel:container:configs:blobstores:innerCachingEnabled", false);
        // Save the changes
        form.submit("submit");

        // Start the page
        tester.startPage(new GWCSettingsPage());
        // Ensure the page is correctly rendered
        tester.assertRenderedPage(GWCSettingsPage.class);
        Result res =
                tester.isVisible(
                        "form:cachingOptionsPanel:container:configs:blobstores:container:persistenceEnabled");
        assertTrue(res.wasFailed());
        // Check the GWCConfig
        config = gwc.getConfig();
        assertFalse(config.isInnerCachingEnabled());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEvictionPolicy() {

        // creating a start the gwc configuration page
        GWCSettingsPage page = new GWCSettingsPage();
        tester.startPage(page);

        // enabling the cache
        FormTester form = tester.newFormTester("form");
        form.setValue("cachingOptionsPanel:container:configs:blobstores:innerCachingEnabled", true);
        form.submit("submit");
        tester.assertRenderedPage(GeoServerHomePage.class);

        tester.startPage(new GWCSettingsPage());

        // check that the cache provider is guava
        tester.assertComponent(
                "form:cachingOptionsPanel:container:configs:blobstores:container:caches",
                DropDownChoice.class);
        DropDownChoice<String> choice =
                (DropDownChoice<String>)
                        tester.getComponentFromLastRenderedPage(
                                "form:cachingOptionsPanel:container:configs:blobstores:container:caches");
        assertTrue(
                choice.getChoices().get(0).equalsIgnoreCase(GuavaCacheProvider.class.toString()));

        // check that only guava supported eviction policies are available
        DropDownChoice<String> evictionPoliciesDropDown =
                (DropDownChoice<String>)
                        tester.getComponentFromLastRenderedPage(
                                "form:cachingOptionsPanel:container:configs:blobstores:container:cacheConfContainer:policy");
        List evictionPolicies = evictionPoliciesDropDown.getChoices();
        assertEquals(3, evictionPolicies.size());
        assertTrue(evictionPolicies.contains(CacheConfiguration.EvictionPolicy.NULL));
        assertTrue(
                evictionPolicies.contains(CacheConfiguration.EvictionPolicy.EXPIRE_AFTER_ACCESS));
        assertTrue(evictionPolicies.contains(CacheConfiguration.EvictionPolicy.EXPIRE_AFTER_WRITE));
    }

    @Test
    public void testApply() {
        // creating a start the gwc configuration page
        GWCSettingsPage page = new GWCSettingsPage();
        tester.startPage(page);

        // enabling the cache
        FormTester form = tester.newFormTester("form");
        form.setValue("cachingOptionsPanel:container:configs:blobstores:innerCachingEnabled", true);
        form.submit("apply");

        // check it did not go back to home
        tester.assertRenderedPage(GWCSettingsPage.class);

        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        assertTrue(config.isInnerCachingEnabled());
    }

    @Test
    public void testMemoryCachePanelOpen() throws IOException {
        // enable in memory caching
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setInnerCachingEnabled(true);
        gwc.saveConfig(config);

        // used to blow because an unused label element was added in the code but not in HTML
        GWCSettingsPage page = new GWCSettingsPage();
        tester.startPage(page);
        tester.assertVisible("form:cachingOptionsPanel:container:configs:blobstores:container");
    }

    @Test
    public void testSaveWarningSkips() {
        // Start the page
        tester.startPage(GWCSettingsPage.class);

        FormTester ft = tester.newFormTester("form");
        String checksPath = "cachingOptionsPanel:container:configs:warningSkips:warningSkipsGroup";
        ft.select(checksPath, 0);
        ft.select(checksPath, 2);
        ft.submit("submit");

        tester.assertNoErrorMessage();

        // check skips have been configured
        GWC gwc = GWC.get();
        GWCConfig gwcConfig = gwc.getConfig();
        assertThat(
                gwcConfig.getCacheWarningSkips(),
                Matchers.containsInAnyOrder(WarningType.Default, WarningType.FailedNearest));
    }
}
