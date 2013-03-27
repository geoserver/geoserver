package org.geoserver.data.test;

import java.io.IOException;
import java.net.URL;


/**
 * This class defines the required data to test the capabilities' ScaleHint property.
 * <p>
 * The Style contains many rules with different maximum and minimum denominators.   
 * </p>
 * 
 * @author Mauricio Pazos
 */
public class ScaleHintData extends MockData {

    
	public ScaleHintData() throws IOException {
		super();

        URL style = MockData.class.getResource("ScaleHintData.sld");
        addStyle("ScaleHintData", style);
	}
	
	
	

}
