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

import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.expression.Function;

/** Duplicating filter visitor injecting a DGGS instance in well known DGGS related functions */
public class DGGSFilterVisitor extends DuplicatingFilterVisitor {

    DGGSInstance dggs;

    public DGGSFilterVisitor(DGGSInstance dggs) {
        this.dggs = dggs;
    }

    @Override
    public Object visit(Function expression, Object extraData) {
        Function clone = (Function) super.visit(expression, extraData);

        // extend with DGGS parameter if needed
        if (clone instanceof DGGSFunction) {
            ((DGGSFunction) clone).setDGGSInstance(dggs);
        }

        return clone;
    }
}
