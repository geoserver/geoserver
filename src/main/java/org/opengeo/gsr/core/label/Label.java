/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.label;

import org.opengeo.gsr.core.symbol.TextSymbol;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */
public class Label {

    private String labelExpression;

    private boolean useCodedValues;

    private TextSymbol symbol;

    private int minScale;

    private int maxScale;

    public String getLabelExpression() {
        return labelExpression;
    }

    public void setLabelExpression(String labelExpression) {
        this.labelExpression = labelExpression;
    }

    public boolean isUseCodedValues() {
        return useCodedValues;
    }

    public void setUseCodedValues(boolean useCodedValues) {
        this.useCodedValues = useCodedValues;
    }

    public TextSymbol getSymbol() {
        return symbol;
    }

    public void setSymbol(TextSymbol symbol) {
        this.symbol = symbol;
    }

    public int getMinScale() {
        return minScale;
    }

    public void setMinScale(int minScale) {
        this.minScale = minScale;
    }

    public int getMaxScale() {
        return maxScale;
    }

    public void setMaxScale(int maxScale) {
        this.maxScale = maxScale;
    }

    public Label(String labelExpression, boolean useCodedValues, TextSymbol symbol, int minScale,
            int maxScale) {
        super();
        this.labelExpression = labelExpression;
        this.useCodedValues = useCodedValues;
        this.symbol = symbol;
        this.minScale = minScale;
        this.maxScale = maxScale;
    }

}
