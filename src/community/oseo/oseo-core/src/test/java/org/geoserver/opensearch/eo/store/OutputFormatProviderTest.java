/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.WMSInfo;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

/** Needs to be an integration test as output format lookup requies a working application context */
public class OutputFormatProviderTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // just add a few raster layers
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // remove caching from one raster layer
        Catalog catalog = getCatalog();
        GWC gwc = GWC.get();
        gwc.removeTileLayers(Arrays.asList(getLayerId(SystemTestData.TASMANIA_DEM)));
    }

    @Before
    public void resetServices() throws Exception {
        // reset the WMS service
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.setGetMapMimeTypeCheckingEnabled(false);
        wms.getGetMapMimeTypes().clear();
        gs.save(wms);
    }

    @Test
    public void testWMS() throws Exception {
        List<String> formats = OutputFormatProvider.getFormatNames("wms", null);
        assertThat(formats, hasItems("image/png", "image/jpeg", "image/png8"));
        assertThat(formats, not(hasItems("PNG", "JPEG")));
    }

    @Test
    public void testWMSRestricted() throws Exception {
        // setup restricted formats
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.setGetMapMimeTypeCheckingEnabled(true);
        wms.getGetMapMimeTypes().addAll(List.of("image/png", "image/jpeg"));
        gs.save(wms);

        List<String> formats = OutputFormatProvider.getFormatNames("wms", null);
        assertThat(formats, hasItems("image/png", "image/jpeg"));
        assertThat(formats, not(hasItems("PNG", "JPEG", "image/png8")));
    }

    @Test
    public void testWCS() throws Exception {
        List<String> formats = OutputFormatProvider.getFormatNames("wcs", null);
        assertThat(formats, hasItems("image/geotiff", "application/gml+xml"));
        assertThat(formats, not(hasItems("PNG", "JPEG", "image/png8")));
    }

    @Test
    public void testWMTSUncachedLayer() throws Exception {
        LayerInfo layer = getCatalog().getLayerByName(getLayerId(SystemTestData.TASMANIA_DEM));
        List<String> formats = OutputFormatProvider.getFormatNames("wMtS", layer);
        assertThat(formats, Matchers.empty());
    }

    @Test
    public void testWMTS() throws Exception {
        LayerInfo layer = getCatalog().getLayerByName(getLayerId(SystemTestData.TASMANIA_BM));
        List<String> formats = OutputFormatProvider.getFormatNames("wMtS", layer);
        assertThat(formats, hasItems("image/png", "image/jpeg"));
    }
}
