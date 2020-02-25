/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.gwc;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.jdt.annotation.Nullable;
import org.geotools.geometry.jts.JTS;
import org.locationtech.geogig.model.Bounded;
import org.locationtech.geogig.model.Bucket;
import org.locationtech.geogig.model.NodeRef;
import org.locationtech.geogig.plumbing.diff.PreOrderDiffWalk;
import org.locationtech.geogig.plumbing.diff.PreOrderDiffWalk.BucketIndex;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

class MinimalDiffBoundsConsumer implements PreOrderDiffWalk.Consumer {

    private static final GeometryFactory GEOM_FACTORY = CompactMultiPoint.GEOM_FACTORY;

    /** Accumulates punctual differences to save heap */
    private CompactMultiPoint points = new CompactMultiPoint();

    /** Accumulates non punctual differences (i.e. bounding polygons) */
    private List<Geometry> nonPoints = new LinkedList<>();

    private final Lock lock = new ReentrantLock();

    /**
     * @return a single geometry product of unioning all the bounding boxes acquired while
     *     traversing the diff
     */
    public Geometry buildGeometry() {
        List<Geometry> geomList = nonPoints;
        nonPoints = null;
        if (!points.isEmpty()) {
            geomList.add(points);
        }
        points = null;

        Geometry buildGeometry = GEOM_FACTORY.buildGeometry(geomList);
        geomList.clear();
        Geometry union = buildGeometry.union();
        return union;
    }

    @Override
    public boolean feature(@Nullable final NodeRef left, @Nullable final NodeRef right) {
        addEnv(left);
        addEnv(right);
        return true;
    }

    @Override
    public boolean tree(@Nullable final NodeRef left, @Nullable final NodeRef right) {
        if (left == null) {
            addEnv(right);
            return false;
        } else if (right == null) {
            addEnv(left);
            return false;
        }
        return true;
    }

    @Override
    public boolean bucket(
            final NodeRef leftParent,
            final NodeRef rightParent,
            final BucketIndex bucketIndex,
            @Nullable final Bucket left,
            @Nullable final Bucket right) {
        if (left == null) {
            addEnv(right);
            return false;
        } else if (right == null) {
            addEnv(left);
            return false;
        }
        return true;
    }

    private void addEnv(@Nullable Bounded node) {
        if (node == null) {
            return;
        }
        final Envelope env = node.bounds().orNull();
        if (env == null || env.isNull()) {
            return;
        }
        if (isPoint(env)) {
            lock.lock();
            try {
                points.add(env.getMinX(), env.getMinY());
            } finally {
                lock.unlock();
            }
            return;
        }
        Geometry geom;
        if (isOrthoLine(env)) {
            // handle the case where the envelope is given by an orthogonal line so we don't add a
            // zero area polygon
            double width = env.getWidth();
            GrowableCoordinateSequence cs = new GrowableCoordinateSequence();
            if (width == 0D) {
                cs.add(env.getMinX(), env.getMinY());
                cs.add(env.getMinX(), env.getMaxY());
            } else {
                cs.add(env.getMinX(), env.getMinY());
                cs.add(env.getMaxX(), env.getMinY());
            }
            geom = GEOM_FACTORY.createLineString(cs);
        } else {
            geom = JTS.toGeometry(env, GEOM_FACTORY);
        }
        lock.lock();
        try {
            nonPoints.add(geom);
        } finally {
            lock.unlock();
        }
    }

    private boolean isOrthoLine(Envelope env) {
        return env.getArea() == 0D && env.getWidth() > 0D || env.getHeight() > 0D;
    }

    private boolean isPoint(Envelope env) {
        return env.getWidth() == 0D && env.getHeight() == 0D;
    }

    @Override
    public void endTree(@Nullable final NodeRef left, @Nullable final NodeRef right) {
        // nothing to do, intentionally blank
    }

    @Override
    public void endBucket(
            NodeRef leftParent,
            NodeRef rightParent,
            final BucketIndex bucketIndex,
            @Nullable final Bucket left,
            @Nullable final Bucket right) {
        // nothing to do, intentionally blank
    }
}
