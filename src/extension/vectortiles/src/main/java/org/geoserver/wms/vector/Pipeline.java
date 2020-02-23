/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import com.google.common.base.Preconditions;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/** A chainable unary operation on a geometry. */
public abstract class Pipeline {

    protected static final Geometry EMPTY = new GeometryFactory().createPoint((Coordinate) null);

    /** Pipeline terminator which returns the geometry without change. */
    static final Pipeline END =
            new Pipeline() {

                @Override
                public final Geometry execute(Geometry geom) {
                    return geom;
                }

                @Override
                protected final Geometry _run(Geometry geom) {
                    throw new UnsupportedOperationException();
                }
            };

    private Pipeline next = END;

    /** Set the next operation in the pipeline */
    void setNext(Pipeline step) {
        Preconditions.checkNotNull(next);
        this.next = step;
    }

    /** Execute pipeline including all downstream pipelines. */
    public Geometry execute(Geometry geom) throws Exception {
        Preconditions.checkNotNull(next, getClass().getName());
        Geometry g = _run(geom);
        if (g == null || g.isEmpty()) {
            return EMPTY;
        }
        return next.execute(g);
    }

    /** Implementation of the pipeline. A unary operation on a geometry. */
    protected abstract Geometry _run(Geometry geom) throws Exception;
}
