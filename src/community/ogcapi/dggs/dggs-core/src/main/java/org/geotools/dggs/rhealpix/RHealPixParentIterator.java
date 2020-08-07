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
package org.geotools.dggs.rhealpix;

import java.util.Iterator;
import org.geotools.dggs.Zone;

/** Iterates through the parents of a given cell */
public class RHealPixParentIterator implements Iterator<Zone> {
    String id;
    RHealPixDGGSInstance dggs;

    public RHealPixParentIterator(String id, RHealPixDGGSInstance dggs) {
        this.id = id;
        this.dggs = dggs;
    }

    @Override
    public boolean hasNext() {
        return id.length() > 1;
    }

    @Override
    public Zone next() {
        String parentId = id.substring(0, id.length() - 1);
        this.id = parentId;
        return new RHealPixZone(dggs, parentId);
    }
}
