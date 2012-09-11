/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.font;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public enum FontWeightEnum {

    BOLD("bold"), BOLDER("bolder"), LIGHTER("lighter"), NORMAL("normal");
    private final String weight;

    public String getWeight() {
        return weight;
    }

    private FontWeightEnum(String weight) {
        this.weight = weight;
    }
}
