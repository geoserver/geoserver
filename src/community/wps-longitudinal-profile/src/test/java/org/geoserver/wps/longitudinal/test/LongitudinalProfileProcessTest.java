/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.junit.Assert;
import org.junit.Test;

public class LongitudinalProfileProcessTest extends WPSTestSupport {

    private static final QName PROFILE =
            new QName(MockData.DEFAULT_URI, "dataProfile", MockData.DEFAULT_PREFIX);
    private static final QName ADJ_LAYER =
            new QName(MockData.DEFAULT_URI, "AdjustmentLayer", MockData.DEFAULT_PREFIX);
    private static final String LAYER_NAME =
            "<wps:Input>\n"
                    + "      <ows:Identifier>layerName</ows:Identifier>\n"
                    + "      <wps:Data>\n"
                    + "        <wps:LiteralData>dataProfile</wps:LiteralData>\n"
                    + "      </wps:Data>\n"
                    + "    </wps:Input>\n";
    private static final String RESPONSE_FORM =
            "<wps:ResponseForm>\n"
                    + "    <wps:RawDataOutput mimeType=\"application/json\">\n"
                    + "      <ows:Identifier>result</ows:Identifier>\n"
                    + "    </wps:RawDataOutput>\n"
                    + "  </wps:ResponseForm>\n";
    private static final String LINESTRING =
            "<wps:Input>\n"
                    + "  <ows:Identifier>linestringWkt</ows:Identifier>\n"
                    + "      <wps:Data>\n"
                    + "        <wps:ComplexData mimeType=\"application/wkt\">"
                    + "          <![CDATA[LINESTRING(843478.269971218 6420348.7621933, 843797.900998497 6420021.75658605, 844490.474212848 6420187.03857354, 844102.691178047 6420613.93854596)]]>"
                    + "        </wps:ComplexData>\n"
                    + "      </wps:Data>\n"
                    + "    </wps:Input>\n";
    private static final String HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                    + "  <ows:Identifier>gs:LongitudinalProfile</ows:Identifier>\n"
                    + "  <wps:DataInputs>\n"
                    + "    ";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        String styleName = "raster";
        testData.addStyle(styleName, "raster.sld", MockData.class, getCatalog());

        Map<SystemTestData.LayerProperty, Object> props = new HashMap<>();
        props.put(SystemTestData.LayerProperty.STYLE, styleName);

        testData.addRasterLayer(
                PROFILE, "coverage.zip", null, Collections.emptyMap(), getCatalog());
        testData.addVectorLayer(
                ADJ_LAYER,
                Map.of(SystemTestData.LayerProperty.SRS, 2154),
                "AdjustmentLayer.properties",
                MockData.class,
                getCatalog());
    }

    @Test
    public void testRequiredParams() throws Exception {
        String requestXml =
                HEADER
                        + LAYER_NAME
                        + "   <wps:Input>\n"
                        + "      <ows:Identifier>distance</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>300</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>"
                        + LINESTRING
                        + "  </wps:DataInputs>\n"
                        + "  "
                        + RESPONSE_FORM
                        + "</wps:Execute>\n"
                        + "\n"
                        + "";

        JSONObject response = (JSONObject) postAsJSON(root(), requestXml, "application/xml");
        JSONObject infos = response.getJSONObject("infos");
        Assert.assertEquals(684.95, infos.get("altitudePositive"));
        Assert.assertEquals(-11.08, infos.get("altitudeNegative"));
        Assert.assertEquals(1746.0248, infos.get("totalDistance"));
        Assert.assertEquals(843478.25, infos.get("firstPointX"));
        Assert.assertEquals(6420349.0, infos.get("firstPointY"));
        Assert.assertEquals(844102.7, infos.get("lastPointX"));
        Assert.assertEquals(6420614.0, infos.get("lastPointY"));
        Assert.assertEquals("dataProfile", infos.get("layer"));
        Assert.assertEquals(8, infos.get("processedPoints"));
        Assert.assertNotNull(infos.get("executedTime"));
        JSONArray profile = response.getJSONArray("profile");
        Assert.assertEquals(8, profile.size());
        JSONObject profile3 = (JSONObject) profile.get(3);
        Assert.assertEquals(694.61163, profile3.get("totalDistanceToThisPoint"));
        Assert.assertEquals(-11.08, profile3.get("altitude"));
        Assert.assertEquals(-4.668393, profile3.get("slope"));
        Assert.assertEquals(844028.75, profile3.get("x"));
        Assert.assertEquals(6420077.0, profile3.get("y"));

        JSONObject profile5 = (JSONObject) profile.get(5);
        Assert.assertEquals(1169.2932, profile5.get("totalDistanceToThisPoint"));
        Assert.assertEquals(23.58, profile5.get("altitude"));
        Assert.assertEquals(9.9350815, profile5.get("slope"));
        Assert.assertEquals(844490.5, profile5.get("x"));
        Assert.assertEquals(6420187.0, profile5.get("y"));

        JSONObject profile7 = (JSONObject) profile.get(7);
        Assert.assertEquals(1746.0248, profile7.get("totalDistanceToThisPoint"));
        Assert.assertEquals(22.83, profile7.get("altitude"));
        Assert.assertEquals(7.9170284, profile7.get("slope"));
        Assert.assertEquals(844102.7, profile7.get("x"));
        Assert.assertEquals(6420614.0, profile7.get("y"));
    }

    @Test
    public void testReprojectCRS() throws Exception {
        String requestXml =
                HEADER
                        + LAYER_NAME
                        + "   <wps:Input>\n"
                        + "      <ows:Identifier>distance</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>300</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>"
                        + LINESTRING
                        + "   <wps:Input>\n"
                        + "       <ows:Identifier>projection</ows:Identifier>\n"
                        + "           <wps:Data>\n"
                        + "               <wps:LiteralData>EPSG:3857</wps:LiteralData>\n"
                        + "           </wps:Data>\n"
                        + "   </wps:Input>"
                        + "  </wps:DataInputs>\n"
                        + "  "
                        + RESPONSE_FORM
                        + "</wps:Execute>\n"
                        + "\n"
                        + "";

        JSONObject response = (JSONObject) postAsJSON(root(), requestXml, "application/xml");
        JSONObject infos = response.getJSONObject("infos");
        Assert.assertEquals(684.95, infos.get("altitudePositive"));
        Assert.assertEquals(-11.08, infos.get("altitudeNegative"));
        Assert.assertEquals(2463.6123, infos.get("totalDistance"));
        Assert.assertEquals(536188.94, infos.get("firstPointX"));
        Assert.assertEquals(5600680.0, infos.get("firstPointY"));
        Assert.assertEquals(537077.4, infos.get("lastPointX"));
        Assert.assertEquals(5601034.5, infos.get("lastPointY"));
        Assert.assertEquals("dataProfile", infos.get("layer"));
        Assert.assertEquals(8, infos.get("processedPoints"));
        Assert.assertNotNull(infos.get("executedTime"));
        JSONArray profile = response.getJSONArray("profile");
        Assert.assertEquals(8, profile.size());
        // Since checking all profiles will be excessive we will check only some in the middle
        JSONObject profile3 = (JSONObject) profile.get(3);
        Assert.assertEquals(980.1413, profile3.get("totalDistanceToThisPoint"));
        Assert.assertEquals(-11.08, profile3.get("altitude"));
        Assert.assertEquals(-3.3120894, profile3.get("slope"));
        Assert.assertEquals(536955.75, profile3.get("x"));
        Assert.assertEquals(5600277.5, profile3.get("y"));

        JSONObject profile5 = (JSONObject) profile.get(5);
        Assert.assertEquals(1649.2133, profile5.get("totalDistanceToThisPoint"));
        Assert.assertEquals(23.58, profile5.get("altitude"));
        Assert.assertEquals(7.0485406, profile5.get("slope"));
        Assert.assertEquals(537609.9, profile5.get("x"));
        Assert.assertEquals(5600418.0, profile5.get("y"));

        JSONObject profile7 = (JSONObject) profile.get(7);
        Assert.assertEquals(2463.6123, profile7.get("totalDistanceToThisPoint"));
        Assert.assertEquals(22.83, profile7.get("altitude"));
        Assert.assertEquals(5.6064906, profile7.get("slope"));
        Assert.assertEquals(537077.4, profile7.get("x"));
        Assert.assertEquals(5601034.5, profile7.get("y"));
    }

    @Test
    public void testAllParams() throws Exception {
        String requestXml =
                HEADER
                        + LAYER_NAME
                        + "    <wps:Input>\n"
                        + "         <ows:Identifier>adjustmentLayerName</ows:Identifier>\n"
                        + "             <wps:Data>\n"
                        + "         <wps:LiteralData>AdjustmentLayer</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>"
                        + "   <wps:Input>\n"
                        + "     <ows:Identifier>altitudeName</ows:Identifier>\n"
                        + "         <wps:Data>\n"
                        + "             <wps:LiteralData>altitude</wps:LiteralData>\n"
                        + "         </wps:Data>\n"
                        + "     </wps:Input>"
                        + "   <wps:Input>\n"
                        + "      <ows:Identifier>distance</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>200</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>"
                        + LINESTRING
                        + "   <wps:Input>\n"
                        + "       <ows:Identifier>projection</ows:Identifier>\n"
                        + "           <wps:Data>\n"
                        + "               <wps:LiteralData>EPSG:4326</wps:LiteralData>\n"
                        + "           </wps:Data>\n"
                        + "   </wps:Input>"
                        + "  </wps:DataInputs>\n"
                        + RESPONSE_FORM
                        + "</wps:Execute>\n"
                        + "\n"
                        + "";

        JSONObject response = (JSONObject) postAsJSON(root(), requestXml, "application/xml");
        JSONObject infos = response.getJSONObject("infos");
        Assert.assertEquals(950.55, infos.get("altitudePositive"));
        Assert.assertEquals(-64.26, infos.get("altitudeNegative"));
        Assert.assertEquals(0.020068234, infos.get("totalDistance"));
        Assert.assertEquals(4.8166676, infos.get("firstPointX"));
        Assert.assertEquals(44.867462, infos.get("firstPointY"));
        Assert.assertEquals(4.8246484, infos.get("lastPointX"));
        Assert.assertEquals(44.869717, infos.get("lastPointY"));
        Assert.assertEquals("dataProfile", infos.get("layer"));
        Assert.assertEquals(11, infos.get("processedPoints"));
        Assert.assertNotNull(infos.get("executedTime"));
        JSONArray profile = response.getJSONArray("profile");
        Assert.assertEquals(11, profile.size());

        JSONObject profile3 = (JSONObject) profile.get(3);
        Assert.assertEquals(0.0049660658, profile3.get("totalDistanceToThisPoint"));
        Assert.assertEquals(5.16, profile3.get("altitude"));
        Assert.assertEquals(2.3977778, profile3.get("slope"));
        Assert.assertEquals(4.8206177, profile3.get("x"));
        Assert.assertEquals(44.864452, profile3.get("y"));

        JSONObject profile5 = (JSONObject) profile.get(6);
        Assert.assertEquals(0.011652884, profile5.get("totalDistanceToThisPoint"));
        Assert.assertEquals(166.35, profile5.get("altitude"));
        Assert.assertEquals(66.30085, profile5.get("slope"));
        Assert.assertEquals(4.827228, profile5.get("x"));
        Assert.assertEquals(44.86546, profile5.get("y"));

        JSONObject profile7 = (JSONObject) profile.get(9);
        Assert.assertEquals(0.018006066, profile7.get("totalDistanceToThisPoint"));
        Assert.assertEquals(-33.08, profile7.get("altitude"));
        Assert.assertEquals(-12.185672, profile7.get("slope"));
        Assert.assertEquals(4.826243, profile7.get("x"));
        Assert.assertEquals(44.86841, profile7.get("y"));
    }
}
