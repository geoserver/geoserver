/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;

/**
 * A helper that builds records as GeoTools features
 * 
 * @author Niels Charlier
 */
public interface RecordBuilder {
   
    /**
     * Adds an element to the current record
     * 
     * @param name name of property
     * @param values value(s) to be attached to property
     */
    public void addElement(String name, String... values);
    
    public void addElement(String name, int splitIndex, String... values);

    /**
     * Adds a bounding box to the record. The envelope must be in WGS84
     * 
     * @param env the bounding box
     */
    public void addBoundingBox(ReferencedEnvelope env);

    /**
     * Builds a record and sets up to work on the next one
     * 
     * @param id the feature identifier
     * @return the feature
     */
    public Feature build(String id) ;
    
}
