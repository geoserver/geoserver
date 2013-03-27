/**
 * 
 */
package org.geoserver.wms.capabilities;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * 
 * @author Mauricio Pazos
 *
 */
public class GetCapabilitiesScaleHintTest extends GeoServerSystemTestSupport {

    private final XpathEngine xpath;

    /** default base url to feed a GetCapabilitiesTransformer with for it to append the DTD location */
    private static final String BASE_URL = "http://localhost/geoserver";

    /** test map formats to feed a GetCapabilitiesTransformer with */
    private static final Set<String> FORMATS = Collections.singleton("image/png");

    /** test legend formats to feed a GetCapabilitiesTransformer with */
    private static final Set<String> LEGEND_FORMAT = Collections.singleton("image/png");

	private Catalog catalog;

    public GetCapabilitiesScaleHintTest(){
    	
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }


	@Override
	protected void onSetUp(SystemTestData testData) throws Exception {
		super.onSetUp(testData);
		
		Catalog catalog = getCatalog();

        // add new style
		String styleName = "ScaleHintData";
        testData.addStyle( styleName , styleName + ".sld",MockData.class, catalog);
		StyleInfo style = catalog.getStyleByName(styleName);
		
		LayerInfo layer = catalog.getLayerByName(getLayerId(MockData.LAKES));
		layer.setDefaultStyle(style);
		
		this.catalog = catalog;
	}
	
	@Before
	public void initCatalog(){
		
		this.catalog = getCatalog();
	}

    protected WMS getWMS() {
        WMS wms = (WMS) applicationContext.getBean("wms");
        return wms;
    }

    /**
     * Default values for ScaleHint should be set.
     * <pre>
     * Min: 0.0
     * Max: infinity
     * </pre>
     * 
     * @throws Exception
     */
    @Test
    public void testWMS_1_1_1_DefaultScaleHint()throws Exception{
        
    	GetCapabilitiesTransformer tr = new GetCapabilitiesTransformer(getWMS(), BASE_URL, FORMATS, LEGEND_FORMAT, null);
    	GetCapabilitiesRequest req = new GetCapabilitiesRequest();
        req.setBaseUrl(BASE_URL);
        req.setVersion(WMS.VERSION_1_1_1.toString());

    	Document dom = WMSTestSupport.transform(req, tr);

    	Element root = dom.getDocumentElement();
		Assert.assertEquals(WMS.VERSION_1_1_1.toString(), root.getAttribute("version"));

        Element layerElement= searchLayerElement("cite:BasicPolygons", dom);
		NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");
		Element scaleElement = (Element)scaleNode.item(0);
		
        String min = scaleElement.getAttribute("min");
        String max = scaleElement.getAttribute("max");
            
        Assert.assertEquals(0.0, Double.valueOf(min));
        Assert.assertEquals(Double.POSITIVE_INFINITY, Double.valueOf(max));
    }


	
    /**
     * <pre>
     * Max is the maximum value found in the rules
     * Min is the minimum value found in the rules
     * </pre>
     * 
     * @throws Exception
     */
    @Test
    public void testWMS_1_1_1_ScaleHint()throws Exception{

    	GetCapabilitiesTransformer tr = new GetCapabilitiesTransformer(getWMS(), BASE_URL, FORMATS, LEGEND_FORMAT, null);
    	GetCapabilitiesRequest req = new GetCapabilitiesRequest();
        req.setBaseUrl(BASE_URL);
        req.setVersion(WMS.VERSION_1_1_1.toString());

    	Document dom = WMSTestSupport.transform(req, tr);

    	Element root = dom.getDocumentElement();
		Assert.assertEquals(WMS.VERSION_1_1_1.toString(), root.getAttribute("version"));

        Element layerElement= searchLayerElement("cite:Lakes", dom);
		NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");
		Element scaleElement = (Element)scaleNode.item(0);
		
        String min = scaleElement.getAttribute("min");
        String max = scaleElement.getAttribute("max");
            
        Assert.assertEquals(160000000, Double.valueOf(min));
	    Assert.assertEquals(320000000, Double.valueOf(max));
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
		
		NodeList layersNodes = xpath.getMatchingNodes("//Layer/Name",capabilities);
        for (int i = 0; i < layersNodes.getLength(); i++) {
        	
            Element e = (Element) layersNodes.item(i);
            NodeList childNodes = e.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
            	
				Node item = childNodes.item(j);
            	String nodeValue = item.getNodeValue();
            	
            	if(layerRequired.equalsIgnoreCase(nodeValue)){
            		
            		return  (Element) e.getParentNode(); // returns the layer element associated to the required layer name.
            	}
			}
        }
        return null; // not found
	}

	
	
}
