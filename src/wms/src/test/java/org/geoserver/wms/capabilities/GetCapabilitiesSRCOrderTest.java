/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2007 OpenPlans
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.HashSet;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Unit test suite for {@link GetCapabilitiesResponse}.
 *
 * @author Antonio Cerciello - Geocat
 * @version $Id$
 */
public class GetCapabilitiesSRCOrderTest extends WMSTestSupport {

    /** The Constant BASE_URL. */
    private static final String BASE_URL = "http://localhost/geoserver";

    /**
     * Test root layer.
     *
     * @throws Exception the exception
     */
    @Test
    public void testRootLayer() throws Exception {

        Document dom = findCapabilities(false);

        // print(dom);
        checkWms13ValidationErrors(dom);

        DOMSource domSource = new DOMSource(dom);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);

        assertTrue(writer.toString().indexOf("22222") < writer.toString().indexOf("11111"));
    }

    /**
     * Find capabilities.
     *
     * @param scaleHintUnitsPerDiaPixel the scale hint units per dia pixel
     * @return the document
     * @throws Exception the exception
     */
    private Document findCapabilities(Boolean scaleHintUnitsPerDiaPixel) throws Exception {
        // set the Scalehint units per diagonal pixel setting.
        WMS wms = getWMS();
        WMSInfo info = wms.getServiceInfo();
        info.getSRS().add("22222");
        info.getSRS().add("11111");
        MetadataMap mm = info.getMetadata();
        mm.put(WMS.SCALEHINT_MAPUNITS_PIXEL, scaleHintUnitsPerDiaPixel);
        info.getGeoServer().save(info);

        Capabilities_1_3_0_Transformer tr =
                new Capabilities_1_3_0_Transformer(
                        wms, BASE_URL, wms.getAllowedMapFormats(), new HashSet<>());
        GetCapabilitiesRequest req = new GetCapabilitiesRequest();
        req.setBaseUrl(BASE_URL);
        req.setVersion(WMS.VERSION_1_3_0.toString());

        Document dom = WMSTestSupport.transform(req, tr);

        Element root = dom.getDocumentElement();
        assertEquals(WMS.VERSION_1_3_0.toString(), root.getAttribute("version"));

        return dom;
    }
}
