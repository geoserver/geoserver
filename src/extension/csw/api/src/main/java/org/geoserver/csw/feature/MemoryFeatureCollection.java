/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.geoserver.csw.feature.sort.ComplexComparatorFactory;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * A fully in memory feature collection
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MemoryFeatureCollection extends AbstractFeatureCollection<FeatureType, Feature> {

    protected ArrayList<Feature> features;

    public MemoryFeatureCollection(FeatureType memberType) {
        this(memberType, null);
    }

    public MemoryFeatureCollection(FeatureType memberType, List<Feature> features) {
        super(memberType);
        this.features = new ArrayList<Feature>();
        if (features != null) {
            for (Feature f : features) {
                if (!f.getType().equals(memberType)) {
                    // TODO: handle inheritance
                    throw new IllegalArgumentException(
                            "Found a feature whose feature type is not equal to the declared one: "
                                    + f);
                }
                this.features.add(f);
            }
        }
    }

    @Override
    public FeatureCollection<FeatureType, Feature> subCollection(Filter filter) {
        List<Feature> results = new ArrayList<Feature>();
        for (Feature f : features) {
            if (filter.evaluate(f)) {
                results.add(f);
            }
        }

        return new MemoryFeatureCollection(getSchema(), results);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> sort(SortBy order) {
        List<Feature> results = new ArrayList<Feature>(features);
        Comparator<Feature> comparator = ComplexComparatorFactory.buildComparator(order);
        Collections.sort(results, comparator);

        return new MemoryFeatureCollection(getSchema(), results);
    }

    @Override
    protected Iterator<Feature> openIterator() {
        return features.iterator();
    }

    @Override
    protected void closeIterator(Iterator<Feature> close) {
        // nothing to do
    }

    @Override
    public int size() {
        return features.size();
    }

    /**
     * Removes a single instance of the specified element from this collection, if it is present
     * (optional operation).
     *
     * @param o element to be removed from this collection, if present.
     * @return <tt>true</tt> if the collection contained the specified element.
     * @throws UnsupportedOperationException if the <tt>remove</tt> method is not supported by this
     *     collection.
     */
    public boolean remove(Object o) {
        return features.remove(o);
    }

    /**
     * Removes from this collection all of its elements that are contained in the specified
     * collection (optional operation).
     *
     * <p>
     *
     * @param c elements to be removed from this collection.
     * @return <tt>true</tt> if this collection changed as a result of the call.
     * @throws UnsupportedOperationException if the <tt>removeAll</tt> method is not supported by
     *     this collection.
     * @throws NullPointerException if the specified collection is null.
     * @see #remove(Object)
     * @see #contains(Object)
     */
    public final boolean removeAll(Collection<?> c) {
        return features.removeAll(c);
    }

    /**
     * Retains only the elements in this collection that are contained in the specified collection
     * (optional operation).
     *
     * @param c elements to be retained in this collection.
     * @return <tt>true</tt> if this collection changed as a result of the call.
     * @throws UnsupportedOperationException if the <tt>retainAll</tt> method is not supported by
     *     this Collection.
     * @throws NullPointerException if the specified collection is null.
     * @see #remove(Object)
     * @see #contains(Object)
     */
    public final boolean retainAll(Collection<?> c) {
        return features.removeAll(c);
    }

    /**
     * Implement to support modification.
     *
     * @param o element whose presence in this collection is to be ensured.
     * @return <tt>true</tt> if the collection changed as a result of the call.
     * @throws UnsupportedOperationException if the <tt>add</tt> method is not supported by this
     *     collection.
     * @throws NullPointerException if this collection does not permit <tt>null</tt> elements, and
     *     the specified element is <tt>null</tt>.
     * @throws ClassCastException if the class of the specified element prevents it from being added
     *     to this collection.
     * @throws IllegalArgumentException if some aspect of this element prevents it from being added
     *     to this collection.
     */
    public boolean add(Feature o) {
        return features.add(o);
    }

    /**
     * Adds all of the elements in the specified collection to this collection (optional operation).
     *
     * @param c collection whose elements are to be added to this collection.
     * @return <tt>true</tt> if this collection changed as a result of the call.
     * @throws UnsupportedOperationException if this collection does not support the <tt>addAll</tt>
     *     method.
     * @throws NullPointerException if the specified collection is null.
     * @see #add(Feature)
     */
    public boolean addAll(Collection<Feature> c) {
        return features.addAll(c);
    }

    public boolean addAll(FeatureCollection<FeatureType, Feature> c) {
        Feature[] array = (Feature[]) c.toArray(new Feature[c.size()]);
        return features.addAll(Arrays.asList(array));
    }
}
