/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.BaseFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * FeatureCollection implementation wrapping around a java.util.List. Derived from {@link
 * ListFeatureCollection}, but adapted to work with complex features too
 *
 * @see Hints#FEATURE_DETACHED
 * @author Oliver Gottwald
 * @author Jody
 * @author Andrea Aime - GeoSolutions
 * @source $URL$
 */
@SuppressWarnings("unchecked")
public class ListComplexFeatureCollection extends BaseFeatureCollection {

    /** wrapped list of features containing the contents */
    private List<Feature> list;

    /** Cached bounds */
    private ReferencedEnvelope bounds = null;

    /**
     * Create a ListFeatureCollection around the provided list. The contents of the list should all
     * be of the provided schema for this to make sense. Please keep in mind the feature collection
     * control, no two Features in the list should have the same feature id, and you should not
     * insert the same feature more then once.
     *
     * <p>The provided list is directly used for storage, most feature collection operations just
     * use a simple iterator so there is no performance advantaged to be gained over using an
     * ArrayList vs a LinkedList (other then for the size() method of course).
     */
    public ListComplexFeatureCollection(FeatureType schema, List<Feature> list) {
        super(schema);
        this.list = list;
    }

    /** Create a ListFeatureCollection around the provided feature. */
    public ListComplexFeatureCollection(Feature feature) {
        super(feature.getType());
        this.list = new ArrayList<>();
        this.list.add(feature);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public FeatureIterator features() {
        return new ListComplexFeatureIterator(list);
    }

    @Override
    public synchronized ReferencedEnvelope getBounds() {
        if (bounds == null) {
            bounds = calculateBounds();
        }
        return bounds;
    }

    /** Calculate bounds from features */
    private ReferencedEnvelope calculateBounds() {
        ReferencedEnvelope extent = new ReferencedEnvelope();
        for (Feature feature : list) {
            if (feature == null) continue;
            ReferencedEnvelope bbox = ReferencedEnvelope.reference(feature.getBounds());
            if (bbox == null || bbox.isEmpty() || bbox.isNull()) continue;
            extent.expandToInclude(bbox);
        }
        return new ReferencedEnvelope(extent, schema.getCoordinateReferenceSystem());
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * SimpleFeatureIterator that will use collection close method.
     *
     * @author Jody
     */
    private static class ListComplexFeatureIterator implements FeatureIterator {
        private Iterator<? extends Feature> iter;

        public ListComplexFeatureIterator(List<? extends Feature> features) {
            iter = features.iterator();
        }

        @Override
        public void close() {
            if (iter instanceof FeatureIterator) {
                ((FeatureIterator<?>) iter).close();
            }
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public Feature next() throws NoSuchElementException {
            return iter.next();
        }
    }
}
