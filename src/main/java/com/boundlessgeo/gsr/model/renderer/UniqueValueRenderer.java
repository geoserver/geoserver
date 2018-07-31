/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.renderer;

import java.util.List;

import com.boundlessgeo.gsr.model.symbol.Symbol;

/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public class UniqueValueRenderer extends Renderer {

    private String field1;

    private String field2;

    private String field3;

    private String fieldDelimiter;

    private Symbol defaultSymbol;

    private String defaultLabel;

    private List<UniqueValueInfo> uniqueValueInfos;

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    public String getField3() {
        return field3;
    }

    public void setField3(String field3) {
        this.field3 = field3;
    }

    public String getFieldDelimiter() {
        return fieldDelimiter;
    }

    public void setFieldDelimiter(String fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
    }

    public Symbol getDefaultSymbol() {
        return defaultSymbol;
    }

    public void setDefaultSymbol(Symbol defaultSymbol) {
        this.defaultSymbol = defaultSymbol;
    }

    public String getDefaultLabel() {
        return defaultLabel;
    }

    public void setDefaultLabel(String defaultLabel) {
        this.defaultLabel = defaultLabel;
    }

    public List<UniqueValueInfo> getUniqueValueInfos() {
        return uniqueValueInfos;
    }

    public void setUniqueValueInfos(List<UniqueValueInfo> uniqueValueInfos) {
        this.uniqueValueInfos = uniqueValueInfos;
    }

    public UniqueValueRenderer(String field1, String field2, String field3, String fieldDelimiter,
            Symbol defaultSymbol, String defaultLabel, List<UniqueValueInfo> uniqueValueInfos) {
        super("uniqueValue");
        this.field1 = field1;
        this.field2 = field2;
        this.field3 = field3;
        this.fieldDelimiter = fieldDelimiter;
        this.defaultSymbol = defaultSymbol;
        this.defaultLabel = defaultLabel;
        this.uniqueValueInfos = uniqueValueInfos;
    }
}
