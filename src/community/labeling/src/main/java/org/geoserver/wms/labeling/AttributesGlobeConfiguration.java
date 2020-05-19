/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import java.io.Serializable;
import org.geoserver.wms.labeling.GlobeRender.TailDimensions;

/** Configuration class for attributes labeling globe. */
class AttributesGlobeConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    private int roundCornerRadius = 10;
    private int maxValueChars = 25;
    private int interLineSpace = 4;
    private int margin = 10;
    private int tailHeight = 12;
    private int tailWidth = 12;

    public AttributesGlobeConfiguration() {}

    public int getRoundCornerRadius() {
        return roundCornerRadius;
    }

    public void setRoundCornerRadius(int roundCornerRadius) {
        this.roundCornerRadius = roundCornerRadius;
    }

    public int getMaxValueChars() {
        return maxValueChars;
    }

    public void setMaxValueChars(int maxValueChars) {
        this.maxValueChars = maxValueChars;
    }

    public int getInterLineSpace() {
        return interLineSpace;
    }

    public void setInterLineSpace(int interLineSpace) {
        this.interLineSpace = interLineSpace;
    }

    public int getMargin() {
        return margin;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    public int getTailHeight() {
        return tailHeight;
    }

    public void setTailHeight(int tailHeight) {
        this.tailHeight = tailHeight;
    }

    public int getTailWidth() {
        return tailWidth;
    }

    public void setTailWidth(int tailWidth) {
        this.tailWidth = tailWidth;
    }

    public TailDimensions getTailDimensions() {
        return new TailDimensions(tailWidth, tailHeight);
    }

    @Override
    public String toString() {
        return "AttributesGlobeConfiguration [roundCornerRadius="
                + roundCornerRadius
                + ", maxValueChars="
                + maxValueChars
                + ", interLineSpace="
                + interLineSpace
                + ", margin="
                + margin
                + ", tailHeight="
                + tailHeight
                + ", tailWidth="
                + tailWidth
                + "]";
    }
}
