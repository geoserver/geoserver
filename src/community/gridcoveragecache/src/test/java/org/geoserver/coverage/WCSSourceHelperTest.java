/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage;

import static org.junit.Assert.assertEquals;

import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.media.jai.Interpolation;

import org.geoserver.coverage.layer.CoverageTileLayer;
import org.geoserver.data.test.SystemTestData;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.mime.MimeType;
import org.junit.Test;

public class WCSSourceHelperTest extends GridCoverageCacheBaseTest {

    private WCSSourceHelper sourceHelper;

    private OverviewPolicy overviewPolicy = OverviewPolicy.NEAREST;

    private Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        sourceHelper = coverageTileLayer.getSourceHelper();
    }

    @Test
    public void testValidRequest() throws GeoWebCacheException, IOException {
        GridSubset gridSubset = subsets.get(0);
        String gridSetName = gridSubset.getGridSet().getName();

        // Creating a conveyorTile
        ConveyorTile tile = new ConveyorTile(storageBroker, coverageTileLayer.getName(),
                gridSetName, new long[] { 2, 1, 2 }, CoverageTileLayer.TIFF_MIME_TYPE, null, null,
                null);
        long[] tileGridPosition = tile.getTileIndex();
        final MimeType responseFormat = tile.getMimeType();
        final int gutter = 5;

        // Setting up metaTile
        CoverageMetaTileTest.TestingCoverageMetaTile metaTile = new CoverageMetaTileTest.TestingCoverageMetaTile(
                coverageTileLayer, gridSubset, responseFormat, tileGridPosition, 2, 2,
                tile.getFullParameters(), gutter);

        // Using WCS request to populate metaTile data
        sourceHelper.makeRequest(metaTile, tile, interpolation, overviewPolicy, null);

        final int referenceX = 88;
        RenderedImage ri = metaTile.getMetaTileImage();
        assertEquals(522, ri.getWidth());
        assertEquals(517, ri.getHeight());
        assertEquals(170, ri.getData().getSample(referenceX + gutter, 0, 0));

        RenderedImage subTile = metaTile.getSubTile(2);
        assertEquals(256, subTile.getWidth());
        assertEquals(256, subTile.getHeight());
        assertEquals(170, subTile.getData().getSample(referenceX, 0, 0));

        metaTile.dispose();

    }
}
