package org.geoserver.data.versioning;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.AbstractDataStore;
import org.geotools.data.DataAccess;
import org.geotools.data.DataSourceException;
import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.SchemaNotFoundException;
import org.geotools.data.ServiceInfo;
import org.geotools.data.Transaction;
import org.geotools.factory.Hints;
import org.geotools.factory.Hints.Key;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * This is an example implementation of a {@link DataAccess} used for testing.
 * 
 * <p>
 * It serves as an example implementation of:
 * </p>
 * 
 * <ul>
 * <li>
 * FeatureListenerManager use: allows handling of FeatureEvents</li>
 * </ul>
 * 
 * <p>
 * This class will also illustrate the use of In-Process locking when the time comes.
 * </p>
 * 
 * @author jgarnett
 * 
 * @source $URL$
 */
public class SimpleMemoryDataAccess extends AbstractDataStore implements
        DataAccess<SimpleFeatureType, SimpleFeature> {

    private static final Logger LOGGER = Logging.getLogger(SimpleMemoryDataAccess.class);

    /** Memory holds Map of Feature by fid by typeName. */
    protected Map<Name, Map<String, SimpleFeature>> memory = new LinkedHashMap<Name, Map<String, SimpleFeature>>();

    /** Schema holds FeatureType by typeName */
    protected Map<Name, SimpleFeatureType> schema = new HashMap<Name, SimpleFeatureType>();

    /**
     * Construct an MemoryDataStore around an empty collection of the provided FeatureType
     * 
     * @param schema
     *            An empty feature collection of this type will be made available
     */
    public SimpleMemoryDataAccess(SimpleFeatureType featureType) {
        Map<String, SimpleFeature> featureMap = new LinkedHashMap<String, SimpleFeature>();
        Name typeName = featureType.getName();
        schema.put(typeName, featureType);
        memory.put(typeName, featureMap);
    }

    public SimpleMemoryDataAccess(FeatureCollection collection) {
        addFeatures(collection);
    }

    public SimpleMemoryDataAccess() {

    }

    public SimpleMemoryDataAccess(Feature... array) {
        addFeatures(array);
    }

    public SimpleMemoryDataAccess(FeatureReader reader) throws IOException {
        addFeatures(reader);
    }

    public SimpleMemoryDataAccess(FeatureIterator reader) throws IOException {
        addFeatures(reader);
    }

    /**
     * Configures MemoryDataStore with FeatureReader.
     * 
     * @param reader
     *            New contents to add
     * 
     * @throws IOException
     *             If problems are encountered while adding
     * @throws DataSourceException
     *             See IOException
     */
    public void addFeatures(FeatureReader reader) throws IOException {
        try {
            FeatureType featureType;
            // use an order preserving map, so that features are returned in the same
            // order as they were inserted. This is important for repeatable rendering
            // of overlapping features.
            Map<String, SimpleFeature> featureMap = new LinkedHashMap<String, SimpleFeature>();
            Name typeName;
            Feature feature;

            feature = reader.next();

            if (feature == null) {
                throw new IllegalArgumentException(
                        "Provided  FeatureReader<FeatureType, Feature> is closed");
            }

            featureType = feature.getType();
            typeName = featureType.getName();

            featureMap.put(feature.getIdentifier().getID(), (SimpleFeature) feature);

            while (reader.hasNext()) {
                feature = reader.next();
                featureMap.put(feature.getIdentifier().getID(), (SimpleFeature) feature);
            }

            schema.put(typeName, (SimpleFeatureType) featureType);
            memory.put(typeName, featureMap);
        } catch (IllegalAttributeException e) {
            throw new DataSourceException("Problem using reader", e);
        } finally {
            reader.close();
        }
    }

    /**
     * Configures MemoryDataStore with FeatureReader.
     * 
     * @param reader
     *            New contents to add
     * 
     * @throws IOException
     *             If problems are encountered while adding
     * @throws DataSourceException
     *             See IOException
     */
    public void addFeatures(FeatureIterator reader) throws IOException {
        try {
            FeatureType featureType;
            Map<String, SimpleFeature> featureMap = new LinkedHashMap<String, SimpleFeature>();
            Name typeName;
            Feature feature;

            feature = reader.next();

            if (feature == null) {
                throw new IllegalArgumentException(
                        "Provided  FeatureReader<FeatureType, Feature> is closed");
            }

            featureType = feature.getType();
            typeName = featureType.getName();

            featureMap.put(feature.getIdentifier().getID(), (SimpleFeature) feature);

            while (reader.hasNext()) {
                feature = reader.next();
                featureMap.put(feature.getIdentifier().getID(), (SimpleFeature) feature);
            }

            schema.put(typeName, (SimpleFeatureType) featureType);
            memory.put(typeName, featureMap);
        } finally {
            reader.close();
        }
    }

    /**
     * Configures MemoryDataStore with Collection.
     * 
     * <p>
     * You may use this to create a MemoryDataStore from a FeatureCollection.
     * </p>
     * 
     * @param collection
     *            Collection of features to add
     * 
     * @throws IllegalArgumentException
     *             If provided collection is empty
     */
    public void addFeatures(Collection<?> collection) {
        if ((collection == null) || collection.isEmpty()) {
            throw new IllegalArgumentException("Provided FeatureCollection is empty");
        }

        synchronized (memory) {
            for (Iterator<?> i = collection.iterator(); i.hasNext();) {
                addFeatureInternal((Feature) i.next());
            }
        }
    }

    public void addFeatures(FeatureCollection collection) {
        if ((collection == null)) {
            throw new IllegalArgumentException("Provided FeatureCollection is null");
        }
        synchronized (memory) {
            try {
                collection.accepts(new FeatureVisitor() {
                    public void visit(Feature feature) {
                        addFeatureInternal((Feature) feature);
                    }
                }, null);
            } catch (IOException ignore) {
                LOGGER.log(Level.FINE, "Unable to add all features", ignore);
            }
        }
    }

    /**
     * Configures MemoryDataStore with feature array.
     * 
     * @param features
     *            Array of features to add
     * 
     * @throws IllegalArgumentException
     *             If provided feature array is empty
     */
    public void addFeatures(Feature... features) {
        if ((features == null) || (features.length == 0)) {
            throw new IllegalArgumentException("Provided features are empty");
        }

        synchronized (memory) {
            for (int i = 0; i < features.length; i++) {
                addFeatureInternal(features[i]);
            }
        }
    }

    /**
     * Adds a single Feature to the correct typeName entry.
     * 
     * <p>
     * This is an internal opperation used for setting up MemoryDataStore - please use FeatureWriter
     * for generatl use.
     * </p>
     * 
     * <p>
     * This method is willing to create new FeatureTypes for MemoryDataStore.
     * </p>
     * 
     * @param feature
     *            Individual feature to add
     */
    public void addFeature(Feature feature) {
        synchronized (memory) {
            addFeatureInternal(feature);
        }
    }

    private void addFeatureInternal(Feature feature) {
        if (feature == null) {
            throw new IllegalArgumentException("Provided Feature is empty");
        }

        FeatureType featureType;
        featureType = feature.getType();

        Name typeName = featureType.getName();

        Map<String, SimpleFeature> featuresMap;

        if (!memory.containsKey(typeName)) {
            try {
                createSchema((SimpleFeatureType) featureType);
            } catch (IOException e) {
                // this should not of happened ?!?
                // only happens if typeNames is taken and
                // we just checked
            }
        }

        featuresMap = memory.get(typeName);
        featuresMap.put(feature.getIdentifier().getID(), (SimpleFeature) feature);
    }

    protected Map<String, SimpleFeature> features(final String nsUri, final String typeName)
            throws IOException {
        synchronized (memory) {
            for (Name name : memory.keySet()) {
                if (nsUri == null && name.getLocalPart().equals(typeName)) {
                    return features(name);
                } else if (name.getNamespaceURI().equals(nsUri)
                        && name.getLocalPart().equals(typeName)) {
                    return features(name);
                }
            }
        }

        throw new IOException("Type name " + typeName + " not found");
    }

    /**
     * Access featureMap for typeName.
     * 
     * @param typeName
     * 
     * @return A Map of Features by FID
     * 
     * @throws IOException
     *             If typeName cannot be found
     */
    protected Map<String, SimpleFeature> features(Name typeName) throws IOException {
        synchronized (memory) {
            if (memory.containsKey(typeName)) {
                return memory.get(typeName);
            }
        }

        throw new IOException("Type name " + typeName + " not found");
    }

    /**
     * Adds support for a new featureType to MemoryDataStore.
     * 
     * <p>
     * FeatureTypes are stored by typeName, an IOException will be thrown if the requested typeName
     * is already in use.
     * </p>
     * 
     * @param featureType
     *            FeatureType to be added
     * 
     * @throws IOException
     *             If featureType already exists
     * 
     * @see org.geotools.data.DataAccess#createSchema(org.opengis.feature.type.FeatureType)
     */
    @Override
    public void createSchema(SimpleFeatureType featureType) throws IOException {
        final Name name = featureType.getName();
        if (memory.containsKey(name)) {
            // we have a conflict
            throw new IOException(featureType.getName() + " already exists");
        }
        // insertion order preserving map
        Map<String, SimpleFeature> featuresMap = new LinkedHashMap<String, SimpleFeature>();
        schema.put(name, featureType);
        memory.put(name, featuresMap);
    }

    private FeatureReader getFeatureReader(final Name typeName) throws IOException {
        return new FeatureReader<FeatureType, Feature>() {
            FeatureType featureType = getSchema(typeName);

            Iterator<SimpleFeature> iterator = features(typeName).values().iterator();

            public FeatureType getFeatureType() {
                return featureType;
            }

            public Feature next() throws IOException, IllegalAttributeException,
                    NoSuchElementException {
                if (iterator == null) {
                    throw new IOException("Feature Reader has been closed");
                }

                try {
                    Feature next = iterator.next();
                    if (next instanceof SimpleFeature) {
                        return SimpleFeatureBuilder.copy((SimpleFeature) next);
                    }
                    // TODO: clone complex Feature
                    return next;
                } catch (NoSuchElementException end) {
                    throw new DataSourceException("There are no more Features", end);
                }
            }

            public boolean hasNext() {
                return (iterator != null) && iterator.hasNext();
            }

            public void close() {
                if (iterator != null) {
                    iterator = null;
                }

                if (featureType != null) {
                    featureType = null;
                }
            }
        };
    }

    /**
     * Provides FeatureWriter over the entire contents of <code>typeName</code>.
     * 
     * <p>
     * Implements getFeatureWriter contract for AbstractDataStore.
     * </p>
     * 
     * @param typeName
     *            name of FeatureType we wish to modify
     * 
     * @return FeatureWriter of entire contents of typeName
     * 
     * @throws IOException
     *             If writer cannot be obtained for typeName
     * @throws DataSourceException
     *             See IOException
     * 
     * @see org.geotools.data.AbstractDataStore#getFeatureSource(java.lang.String)
     */
    public FeatureWriter<SimpleFeatureType, SimpleFeature> createFeatureWriter(
            final String typeName, final Transaction transaction) throws IOException {

        return new FeatureWriter<SimpleFeatureType, SimpleFeature>() {
            SimpleFeatureType featureType = getSchema(typeName);

            Map<String, SimpleFeature> contents = features(featureType.getName());

            Iterator<SimpleFeature> iterator = contents.values().iterator();

            SimpleFeature live = null;

            SimpleFeature current = null; // current Feature returned to user

            public SimpleFeatureType getFeatureType() {
                return featureType;
            }

            public SimpleFeature next() throws IOException, NoSuchElementException {
                if (hasNext()) {
                    // existing content
                    live = iterator.next();

                    try {
                        current = SimpleFeatureBuilder.copy(live);
                    } catch (IllegalAttributeException e) {
                        throw new DataSourceException("Unable to edit " + live.getID() + " of "
                                + typeName);
                    }
                } else {
                    // new content
                    live = null;

                    try {
                        current = SimpleFeatureBuilder.template(featureType, null);
                    } catch (IllegalAttributeException e) {
                        throw new DataSourceException("Unable to add additional Features of "
                                + typeName);
                    }
                }

                return current;
            }

            public void remove() throws IOException {
                if (contents == null) {
                    throw new IOException("FeatureWriter has been closed");
                }

                if (current == null) {
                    throw new IOException("No feature available to remove");
                }

                if (live != null) {
                    // remove existing content
                    iterator.remove();
                    listenerManager.fireFeaturesRemoved(typeName, transaction,
                            new ReferencedEnvelope(live.getBounds()), true);
                    live = null;
                    current = null;
                } else {
                    // cancel add new content
                    current = null;
                }
            }

            public void write() throws IOException {
                if (contents == null) {
                    throw new IOException("FeatureWriter has been closed");
                }

                if (current == null) {
                    throw new IOException("No feature available to write");
                }

                if (live != null) {
                    if (live.equals(current)) {
                        // no modifications made to current
                        //
                        live = null;
                        current = null;
                    } else {
                        // accept modifications
                        //
                        try {
                            live.setAttributes(current.getAttributes());
                        } catch (Exception e) {
                            throw new DataSourceException("Unable to accept modifications to "
                                    + live.getID() + " on " + typeName);
                        }

                        ReferencedEnvelope bounds = new ReferencedEnvelope();
                        bounds.expandToInclude(new ReferencedEnvelope(live.getBounds()));
                        bounds.expandToInclude(new ReferencedEnvelope(current.getBounds()));
                        listenerManager.fireFeaturesChanged(typeName, transaction, bounds, true);
                        live = null;
                        current = null;
                    }
                } else {
                    // add new content
                    //
                    String id = current.getID();
                    if (Boolean.TRUE.equals(current.getUserData().get(Hints.USE_PROVIDED_FID))
                            && null != current.getUserData().get(Hints.PROVIDED_FID)) {
                        id = (String) current.getUserData().get(Hints.PROVIDED_FID);
                        current = SimpleFeatureBuilder.build(current.getFeatureType(),
                                current.getAttributes(), id);
                    }
                    contents.put(id, current);
                    listenerManager.fireFeaturesAdded(typeName, transaction,
                            new ReferencedEnvelope(current.getBounds()), true);
                    current = null;
                }
            }

            public boolean hasNext() throws IOException {
                if (contents == null) {
                    throw new IOException("FeatureWriter has been closed");
                }

                return (iterator != null) && iterator.hasNext();
            }

            public void close() {
                if (iterator != null) {
                    iterator = null;
                }

                if (featureType != null) {
                    featureType = null;
                }

                contents = null;
                current = null;
                live = null;
            }
        };
    }

    /**
     * @see org.geotools.data.AbstractDataStore#getBounds(java.lang.String, org.geotools.data.Query)
     */
    protected ReferencedEnvelope getBounds(Query query) throws IOException {
        String localName = query.getTypeName();
        URI namespace = query.getNamespace();
        String nsUri = namespace == null ? null : namespace.toString();

        Map<String, SimpleFeature> contents = features(nsUri, localName);
        Iterator<SimpleFeature> iterator = contents.values().iterator();

        CoordinateReferenceSystem coordinateSystem = query.getCoordinateSystem();
        ReferencedEnvelope envelope = null;

        Filter filter = query.getFilter();

        int count = 0;
        while (iterator.hasNext() && (count < query.getMaxFeatures())) {
            count++;
            Feature feature = iterator.next();
            if (filter.evaluate(feature)) {
                count++;
                BoundingBox env = feature.getBounds();
                if (null == envelope) {
                    envelope = new ReferencedEnvelope(coordinateSystem);
                }
                envelope.expandToInclude(env.getMinimum(0), env.getMinimum(1));
                envelope.expandToInclude(env.getMaximum(0), env.getMaximum(1));
            }
        }

        return envelope;
    }

    /**
     * @see org.geotools.data.AbstractDataStore#getCount(java.lang.String, org.geotools.data.Query)
     */
    protected int getCount(Query query) throws IOException {
        String localName = query.getTypeName();
        URI namespace = query.getNamespace();
        String ns = namespace == null ? null : namespace.toString();
        Map<String, SimpleFeature> contents = features(ns, localName);
        Iterator<SimpleFeature> iterator = contents.values().iterator();

        int count = 0;

        Filter filter = query.getFilter();

        while (iterator.hasNext() && (count < query.getMaxFeatures())) {
            if (filter.evaluate(iterator.next())) {
                count++;
            }
        }

        return count;
    }

    @Override
    public ServiceInfo getInfo() {
        return new DefaultServiceInfo();
    }

    @Override
    public List<Name> getNames() throws IOException {
        return new ArrayList<Name>(schema.keySet());
    }

    @Override
    public SimpleFeatureType getSchema(Name name) throws IOException {
        synchronized (memory) {
            if (schema.containsKey(name)) {
                return schema.get(name);
            }
            throw new SchemaNotFoundException(name.toString());
        }
    }

    @Override
    public String[] getTypeNames() throws IOException {
        List<Name> names = getNames();
        String[] simpleNames = new String[names.size()];
        for (int i = 0; i < simpleNames.length; i++) {
            simpleNames[i] = names.get(i).getLocalPart();
        }
        return simpleNames;
    }

    @Override
    public SimpleFeatureType getSchema(String typeName) throws IOException {
        for (Name name : getNames()) {
            if (name.getLocalPart().equals(typeName)) {
                return getSchema(name);
            }
        }
        throw new SchemaNotFoundException(typeName);
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName)
            throws IOException {
        for (Name name : getNames()) {
            if (name.getLocalPart().equals(typeName)) {
                return getFeatureReader(name);
            }
        }
        throw new SchemaNotFoundException(typeName);
    }

    @Override
    protected Set<Key> getSupportedHints() {
        return Collections.singleton(Hints.USE_PROVIDED_FID);
    }
}
