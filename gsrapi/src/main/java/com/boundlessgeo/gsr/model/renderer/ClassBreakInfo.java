/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.renderer;

import com.boundlessgeo.gsr.model.symbol.Symbol;

/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public class ClassBreakInfo {

    private Double classMinValue;

    private double classMaxValue;

    private String label;

    private String description;

    private Symbol symbol;

    public Double getClassMinValue() {
        return classMinValue;
    }

    public void setClassMinValue(Double classMinValue) {
        this.classMinValue = classMinValue;
    }

    public double getClassMaxValue() {
        return classMaxValue;
    }

    public void setClassMaxValue(double classMaxValue) {
        this.classMaxValue = classMaxValue;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    public ClassBreakInfo(Double classMinValue, double classMaxValue, String label, String description, Symbol symbol) {
        super();
        this.classMinValue = classMinValue;
        this.classMaxValue = classMaxValue;
        this.label = label;
        this.description = description;
        this.symbol = symbol;
    }

}
