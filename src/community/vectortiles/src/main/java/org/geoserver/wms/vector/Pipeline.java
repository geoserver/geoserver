/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

abstract class Pipeline {

    protected static final Geometry EMPTY = new GeometryFactory().createPoint((Coordinate) null);

    static final Pipeline END = new Pipeline() {

        @Override
        protected final Geometry execute(Geometry geom) {
            return geom;
        }

        @Override
        protected final Geometry _run(Geometry geom) {
            throw new UnsupportedOperationException();
        }

    };

    private Pipeline next = END;

    void setNext(Pipeline step) {
        Preconditions.checkNotNull(next);
        this.next = step;
    }

    Geometry execute(Geometry geom) throws Exception {
        Preconditions.checkNotNull(next, getClass().getName());
        Geometry g = _run(geom);
        if (g == null || g.isEmpty()) {
            return EMPTY;
        }
        return next.execute(g);
    }

    protected abstract Geometry _run(Geometry geom) throws Exception;
}