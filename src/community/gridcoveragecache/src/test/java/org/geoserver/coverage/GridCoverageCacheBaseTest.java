/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.coverage.layer.CoverageTileLayer;
import org.geoserver.coverage.layer.CoverageTileLayerInfo;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.InterpolationType;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.SeedingPolicy;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.TiffCompression;
import org.geoserver.coverage.layer.CoverageTileLayerInfoImpl;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSetFactory;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.StorageBroker;

public class GridCoverageCacheBaseTest extends GeoServerSystemTestSupport {

    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);

    protected static final int META_TILING_X = 2;

    protected static final int META_TILING_Y = 2;

    protected static final int GUTTER = 2;

    protected static final SeedingPolicy SEEDING_POLICY = SeedingPolicy.DIRECT;

    protected static final OverviewPolicy OVERVIEW_POLICY = OverviewPolicy.SPEED;

    protected static final InterpolationType INTERPOLATION_TYPE = InterpolationType.NEAREST;

    protected static final TiffCompression TIFF_COMPRESSION = TiffCompression.NONE;

    protected CoverageInfo coverageInfo;

    protected LayerInfoImpl layerInfo;

    protected CoverageTileLayer coverageTileLayer;

    protected Catalog catalog;

    protected GridSetBroker gridSetBroker;

    protected StorageBroker storageBroker;

    protected GWCConfig defaults;

    protected GeoServerTileLayerInfoImpl geoserverTileLayerInfo;

    protected CoverageTileLayerInfo coverageTileLayerInfo;

    protected ArrayList<GridSubset> subsets = new ArrayList<GridSubset>();

    protected GridCoveragesCache gridCoveragesCache;

    protected BlobStore blobStore;

    /**
     * Only setup coverages
     */
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpRasterLayer(CiteTestData.WORLD, "world.tiff", null);
        testData.setUpRasterLayer(WATTEMP, "watertemp.zip", null);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        gridCoveragesCache = GeoServerExtensions.bean(GridCoveragesCache.class);
        storageBroker = gridCoveragesCache.getStorageBroker();
        gridSetBroker = gridCoveragesCache.getGridSetBroker();

        blobStore = GeoServerExtensions.bean(BlobStore.class);

        defaults = GWCConfig.getOldDefaults();
        defaults.setMetaTilingX(2);
        defaults.setMetaTilingY(2);
        GridSet gridSet = gridSetBroker.get("EPSG:4326");
        GridSet reducedGridSet = GridSetFactory.createGridSet("myEPSG:4326", gridSet.getSrs(),
                gridSet.getBounds(), false, 6, 111319.49079327358, 2.8E-4, 256, 256, false);
        gridSetBroker.put(reducedGridSet);
        GridSubset subset = GridSubsetFactory.createGridSubSet(reducedGridSet);
        subsets.add(subset);

        catalog = getCatalog();
        addWorlCoverage(subset);
        addWaterTemp(subset);

    }

    private void addWorlCoverage(GridSubset subset) throws Exception {
        coverageInfo = catalog.getCoverageByName(SystemTestData.WORLD.getLocalPart());
        CoverageStoreInfo coverageStoreInfo = coverageInfo.getStore();

        WorkspaceInfo workspace = coverageStoreInfo.getWorkspace();

        geoserverTileLayerInfo = new GeoServerTileLayerInfoImpl();
        geoserverTileLayerInfo.setEnabled(true);
        geoserverTileLayerInfo.setMetaTilingX(META_TILING_X);
        geoserverTileLayerInfo.setMetaTilingY(META_TILING_Y);
        geoserverTileLayerInfo.setGutter(GUTTER);
        geoserverTileLayerInfo.setName(workspace.getName() + coverageInfo.getName());

        coverageTileLayerInfo = new CoverageTileLayerInfoImpl(geoserverTileLayerInfo);
        coverageTileLayerInfo.setOverviewPolicy(OVERVIEW_POLICY);
        coverageTileLayerInfo.setInterpolationType(INTERPOLATION_TYPE);
        coverageTileLayerInfo.setTiffCompression(TIFF_COMPRESSION);
        coverageTileLayerInfo.setSeedingPolicy(SEEDING_POLICY);

        XMLGridSubset xmlGridSubset = new XMLGridSubset(subset);
        Set<XMLGridSubset> xmlSubsets = new HashSet<XMLGridSubset>();
        xmlSubsets.add(xmlGridSubset);
        coverageTileLayerInfo.setGridSubsets(xmlSubsets);

        coverageTileLayer = new CoverageTileLayer(coverageInfo, gridSetBroker, subsets,
                coverageTileLayerInfo, true);
        coverageInfo.getMetadata().put(CachingGridCoverageReaderCallback.COVERAGETILELAYERINFO_KEY,
                coverageTileLayerInfo);
        catalog.save(coverageInfo);
        GWC.get().add(coverageTileLayer);

    }

    private void addWaterTemp(GridSubset subset) throws Exception {
        CoverageInfo coverageInfo = catalog.getCoverageByName(WATTEMP.getLocalPart());
        CoverageStoreInfo coverageStoreInfo = coverageInfo.getStore();

        WorkspaceInfo workspace = coverageStoreInfo.getWorkspace();

        GeoServerTileLayerInfoImpl geoserverTileLayerInfo = new GeoServerTileLayerInfoImpl();
        geoserverTileLayerInfo.setEnabled(true);
        geoserverTileLayerInfo.setMetaTilingX(META_TILING_X);
        geoserverTileLayerInfo.setMetaTilingY(META_TILING_Y);
        geoserverTileLayerInfo.setGutter(GUTTER);
        geoserverTileLayerInfo.setName(workspace.getName() + coverageInfo.getName());

        CoverageTileLayerInfoImpl coverageTileLayerInfo = new CoverageTileLayerInfoImpl(
                geoserverTileLayerInfo);
        coverageTileLayerInfo.setOverviewPolicy(OVERVIEW_POLICY);
        coverageTileLayerInfo.setInterpolationType(INTERPOLATION_TYPE);
        coverageTileLayerInfo.setTiffCompression(TIFF_COMPRESSION);
        coverageTileLayerInfo.setSeedingPolicy(SEEDING_POLICY);

        XMLGridSubset xmlGridSubset = new XMLGridSubset(subset);
        Set<XMLGridSubset> xmlSubsets = new HashSet<XMLGridSubset>();
        xmlSubsets.add(xmlGridSubset);
        coverageTileLayerInfo.setGridSubsets(xmlSubsets);

        CoverageTileLayer coverageTileLayer = new CoverageTileLayer(coverageInfo, gridSetBroker,
                subsets, coverageTileLayerInfo, true);
        coverageInfo.getMetadata().put(CachingGridCoverageReaderCallback.COVERAGETILELAYERINFO_KEY,
                coverageTileLayerInfo);
        catalog.save(coverageInfo);
        GWC.get().add(coverageTileLayer);
    }

}
