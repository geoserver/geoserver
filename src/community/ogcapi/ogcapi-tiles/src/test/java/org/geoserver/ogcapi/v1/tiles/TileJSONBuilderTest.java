package org.geoserver.ogcapi.v1.tiles;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.geoserver.ogcapi.APIRequestInfo;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.meta.LayerMetaInformation;
import org.geowebcache.layer.meta.TileJSON;
import org.geowebcache.mbtiles.layer.MBTilesLayer;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class TileJSONBuilderTest {

    @Test
    public void testMBTiles() throws Exception {
        try (MockedStatic<APIRequestInfo> staticRequestInfos =
                Mockito.mockStatic(APIRequestInfo.class)) {
            APIRequestInfo requestInfo = mock(APIRequestInfo.class);
            when(APIRequestInfo.get()).thenReturn(requestInfo);
            staticRequestInfos.when(APIRequestInfo::get).thenReturn(requestInfo);

            when(requestInfo.getBaseURL()).thenReturn("http://localhost:8081/geoserver");

            MBTilesLayer mbTilesLayer = mock(MBTilesLayer.class);
            LayerMetaInformation metaInformation = mock(LayerMetaInformation.class);
            GridSubset subset = mock(GridSubset.class);

            when(mbTilesLayer.getName()).thenReturn("countries");
            when(mbTilesLayer.getGridSubset(anyString())).thenReturn(subset);
            when(mbTilesLayer.getMetaInformation()).thenReturn(metaInformation);
            when(mbTilesLayer.supportsTileJSON()).thenReturn(true);
            TileJSON tileJSON = new TileJSON();
            tileJSON.setName("countries");
            tileJSON.setDescription("Natural Earth Data with .shp data from  TileMill");
            tileJSON.setAttribution("Natural Earth Data");
            tileJSON.setMinZoom(0);
            tileJSON.setMaxZoom(6);
            tileJSON.setScheme("xyz");
            when(mbTilesLayer.getTileJSON()).thenReturn(tileJSON);

            TileJSONBuilder tileJSONBuilder =
                    new TileJSONBuilder(
                            "countries",
                            "application/vnd.mapbox-vector-tile",
                            "EPSG:900913",
                            mbTilesLayer);
            TileJSON actualJson = tileJSONBuilder.build();

            assertEquals("countries", actualJson.getName());
            assertEquals(
                    "http://localhost:8081/geoserver/ogc/tiles/v1/collections/countries/tiles/EPSG:900913/{z}/{y}/{x}?f=application%2Fvnd.mapbox-vector-tile",
                    actualJson.getTiles()[0]);
            assertEquals(
                    "Natural Earth Data with .shp data from  TileMill",
                    actualJson.getDescription());
            assertEquals("Natural Earth Data", tileJSON.getAttribution());
            assertEquals(0, tileJSON.getMinZoom().longValue());
            assertEquals(6, tileJSON.getMaxZoom().longValue());
            assertEquals("xyz", tileJSON.getScheme());
        }
    }
}
