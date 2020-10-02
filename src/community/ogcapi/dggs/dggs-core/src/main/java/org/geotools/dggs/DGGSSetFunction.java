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

import java.util.Iterator;

/**
 * Implemented by functions matching a fixed set of zones, when invoked using static reference
 * parameters (reference zone id, distances, resolutions or levels).
 *
 * <p>A DGGSFunction can be used in an expression ``function(....) = true`` to * express a well
 * known filter against DGGSs, which stores can then optimize out for a more * efficient execution
 * plan.
 */
public interface DGGSSetFunction extends DGGSFunction {

    public Iterator<Zone> getMatchedZones();

    public long countMatched();

    /**
     * Returns true if the set of matched zones is stable, that is, won't change from one invocation
     * to the next
     */
    public boolean isStable();
}
