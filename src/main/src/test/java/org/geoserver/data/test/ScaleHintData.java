package org.geoserver.data.test;

import java.io.IOException;
import java.net.URL;

import javax.xml.namespace.QName;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;


/**
 * This class defines the required data to test the capabilities' ScaleHint property.
 * <p>
 * The Style contains many rules with different maximum and minimum denominators.   
 * </p>
 * 
 * @author Mauricio Pazos
 */
public class ScaleHintData extends SystemTestData {

    public static final String LAYER_NAME = "ImportantLakes";
	public static final QName IMPORTANT_LAKES = new QName(CITE_URI, LAYER_NAME, CITE_PREFIX);
	public static LayerInfo layer;
    
	public ScaleHintData() throws IOException {
		super();
	}

    public void setUpLayers() throws IOException {

    	super.setUpDefaultLayers();
    	
    	addVectorLayer(IMPORTANT_LAKES, catalog);
    	
    	final String styleName = "ScaleHintData";
        StyleInfo defaultStyle = catalog.getStyleByName(styleName);
        if (defaultStyle == null) {
            //see if the resource exists and we just need to create it
            if (getClass().getResource(styleName + ".sld") != null) {
                addStyle(styleName, catalog);
                defaultStyle = catalog.getStyleByName(styleName);
            }
        }
        layer = catalog.getLayerByName(LAYER_NAME);
        layer.setDefaultStyle(defaultStyle);
    }

	
	

}
