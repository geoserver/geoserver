/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.util.List;
import org.geoserver.data.test.SystemTestData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** @author etj (Emanuele Tajariol @ GeoSolutions) */
public class ChainedProcessListenerTest extends WPSTestSupport {

    ChainedProcessListenerImpl chainedListener;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/chainedProcessListenerContext.xml");
    }

    @Before
    public void retrieveListener() throws Exception {
        chainedListener =
                applicationContext.getBean(
                        "chainedProcessListener", ChainedProcessListenerImpl.class);
        chainedListener.cleanup();
    }

    @Before
    public void oneTimeSetUp() throws Exception {
        WPSInfo wps = getGeoServer().getService(WPSInfo.class);

        // want at least two asynchronous processes to test concurrency
        wps.setMaxAsynchronousProcesses(Math.max(2, wps.getMaxAsynchronousProcesses()));

        // avoid timeout while debugging
        wps.setMaxSynchronousExecutionTime(Integer.MAX_VALUE);
        wps.setMaxAsynchronousExecutionTime(Integer.MAX_VALUE);
        wps.setMaxSynchronousTotalTime(Integer.MAX_VALUE);
        wps.setMaxAsynchronousTotalTime(Integer.MAX_VALUE);

        getGeoServer().save(wps);
    }

    @Test
    public void testChainedRequest() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0'"
                        + "  xmlns:wps='http://www.opengis.net/wps/1.0.0' xmlns:ows='http://www.opengis.net/ows/1.1' xmlns:xlink='http://www.w3.org/1999/xlink'>"
                        + "  <ows:Identifier>JTS:area</ows:Identifier>"
                        + "  <wps:DataInputs>"
                        + "    <wps:Input>"
                        + "      <ows:Identifier>geom</ows:Identifier>"
                        + "      <wps:Reference xlink:href='http://geoserver/wps' method='POST' mimeType='application/xml'>"
                        + "        <wps:Body>"
                        + "          <wps:Execute service='WPS' version='1.0.0'>"
                        + "            <ows:Identifier>JTS:union</ows:Identifier>"
                        + "            <wps:DataInputs>"
                        + "              <wps:Input>"
                        + "                <ows:Identifier>geom</ows:Identifier>"
                        + "                <wps:Reference xlink:href='http://geoserver/wps' method='POST' mimeType='application/xml'>"
                        + "                  <wps:Body>"
                        + "                    <wps:Execute service='WPS' version='1.0.0'>"
                        + "                      <ows:Identifier>JTS:buffer</ows:Identifier>"
                        + "                      <wps:DataInputs>"
                        + "                        <wps:Input>"
                        + "                          <ows:Identifier>geom</ows:Identifier>"
                        + "                          <wps:Data>"
                        + "                            <wps:ComplexData mimeType='application/wkt'>"
                        + "                              <![CDATA[POLYGON((10.0 10.0, 10.0 50.0, 50.0 50.0, 50.0 10.0, 10.0 10.0))]]>"
                        + "                            </wps:ComplexData>"
                        + "                          </wps:Data>"
                        + "                        </wps:Input>"
                        + "                        <wps:Input>"
                        + "                          <ows:Identifier>distance</ows:Identifier>"
                        + "                          <wps:Data>"
                        + "                            <wps:LiteralData>10.0</wps:LiteralData>"
                        + "                          </wps:Data>"
                        + "                        </wps:Input>"
                        + "                      </wps:DataInputs>"
                        + "                      <wps:ResponseForm>"
                        + "                        <wps:RawDataOutput mimeType='application/wkt'>"
                        + "                          <ows:Identifier>result</ows:Identifier>"
                        + "                        </wps:RawDataOutput>"
                        + "                      </wps:ResponseForm>"
                        + "                    </wps:Execute>"
                        + "                  </wps:Body>"
                        + "                </wps:Reference>"
                        + "              </wps:Input>"
                        + "              <wps:Input>"
                        + "                <ows:Identifier>geom</ows:Identifier>"
                        + "                <wps:Reference xlink:href='http://geoserver/wps' method='POST' mimeType='application/xml'>"
                        + "                  <wps:Body>"
                        + "                    <wps:Execute service='WPS' version='1.0.0'>"
                        + "                      <ows:Identifier>JTS:difference</ows:Identifier>"
                        + "                      <wps:DataInputs>"
                        + "                        <wps:Input>"
                        + "                          <ows:Identifier>a</ows:Identifier>"
                        + "                          <wps:Data>"
                        + "                            <wps:ComplexData mimeType='application/wkt'>"
                        + "                              <![CDATA[POLYGON((40.0 40.0, 40.0 100.0, 100.0 100.0, 100.0 40.0, 40.0 40.0))]]>"
                        + "                            </wps:ComplexData>"
                        + "                          </wps:Data>"
                        + "                        </wps:Input>"
                        + "                        <wps:Input>"
                        + "                          <ows:Identifier>b</ows:Identifier>"
                        + "                          <wps:Data>"
                        + "                            <wps:ComplexData mimeType='application/wkt'>"
                        + "                              <![CDATA[POLYGON((80.0 80.0, 80.0 120.0, 120.0 120.0, 120.0 80.0, 80.0 80.0))]]>"
                        + "                            </wps:ComplexData>"
                        + "                          </wps:Data>"
                        + "                        </wps:Input>                    "
                        + "                      </wps:DataInputs>"
                        + "                      <wps:ResponseForm>"
                        + "                        <wps:RawDataOutput mimeType='application/wkt'>"
                        + "                          <ows:Identifier>result</ows:Identifier>"
                        + "                        </wps:RawDataOutput>"
                        + "                      </wps:ResponseForm>"
                        + "                    </wps:Execute>"
                        + "                  </wps:Body>"
                        + "                </wps:Reference>"
                        + "              </wps:Input>"
                        + "            </wps:DataInputs>"
                        + "            <wps:ResponseForm>"
                        + "              <wps:RawDataOutput mimeType='application/wkt'>"
                        + "                <ows:Identifier>result</ows:Identifier>"
                        + "              </wps:RawDataOutput>"
                        + "            </wps:ResponseForm>"
                        + "          </wps:Execute>"
                        + "        </wps:Body>"
                        + "      </wps:Reference>"
                        + "    </wps:Input>"
                        + "  </wps:DataInputs>"
                        + "  <wps:ResponseForm>"
                        + "    <wps:RawDataOutput mimeType='application/xml'>"
                        + "      <ows:Identifier>result</ows:Identifier>"
                        + "    </wps:RawDataOutput>"
                        + "  </wps:ResponseForm>"
                        + "</wps:Execute>";

        MockHttpServletResponse resp = postAsServletResponse(root(), xml);
        Assert.assertEquals("text/plain", resp.getContentType());
        // the result may be inaccurate since there's a buffer operation
        Assert.assertTrue(resp.getContentAsString().startsWith("6334.10838"));

        String expectedNotifications[] = {
            "started JTS:area",
            "started JTS:union",
            "started JTS:buffer",
            "completed JTS:buffer",
            "started JTS:difference",
            "completed JTS:difference",
            "completed JTS:union",
            "completed JTS:area"
        };

        Assert.assertNotNull(chainedListener);
        Assert.assertArrayEquals(expectedNotifications, chainedListener.recorded.toArray());
    }

    @Test
    public void testErroringChainedRequest() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0'"
                        + "  xmlns:wps='http://www.opengis.net/wps/1.0.0' xmlns:ows='http://www.opengis.net/ows/1.1' xmlns:xlink='http://www.w3.org/1999/xlink'>"
                        + "  <ows:Identifier>JTS:area</ows:Identifier>"
                        + "  <wps:DataInputs>"
                        + "    <wps:Input>"
                        + "      <ows:Identifier>geom</ows:Identifier>"
                        + "      <wps:Reference xlink:href='http://geoserver/wps' method='POST' mimeType='application/xml'>"
                        + "        <wps:Body>"
                        + "          <wps:Execute service='WPS' version='1.0.0'>"
                        + "            <ows:Identifier>JTS:union</ows:Identifier>"
                        + "            <wps:DataInputs>"
                        + "              <wps:Input>"
                        + "                <ows:Identifier>geom</ows:Identifier>"
                        + "                <wps:Reference xlink:href='http://geoserver/wps' method='POST' mimeType='application/xml'>"
                        + "                  <wps:Body>"
                        + "                    <wps:Execute service='WPS' version='1.0.0'>"
                        + "                      <ows:Identifier>JTS:buffer</ows:Identifier>"
                        + "                      <wps:DataInputs>"
                        + "                        <wps:Input>"
                        + "                          <ows:Identifier>geom</ows:Identifier>"
                        + "                          <wps:Data>"
                        + "                            <wps:ComplexData mimeType='application/wkt'>"
                        + "                              <![CDATA[POLYGON((10.0 10.0, 10.0 50.0, 50.0 50.0, 50.0 10.0, 10.0 10.0))]]>"
                        + "                            </wps:ComplexData>"
                        + "                          </wps:Data>"
                        + "                        </wps:Input>"
                        + "                        <wps:Input>"
                        + "                          <ows:Identifier>distance</ows:Identifier>"
                        + "                          <wps:Data>"
                        + "                            <wps:LiteralData>10.0</wps:LiteralData>"
                        + "                          </wps:Data>"
                        + "                        </wps:Input>"
                        + "                      </wps:DataInputs>"
                        + "                      <wps:ResponseForm>"
                        + "                        <wps:RawDataOutput mimeType='application/wkt'>"
                        + "                          <ows:Identifier>result</ows:Identifier>"
                        + "                        </wps:RawDataOutput>"
                        + "                      </wps:ResponseForm>"
                        + "                    </wps:Execute>"
                        + "                  </wps:Body>"
                        + "                </wps:Reference>"
                        + "              </wps:Input>"
                        + "              <wps:Input>"
                        + "                <ows:Identifier>geom</ows:Identifier>"
                        + "                <wps:Reference xlink:href='http://geoserver/wps' method='POST' mimeType='application/xml'>"
                        + "                  <wps:Body>"
                        + "                    <wps:Execute service='WPS' version='1.0.0'>"
                        + "                      <ows:Identifier>JTS:difference</ows:Identifier>"
                        + "                      <wps:DataInputs>"
                        + "                        <wps:Input>"
                        + "                          <ows:Identifier>a</ows:Identifier>"
                        + "                          <wps:Data>"
                        + "                            <wps:ComplexData mimeType='application/wkt'>"
                        + "                              <![CDATA[POLYGON((40.0 40.0, 40.0 100.0, 100.0 100.0, 100.0 40.0, 40.0 40.0))]]>"
                        + "                            </wps:ComplexData>"
                        + "                          </wps:Data>"
                        + "                        </wps:Input>"
                        + "                        <wps:Input>"
                        + "                          <ows:Identifier>b</ows:Identifier>"
                        + "                          <wps:Data>"
                        + "                            <wps:ComplexData mimeType='application/wkt'>"
                        + "                              This is a very very bad WKT"
                        + "                            </wps:ComplexData>"
                        + "                          </wps:Data>"
                        + "                        </wps:Input>                    "
                        + "                      </wps:DataInputs>"
                        + "                      <wps:ResponseForm>"
                        + "                        <wps:RawDataOutput mimeType='application/wkt'>"
                        + "                          <ows:Identifier>result</ows:Identifier>"
                        + "                        </wps:RawDataOutput>"
                        + "                      </wps:ResponseForm>"
                        + "                    </wps:Execute>"
                        + "                  </wps:Body>"
                        + "                </wps:Reference>"
                        + "              </wps:Input>"
                        + "            </wps:DataInputs>"
                        + "            <wps:ResponseForm>"
                        + "              <wps:RawDataOutput mimeType='application/wkt'>"
                        + "                <ows:Identifier>result</ows:Identifier>"
                        + "              </wps:RawDataOutput>"
                        + "            </wps:ResponseForm>"
                        + "          </wps:Execute>"
                        + "        </wps:Body>"
                        + "      </wps:Reference>"
                        + "    </wps:Input>"
                        + "  </wps:DataInputs>"
                        + "  <wps:ResponseForm>"
                        + "    <wps:RawDataOutput mimeType='application/xml'>"
                        + "      <ows:Identifier>result</ows:Identifier>"
                        + "    </wps:RawDataOutput>"
                        + "  </wps:ResponseForm>"
                        + "</wps:Execute>";

        MockHttpServletResponse resp = postAsServletResponse(root(), xml);
        Assert.assertEquals("text/xml", resp.getContentType()); // there's an ExceptionResponse here

        String expectedNotifications[] = {
            "started JTS:area",
            "started JTS:union",
            "started JTS:buffer",
            "completed JTS:buffer",
            "started JTS:difference",
            "failed JTS:difference",
            "failed JTS:union",
            "failed JTS:area"
        };

        Assert.assertNotNull(chainedListener);
        Assert.assertArrayEquals(expectedNotifications, chainedListener.recorded.toArray());
    }
}
