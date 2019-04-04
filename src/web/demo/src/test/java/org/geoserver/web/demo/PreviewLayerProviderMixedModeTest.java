/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */ package org.geoserver.web.demo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;
import org.opengis.filter.Filter;

public class PreviewLayerProviderMixedModeTest extends GeoServerWicketTestSupport {

    /** Add the test resource access manager in the spring context */
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                "classpath:/org/geoserver/web/demo/ResourceAccessManagerContext.xml");
    }

    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    /** Add the users */
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addUser("cite", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_mixed", "cite", null, Collections.singletonList("ROLE_DUMMY"));

        // populate the access manager
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        Catalog catalog = getCatalog();
        FeatureTypeInfo buildings =
                catalog.getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));

        // user in mixed mode
        tam.putLimits(
                "cite_mixed",
                buildings,
                new VectorAccessLimits(
                        CatalogMode.MIXED, null, Filter.EXCLUDE, null, Filter.EXCLUDE));
    }

    @Test
    public void testMixedMode() throws Exception {
        PreviewLayerProvider provider = new PreviewLayerProvider();

        // full access
        login("cite", "cite");
        assertTrue(previewHasBuildings(provider));

        // no access, but no exception either, since this is not a direct access
        login("cite_mixed", "cite");
        assertFalse(previewHasBuildings(provider));
    }

    private boolean previewHasBuildings(PreviewLayerProvider provider) {
        Iterator<PreviewLayer> it = provider.iterator(0, provider.size());
        String buildingsPrefixedName = getLayerId(SystemTestData.BUILDINGS);
        while (it.hasNext()) {
            PreviewLayer pl = it.next();
            if (buildingsPrefixedName.equals(pl.getName())) {
                return true;
            }
        }
        return false;
    }

    private PreviewLayer getPreviewLayer(PreviewLayerProvider provider, String prefixedName) {
        for (PreviewLayer pl : Lists.newArrayList(provider.iterator(0, Integer.MAX_VALUE))) {
            if (pl.getName().equals(prefixedName)) {
                return pl;
            }
        }
        return null;
    }
}
