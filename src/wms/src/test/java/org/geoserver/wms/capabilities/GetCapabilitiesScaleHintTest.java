/**
 * 
 */
package org.geoserver.wms.capabilities;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Test cases for Capabilities' ScaleHint  
 * 
 * @author Mauricio Pazos
 *
 */
public class GetCapabilitiesScaleHintTest extends GeoServerSystemTestSupport {

    private final XpathEngine xpath;

    private static final String BASE_URL = "http://localhost/geoserver";

    private static final Set<String> FORMATS = Collections.singleton("image/png");

    private static final Set<String> LEGEND_FORMAT = Collections.singleton("image/png");

    /** Test layers */
    public static final QName REGIONATED = new QName(MockData.SF_URI, "Regionated", MockData.SF_PREFIX);
    public static final QName ACCIDENT = new QName(MockData.SF_URI, "Accident", MockData.SF_PREFIX);
    public static final QName ACCIDENT2 = new QName(MockData.SF_URI, "Accident2", MockData.SF_PREFIX);
    public static final QName ACCIDENT3 = new QName(MockData.SF_URI, "Accident3", MockData.SF_PREFIX);
    

    private Catalog catalog;

    public GetCapabilitiesScaleHintTest(){
    	
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }
	

	/**
	 * Adds required styles to test the selection of maximum and minimum denominator from style's rules.
	 */
	@Override
	protected void onSetUp(SystemTestData testData) throws Exception {
		
		this.catalog = getCatalog();

		// creates the Regionated layer and adds it to the geoserver catalog
		{
			testData.addVectorLayer(
		                REGIONATED,
		                null, 
		                REGIONATED.getLocalPart()+".properties",
		                getClass(), 
		                this.catalog             
	                );
			
	    	final String styleName = "Regionated";
			testData.addStyle(styleName, this.catalog);
			
			StyleInfo defaultStyle = this.catalog.getStyleByName(styleName);
			
			String layerId = getLayerId(REGIONATED);
			LayerInfo layerInfo =  this.catalog.getLayerByName(layerId);
			layerInfo.setDefaultStyle(defaultStyle);
		}
		
		// creates the Accident layer and adds it to the geoserver catalog 
		{
			testData.addVectorLayer(
	                ACCIDENT,
	                null, 
	                ACCIDENT.getLocalPart()+".properties",
	                getClass(), 
	                this.catalog             
            );
	
			final String styleName = "Accident";
			testData.addStyle(styleName, this.catalog);
			
			StyleInfo defaultStyle = this.catalog.getStyleByName(styleName);
			
			String layerId = getLayerId(ACCIDENT);
			LayerInfo layerInfo =  this.catalog.getLayerByName(layerId);
			layerInfo.setDefaultStyle(defaultStyle);
		}
		// creates the Accident2 layer and adds it to the geoserver catalog. The minimum scale denominator was not set in the style file. 
		{
			testData.addVectorLayer(
	                ACCIDENT2,
	                null, 
	                ACCIDENT2.getLocalPart()+".properties",
	                getClass(), 
	                this.catalog             
            );
	
			final String styleName = "Accident2";
			testData.addStyle(styleName, this.catalog);
			
			StyleInfo defaultStyle = this.catalog.getStyleByName(styleName);
			
			String layerId = getLayerId(ACCIDENT2);
			LayerInfo layerInfo =  this.catalog.getLayerByName(layerId);
			layerInfo.setDefaultStyle(defaultStyle);
		}
		
        WMS wms = new WMS(getGeoServer());
        GeoServerInfo geoserverInfo = wms.getGeoServer().getGlobal();
		
		// creates the Accident3 layer and adds it to the geoserver catalog. The maximum scale denominator was not set in the style file. 
		{
			testData.addVectorLayer(
	                ACCIDENT3,
	                null, 
	                ACCIDENT3.getLocalPart()+".properties",
	                getClass(), 
	                this.catalog             
            );
	
			final String styleName = "Accident3";
			testData.addStyle(styleName, this.catalog);
			
			StyleInfo defaultStyle = this.catalog.getStyleByName(styleName);
			
			String layerId = getLayerId(ACCIDENT3);
			LayerInfo layerInfo =  this.catalog.getLayerByName(layerId);
			layerInfo.setDefaultStyle(defaultStyle);
		}
		
	}


    /**
     * Default values for ScaleHint should be set.
     * 
     * <pre>
     * The computation of Min and Max values return: 
     * 		Min: 0.0
     * 		Max: infinity
     * 
     * Capabilities document Expected:
     *  
     * 		ScaleHint element shouldn't be generated.
     * </pre>
     * 
     * @throws Exception
     */
    @Test
    public void scaleHintDefaultValues()throws Exception{
        
    	Document dom = findCapabilities();

		Element layerElement= searchLayerElement(getLayerId(ACCIDENT), dom);
		
		NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");
		
		Element scaleElement = (Element)scaleNode.item(0);

		assertTrue(scaleElement == null); // scale hint is not generated
    }

    /**
     * Default values for ScaleHint should be set.
     * 
     * <pre>
     * The computation of Min and Max values return: 
     * 		Min: 0.0
     * 		Max: a value
     * 
     * Capabilities document Expected:
     *  
     *   <ScaleHint min=0 max=value/>     
     * </pre>
     * 
     * @throws Exception
     */
    @Test
    public void scaleHintDefaultMinValue()throws Exception{
        
    	Document dom = findCapabilities();

		Element layerElement= searchLayerElement(getLayerId(ACCIDENT2), dom);
		
		NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");
		
		Element scaleElement = (Element)scaleNode.item(0);

        assertEquals(0.0, Double.valueOf(scaleElement.getAttribute("min")));
	    assertEquals(Double.valueOf(640000000), Double.valueOf(scaleElement.getAttribute("max")));

    }

    /**
     * Default values for ScaleHint should be set.
     * 
     * <pre>
     * The computation of Min and Max values return: 
     * 		Min: a value
     * 		Max: Infinity
     * 
     * Capabilities document Expected:
     *  
     *   <ScaleHint min=value max=infinity/>     
     * </pre>
     * 
     * @throws Exception
     */
    @Test
    public void scaleHintDefaultMaxValue()throws Exception{
        
    	Document dom = findCapabilities();

		Element layerElement= searchLayerElement(getLayerId(ACCIDENT3), dom);
		
		NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");
		
		Element scaleElement = (Element)scaleNode.item(0);

        assertEquals(Double.valueOf(320000000), Double.valueOf(scaleElement.getAttribute("min")));
	    assertEquals(Double.POSITIVE_INFINITY, Double.valueOf(scaleElement.getAttribute("max")));

    }
	
    /**
     * <pre>
     * Max is the maximum value found in the set of rules 
     * Min is the minimum value found in the set of rules
     * </pre>
     * 
     * @throws Exception
     */
    @Test
    public void scaleHintFoundMaxMinDenominators()throws Exception{

    	Document dom = findCapabilities();

		final String layerName = getLayerId(REGIONATED);
        Element layerElement= searchLayerElement(layerName, dom);
        
		NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");
		Element scaleElement = (Element)scaleNode.item(0);
            
        assertEquals(Double.valueOf(80000000), Double.valueOf(scaleElement.getAttribute("min")));
	    assertEquals(Double.valueOf(640000000), Double.valueOf(scaleElement.getAttribute("max")));
    }
    
    /**
     * Retrieves the WMS's capabilities document.
     * 
     * @return Capabilities as {@link Document}
     * 
     * @throws Exception
     */
    private Document findCapabilities() throws Exception{
    	
    	WMS wms = (WMS) applicationContext.getBean("wms");
    	
    	GetCapabilitiesTransformer tr = new GetCapabilitiesTransformer(wms, BASE_URL, FORMATS, LEGEND_FORMAT, null);
    	GetCapabilitiesRequest req = new GetCapabilitiesRequest();
        req.setBaseUrl(BASE_URL);
        req.setVersion(WMS.VERSION_1_1_1.toString());

    	Document dom = WMSTestSupport.transform(req, tr);

    	Element root = dom.getDocumentElement();
		Assert.assertEquals(WMS.VERSION_1_1_1.toString(), root.getAttribute("version"));
    	
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
