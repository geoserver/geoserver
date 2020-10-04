/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs;

import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/** A particular DGGS zone */
public interface Zone {

    /**
     * The zone identifier
     *
     * @return
     */
    String getId();

    /**
     * Returns the resoution level of a particular zone
     *
     * @return
     */
    int getResolution();

    /**
     * Returns the zone center
     *
     * @return
     */
    Point getCenter();

    /**
     * Returns the zone boundary, in CRS84 (WGS84 with lon/lat axis order)
     *
     * @return
     */
    Polygon getBoundary();

    /**
     * Returns the value of a given extra property value of a zone. Valid values are described by
     * {@link DGGSInstance#getExtraProperties()}.
     *
     * @return
     */
    public Object getExtraProperty(String name);
}
