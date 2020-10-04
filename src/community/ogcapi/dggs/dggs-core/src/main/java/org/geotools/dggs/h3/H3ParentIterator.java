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
package org.geotools.dggs.h3;

import java.util.Iterator;
import org.geotools.dggs.Zone;

/** Iterates through the parents of a given cell */
public class H3ParentIterator implements Iterator<Zone> {
    long id;
    int resolution;
    H3DGGSInstance dggs;

    public H3ParentIterator(long id, H3DGGSInstance dggs) {
        this.id = id;
        this.resolution = dggs.h3.h3GetResolution(id);
        this.dggs = dggs;
    }

    @Override
    public boolean hasNext() {
        return resolution > 0;
    }

    @Override
    public Zone next() {
        long parentId = dggs.h3.h3ToParent(id, resolution);
        this.id = parentId;
        this.resolution--;
        return new H3Zone(dggs, parentId);
    }
}
