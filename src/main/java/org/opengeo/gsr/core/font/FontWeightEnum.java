/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
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
