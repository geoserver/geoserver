/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.util.Optional;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.feature.NameImpl;
import org.junit.Test;
import org.opengis.feature.type.Name;
import org.w3c.dom.Document;

/** Testing for Coordinates formatting configurations on WFS GML 3.1 & 3.2 on complex features */
public class CoordinatesFormattingTest extends StationsAppSchemaTestSupport {

    @Test
    public void testCoordinateFormatWfs11() throws Exception {
        enableCoordinatesFormattingGml31();
        Document doc =
                getAsDOM(
                        "st_gml31/wfs?request=GetFeature&version=1.1.0&typename=st_gml31:Station_gml31");
        checkCount(
                WFS11_XPATH_ENGINE,
                doc,
                1,
                "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id=\"st.1\"]/st_gml31:location/gml:Point/gml:pos"
                        + "[text()=\"1.00000000 -1.00000000\"]");
        // check force decimal notation
        checkCount(
                WFS11_XPATH_ENGINE,
                doc,
                1,
                "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id=\"st.2\"]/st_gml31:location/gml:Point/gml:pos"
                        + "[text()=\"0.00000010 -0.00000010\"]");
    }

    @Test
    public void testCoordinateFormatDisabledWfs11() throws Exception {
        disableCoordinatesFormattingGml31();
        Document doc =
                getAsDOM(
                        "st_gml31/wfs?request=GetFeature&version=1.1.0&typename=st_gml31:Station_gml31");
        checkCount(
                WFS11_XPATH_ENGINE,
                doc,
                1,
                "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id=\"st.1\"]/st_gml31:location/gml:Point/gml:pos"
                        + "[text()=\"1 -1\"]");
        // check force decimal notation
        checkCount(
                WFS11_XPATH_ENGINE,
                doc,
                1,
                "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id=\"st.2\"]/st_gml31:location/gml:Point/gml:pos"
                        + "[text()=\"1.0E-7 -1.0E-7\"]");
    }

    @Test
    public void testCoordinateFormatWfs20() throws Exception {
        enableCoordinatesFormattingGml32();
        Document document =
                getAsDOM("wfs?request=GetFeature&version=2.0&typename=st_gml32:Station_gml32");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32[@gml:id=\"st.1\"]/st_gml32:location/gml:Point/gml:pos"
                        + "[text()=\"1.00000000 -1.00000000\"]");
        // check force decimal notation
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32[@gml:id=\"st.2\"]/st_gml32:location/gml:Point/gml:pos"
                        + "[text()=\"0.00000010 -0.00000010\"]");
    }

    @Test
    public void testCoordinateFormatDisabledWfs20() throws Exception {
        disableCoordinatesFormattingGml32();
        Document document =
                getAsDOM("wfs?request=GetFeature&version=2.0&typename=st_gml32:Station_gml32");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32[@gml:id=\"st.1\"]/st_gml32:location/gml:Point/gml:pos"
                        + "[text()=\"1 -1\"]");
        // check force decimal notation
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32[@gml:id=\"st.2\"]/st_gml32:location/gml:Point/gml:pos"
                        + "[text()=\"1.0E-7 -1.0E-7\"]");
    }

    private void enableCoordinatesFormattingGml31() {
        enableCoordinateFormatting(
                new NameImpl(StationsMockData.STATIONS_PREFIX_GML31, "Station_gml31"));
    }

    private void disableCoordinatesFormattingGml31() {
        disableCoordinateFormatting(
                new NameImpl(StationsMockData.STATIONS_PREFIX_GML31, "Station_gml31"));
    }

    private void enableCoordinatesFormattingGml32() {
        enableCoordinateFormatting(
                new NameImpl(StationsMockData.STATIONS_PREFIX_GML32, "Station_gml32"));
    }

    private void disableCoordinatesFormattingGml32() {
        disableCoordinateFormatting(
                new NameImpl(StationsMockData.STATIONS_PREFIX_GML32, "Station_gml32"));
    }

    private void enableCoordinateFormatting(Name qname) {
        FeatureTypeInfo info =
                getGeoServer().getCatalog().getResourceByName(qname, FeatureTypeInfo.class);
        info.setNumDecimals(8);
        info.setForcedDecimal(true);
        info.setPadWithZeros(true);
        getGeoServer().getCatalog().save(info);
    }

    private void disableCoordinateFormatting(Name qname) {
        FeatureTypeInfo info =
                getGeoServer().getCatalog().getResourceByName(qname, FeatureTypeInfo.class);
        info.setForcedDecimal(false);
        info.setPadWithZeros(false);
        getGeoServer().getCatalog().save(info);
    }

    @Override
    protected StationsMockData createTestData() {
        return new StationsMockData() {
            @Override
            protected Optional<String> extraStationFeatures() {
                String features =
                        "\nst.2=st.2|station2|32154895|station2@stations.org|POINT(-1.0E-7 1.0E-7)";
                return Optional.of(features);
            }

            @Override
            protected Optional<String> extraMeasurementFeatures() {
                String features = "\nms.3=ms.3|wind|km/h|st.2";
                return Optional.of(features);
            }
        };
    }
}
