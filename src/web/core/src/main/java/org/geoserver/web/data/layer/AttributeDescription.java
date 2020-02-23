/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@SuppressWarnings("serial")
class AttributeDescription implements Serializable {

    static final List<Class<?>> BINDINGS =
            Arrays.asList(
                    String.class,
                    Boolean.class,
                    Integer.class,
                    Long.class,
                    Float.class,
                    Double.class,
                    Date.class,
                    Time.class,
                    Timestamp.class,
                    Geometry.class,
                    Point.class,
                    LineString.class,
                    Polygon.class,
                    MultiPoint.class,
                    MultiLineString.class,
                    MultiPolygon.class,
                    GeometryCollection.class);

    static final CoordinateReferenceSystem WGS84;

    static {
        try {
            WGS84 = CRS.decode("EPSG:4326");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String name;

    Class<?> binding = String.class;

    boolean nullable = true;

    int size = 100;

    CoordinateReferenceSystem crs = WGS84;

    /** Returns the localized named of the attribute type */
    static String getLocalizedName(Class<?> binding) {
        if (binding == null) {
            return "-";
        } else if (BINDINGS.contains(binding)) {
            return new ParamResourceModel("AttributeType." + binding.getSimpleName(), null)
                    .getString();
        } else {
            return binding.getSimpleName();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<?> getBinding() {
        return binding;
    }

    public void setBinding(Class<?> binding) {
        this.binding = binding;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    public void setCrs(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }
}
