package org.geotools.data.graticule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.LockingManager;
import org.geotools.api.data.Query;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.CollectionFeatureReader;
import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.graticule.gridsupport.LineFeatureBuilder;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Lines;
import org.geotools.grid.ortholine.LineOrientation;
import org.geotools.grid.ortholine.OrthoLineDef;
import org.locationtech.jts.geom.LineString;

public class GraticuleDataStore implements DataStore {
    static final Logger log = Logger.getLogger("GraticuleDataStore");
    private final ReferencedEnvelope bounds;
    private final List<Double> steps;
    private final HashMap<Name, SimpleFeatureSource> sources = new HashMap<>();

    private final ArrayList<Name> names = new ArrayList<>();

    public GraticuleDataStore(ReferencedEnvelope env, List<Double> steps) {
        this.steps = steps;
        this.bounds = env;

        int level = 0;
        Collections.sort(steps);
        for (double step : steps) {
            Name name = new NameImpl(Double.toString(step));
            SimpleFeatureType schema = buildType(name, bounds.getCoordinateReferenceSystem());
            log.fine("Creating graticule with name " + name);
            names.add(name);

            List<OrthoLineDef> lineDefs =
                    Arrays.asList(
                            // vertical (longitude) lines
                            new OrthoLineDef(LineOrientation.VERTICAL, level, step),
                            // horizontal (latitude) lines
                            new OrthoLineDef(LineOrientation.HORIZONTAL, level, step));

            // Specify vertex spacing to get "densified" polygons
            double vertexSpacing = (bounds.getHeight() / 20); // should be dynamic
            SimpleFeatureSource grid =
                    Lines.createOrthoLines(
                            bounds, lineDefs, vertexSpacing, new LineFeatureBuilder(schema));
            sources.put(name, grid);
            level++;
        }
    }

    private static SimpleFeatureType buildType(Name name, CoordinateReferenceSystem crs) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(name);
        tb.add(LineFeatureBuilder.DEFAULT_GEOMETRY_ATTRIBUTE_NAME, LineString.class, crs);
        tb.add(LineFeatureBuilder.ID_ATTRIBUTE_NAME, Integer.class);
        tb.add(LineFeatureBuilder.LEVEL_ATTRIBUTE_NAME, Integer.class);
        tb.add(LineFeatureBuilder.VALUE_LABEL_NAME, String.class);
        tb.add(LineFeatureBuilder.VALUE_ATTRIBUTE_NAME, Double.class);
        tb.add(LineFeatureBuilder.ORIENTATION, Boolean.class);
        return tb.buildFeatureType();
    }

    /**
     * Information about this service.
     *
     * <p>This method offers access to a summary of header or metadata information describing the
     * service. Subclasses may return a specific ServiceInfo instance that has additional
     * information (such as FilterCapabilities).
     *
     * @return SeviceInfo
     */
    @Override
    public ServiceInfo getInfo() {
        DefaultServiceInfo info = new DefaultServiceInfo();
        info.setDescription("Features from " + getClass().getSimpleName());
        info.setSchema(FeatureTypes.DEFAULT_NAMESPACE);
        return info;
    }

    /**
     * Creates storage for a new <code>featureType</code>.
     *
     * <p>The provided <code>featureType</code> we be accessable by the typeName provided by
     * featureType.getTypeName().
     *
     * @param featureType FetureType to add to DataStore
     * @throws IOException If featureType cannot be created
     */
    @Override
    public void createSchema(SimpleFeatureType featureType) throws IOException {
        throw new IllegalAccessError("Modification of schema is not permitted");
    }

    /**
     * Used to update a schema in place.
     *
     * <p>This functionality is similar to an "alter table" statement in SQL. Implementation is
     * optional; it may not be supported by all servers or files.
     *
     * @param typeName
     * @param featureType
     * @throws IOException if the operation failed
     * @throws UnsupportedOperation if functionality is not available
     */
    @Override
    public void updateSchema(Name typeName, SimpleFeatureType featureType) throws IOException {
        // do nothing?
    }

    /**
     * Used to permanently remove a schema from the underlying storage
     *
     * <p>This functionality is similar to an "drop table" statement in SQL. Implementation is
     * optional; it may not be supported by all servers or files.
     *
     * @param typeName
     * @throws IOException if the operation failed
     * @throws UnsupportedOperation if functionality is not available
     */
    @Override
    public void removeSchema(Name typeName) throws IOException {}

    /**
     * Names of the available Resources.
     *
     * <p>For additional information please see getInfo( Name ) and getSchema( Name ).
     *
     * @return Names of the available contents.
     */
    @Override
    public List<Name> getNames() throws IOException {
        return new ArrayList<Name>(sources.keySet());
    }

    /**
     * Description of the named resource.
     *
     * <p>The FeatureType returned describes the contents being published. For additional metadata
     * please review getInfo( Name ).
     *
     * @param name Type name a the resource from getNames()
     * @return Description of the FeatureType being made avaialble
     */
    @Override
    public SimpleFeatureType getSchema(Name name) throws IOException {
        log.finest("asking for " + name + "'s schema");
        return sources.get(cleanName(name)).getSchema();
    }

    /**
     * There is an issue that the requested name (if coming from GeoServer) will have a FQN and when
     * we created the sources map we didn't have a domain so we need to strip the domain and
     * generate a new Name.
     *
     * @param name
     * @return
     */
    private Name cleanName(Name name) {
        return new NameImpl(name.getLocalPart());
    }

    /**
     * Disposes of this data store and releases any resource that it is using.
     *
     * <p>A <code>DataStore</code> cannot be used after <code>dispose</code> has been called,
     * neither can any data access object it helped create, such as {@link FeatureReader}, {@link
     * FeatureSource} or {@link FeatureCollection}.
     *
     * <p>This operation can be called more than once without side effects.
     *
     * <p>There is no thread safety assurance associated with this method. For example, client code
     * will have to make sure this method is not called while retrieving/saving data from/to the
     * storage, or be prepared for the consequences.
     */
    @Override
    public void dispose() {}

    /**
     * Applies a new schema to the given feature type. This can be used to add or remove properties.
     * The resulting update will be persistent.
     *
     * @param typeName name of the feature type to update
     * @param featureType the new schema to apply
     * @throws IOException on error
     */
    @Override
    public void updateSchema(String typeName, SimpleFeatureType featureType) throws IOException {}

    /**
     * Used to permanently remove a schema from the underlying storage
     *
     * <p>This functionality is similar to an "drop table" statement in SQL. Implementation is
     * optional; it may not be supported by all servers or files.
     *
     * @param typeName
     * @throws IOException if the operation failed
     * @throws UnsupportedOperation if functionality is not available
     */
    @Override
    public void removeSchema(String typeName) throws IOException {}

    /**
     * Gets the names of feature types available in this {@code DataStore}. Please note that this is
     * not guaranteed to return a list of unique names since the same unqualified name may be
     * present in separate namespaces within the {@code DataStore}.
     *
     * @return names of feature types available in this {@code DataStore}
     * @throws IOException if data access errors occur
     */
    @Override
    public String[] getTypeNames() throws IOException {
        return (String[]) sources.keySet().stream().map(x -> x.toString()).toArray(String[]::new);
    }

    /**
     * Gets the type information (schema) for the specified feature type.
     *
     * @param typeName the feature type name
     * @return the requested feature type
     * @throws IOException if {@code typeName} is not available
     */
    @Override
    public SimpleFeatureType getSchema(String typeName) throws IOException {
        return getSchema(new NameImpl(typeName));
    }

    /**
     * Gets a {@code SimpleFeatureSource} for features of the specified type. {@code
     * SimpleFeatureSource} provides a high-level API for feature operations.
     *
     * <p>The resulting {@code SimpleFeatureSource} may implment more functionality as in this
     * example:
     *
     * <pre><code>
     *
     * SimpleFeatureSource fsource = dataStore.getFeatureSource("roads");
     * if (fsource instanceof SimpleFeatureStore) {
     *     // we have write access to the feature data
     *     SimpleFeatureStore fstore = (SimpleFeatureStore) fs;
     * }
     * else {
     *     // System.out.println("We do not have write access to roads");
     * }
     * </code></pre>
     *
     * @param typeName the feature type
     * @return a {@code SimpleFeatureSource} (or possibly a subclass) providing operations for
     *     features of the specified type
     * @throws IOException if data access errors occur
     * @see SimpleFeatureSource
     * @see SimpleFeatureStore
     */
    @Override
    public SimpleFeatureSource getFeatureSource(String typeName) throws IOException {
        return sources.get(new NameImpl(typeName));
    }

    /**
     * Gets a {@code SimpleFeatureSource} for features of the type specified by a qualified name
     * (namespace plus type name).
     *
     * @param typeName the qualified name of the feature type
     * @return a {@code SimpleFeatureSource} (or possibly a subclass) providing operations for
     *     features of the specified type
     * @throws IOException if data access errors occur
     * @see #getFeatureSource(String)
     * @see SimpleFeatureSource
     * @see SimpleFeatureStore
     */
    @Override
    public SimpleFeatureSource getFeatureSource(Name typeName) throws IOException {
        return sources.get(cleanName(typeName));
    }

    /**
     * Gets a {@code FeatureReader} for features selected by the given {@code Query}. {@code
     * FeatureReader} provies an iterator-style API to feature data.
     *
     * <p>The {@code Query} provides the schema for the form of the returned features as well as a
     * {@code Filter} to constrain the features available via the reader.
     *
     * <p>The {@code Transaction} can be used to externalize the state of the {@code DataStore}.
     * Examples of this include a {@code JDBCDataStore} sharing a connection for use across several
     * {@code FeatureReader} requests; and a {@code ShapefileDataStore} redirecting requests to an
     * alternate file during the course of a {@code Transaction}.
     *
     * @param query a query providing the schema and constraints for features that the reader will
     *     return
     * @param transaction a transaction that this reader will operate against
     * @return an instance of {@code FeatureReader}
     * @throws IOException if data access errors occur
     */
    @Override
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(
            Query query, Transaction transaction) throws IOException {
        Name typeName = new NameImpl(query.getTypeName());
        log.info("requesting feature reader for " + typeName);
        return new CollectionFeatureReader(
                sources.get(typeName).getFeatures(query), sources.get(typeName).getSchema());
    }

    /**
     * Gets a {@code FeatureWriter} to modify features in this {@code DataStore}. {@code
     * FeatureWriter} provides an iterator style API to features.
     *
     * <p>The returned writer does <b>not</b> allow features to be added.
     *
     * @param typeName the type name for features that will be accessible
     * @param filter defines additional constraints on the features that will be accessible
     * @param transaction the transaction that the returned writer operates against
     * @return an instance of {@code FeatureWriter}
     * @throws IOException if data access errors occur
     * @see #getFeatureWriterAppend(String, Transaction)
     */
    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(
            String typeName, Filter filter, Transaction transaction) throws IOException {
        return null;
    }

    /**
     * Gets a {@code FeatureWriter} to modify features in this {@code DataStore}. {@code
     * FeatureWriter} provides an iterator style API to features.
     *
     * <p>The returned writer does <b>not</b> allow features to be added.
     *
     * @param typeName the type name for features that will be accessible
     * @param transaction the transaction that the returned writer operates against
     * @return an instance of {@code FeatureWriter}
     * @throws IOException if data access errors occur
     * @see #getFeatureWriterAppend(String, Transaction)
     */
    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(
            String typeName, Transaction transaction) throws IOException {
        return null;
    }

    /**
     * Gets a {@code FeatureWriter} that can add new features to the {@code DataStore}.
     *
     * <p>The {@code FeatureWriter} will return {@code false} when its {@code hasNext()} method is
     * called, but {@code next()} can be used to acquire new features.
     *
     * @param typeName name of the feature type for which features will be added
     * @param transaction the transaction to operate against
     * @return an instance of {@code FeatureWriter} that can only be used to append new features
     * @throws IOException if data access errors occur
     */
    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(
            String typeName, Transaction transaction) throws IOException {
        return null;
    }

    /**
     * Retrieve a per featureID based locking service from this {@code DataStore}.
     *
     * @return an instance of {@code LockingManager}; or {@code null} if locking is handled by the
     *     {@code DataStore} in a different fashion
     */
    @Override
    public LockingManager getLockingManager() {
        return null;
    }
}
