/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.gwc;

import static com.google.common.base.Optional.fromNullable;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevTree;
import org.locationtech.geogig.plumbing.ResolveTreeish;
import org.locationtech.geogig.plumbing.diff.PathFilteringDiffConsumer;
import org.locationtech.geogig.plumbing.diff.PreOrderDiffWalk;
import org.locationtech.geogig.plumbing.diff.PreOrderDiffWalk.Consumer;
import org.locationtech.geogig.repository.AbstractGeoGigOp;
import org.locationtech.geogig.storage.ObjectDatabase;
import org.locationtech.jts.geom.Geometry;

/**
 * An operation that computes the "approximate minimal bounds" difference between two {@link RevTree
 * trees}.
 *
 * <p>The "approximate minimal bounds" is defined as the geometry union of the bounds of each
 * individual difference, with the exception that when a tree node or bucket tree does not exist at
 * either side of the comparison, the traversal of the existing tree is skipped and its whole bounds
 * are used instead of adding up the bounds of each individual feature.
 *
 * <p>One depth level filtering by tree name is supported through {@link #setTreeNameFilter(String)}
 * in order to skip root node's children sibling of the tree of interest.
 *
 * <p>The tree-ish at the left side of the comparison is set through {@link #setOldVersion(String)},
 * and defaults to {@link Ref#HEAD} if not set.
 *
 * <p>The tree-ish at the right side of the comparison is set through {@link
 * #setNewVersion(String)}, and defaults to {@link Ref#WORK_HEAD} if not set.
 */
public class MinimalDiffBounds extends AbstractGeoGigOp<Geometry> {

    private String oldVersion;

    private String newVersion;

    private String treeName;

    public MinimalDiffBounds setOldVersion(String oldTreeish) {
        this.oldVersion = oldTreeish;
        return this;
    }

    public MinimalDiffBounds setNewVersion(String newTreeish) {
        this.newVersion = newTreeish;
        return this;
    }

    public MinimalDiffBounds setTreeNameFilter(String treeName) {
        this.treeName = treeName;
        return this;
    }

    @Override
    protected Geometry _call() {
        final String leftRefSpec = fromNullable(oldVersion).or(Ref.HEAD);
        final String rightRefSpec = fromNullable(newVersion).or(Ref.WORK_HEAD);

        RevTree left = resolveTree(leftRefSpec);
        RevTree right = resolveTree(rightRefSpec);

        ObjectDatabase leftSource = objectDatabase();
        ObjectDatabase rightSource = objectDatabase();

        PreOrderDiffWalk visitor = new PreOrderDiffWalk(left, right, leftSource, rightSource);
        MinimalDiffBoundsConsumer boundsBuilder = new MinimalDiffBoundsConsumer();
        Consumer consumer = boundsBuilder;
        if (treeName != null) {
            consumer = new PathFilteringDiffConsumer(ImmutableList.of(treeName), boundsBuilder);
        }
        visitor.walk(consumer);
        Geometry minimalBounds = boundsBuilder.buildGeometry();
        return minimalBounds;
    }

    private RevTree resolveTree(String refSpec) {

        Optional<ObjectId> id = command(ResolveTreeish.class).setTreeish(refSpec).call();
        Preconditions.checkState(id.isPresent(), "%s did not resolve to a tree", refSpec);

        return objectDatabase().getTree(id.get());
    }
}
