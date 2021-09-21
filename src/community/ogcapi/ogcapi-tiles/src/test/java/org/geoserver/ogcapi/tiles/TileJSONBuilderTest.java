package org.geoserver.ogcapi.tiles;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.geoserver.ogcapi.APIRequestInfo;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.meta.LayerMetaInformation;
import org.geowebcache.layer.meta.TileJSON;
import org.geowebcache.mbtiles.layer.MBTilesLayer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(APIRequestInfo.class)
@PowerMockIgnore({"jdk.internal.reflect.*"})
public class TileJSONBuilderTest {

    @Test
    public void testMBTiles() throws Exception {

        PowerMock.mockStatic(APIRequestInfo.class);
        APIRequestInfo requestInfo = createNiceMock(APIRequestInfo.class);
        expect(APIRequestInfo.get()).andReturn(requestInfo).anyTimes();
        PowerMock.replay(APIRequestInfo.class);

        expect(requestInfo.getBaseURL()).andReturn("http://localhost:8081/geoserver");
        replay(requestInfo);

        MBTilesLayer mbTilesLayer = mock(MBTilesLayer.class);
        LayerMetaInformation metaInformation = mock(LayerMetaInformation.class);
        GridSubset subset = mock(GridSubset.class);

        expect(mbTilesLayer.getName()).andReturn("countries").anyTimes();
        expect(mbTilesLayer.getGridSubset(anyString())).andReturn(subset).anyTimes();
        expect(mbTilesLayer.getMetaInformation()).andReturn(metaInformation).anyTimes();
        expect(mbTilesLayer.supportsTileJSON()).andReturn(true).anyTimes();
        TileJSON tileJSON = new TileJSON();
        tileJSON.setName("countries");
        tileJSON.setDescription("Natural Earth Data with .shp data from  TileMill");
        tileJSON.setAttribution("Natural Earth Data");
        tileJSON.setMinZoom(0);
        tileJSON.setMaxZoom(6);
        tileJSON.setScheme("xyz");
        expect(mbTilesLayer.getTileJSON()).andReturn(tileJSON);
        replay(mbTilesLayer);

        TileJSONBuilder tileJSONBuilder =
                new TileJSONBuilder(
                        "countries",
                        "application/vnd.mapbox-vector-tile",
                        "EPSG:900913",
                        mbTilesLayer);
        TileJSON actualJson = tileJSONBuilder.build();

        assertEquals("countries", actualJson.getName());
        assertEquals(
                "http://localhost:8081/geoserver/ogc/tiles/collections/countries/tiles/EPSG:900913/{z}/{y}/{x}?f=application%2Fvnd.mapbox-vector-tile",
                actualJson.getTiles()[0]);
        assertEquals(
                "Natural Earth Data with .shp data from  TileMill", actualJson.getDescription());
        assertEquals("Natural Earth Data", tileJSON.getAttribution());
        assertEquals(0, tileJSON.getMinZoom().longValue());
        assertEquals(6, tileJSON.getMaxZoom().longValue());
        assertEquals("xyz", tileJSON.getScheme());
    }
}
