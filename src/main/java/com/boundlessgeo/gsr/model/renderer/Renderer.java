/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.renderer;

/**
 *
 * See https://developers.arcgis.com/documentation/common-data-types/renderer-objects.htm
 *
 * @author Juan Marin, OpenGeo
 */
public abstract class Renderer {

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Renderer(String type) {
        super();
        this.type = type;
    }
}
