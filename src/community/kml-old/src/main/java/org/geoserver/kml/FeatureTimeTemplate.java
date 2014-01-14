/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.io.IOException;

import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.opengis.feature.simple.SimpleFeature;


/**
 * Template which supports timestamps and timespans for features.
 * <p>
 * 
 * </p>
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class FeatureTimeTemplate {

    FeatureTemplate delegate;
    
    public FeatureTimeTemplate() {
        this( new FeatureTemplate() );
    }
    
    public FeatureTimeTemplate( FeatureTemplate delegate ) {
        this.delegate = delegate;
    }
    
    /**
     * Executes the template against the feature.
     * <p>
     * This method returns:
     * <ul>
     *  <li><code>{"01/01/07"}</code>: timestamp as 1 element array
     *  <li><code>{"01/01/07","01/12/07"}</code>: timespan as 2 element array
     *  <li><code>{null,"01/12/07"}</code>: open ended (start) timespan as 2 element array
     *  <li><code>{"01/12/07",null}</code>: open ended (end) timespan as 2 element array 
     *  <li><code>{}</code>: no timestamp information as empty array
     * </ul>
     * </p>
     * @param feature The feature to execute against.
     */
    public String[] execute(SimpleFeature feature) throws IOException {
        String output = delegate.template(feature, "time.ftl", FeatureTemplate.class);
    
        if ( output != null ) {
            output = output.trim();
        }
        
        //case of nothing specified
        if ( output == null || "".equals( output ) ) {
            return new String[]{};
        }
        
        //JD: split() returns a single value when the delimiter is at the 
        // end... but two when at the start do another check
        String[] timespan = output.split("\\|\\|");
        if ( output.endsWith("||") ) {
            timespan = new String[]{ timespan[0], null };
        }
                
        if ( timespan.length > 2 ) {
            String msg = "Incorrect time syntax. Should be: <date>||<date>";
            throw new IllegalArgumentException( msg );
        }
        
        //case of just a timestamp
        if ( timespan.length == 1 ) {
            return timespan;    
        }
        
        //case of open ended timespan
        if ( timespan[0] == null || "".equals( timespan[0].trim() ) ) {
            timespan[0] = null;
        }
        if ( timespan[1] == null || "".equals( timespan[1].trim() ) ) {
            timespan[1] = null;
        }
        
        return timespan;
    }
}
