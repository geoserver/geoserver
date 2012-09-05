/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.domain;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class Domain {

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Domain(String type) {
        this.type = type;
    }
}
