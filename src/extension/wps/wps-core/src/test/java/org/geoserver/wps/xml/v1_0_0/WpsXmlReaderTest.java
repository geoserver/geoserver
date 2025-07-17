/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.xml.v1_0_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Map;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.InputType;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.xml.WPSConfiguration;
import org.junit.Test;

public class WpsXmlReaderTest extends WPSTestSupport {
    @Test
    public void testWpsExecuteWithGetFeatureAndViewParams() throws Exception {
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.opengis.net/wps/1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wcs="http://www.opengis.net/wcs/1.1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">
                  <ows:Identifier>gs:Bounds</ows:Identifier>
                  <wps:DataInputs>
                    <wps:Input>
                      <ows:Identifier>features</ows:Identifier>
                      <wps:Reference mimeType="text/xml; subtype=wfs-collection/1.0" \
                 xlink:href="http://geoserver/wfs" method="POST">
                         <wps:Body>
                           <wfs:GetFeature service="WFS" version="1.0.0" outputFormat="GML2" xmlns:test="test" viewParams="A:b;C:d">
                             <wfs:Query typeName="test:test"/>
                           </wfs:GetFeature>
                         </wps:Body>
                      </wps:Reference>
                    </wps:Input>
                  </wps:DataInputs>
                  <wps:ResponseForm>
                    <wps:RawDataOutput>
                      <ows:Identifier>bounds</ows:Identifier>
                    </wps:RawDataOutput>
                  </wps:ResponseForm>
                </wps:Execute>""";

        WpsXmlReader reader = new WpsXmlReader(
                "Execute",
                "1.0.0",
                new WPSConfiguration(),
                new org.geoserver.util.EntityResolverProvider(this.getGeoServer()));
        Object parsed = reader.read(null, new StringReader(xml), null);
        assertTrue(parsed instanceof ExecuteType);

        ExecuteType request = (ExecuteType) parsed;
        assertFalse(request.getDataInputs().eContents().isEmpty());
        InputType features = (InputType) request.getDataInputs().eContents().get(0);
        assertNotNull(features.getReference());
        assertNotNull(features.getReference().getBody());
        assertTrue(features.getReference().getBody() instanceof net.opengis.wfs.GetFeatureType);
        net.opengis.wfs.GetFeatureType getFeature =
                (net.opengis.wfs.GetFeatureType) features.getReference().getBody();
        assertFalse(getFeature.getViewParams().isEmpty());
        Map viewParams = (Map) getFeature.getViewParams().get(0);
        assertEquals(2, viewParams.keySet().size());
        assertEquals("b", viewParams.get("A"));
        assertEquals("d", viewParams.get("C"));
    }

    @Test
    public void testWpsExecuteWithGetFeature_2_0_0() throws Exception {
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:wfs="http://www.opengis.net/wfs/2.0" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">
                  <ows:Identifier>gs:Bounds</ows:Identifier>
                  <wps:DataInputs>
                    <wps:Input>
                      <ows:Identifier>features</ows:Identifier>
                      <wps:Reference mimeType="text/xml" \
                 xlink:href="http://geoserver/wfs" method="POST">
                         <wps:Body>
                           <wfs:GetFeature service="WFS" version="2.0.0" outputFormat="GML2" xmlns:test="test" >
                             <wfs:Query typeNames="test:test"/>
                           </wfs:GetFeature>
                         </wps:Body>
                      </wps:Reference>
                    </wps:Input>
                  </wps:DataInputs>
                  <wps:ResponseForm>
                    <wps:RawDataOutput>
                      <ows:Identifier>bounds</ows:Identifier>
                    </wps:RawDataOutput>
                  </wps:ResponseForm>
                </wps:Execute>""";

        WpsXmlReader reader = new WpsXmlReader(
                "Execute",
                "1.0.0",
                new WPSConfiguration(),
                new org.geoserver.util.EntityResolverProvider(this.getGeoServer()));
        Object parsed = reader.read(null, new StringReader(xml), null);
        assertTrue(parsed instanceof ExecuteType);

        ExecuteType request = (ExecuteType) parsed;
        assertFalse(request.getDataInputs().eContents().isEmpty());
        InputType features = (InputType) request.getDataInputs().eContents().get(0);
        assertNotNull(features.getReference());
        assertNotNull(features.getReference().getBody());
        assertTrue(features.getReference().getBody() instanceof net.opengis.wfs20.GetFeatureType);
    }
}
