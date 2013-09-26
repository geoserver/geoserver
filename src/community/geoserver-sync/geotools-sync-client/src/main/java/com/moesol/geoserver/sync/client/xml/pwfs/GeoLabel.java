/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.client.xml.pwfs;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * A Polexis GeoLabel object. Derived from WFS-polexis.xsd (q.v.), basically a point
 * (first &lt;choice&gt; particle) with a few label-specific attributes. Don't know
 * why they didn't extend point, but oh well.
 * @author parkerd
 *
 */
public class GeoLabel extends Point {
    public static enum XAnchorType {
        CENTER, LEFT, RIGHT
    }
    
    public static enum YAnchorType {
        BASELINE, BOTTOM, MIDDLE, TOP
    }
    
    private String text;
    private double rotation;
    private XAnchorType xAnchor = XAnchorType.CENTER;
    private YAnchorType yAnchor = YAnchorType.BOTTOM;
    
    public GeoLabel(CoordinateSequence coordinates, GeometryFactory factory) {
        super(coordinates, factory);
    }

    /**
     * @deprecated
     */
    public GeoLabel(Coordinate coordinate, PrecisionModel precisionModel, int SRID) {
        super(coordinate, precisionModel, SRID);
    }

    public String getText() {
        return text;
    }

    void setText(String text) {
        this.text = text;
    }

    public double getRotation() {
        return rotation;
    }

    void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public XAnchorType getxAnchor() {
        return xAnchor;
    }

    void setxAnchor(XAnchorType xAnchor) {
        this.xAnchor = xAnchor;
    }

    public YAnchorType getyAnchor() {
        return yAnchor;
    }

    void setyAnchor(YAnchorType yAnchor) {
        this.yAnchor = yAnchor;
    }
}
