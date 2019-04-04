/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wmts;

import org.geoserver.gwc.GWC;
import org.geoserver.platform.ContextLoadedEvent;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetFactory;
import org.geowebcache.grid.SRS;
import org.springframework.context.ApplicationListener;

/** Loads the inspire grid set and mark it as non editable by the user. */
public class InspireGridSetLoader implements ApplicationListener<ContextLoadedEvent> {

    public static final String INSPIRE_GRID_SET_NAME = "InspireCRS84Quad";

    @Override
    public synchronized void onApplicationEvent(ContextLoadedEvent event) {
        GWC gwc = GWC.get();
        // this grid set should not be editable by the user
        gwc.addEmbeddedGridSet(INSPIRE_GRID_SET_NAME);
        GridSet gridSet = gwc.getGridSetBroker().get(INSPIRE_GRID_SET_NAME);
        if (gridSet != null) {
            // this grid set already exists
            return;
        }
        // the grid set resolutions
        double[] resolutions =
                new double[] {
                    0.703125,
                    0.3515625,
                    0.17578125,
                    0.087890625,
                    0.0439453125,
                    0.02197265625,
                    0.010986328125,
                    0.0054931640625,
                    0.00274658203125,
                    0.001373291015625,
                    6.866455078125E-4,
                    3.433227539062E-4,
                    1.716613769531E-4,
                    8.58306884766E-5,
                    4.29153442383E-5,
                    2.14576721191E-5,
                    1.07288360596E-5,
                    5.3644180298E-6
                };
        // the grid sets scale names
        String[] scaleNames =
                new String[] {
                    "InspireCRS84Quad:0", "InspireCRS84Quad:1", "InspireCRS84Quad:2",
                            "InspireCRS84Quad:3", "InspireCRS84Quad:4",
                    "InspireCRS84Quad:5", "InspireCRS84Quad:6", "InspireCRS84Quad:7",
                            "InspireCRS84Quad:8", "InspireCRS84Quad:9",
                    "InspireCRS84Quad:10", "InspireCRS84Quad:11", "InspireCRS84Quad:12",
                            "InspireCRS84Quad:13", "InspireCRS84Quad:14",
                    "InspireCRS84Quad:15", "InspireCRS84Quad:16", "InspireCRS84Quad:17"
                };
        // creating thee grid set
        gridSet =
                GridSetFactory.createGridSet(
                        INSPIRE_GRID_SET_NAME,
                        SRS.getEPSG4326(),
                        BoundingBox.WORLD4326,
                        false,
                        resolutions,
                        null,
                        111319.49079327358,
                        GridSetFactory.DEFAULT_PIXEL_SIZE_METER,
                        scaleNames,
                        256,
                        256,
                        false);
        // set a proper description
        gridSet.setDescription(
                "Every layer offered by a INSPIRE WMTS should use the InspireCRS84Quad Matrix Set");
        try {
            // add the grid set
            gwc.addGridSet(gridSet);
        } catch (Exception exception) {
            throw new RuntimeException("Error adding grid set InspireCRS84Quad.", exception);
        }
    }
}
