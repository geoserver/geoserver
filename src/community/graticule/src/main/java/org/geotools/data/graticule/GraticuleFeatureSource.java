/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.data.graticule;

import java.awt.RenderingHints;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.NotImplementedException;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.FeatureListener;
import org.geotools.api.data.Query;
import org.geotools.api.data.QueryCapabilities;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.FeatureVisitor;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;

public class GraticuleFeatureSource implements SimpleFeatureSource {
    private final List<Double> steps;

    private final SimpleFeatureType schema;
    private final ReferencedEnvelope bounds;
    private final GraticuleDataStore parent;

    public GraticuleFeatureSource(
            GraticuleDataStore parent,
            List<Double> steps,
            ReferencedEnvelope bounds,
            SimpleFeatureType schema) {

        this.parent = parent;
        this.steps = steps;
        this.bounds = bounds;
        this.schema = schema;
    }

    @Override
    public SimpleFeatureCollection getFeatures() throws IOException {
        Query q = new Query();
        return getFeatures(q);
    }

    /**
     * Retrieves the schema (feature type) that will apply to features retrieved from this {@code
     * FeatureSource}.
     *
     * <p>For a homogeneous data source such as a shapefile or a database table, this schema be that
     * of all features. For a heterogeneous data source, e.g. a GML document, the schema returned is
     * the lowest common denominator across all features.
     *
     * @return the schema that will apply to features retrieved from this {@code FeatureSource}
     */
    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    /**
     * Get the spatial bounds of the feature data. This is equivalent to calling <code>
     * getBounds(Query.ALL)</code>.
     *
     * <p>It is possible that this method will return null if the calculation of bounds is judged to
     * be too costly by the implementing class. In this case, you might call <code>
     * getFeatures().getBounds()</code> instead.
     *
     * @return The bounding envelope of the feature data; or {@code null} if the bounds are unknown
     *     or too costly to calculate.
     * @throws IOException on any errors calculating the bounds
     */
    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        return bounds;
    }

    /**
     * Get the spatial bounds of the features that would be returned by the given {@code Query}.
     *
     * <p>It is possible that this method will return null if the calculation of bounds is judged to
     * be too costly by the implementing class. In this case, you might call <code>
     * getFeatures(query).getBounds()</code> instead.
     *
     * @param query the query to select features
     * @return The bounding envelope of the feature data; or {@code null} if the bounds are unknown
     *     or too costly to calculate.
     * @throws IOException on any errors calculating the bounds
     */
    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return bounds;
    }

    /**
     * Gets the number of the features that would be returned by the given
     * {@code Query}, taking into account any settings for max features and
     * start index set on the {@code Query}.
     * <p>
     * It is possible that this method will return {@code -1} if the calculation
     * of number of features is judged to be too costly by the implementing class.
     * In this case, you might call <code>getFeatures(query).size()</code>
     * instead.
     * <p>
     * Example use:<pre><code> int count = featureSource.getCount();
     * if( count == -1 ){
     *    count = featureSource.getFeatures( "typeName", count ).size();
     * }
     *
     * @param query the query to select features
     *
     * @return the numer of features that would be returned by the {@code Query};
     *         or {@code -1} if this cannot be calculated.
     *
     * @throws IOException if there are errors getting the count
     */
    @Override
    public int getCount(Query query) throws IOException {
        return -1;
    }

    /**
     * Returns the set of hints that this {@code FeatureSource} supports via {@code Query} requests.
     *
     * <p>Note: the existence of a specific hint does not guarantee that it will always be honored
     * by the implementing class.
     *
     * @return a set of {@code RenderingHints#Key} objects; may be empty but never {@code null}
     * @see Hints#FEATURE_DETACHED
     * @see Hints#JTS_GEOMETRY_FACTORY
     * @see Hints#JTS_COORDINATE_SEQUENCE_FACTORY
     * @see Hints#JTS_PRECISION_MODEL
     * @see Hints#JTS_SRID
     * @see Hints#GEOMETRY_DISTANCE
     * @see Hints#FEATURE_2D
     */
    @Override
    public Set<RenderingHints.Key> getSupportedHints() {
        return new HashSet<RenderingHints.Key>();
    }

    /**
     * Returns the name of the features (strictly, the name of the {@code AttributeDescriptor} for
     * the features) accessible through this {@code FeatureSource}.
     *
     * <p>The value returned by this method can be different to that returned by {@code
     * featureSource.getSchema().getType().getName()}. This is because there is a distinction
     * between the name applied to features and the name of a feature type. When working with {@code
     * SimpleFeature} and {@code SimpleFeatureType}, for example with a shapefile data source, it is
     * common practice for feature and feature type names to be the same. However, this is not the
     * case more generally. For instance, a database can contain two tables with the same structure.
     * The feature name will refer to the table while the feature type name refers to the schema
     * (table structure).
     *
     * @return the name of the features accessible through this {@code FeatureSource}
     * @since 2.5
     */
    @Override
    public Name getName() {
        return schema.getName();
    }

    /**
     * Returns information describing this {@code FeatureSource} which may include title,
     * description and spatial parameters. Note that in the returned {@code ResourceInfo} object,
     * the distinction between feature name and schema (feature type) name applies as discussed for
     * {@linkplain #getName()}.
     */
    @Override
    public ResourceInfo getInfo() {
        return new ResourceInfo() {
            final Set<String> words = new HashSet<>();

            {
                words.add("features");
                words.add(GraticuleFeatureSource.this.getSchema().getTypeName());
            }

            @Override
            public ReferencedEnvelope getBounds() {
                try {
                    return GraticuleFeatureSource.this.getBounds();
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            public CoordinateReferenceSystem getCRS() {
                return GraticuleFeatureSource.this.getSchema().getCoordinateReferenceSystem();
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public Set<String> getKeywords() {
                return words;
            }

            @Override
            public String getName() {
                return GraticuleFeatureSource.this.getSchema().getTypeName();
            }

            @Override
            public URI getSchema() {
                Name name = GraticuleFeatureSource.this.getSchema().getName();
                URI namespace;
                try {
                    namespace = new URI(name.getNamespaceURI());
                    return namespace;
                } catch (URISyntaxException e) {
                    return null;
                }
            }

            @Override
            public String getTitle() {
                Name name = GraticuleFeatureSource.this.getSchema().getName();
                return name.getLocalPart();
            }
        };
    }

    /**
     * Returns the data source, as a {@code DataAccess} object, providing this {@code
     * FeatureSource}.
     *
     * @return the data source providing this {@code FeatureSource}
     */
    @Override
    public DataAccess<SimpleFeatureType, SimpleFeature> getDataStore() {
        return null;
    }

    /**
     * Enquire what what query capabilities this {@code FeatureSource} natively supports. For
     * example, whether queries can return sorted results.
     *
     * @return the native query capabilities of this {@code FeatureSource}
     * @since 2.5
     */
    @Override
    public QueryCapabilities getQueryCapabilities() {
        return null;
    }

    /**
     * Registers a listening object that will be notified of changes to this {@code FeatureSource}.
     *
     * @param listener the new listener
     */
    @Override
    public void addFeatureListener(FeatureListener listener) {}

    /**
     * Removes an object from this {@code FeatureSource's} listeners.
     *
     * @param listener the listener to remove
     */
    @Override
    public void removeFeatureListener(FeatureListener listener) {}

    @Override
    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        Query q = new Query();
        q.setFilter(filter);
        return this.getFeatures(q);
    }

    @Override
    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
        Transaction transaction = null;
        GraticuleFeatureReader reader = new GraticuleFeatureReader(parent, query);
        return new SimpleFeatureCollection() {

            /**
             * Obtain a SimpleFeatureIterator of the Features within this SimpleFeatureCollection.
             *
             * <p>The implementation of FeatureIterator must adhere to the rules of fail-fast
             * concurrent modification. In addition (to allow for resource backed collections) the
             * <code>
             * SimpleFeatureIterator.close()</code> method must be called.
             *
             * <p>Example use:
             *
             * <pre><code>
             * SimpleFeatureIterator iterator=collection.features();
             * try {
             *     while( iterator.hasNext()  ){
             *          SimpleFeature feature = iterator.next();
             *          System.out.println( feature.getID() );
             *     }
             * }
             * finally {
             *     iterator.close();
             * }
             * </code></pre>
             */
            @Override
            public SimpleFeatureIterator features() {
                return reader.getDelegate();
            }

            /**
             * The schema for the child feature members of this collection.
             *
             * <p>Represents the most general FeatureType in common to all the features in this
             * collection.
             *
             * <ul>
             *   <li>For a collection backed by a shapefiles (or database tables) the FeatureType
             *       returned by getSchema() will complete describe each and every child in the
             *       collection.
             *   <li>For mixed content FeatureCollections you will need to check the FeatureType of
             *       each Feature as it is retrived from the collection
             *   <li>The degenerate case returns the "_Feature" FeatureType, where the only thing
             *       known is that the contents are Features.
             * </ul>
             *
             * @return FeatureType describing the "common" schema to all child features of this
             *     collection
             */
            @Override
            public SimpleFeatureType getSchema() {
                return reader.schema;
            }

            /** ID used when serializing to GML */
            @Override
            public String getID() {
                return "id";
            }

            /**
             * Visit the contents of a feature collection.
             *
             * <p>The order of traversal is dependent on the FeatureCollection implementation; some
             * collections are able to make efficient use of an internal index in order to quickly
             * visit features located in the same region.
             *
             * @param visitor Closure applied to each feature in turn.
             * @param progress Used to report progress, may be used to interrupt the operation
             * @since 2.5
             */
            @Override
            public void accepts(FeatureVisitor visitor, ProgressListener progress)
                    throws IOException {
                DataUtilities.visit(this, visitor, progress);
            }

            @Override
            public SimpleFeatureCollection subCollection(Filter filter) {
                throw new NotImplementedException();
            }

            @Override
            public SimpleFeatureCollection sort(SortBy order) {
                throw new NotImplementedException();
            }

            /**
             * Get the total bounds of this collection which is calculated by doing a union of the
             * bounds of each feature inside of it
             *
             * @return An Envelope containing the total bounds of this collection.
             */
            @Override
            public ReferencedEnvelope getBounds() {
                return bounds;
            }

            /**
             * @param o
             * @see Collection#contains(Object)
             */
            @Override
            public boolean contains(Object o) {
                return false;
            }

            /**
             * @param o
             * @see Collection#containsAll(Collection)
             */
            @Override
            public boolean containsAll(Collection<?> o) {
                return false;
            }

            /**
             * Returns <tt>true</tt> if this feature collection contains no features.
             *
             * @return <tt>true</tt> if this collection contains no features
             */
            @Override
            public boolean isEmpty() {
                return false;
            }

            /**
             * Please note this operation may be expensive when working with remote content.
             *
             * @see Collection#size()
             */
            @Override
            public int size() {
                return -1;
            }

            /** @see Collection#toArray() */
            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            /**
             * @param a
             * @see Collection#toArray(Object[])
             */
            @Override
            public <O> O[] toArray(O[] a) {
                return null;
            }
        };
    }
}
