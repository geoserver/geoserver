/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml.gwc.gridset;

import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geoserver.gwc.GWC;
import org.geoserver.mapml.tcrs.Bounds;
import org.geoserver.mapml.tcrs.TiledCRSConstants;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.config.SimpleGridSetConfiguration;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetFactory;
import org.geowebcache.grid.SRS;
import org.springframework.beans.factory.annotation.Autowired;

public class MapMLGridsets extends SimpleGridSetConfiguration {
    private static final Log log = LogFactory.getLog(MapMLGridsets.class);

    private final GridSet WGS84;
    private final GridSet OSMTILE;
    private final GridSet CBMTILE;
    private final GridSet APSTILE;
    @Autowired private GWC gwc = GWC.get();

    public MapMLGridsets() {
        log.debug("Adding MapML WGS84 gridset");
        WGS84 =
                GridSetFactory.createGridSet(
                        "WGS84",
                        SRS.getEPSG4326(),
                        BoundingBox.WORLD4326,
                        true,
                        GridSetFactory.DEFAULT_LEVELS,
                        null,
                        GridSetFactory.DEFAULT_PIXEL_SIZE_METER,
                        256,
                        256,
                        false);
        WGS84.setDescription("World Geodetic System 1984");
        for (int i = 0; i < GridSetFactory.DEFAULT_LEVELS; i++) {
            WGS84.getGrid(i).setName(Integer.toString(i));
        }
        addInternal(WGS84);

        log.debug("Adding MapML OSMTILE gridset");
        OSMTILE =
                GridSetFactory.createGridSet(
                        "OSMTILE",
                        SRS.getEPSG3857(),
                        BoundingBox.WORLD3857,
                        true,
                        OSMTILEResolutions(),
                        null,
                        1.0D,
                        GridSetFactory.DEFAULT_PIXEL_SIZE_METER,
                        integerLevelNames(OSMTILEResolutions().length),
                        256,
                        256,
                        false);
        OSMTILE.setDescription(
                "Web Mercator-based tiled coordinate reference system. "
                        + "Applied by many global map applications, "
                        + "for areas excluding polar latitudes.");
        addInternal(OSMTILE);

        log.debug("Adding MapML CBMTILE gridset");
        Bounds cb = TiledCRSConstants.tiledCRSDefinitions.get("CBMTILE").getBounds();
        BoundingBox cb_bbox =
                new BoundingBox(
                        cb.getMin().getX(),
                        cb.getMin().getY(),
                        cb.getMax().getX(),
                        cb.getMax().getY());

        CBMTILE =
                GridSetFactory.createGridSet(
                        "CBMTILE",
                        SRS.getSRS(3978),
                        cb_bbox,
                        true,
                        CBMTILEResolutions(),
                        null,
                        1.0D,
                        GridSetFactory.DEFAULT_PIXEL_SIZE_METER,
                        integerLevelNames(CBMTILEResolutions().length),
                        256,
                        256,
                        false);
        CBMTILE.setDescription(
                "Lambert Conformal Conic-based tiled " + "coordinate reference system for Canada.");
        addInternal(CBMTILE);

        log.debug("Adding MapML APSTILE gridset");
        Bounds at_b = TiledCRSConstants.tiledCRSDefinitions.get("APSTILE").getBounds();
        BoundingBox at_bbox =
                new BoundingBox(
                        at_b.getMin().getX(),
                        at_b.getMin().getY(),
                        at_b.getMax().getX(),
                        at_b.getMax().getY());

        APSTILE =
                GridSetFactory.createGridSet(
                        "APSTILE",
                        SRS.getSRS(5936),
                        at_bbox,
                        true,
                        APSTILEResolutions(),
                        null,
                        1.0D,
                        GridSetFactory.DEFAULT_PIXEL_SIZE_METER,
                        integerLevelNames(APSTILEResolutions().length),
                        256,
                        256,
                        false);

        APSTILE.setDescription(
                "Alaska Polar Stereographic-based tiled "
                        + "coordinate reference system for the Arctic region.");
        addInternal(APSTILE);
        getGridSets()
                .stream()
                .forEach(
                        g -> {
                            if (!gwc.getGridSetBroker().getGridSetNames().contains(g.getName())) {
                                try {
                                    gwc.getGridSetBroker().addGridSet(g);
                                } catch (UnsupportedOperationException ioe) {
                                    log.info("Error occurred adding gridset: " + g.getName(), ioe);
                                }
                            }
                            // embedded gridsets aren't editable by the user,
                            // which is what we want, so push this onto that list
                            // needs to be added to list first time and every time
                            // we start up, because it's not a "default" gridset.
                            gwc.addEmbeddedGridSet(g.getName());
                        });
        gwc.getConfig()
                .setDefaultCachingGridSetIds(
                        getGridSets().stream().map(g -> g.getName()).collect(toSet()));
        try {
            gwc.saveConfig(gwc.getConfig());
        } catch (IOException ioe) {
            log.info("Error occured saving MapMLGridsets config.", ioe);
        }
    }

    private double[] CBMTILEResolutions() {
        double[] CBMTILEResolutions = {
            38364.660062653464D,
            22489.62831258996D,
            13229.193125052918D,
            7937.5158750317505D,
            4630.2175937685215D,
            2645.8386250105837D,
            1587.5031750063501D,
            926.0435187537042D,
            529.1677250021168D,
            317.50063500127004D,
            185.20870375074085D,
            111.12522225044451D,
            66.1459656252646D,
            38.36466006265346D,
            22.48962831258996D,
            13.229193125052918D,
            7.9375158750317505D,
            4.6302175937685215D,
            2.6458386250105836D,
            1.5875031750063502D,
            0.92604351875370428D,
            0.52916772500211673D,
            0.31750063500127002D,
            0.18520870375074083D,
            0.11112522225044451D,
            0.066145965625264591D
        };
        return CBMTILEResolutions;
    }

    private String[] integerLevelNames(int resolutions) {
        String[] names = new String[resolutions];
        for (int i = 0; i < resolutions; i++) {
            names[i] = Integer.toString(i);
        }
        return names;
    }

    private double[] APSTILEResolutions() {
        double[] APSTILEResolutions = {
            238810.813354D,
            119405.406677D,
            59702.7033384999D,
            29851.3516692501D,
            14925.675834625D,
            7462.83791731252D,
            3731.41895865639D,
            1865.70947932806D,
            932.854739664032D,
            466.427369832148D,
            233.213684916074D,
            116.606842458037D,
            58.3034212288862D,
            29.1517106145754D,
            14.5758553072877D,
            7.28792765351156D,
            3.64396382688807D,
            1.82198191331174D,
            0.910990956788164D,
            0.45549547826179D
        };
        return APSTILEResolutions;
    }

    private double[] OSMTILEResolutions() {
        double[] OSMTILEResolutions = {
            156543.03390625D,
            78271.516953125D,
            39135.7584765625D,
            19567.87923828125D,
            9783.939619140625D,
            4891.9698095703125D,
            2445.9849047851562D,
            1222.9924523925781D,
            611.4962261962891D,
            305.74811309814453D,
            152.87405654907226D,
            76.43702827453613D,
            38.218514137268066D,
            19.109257068634033D,
            9.554628534317017D,
            4.777314267158508D,
            2.388657133579254D,
            1.194328566789627D,
            0.5971642833948135D
            //            0.2985821416974068D, these are not defined in the spec
            //            0.1492910708487034D,
            //            0.0746455354243517D,
            //            0.0373227677121758D,
            //            0.0186613838560879D,
            //            0.009330691928044D,
            //            0.004665345964022D,
            //            0.002332672982011D,
            //            0.0011663364910055D,
            //            0.0005831682455027D,
            //            0.0002915841227514D,
            //            0.0001457920613757D
        };
        return OSMTILEResolutions;
    }

    @Override
    public void afterPropertiesSet() throws GeoWebCacheException {}

    @Override
    public String getIdentifier() {
        return "DefaultGridsets";
    }

    @Override
    public String getLocation() {
        return "Default";
    }
}
