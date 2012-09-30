/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class GWCSettingsPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testPageLoad() {
        GWCSettingsPage page = new GWCSettingsPage();

        tester.startPage(page);
        tester.assertRenderedPage(GWCSettingsPage.class);

        // print(page, true, true);
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
}
