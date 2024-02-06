package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.MAPML_USE_FEATURES;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.bind.DataBindingException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.Polygon;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class MapMLWMSFeatureTest extends WMSTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        String points = MockData.POINTS.getLocalPart();
        String lines = MockData.LINES.getLocalPart();
        String polygons = MockData.POLYGONS.getLocalPart();
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("layerGroup");
        lg.getLayers().add(catalog.getLayerByName(points));
        lg.getLayers().add(catalog.getLayerByName(lines));
        lg.getLayers().add(catalog.getLayerByName(polygons));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg, DefaultGeographicCRS.WGS84);
        catalog.add(lg);
    }

    @Test
    public void testMapMLUseFeatures() throws Exception {

        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        li.getMetadata().put(MAPML_USE_FEATURES, true);
        li.getMetadata().put(MAPML_USE_TILES, false);
        cat.save(li);

        MockRequestResponse requestResponse =
                getMockRequestResponse(
                        MockData.POLYGONS.getLocalPart(), null, null, "EPSG:3857", null);

        MapMLEncoder encoder = new MapMLEncoder();
        StringReader reader = new StringReader(requestResponse.response.getContentAsString());
        Mapml mapmlFeatures = null;
        try {
            mapmlFeatures = encoder.decode(reader);
        } catch (DataBindingException e) {
            fail("MapML response is not valid XML");
        }
        assertEquals(
                "Polygons layer has one feature, so one should show up in the conversion",
                1,
                mapmlFeatures.getBody().getFeatures().size());
        assertEquals(
                "Polygons layer coordinates should match original feature's coordinates",
                "500225,500025,500225,500075,500275,500050,500275,500025,500225,500025",
                ((Polygon)
                                mapmlFeatures
                                        .getBody()
                                        .getFeatures()
                                        .get(0)
                                        .getGeometry()
                                        .getGeometryContent()
                                        .getValue())
                        .getThreeOrMoreCoordinatePairs().get(0).getValue().stream()
                                .collect(Collectors.joining(",")));
    }

    @Test
    public void testExceptionBecauseMoreThanOneFeatureType() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        li.getMetadata().put(MAPML_USE_FEATURES, true);
        li.getMetadata().put(MAPML_USE_TILES, false);
        cat.save(li);
        LayerGroupInfo lgi = cat.getLayerGroupByName("layerGroup");
        lgi.getMetadata().put(MAPML_USE_FEATURES, true);
        lgi.getMetadata().put(MAPML_USE_TILES, false);
        cat.save(lgi);
        MockRequestResponse requestResponse =
                getMockRequestResponse(
                        "layerGroup" + "," + MockData.POLYGONS.getLocalPart(),
                        null,
                        null,
                        "EPSG:3857",
                        null);

        assertTrue(
                "MapML response contains an exception due to multiple feature types",
                requestResponse
                        .response
                        .getContentAsString()
                        .contains(
                                "MapML WMS Feature format does not support Multiple Feature Type output."));
    }

    private MockRequestResponse getMockRequestResponse(
            String name, Map kvp, Locale locale, String srs, String styles) throws Exception {
        String path = null;
        MockHttpServletRequest request = null;
        if (kvp != null) {
            path = "wms";
            request = createRequest(path, kvp);
        } else {
            path =
                    "wms?LAYERS="
                            + name
                            + "&STYLES="
                            + (styles != null ? styles : "")
                            + "&FORMAT="
                            + MapMLConstants.MAPML_MIME_TYPE
                            + "&SERVICE=WMS&VERSION=1.3.0"
                            + "&REQUEST=GetMap"
                            + "&SRS="
                            + srs
                            + "&BBOX=0,0,1,1"
                            + "&WIDTH=150"
                            + "&HEIGHT=150"
                            + "&format_options="
                            + MapMLConstants.MAPML_FEATURE_FORMAT_OPTIONS
                            + ":image/png";
            request = createRequest(path);
        }

        if (locale != null) {
            request.addPreferredLocale(locale);
        }
        request.setMethod("GET");
        request.setContent(new byte[] {});
        MockHttpServletResponse response = dispatch(request, "UTF-8");
        MockRequestResponse result = new MockRequestResponse(request, response);
        return result;
    }

    private static class MockRequestResponse {
        public final MockHttpServletRequest request;
        public final MockHttpServletResponse response;

        public MockRequestResponse(
                MockHttpServletRequest request, MockHttpServletResponse response) {
            this.request = request;
            this.response = response;
        }
    }
}
