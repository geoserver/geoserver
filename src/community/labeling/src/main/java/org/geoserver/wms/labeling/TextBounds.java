/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import java.io.Serializable;

/** Holds relevant data for the computed bounds of text inside the attributes globe. */
class TextBounds implements Serializable {

    private static final long serialVersionUID = 1L;

    private final double width;
    private final double height;
    private final double titleWidth;
    private final double valueWidth;

    public TextBounds(double titleWidth, double valueWidth, double height) {
        super();
        this.titleWidth = titleWidth;
        this.valueWidth = valueWidth;
        this.width = titleWidth + valueWidth;
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getTitleWidth() {
        return titleWidth;
    }

    public double getValueWidth() {
        return valueWidth;
    }

    @Override
    public String toString() {
        return "TextBounds [width="
                + width
                + ", height="
                + height
                + ", titleWidth="
                + titleWidth
                + ", valueWidth="
                + valueWidth
                + "]";
    }
}
