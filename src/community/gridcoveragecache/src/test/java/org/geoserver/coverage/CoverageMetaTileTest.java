/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage;

import java.awt.image.RenderedImage;
import java.util.Map;

import org.geoserver.coverage.layer.CoverageMetaTile;
import org.geoserver.coverage.layer.CoverageTileLayer;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.mime.MimeType;

/**
 * CoverageMetaTile testing class. It also extends CoverageMetaTile to expose constructor 
 * and some methods for internal testing purposes
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class CoverageMetaTileTest {

    public static class TestingCoverageMetaTile extends CoverageMetaTile {

        protected TestingCoverageMetaTile(CoverageTileLayer layer, GridSubset gridSubset,
                MimeType responseFormat, long[] tileGridPosition, int metaX, int metaY,
                Map<String, String> fullParameters, int gutter) {
            super(layer, gridSubset, responseFormat, tileGridPosition, metaX, metaY,
                    fullParameters, gutter);
        }

        public RenderedImage getMetaTileImage() {
            return metaTileImage;
        }

        public RenderedImage getSubTile(final int tileIdx){
            return super.getSubTile(tileIdx);
        }
    }
}
