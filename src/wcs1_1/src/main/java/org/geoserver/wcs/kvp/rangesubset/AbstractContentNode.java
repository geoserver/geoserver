/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp.rangesubset;

/**
 * Abstract node that add the concept of a node content 
 * @author Andrea Aime - TOPP
 *
 */
public abstract class AbstractContentNode implements Node {
    protected String content;

    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }

}
