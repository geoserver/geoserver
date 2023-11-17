package org.geoserver.wps.gs.download;

import static org.geoserver.catalog.DimensionInfo.NearestFailBehavior.EXCEPTION;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.renderer.style.FontCache;

public class BaseDownloadImageProcessTest extends WPSTestSupport {
    protected static final String SAMPLES = "src/test/resources/org/geoserver/wps/gs/download/";
    protected static final QName WATERTEMP =
            new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    protected static final QName BMTIME = new QName(MockData.SF_URI, "bmtime", MockData.SF_PREFIX);
    protected static final String UNITS = "foot";
    protected static final String UNIT_SYMBOL = "ft";
    protected static QName GIANT_POLYGON =
            new QName(MockData.CITE_URI, "giantPolygon", MockData.CITE_PREFIX);

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:TestContext.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // disable entity resolver as it won't let the tests run in IntelliJ if also GeoTools is
        // loaded...
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setXmlExternalEntitiesEnabled(true);
        getGeoServer().save(global);

        // add water temperature
        testData.addStyle("temperature", "temperature.sld", DownloadMapProcess.class, catalog);
        Map<SystemTestData.LayerProperty, Object> propertyMap = new HashMap<>();
        propertyMap.put(SystemTestData.LayerProperty.STYLE, "temperature");
        testData.addRasterLayer(
                WATERTEMP, "watertemp.zip", null, propertyMap, SystemTestData.class, catalog);
        setupRasterDimension(
                WATERTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATERTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        // add a bluemarble four months mosaic
        testData.addRasterLayer(BMTIME, "bm_time.zip", null, null, getClass(), catalog);
        setupRasterDimension(
                BMTIME, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        // set up the nearest match with exception behavior, should not really throw though
        // as this is a WPS service trying to mimic a WMS client (which would just not display
        // the layer)
        setupNearestMatch(BMTIME, ResourceInfo.TIME, true, "P3D", EXCEPTION, false);

        // a world covering layer with no dimensions
        testData.addVectorLayer(
                GIANT_POLYGON,
                Collections.emptyMap(),
                "giantPolygon.properties",
                SystemTestData.class,
                getCatalog());

        // add decoration layouts
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        copyResource("watermarker.xml", layouts);
        copyResource("dynawatermarker.xml", layouts);
        copyResource("geoserver.png", layouts);
        copyResource("osgeo.png", layouts);
        copyResource("timestamper.xml", layouts);
        copyResource("formattedTimestamper.xml", layouts);

        // register font for timestamping
        Font font = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("Vera.ttf"));
        FontCache.getDefaultInstance().registerFont(font);
    }

    private void copyResource(String name, File layouts) throws IOException {
        FileUtils.copyURLToFile(getClass().getResource(name), new File(layouts, name));
    }

    /** Strips a full URL to a reduced version that works with the test harness */
    protected String getTestReference(String fullLocation) {
        return fullLocation.substring(fullLocation.indexOf('?') - 3);
    }
}
