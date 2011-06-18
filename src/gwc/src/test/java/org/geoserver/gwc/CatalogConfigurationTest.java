/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.util.Iterator;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;

/**
 * Test class for the GWCCatalogListener
 * 
 * @author Arne Kepp / OpenGeo 2009
 */
public class CatalogConfigurationTest extends GeoServerTestSupport {

    private Catalog cat;

    private TileLayerDispatcher tld;

    private GridSetBroker gridSetBroker;

    /**
     * Runs through the Spring based initialization sequence against the mock catalog
     * 
     * Then 1) Check that cite:Lakes is present, from GWCCatalogListener 2) Check sf:GenerictEntity
     * is present and initialized, from GWCCatalogListener 3) Basic get from TileLayerDispatcher 4)
     * Removal of LayerInfo from catalog, test TileLayerDispatcher 5) Introducing new LayerInfo,
     * test TileLayerDispatcher
     * 
     * TODO: this test case really needs to be splitted into more
     * 
     * @throws Exception
     */
    public void _testInit() throws Exception {
        GWC gwc = GWC.get();

        cat = (Catalog) applicationContext.getBean("rawCatalog");

        tld = (TileLayerDispatcher) applicationContext.getBean("gwcTLDispatcher");

        gridSetBroker = (GridSetBroker) applicationContext.getBean("gwcGridSetBroker");

        try {
            tld.getTileLayer("");
        } catch (GeoWebCacheException gwce) {

        }

        Iterable<TileLayer> layerList;
        Iterator<TileLayer> tlIter;

        layerList = gwc.getTileLayers();
        tlIter = layerList.iterator();

        assertTrue(tlIter.hasNext());

        // Disabling tests until I have working build
        // if (tlIter.hasNext()) {
        // return;
        // }

        // 1) Check that cite:Lakes
        boolean foundLakes = false;
        while (tlIter.hasNext()) {
            TileLayer tl = tlIter.next();
            if (tl.getName().equals("cite:Lakes")) {
                // tl.isInitialized();
                foundLakes = true;
                break;
            }
        }
        assertTrue(foundLakes);

        // 2) Check sf:GenerictEntity is present and initialized
        layerList = gwc.getTileLayers();
        tlIter = layerList.iterator();
        boolean foudAGF = false;
        while (tlIter.hasNext()) {
            TileLayer tl = tlIter.next();
            // System.out.println(tl.getName());
            if (tl.getName().equals("sf:AggregateGeoFeature")) {
                // tl.isInitialized();
                foudAGF = true;
                GridSubset epsg4326 = tl.getGridSubset(gridSetBroker.WORLD_EPSG4326.getName());
                assertTrue(epsg4326.getGridSetBounds().equals(
                        new BoundingBox(-180.0, -90.0, 180.0, 90.0)));
                String mime = tl.getMimeTypes().get(1).getMimeType();
                assertTrue(mime.startsWith("image/")
                        || mime.startsWith("application/vnd.google-earth.kml+xml"));
            }
        }

        assertTrue(foudAGF);

        // 3) Basic get
        LayerInfo li = cat.getLayers().get(1);
        String layerName = li.getResource().getPrefixedName();

        TileLayer tl = tld.getTileLayer(layerName);

        assertEquals(layerName, tl.getName());

        // 4) Removal of LayerInfo from catalog
        cat.remove(li);

        assertTrue(cat.getLayerByName(tl.getName()) == null);

        boolean caughtException = false;
        try {
            TileLayer tl2 = tld.getTileLayer(layerName);
        } catch (GeoWebCacheException gwce) {
            caughtException = true;
        }
        assertTrue(caughtException);

        // 5) Introducing new LayerInfo
        ResourceInfo resInfo = li.getResource();

        resInfo.setName("hithere");
        cat.save(resInfo);

        LayerInfo layerInfo = cat.getFactory().createLayer();
        layerInfo.setResource(resInfo);
        layerInfo.setName(resInfo.getPrefixedName());

        cat.add(layerInfo);
        String newLayerName = layerInfo.getResource().getPrefixedName();
        TileLayer tl3 = tld.getTileLayer(newLayerName);
        assertEquals(newLayerName, tl3.getName());

        // 6) Add new LayerGroupInfo
        LayerGroupInfo lgi = cat.getFactory().createLayerGroup();
        lgi.setName("sf:aLayerGroup");
        lgi.setBounds(new ReferencedEnvelope(-180, 180, -90, 90, CRS.decode("EPSG:4326")));
        lgi.getLayers().add(cat.getLayerByName("hithere"));

        cat.add(lgi);
        TileLayer tl4 = tld.getTileLayer("sf:aLayerGroup");
        assertNotNull(tl4);
        assertEquals(lgi.getName(), tl4.getName());

    }
}
