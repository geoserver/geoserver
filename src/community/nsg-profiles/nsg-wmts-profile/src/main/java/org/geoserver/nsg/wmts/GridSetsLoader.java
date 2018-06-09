/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.wmts;

import org.geoserver.gwc.GWC;
import org.geoserver.platform.ContextLoadedEvent;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetFactory;
import org.geowebcache.grid.SRS;
import org.springframework.context.ApplicationListener;

/** Loads NSG WMTS profile mandatory grid sets: EPSG:3395, EPSG:5041 and EPSG:5042. */
public class GridSetsLoader implements ApplicationListener<ContextLoadedEvent> {

    private static final String WORLD_MERCATOR_GRID_SET_NAME = "EPSG3395TiledMercator";
    private static final SRS EPSG_3395 = decodeSrs("EPSG:3395");

    private static final String EPSG_5041_GRID_SET_NAME = "EPSG:5041";
    private static final SRS EPSG_5041 = decodeSrs("EPSG:5041");

    private static final String EPSG_5042_GRID_SET_NAME = "EPSG:5042";
    private static final SRS EPSG_5042 = decodeSrs("EPSG:5042");

    @Override
    public synchronized void onApplicationEvent(ContextLoadedEvent event) {

        // world mercator 3395 grid set, resolutions
        double[] resolutions =
                new double[] {
                    156543.03392804097, 78271.51696402048, 39135.75848201024, 19567.87924100512,
                            9783.93962050256, 4891.96981025128,
                    2445.98490512564, 1222.99245256282, 611.49622628141, 305.748113140705,
                            152.8740565703525, 76.43702828517625,
                    38.21851414258813, 19.109257071294063, 9.554628535647032, 4.777314267823516,
                            2.388657133911758, 1.194328566955879,
                    0.5971642834779395, 0.2985821417389697, 0.1492910708694849, 0.0746455354347424,
                            0.0373227677173712, 0.0186613838586856,
                    0.0093306919293428
                };
        // world mercator 3395 grid set, scale names
        String[] scaleNames =
                new String[] {
                    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14",
                    "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"
                };
        // world mercator 3395 grid set, bounding box
        BoundingBox boundingBox =
                new BoundingBox(-2.0037508343E7, -2.0037508343E7, 2.0037508343E7, 2.0037508343E7);
        // world mercator 3395 grid set, description
        String description =
                "The World Mercator (EPSG::3395) well-known scale set is define in this "
                        + "NSG WMTS Implementation Interoperability Profile.";
        // world mercator 3395 grid set, add grid set
        addGridSet(
                WORLD_MERCATOR_GRID_SET_NAME,
                EPSG_3395,
                resolutions,
                scaleNames,
                boundingBox,
                description,
                1.0);

        // ups tiles EPSG::5041 grid set, resolutions
        resolutions =
                new double[] {
                    128443.43242384374, 64221.71621192187, 32110.858105960935, 16055.429052980468,
                            8027.714526490234, 4013.857263245117,
                    2006.9286316225584, 1003.4643158112792, 501.7321579056396, 250.8660789528198,
                            125.4330394764099, 62.71651973820495,
                    31.358259869102476, 15.679129934551238, 7.839564967275619, 3.9197824836378095,
                            1.9598912418189047, 0.9799456209094524,
                    0.4899728104547262, 0.2449864052273631, 0.1224932026136815, 0.0612466013068408,
                            0.0306233006534204, 0.0153116503267102,
                    0.0076558251633551
                };
        // ups tiles EPSG::5041 grid set, scale names
        scaleNames =
                new String[] {
                    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14",
                    "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"
                };
        // ups tiles EPSG::5041 grid set, bounding box
        boundingBox =
                new BoundingBox(
                        -14440759.350252, -14440759.350252, 18440759.350252, 18440759.350252);
        // ups tiles EPSG::5041 grid set, description
        description =
                "WGS 84 / UPS North (E,N) is a projected CRS last revised on March 14, 2010 and is suitable for use "
                        + "in Northern hemisphere - north of 60°N onshore and offshore, including Arctic. WGS 84 / UPS North (E,N) "
                        + "uses the WGS 84 geographic 2D CRS as its base CRS and the Universal Polar Stereographic North (Polar "
                        + "Stereographic (variant A)) as its projection. WGS 84 / UPS North (E,N) is a CRS for Military mapping by "
                        + "NATO. It was defined by information from US National Geospatial-Intelligence Agency (NGA).";
        // ups tiles EPSG::5041 grid set, add grid set
        addGridSet(
                EPSG_5041_GRID_SET_NAME,
                EPSG_5041,
                resolutions,
                scaleNames,
                boundingBox,
                description,
                1.0);

        // ups tiles EPSG::5042 grid set, resolutions
        resolutions =
                new double[] {
                    128443.43242384374, 64221.71621192187, 32110.858105960935, 16055.429052980468,
                            8027.714526490234, 4013.857263245117,
                    2006.9286316225584, 1003.4643158112792, 501.7321579056396, 250.8660789528198,
                            125.4330394764099, 62.71651973820495,
                    31.358259869102476, 15.679129934551238, 7.839564967275619, 3.9197824836378095,
                            1.9598912418189047, 0.9799456209094524,
                    0.4899728104547262, 0.2449864052273631, 0.1224932026136815, 0.0612466013068408,
                            0.0306233006534204, 0.0153116503267102,
                    0.0076558251633551
                };
        // ups tiles EPSG::5042 grid set, scale names
        scaleNames =
                new String[] {
                    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14",
                    "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"
                };
        // ups tiles EPSG::5042 grid set, bounding box
        boundingBox =
                new BoundingBox(
                        -14440759.350252, -14440759.350252, 18440759.350252, 18440759.350252);
        // ups tiles EPSG::5042 grid set, description
        description =
                "WGS 84 / UPS South (E,N) is a projected CRS last revised on 03/14/2010 and is suitable for use in "
                        + "Southern hemisphere - south of 60°S onshore and offshore - Antarctica. WGS 84 / UPS South (E,N) uses the "
                        + "WGS 84 geographic 2D CRS as its base CRS and the Universal Polar Stereographic South (Polar Stereographic "
                        + "(variant A)) as its projection. WGS 84 / UPS South (E,N) is a CRS for Military mapping by NATO. It was "
                        + "defined by information from US National Geospatial-Intelligence Agency (NGA).";
        // ups tiles EPSG::5041 grid set, add grid set
        addGridSet(
                EPSG_5042_GRID_SET_NAME,
                EPSG_5042,
                resolutions,
                scaleNames,
                boundingBox,
                description,
                1.0);
    }

    /**
     * Helper method that creates a grid set if he didn't exists and marks it a preconfigured one.
     */
    private void addGridSet(
            String gridSetName,
            SRS srs,
            double[] resolutions,
            String[] scaleNames,
            BoundingBox boundingBox,
            String description,
            double metersPerUnit) {
        GWC gwc = GWC.get();
        // this grid set should not be editable by the user
        GridSet gridSet = gwc.getGridSetBroker().get(gridSetName);
        if (gridSet != null) {
            // this grid set already exists
            return;
        }
        gwc.addEmbeddedGridSet(gridSetName);
        // creating thee grid set
        gridSet =
                GridSetFactory.createGridSet(
                        gridSetName,
                        srs,
                        boundingBox,
                        false,
                        resolutions,
                        null,
                        metersPerUnit,
                        GridSetFactory.DEFAULT_PIXEL_SIZE_METER,
                        scaleNames,
                        256,
                        256,
                        false);
        // set a proper description
        gridSet.setDescription(description);
        try {
            // add the grid set
            gwc.addGridSet(gridSet);
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Error adding grid set '%s'.", gridSetName), exception);
        }
    }

    /**
     * Helper method that just takes care of decoding a certain SRS code taking care of the checked
     * exceptions.
     */
    private static SRS decodeSrs(String srsCode) {
        try {
            return SRS.getSRS(srsCode);
        } catch (GeoWebCacheException exception) {
            throw new RuntimeException(
                    String.format("Error decoding SRS with code '%s'.", srsCode), exception);
        }
    }
}
