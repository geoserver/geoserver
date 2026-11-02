/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.hazelcast.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.gwc.ConfigurableLockProvider;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.web.GWCSettingsPage;
import org.geoserver.gwc.web.tilecache.TileCacheBackendType;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.hazelcast.HazelcastClientTileCacheBackendInfo;
import org.geowebcache.hazelcast.HazelcastEmbeddedTileCacheBackendInfo;
import org.geowebcache.hazelcast.HazelcastLockProvider;
import org.geowebcache.storage.tilecache.TileCacheBackendInfo;
import org.geowebcache.storage.tilecache.TileCacheInfo;
import org.junit.Before;
import org.junit.Test;

/** Switches the caching defaults page to each Hazelcast backend and to the Hazelcast locks, and saves them. */
public class HazelcastTileCacheBackendTypeTest extends GeoServerWicketTestSupport {

    private static final String SETTINGS = "tileCachePanel:settings:";

    private static final String BACKEND = SETTINGS + "backend";

    private static final String BACKEND_PANEL = SETTINGS + "backendContainer:backendPanel:";

    @Before
    public void enableTileCache() throws IOException {
        login();
        TileCacheInfo info = new TileCacheInfo();
        info.setEnabled(true);
        xmlConfig().setTileCache(info);
    }

    private static XMLConfiguration xmlConfig() {
        return GeoServerExtensions.bean(XMLConfiguration.class);
    }

    @Test
    public void testClientSettingsSaved() {
        FormTester form = selectBackend(HazelcastClientTileCacheBackendInfo.class);
        form.setValue(BACKEND_PANEL + "clusterName", "gwc");
        // unreachable, should saving ever start the client
        form.setValue(BACKEND_PANEL + "members", "127.0.0.1:1, 127.0.0.2:1");
        form.setValue(BACKEND_PANEL + "mapName", "tiles");
        form.submit("submit");

        tester.assertNoErrorMessage();
        HazelcastClientTileCacheBackendInfo saved = (HazelcastClientTileCacheBackendInfo) savedBackend();
        assertEquals("gwc", saved.getClusterName());
        assertEquals("127.0.0.1:1, 127.0.0.2:1", saved.getMembers());
        assertEquals("tiles", saved.getMapName());
    }

    @Test
    public void testEmbeddedSettingsSaved() {
        FormTester form = selectBackend(HazelcastEmbeddedTileCacheBackendInfo.class);
        form.setValue(BACKEND_PANEL + "mapName", "tiles");
        form.submit("submit");

        tester.assertNoErrorMessage();
        assertEquals("tiles", ((HazelcastEmbeddedTileCacheBackendInfo) savedBackend()).getMapName());
    }

    @Test
    public void testHazelcastLockSelected() {
        String lockProvider = "cachingOptionsPanel:container:configs:lockProvider";
        tester.startPage(GWCSettingsPage.class);
        DropDownChoice<?> locks = (DropDownChoice<?>) tester.getComponentFromLastRenderedPage("form:" + lockProvider);
        int index = locks.getChoices().indexOf("hazelcastLock");
        assertThat(index, greaterThanOrEqualTo(0));
        tester.assertContains("Hazelcast cluster locking");

        FormTester form = tester.newFormTester("form");
        form.select(lockProvider, index);
        form.submit("submit");

        tester.assertNoErrorMessage();
        ConfigurableLockProvider selected = (ConfigurableLockProvider) GWC.get().getLockProvider();
        assertThat(selected.getDelegate(), instanceOf(HazelcastLockProvider.class));
    }

    /** Opens the page and picks the backend editing {@code settingsClass}, returning a form showing its panel. */
    private FormTester selectBackend(Class<? extends TileCacheBackendInfo> settingsClass) {
        tester.startPage(GWCSettingsPage.class);
        DropDownChoice<?> backend = (DropDownChoice<?>) tester.getComponentFromLastRenderedPage("form:" + BACKEND);
        List<?> types = backend.getChoices();
        int index = IntStream.range(0, types.size())
                .filter(i -> ((TileCacheBackendType) types.get(i)).getSettingsClass() == settingsClass)
                .findFirst()
                .orElseThrow();

        tester.newFormTester("form").select(BACKEND, index);
        tester.executeAjaxEvent("form:" + BACKEND, "change");
        return tester.newFormTester("form");
    }

    private static TileCacheBackendInfo savedBackend() {
        return xmlConfig().getTileCache().getBackend();
    }
}
