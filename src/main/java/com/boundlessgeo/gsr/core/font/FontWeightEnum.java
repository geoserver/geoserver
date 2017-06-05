/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.core.font;

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
