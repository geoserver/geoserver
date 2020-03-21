/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.domain;


/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public class RangeDomain extends Domain {

    private String name;

    private int[] range;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int[] getRange() {
        return range;
    }

    public void setRange(int[] range) {
        this.range = range;
    }

    public RangeDomain(String name, int[] range) {
        super("range");
        this.name = name;
        this.range = range;
    }
}
