/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response.dxf;
/**
 * LineType pattern item. Describes an homogeneus part of a line type pattern, composed of a type
 * (DASH, DOT or EMPTY) and a length expressed in terms of base length.
 *
 * @author Mauro Bartolomeoli, mbarto@infosia.it
 */
public class LineTypeItem {
    // type DASH (solid line)
    public static final int DASH = 0;
    // type DOT (point)
    public static final int DOT = 1;
    // type EMPTY (whitespace)
    public static final int EMPTY = 2;

    int type = DASH;

    double length = 0.0;

    public LineTypeItem(int type, double length) {
        super();
        this.type = type;
        this.length = length;
    }

    public LineTypeItem(int type) {
        super();
        this.type = type;
    }

    /** Gets type of item (DASH, DOT, EMPTY). */
    public int getType() {
        return type;
    }

    /** Sets type of item (DASH, DOT, EMPTY). */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Gets length of item. - 0 if it's a DOT - positive if it's a DASH - negative if it's a EMPTY
     */
    public double getLength() {
        switch (type) {
            case DASH:
                return length;
            case EMPTY:
                return -length;
            default:
                return 0.0;
        }
    }

    /** Sets the length of the item (in terms of base length repetitions) */
    public void setLength(double length) {
        this.length = length;
    }
}
