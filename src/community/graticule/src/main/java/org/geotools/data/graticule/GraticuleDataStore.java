/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.data.graticule;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.Query;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.graticule.gridsupport.LineFeatureBuilder;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.UnsupportedImplementationException;
import org.locationtech.jts.geom.LineString;

public class GraticuleDataStore extends ContentDataStore implements DataStore {
    static final Logger log = Logger.getLogger("GraticuleDataStore");
    final ReferencedEnvelope bounds;
    ArrayList<Double> steps;
    final Name name;

    public final SimpleFeatureType schema;
    private ContentEntry entry;

    public GraticuleDataStore(ReferencedEnvelope env, List<Double> steps) {
        this.steps = new ArrayList<>(steps);
        this.bounds = env;
        Collections.sort(steps);
        StringBuilder n = new StringBuilder("Graticule_");
        n.append(steps.get(0));
        if (steps.size() > 1) {
            n.append("-").append(steps.get(steps.size() - 1));
        }

        this.name = cleanName(new NameImpl(n.toString()));
        schema = buildType(n.toString(), bounds.getCoordinateReferenceSystem());
        entry = new ContentEntry(this, this.name);
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
        try {
            info.setSchema(new URI(name.getNamespaceURI()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        return Collections.singletonList(name);
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

        return schema;
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
        String local = name.getLocalPart().toString().replace('(', '_').replace(')', '_');

        return new NameImpl(name.getNamespaceURI(), local);
    }

    @Override
    public void dispose() {}

    @Override
    protected List<Name> createTypeNames() throws IOException {
        return Collections.singletonList(name);
    }

    @Override
    protected ContentFeatureSource createFeatureSource(ContentEntry contentEntry)
            throws IOException {
        return null;
    }

    @Override
    public void removeSchema(String typeName) throws IOException {}

    @Override
    public ContentFeatureSource getFeatureSource(String typeName) throws IOException {
        return new GraticuleFeatureSource(entry, new Query(typeName), steps, bounds, schema);
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

        return new GraticuleFeatureSource(
                entry, new Query(typeName.getLocalPart()), steps, bounds, schema);
    }

    @Override
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(
            Query query, Transaction transaction) throws IOException {
        Name typeName = new NameImpl(query.getTypeName());
        log.finest("requesting feature reader for " + typeName);
        return new GraticuleFeatureReader(this, query);
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(
            String typeName, Filter filter, Transaction transaction) throws IOException {
        throw new UnsupportedImplementationException("Grids are not writable");
    }

    private SimpleFeatureType buildType(String name, CoordinateReferenceSystem crs) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(name);

        tb.add(LineFeatureBuilder.ID_ATTRIBUTE_NAME, Integer.class);
        tb.add(LineFeatureBuilder.DEFAULT_GEOMETRY_ATTRIBUTE_NAME, LineString.class, crs);
        tb.setDefaultGeometry(LineFeatureBuilder.DEFAULT_GEOMETRY_ATTRIBUTE_NAME);
        tb.setCRS(crs);
        tb.add(LineFeatureBuilder.LEVEL_ATTRIBUTE_NAME, Integer.class);
        tb.add(LineFeatureBuilder.VALUE_LABEL_NAME, String.class);
        tb.add(LineFeatureBuilder.VALUE_ATTRIBUTE_NAME, Double.class);
        tb.add(LineFeatureBuilder.ORIENTATION_NAME, Boolean.class);
        SimpleFeatureType type = tb.buildFeatureType();
        return type;
    }

    public List<Double> getSteps() {
        return steps;
    }
}
