/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2007 OpenPlans
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Unit test suite for {@link GetCapabilitiesResponse}
 * 
 * @author Antonio Cerciello - Geocat
 * @version $Id$
 */
public class GetCapabilitiesRootLayerTitleTest extends WMSTestSupport {

    private final XpathEngine xpath;

    private static final String BASE_URL = "http://localhost/geoserver";

    public GetCapabilitiesRootLayerTitleTest() {
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("wms", "http://www.opengis.net/wms");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }

    @Test
    public void testRootLayer()throws Exception{

        Document dom = findCapabilities(false);

        // print(dom);
        checkWms13ValidationErrors(dom);

        WMS wms = getWMS();
        WMSInfo info=wms.getServiceInfo();
        
        DOMSource domSource = new DOMSource(dom);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        
        assertEquals(writer.toString().contains(info.getRootLayerTitle()) , true);
    }

    /**
     * Retrieves the WMS's capabilities document.
     * 
     * @param scaleHintUnitsPerDiaPixel true if the scalehint must be in units per diagonal of a pixel
     * @return Capabilities as {@link Document}
     * 
     */
    private Document findCapabilities(Boolean scaleHintUnitsPerDiaPixel) throws Exception{
        //set the Scalehint units per diagonal pixel setting.
        WMS wms = getWMS();
        WMSInfo info=wms.getServiceInfo();
        info.setRootLayerTitle("test the title");  
        MetadataMap mm= info.getMetadata();
        mm.put(WMS.SCALEHINT_MAPUNITS_PIXEL, scaleHintUnitsPerDiaPixel);
        info.getGeoServer().save(info);
        
        Capabilities_1_3_0_Transformer tr = new Capabilities_1_3_0_Transformer(wms, BASE_URL, 
                wms.getAllowedMapFormats(), new HashSet<ExtendedCapabilitiesProvider>());
        GetCapabilitiesRequest req = new GetCapabilitiesRequest();
        req.setBaseUrl(BASE_URL);
        req.setVersion(WMS.VERSION_1_3_0.toString());

        Document dom = WMSTestSupport.transform(req, tr);

        Element root = dom.getDocumentElement();
        assertEquals(WMS.VERSION_1_3_0.toString(), root.getAttribute("version"));

        return dom;
    }

    /** 
     * Searches the required layer in the capabilities document. 
     * 
     * @param layerRequired
     * @param capabilities
     * @return The layer element or null it the required layer isn't found
     * @throws XpathException
     */
    private Element searchLayerElement(final String layerRequired, Document capabilities) throws XpathException {

        NodeList layersNodes = xpath.getMatchingNodes("//Title",capabilities);
        
        layersNodes.item(0).setNodeValue(layerRequired);
           
        for (int i = 0; i < layersNodes.getLength(); i++) {

            Element e = (Element) layersNodes.item(i);
            
            e.setNodeValue(layerRequired);

            String nodeValue = e.getNodeValue();

            if(layerRequired.equalsIgnoreCase(nodeValue)){

                return  e;
            }

        }
        return null; // not found
    }

}
