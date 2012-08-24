/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.InputStream;

/**
 * Represents a CSW repository item, allows to properly respond to a GetRepositoryItem request
 * 
 * @author Andrea Aime - GeoSolutions
 */
public interface RepositoryItem {

    /**
     * The MIME type describing the repository item contents
     * 
     * @return
     */
    public String getMime();
    
    
    /**
     * The repository item contents
     */
    public InputStream getContents();
}
