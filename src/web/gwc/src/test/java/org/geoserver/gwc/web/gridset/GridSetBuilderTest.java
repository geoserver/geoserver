/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetFactory;
import org.geowebcache.grid.SRS;
import org.junit.Test;

public class GridSetBuilderTest {

    /** Checks yCoordinateFirst value detection based on EPSG:4326 GridSet */
    @Test
    public void testYCoordinateFirstEPSG4326() {
        GridSet epsg4326 =
                GridSetFactory.createGridSet(
                        "GlobalCRS84Geometric",
                        SRS.getEPSG4326(),
                        BoundingBox.WORLD4326,
                        false,
                        GridSetFactory.DEFAULT_LEVELS,
                        null,
                        GridSetFactory.DEFAULT_PIXEL_SIZE_METER,
                        256,
                        256,
                        false);
        epsg4326.setDescription(
                "A default WGS84 tile matrix set where the first zoom level "
                        + "covers the world with two tiles on the horizonal axis and one tile "
                        + "over the vertical axis and each subsequent zoom level is calculated by half "
                        + "the resolution of its previous one.");
        GridSetInfo info = new GridSetInfo(epsg4326, false);
        GridSet finalGridSet = GridSetBuilder.build(info);
        assertTrue(finalGridSet.isyCoordinateFirst());
    }

    /** Checks yCoordinateFirst value detection based on EPSG:3857 GridSet */
    @Test
    public void testYCoordinateFirstEPSG3857() {
        GridSet epsg3857 =
                GridSetFactory.createGridSet(
                        "GoogleMapsCompatible",
                        SRS.getEPSG3857(),
                        BoundingBox.WORLD3857,
                        false,
                        commonPractice900913Resolutions(),
                        null,
                        1.0D,
                        GridSetFactory.DEFAULT_PIXEL_SIZE_METER,
                        null,
                        256,
                        256,
                        false);
        epsg3857.setDescription(
                "This well-known scale set has been defined to be compatible with Google Maps and"
                        + " Microsoft Live Map projections and zoom levels. Level 0 allows representing the whole "
                        + "world in a single 256x256 pixels. The next level represents the whole world in 2x2 tiles "
                        + "of 256x256 pixels and so on in powers of 2. Scale denominator is only accurate near the equator.");
        GridSetInfo info = new GridSetInfo(epsg3857, false);
        GridSet finalGridSet = GridSetBuilder.build(info);
        assertFalse(finalGridSet.isyCoordinateFirst());
    }

    private double[] commonPractice900913Resolutions() {
        return new double[] { //
            156543.03390625,
            78271.516953125,
            39135.7584765625,
            19567.87923828125,
            9783.939619140625,
            4891.9698095703125,
            2445.9849047851562,
            1222.9924523925781,
            611.4962261962891,
            305.74811309814453,
            152.87405654907226,
            76.43702827453613,
            38.218514137268066,
            19.109257068634033,
            9.554628534317017,
            4.777314267158508,
            2.388657133579254,
            1.194328566789627,
            0.5971642833948135,
            0.29858214169740677,
            0.14929107084870338,
            0.07464553542435169,
            0.037322767712175846,
            0.018661383856087923,
            0.009330691928043961,
            0.004665345964021981,
            0.0023326729820109904,
            0.0011663364910054952,
            5.831682455027476E-4,
            2.915841227513738E-4,
            1.457920613756869E-4
        };
    }
}
