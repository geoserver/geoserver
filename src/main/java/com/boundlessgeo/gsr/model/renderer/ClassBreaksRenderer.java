/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.renderer;

import java.util.List;

/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public class ClassBreaksRenderer extends Renderer {

    private String field;

    private double minValue;

    private List<ClassBreakInfo> classBreakInfos;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public List<ClassBreakInfo> getClassBreakInfos() {
        return classBreakInfos;
    }

    public void setClassBreakInfos(List<ClassBreakInfo> classBreakInfos) {
        this.classBreakInfos = classBreakInfos;
    }

    public ClassBreaksRenderer(String field, double minValue, List<ClassBreakInfo> classBreakInfos) {
        super("classBreaks");
        this.field = field;
        this.minValue = minValue;
        this.classBreakInfos = classBreakInfos;
    }

}
