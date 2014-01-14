/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.IOException;

import org.geotools.data.FeatureReader;


/**
 * Base class for vector based formats.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class VectorFormat extends DataFormat {

    /**
     * Reads features from the data for the specified import item. 
     */
    public abstract FeatureReader read(ImportData data, ImportTask item) throws IOException;

    /**
     * Disposes the reader for the specified import item.  
     */
    public abstract void dispose(FeatureReader reader, ImportTask item) throws IOException;
    
    /**
     * Get the number of features from the data for the specified import item.
     */
    public abstract int getFeatureCount(ImportData data, ImportTask item) throws IOException;

}
