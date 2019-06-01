/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.MockWepAppContextRule;
import org.geowebcache.config.ConfigurationResourceProvider;
import org.geowebcache.config.LayerConfigurationTest;
import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.OutsideCoverageException;
import org.geowebcache.layer.AbstractTileLayer;
import org.geowebcache.layer.TileLayer;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** GeoServer integration test for {@link TileLayerConfiguration} */
public class CatalogConfigurationLayerConformanceTest extends LayerConfigurationTest {

    @Rule public MockWepAppContextRule context = new MockWepAppContextRule();

    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private GWCConfig gwcConfig;

    private GridSetBroker gsBroker;

    @Override
    protected void doModifyInfo(TileLayer info, int rand) throws Exception {
        info.setBlobStoreId(Integer.toString(rand));
    }

    @Override
    protected TileLayer getGoodInfo(String id, int rand) throws Exception {
        PublishedInfo pinfo =
                layerCatalog.computeIfAbsent(
                        id,
                        (name) -> {
                            LayerGroupInfo info =
                                    EasyMock.createMock(
                                            "TestPublished_" + id, LayerGroupInfo.class);
                            MetadataMap mMap = new MetadataMap();

                            EasyMock.expect(info.getMetadata()).andStubReturn(mMap);
                            EasyMock.expect(info.prefixedName()).andStubReturn(id);
                            EasyMock.expect(info.getId()).andStubReturn(id);
                            EasyMock.replay(info);
                            return info;
                        });

        GeoServerTileLayer layer = new GeoServerTileLayer(pinfo, gwcConfig, gsBroker);
        doModifyInfo(layer, rand);
        return layer;
    }

    @Override
    protected TileLayer getBadInfo(String id, int rand) throws Exception {
        return new AbstractTileLayer() {

            @Override
            public String getName() {
                return id;
            }

            @Override
            protected boolean initializeInternal(GridSetBroker gridSetBroker) {
                return false;
            }

            @Override
            public String getStyles() {
                return null;
            }

            @Override
            public ConveyorTile getTile(ConveyorTile tile)
                    throws GeoWebCacheException, IOException, OutsideCoverageException {
                return null;
            }

            @Override
            public ConveyorTile getNoncachedTile(ConveyorTile tile) throws GeoWebCacheException {
                return null;
            }

            @Override
            public void seedTile(ConveyorTile tile, boolean tryCache)
                    throws GeoWebCacheException, IOException {
                return;
            }

            @Override
            public ConveyorTile doNonMetatilingRequest(ConveyorTile tile)
                    throws GeoWebCacheException {
                return null;
            }
        };
    }

    @Override
    protected String getExistingInfo() {
        // The tests that add their own layers provide enough coverage.
        Assume.assumeTrue(false);
        return null;
    }

    Map<String, PublishedInfo> layerCatalog = new HashMap<>();

    private GWC mediator;

    private Catalog catalog;

    private File dataDir;

    @Override
    protected TileLayerConfiguration getConfig() throws Exception {
        catalog = EasyMock.createMock("catalog", Catalog.class);
        gsBroker = EasyMock.createMock("gsBroker", GridSetBroker.class);
        mediator = EasyMock.createMock("mediator", GWC.class);

        mediator.syncEnv();
        EasyMock.expectLastCall().anyTimes();
        mediator.layerAdded(EasyMock.anyObject(String.class));
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(mediator.layerRemoved(EasyMock.anyObject(String.class)))
                .andStubReturn(true);
        mediator.layerRenamed(EasyMock.anyObject(String.class), EasyMock.anyObject(String.class));
        EasyMock.expectLastCall().anyTimes();

        EasyMock.replay(catalog, gsBroker, mediator);
        context.addBean("mediator", mediator, GWC.class);
        GWC.set(mediator);

        gwcConfig = new GWCConfig();
        dataDir = temp.newFolder();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(dataDir);
        XMLConfiguration xmlConfig =
                new XMLConfiguration(
                        context.getContextProvider(), (ConfigurationResourceProvider) null);
        TileLayerCatalog tlCatalog = new DefaultTileLayerCatalog(resourceLoader, xmlConfig);

        return new CatalogConfiguration(catalog, tlCatalog, gsBroker);
    }

    @After
    public void removeMediator() throws Exception {
        GWC.set(null);
    }

    @Override
    protected TileLayerConfiguration getSecondConfig() throws Exception {
        gwcConfig = new GWCConfig();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(dataDir);
        XMLConfiguration xmlConfig =
                new XMLConfiguration(
                        context.getContextProvider(), (ConfigurationResourceProvider) null);
        TileLayerCatalog tlCatalog = new DefaultTileLayerCatalog(resourceLoader, xmlConfig);

        return new CatalogConfiguration(catalog, tlCatalog, gsBroker);
    }

    @Override
    protected Matcher<TileLayer> infoEquals(TileLayer expected) {
        return hasProperty("blobStoreId", equalTo(expected.getBlobStoreId()));
    }

    @Override
    protected Matcher<TileLayer> infoEquals(int rand) {
        return hasProperty("blobStoreId", equalTo(rand));
    }

    @Override
    public void failNextRead() {
        // TODO come up with a good way of testing IO failures for this
        Assume.assumeTrue(false);
    }

    @Override
    public void failNextWrite() {
        // TODO come up with a good way of testing IO failures for this
        Assume.assumeTrue(false);
    }

    @Override
    @Ignore // TODO Need to implement a clone/deep copy/modification proxy to make this safe.
    @Test
    public void testModifyCallRequiredToChangeInfoFromGetInfo() throws Exception {
        super.testModifyCallRequiredToChangeInfoFromGetInfo();
    }

    @Override
    @Ignore // TODO Need to implement a clone/deep copy/modification proxy to make this safe.
    @Test
    public void testModifyCallRequiredToChangeInfoFromGetInfos() throws Exception {
        super.testModifyCallRequiredToChangeInfoFromGetInfos();
    }

    @Override
    @Ignore // TODO Need to implement a clone/deep copy/modification proxy to make this safe.
    @Test
    public void testModifyCallRequiredToChangeExistingInfoFromGetInfo() throws Exception {
        super.testModifyCallRequiredToChangeExistingInfoFromGetInfo();
    }

    @Override
    @Ignore // TODO Need to implement a clone/deep copy/modification proxy to make this safe.
    @Test
    public void testModifyCallRequiredToChangeExistingInfoFromGetInfos() throws Exception {
        super.testModifyCallRequiredToChangeExistingInfoFromGetInfos();
    }
}
