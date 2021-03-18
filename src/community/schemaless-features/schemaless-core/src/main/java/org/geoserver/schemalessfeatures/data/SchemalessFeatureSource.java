/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.data;

import java.awt.RenderingHints;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.schemalessfeatures.type.SchemalessFeatureType;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml3.v3_2.GMLSchema;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public abstract class SchemalessFeatureSource implements FeatureSource<FeatureType, Feature> {

    private SchemalessDataAccess store;

    protected Name name;

    protected Set<Hints.Key> hints;

    public static final Logger LOG = Logging.getLogger(SchemalessFeatureSource.class);

    public SchemalessFeatureSource(Name name, SchemalessDataAccess store) {
        this.store = store;
        this.name = name;
        // set up hints
        hints = new HashSet<>();
        hints.add(Hints.JTS_GEOMETRY_FACTORY);
        hints.add(Hints.JTS_COORDINATE_SEQUENCE_FACTORY);

        // add subclass specific hints
        addHints(hints);

        // make hints unmodifiable
        hints = Collections.unmodifiableSet(hints);
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public ResourceInfo getInfo() {
        return new ResourceInfo() {
            final Set<String> words = new HashSet<>();

            {
                words.add("features");
                words.add(SchemalessFeatureSource.this.getSchema().getName().toString());
            }

            @Override
            public ReferencedEnvelope getBounds() {
                try {
                    return SchemalessFeatureSource.this.getBounds();
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            public CoordinateReferenceSystem getCRS() {
                return SchemalessFeatureSource.this.getSchema().getCoordinateReferenceSystem();
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
                return SchemalessFeatureSource.this.getSchema().getName().toString();
            }

            @Override
            public URI getSchema() {
                Name name = SchemalessFeatureSource.this.getSchema().getName();
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
                Name name = SchemalessFeatureSource.this.getSchema().getName();
                return name.getLocalPart();
            }
        };
    }

    @Override
    public SchemalessDataAccess getDataStore() {
        return store;
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        return new QueryCapabilities();
    }

    @Override
    public void addFeatureListener(FeatureListener listener) {}

    @Override
    public void removeFeatureListener(FeatureListener listener) {}

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures(Filter filter) throws IOException {
        Query q = new Query(getName().toString(), filter);
        return new SchemalessFeatureCollection(q, this);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures(Query query) throws IOException {
        return new SchemalessFeatureCollection(query, this);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures() throws IOException {
        return new SchemalessFeatureCollection(Query.ALL, this);
    }

    @Override
    public FeatureType getSchema() {
        FeatureType featureType = null;
        if (featureType == null) {
            GeometryDescriptor descriptor = getGeometryDescriptor();
            featureType =
                    new SchemalessFeatureType(
                            name,
                            new ArrayList<>(),
                            descriptor,
                            false,
                            Collections.emptyList(),
                            GMLSchema.ABSTRACTFEATURETYPE_TYPE,
                            null);
        }
        return featureType;
    }

    protected abstract GeometryDescriptor getGeometryDescriptor();

    protected final Name name(String typeName) {
        return new NameImpl(typeName);
    }

    protected final Name name(String namespaceURI, String typeName) {
        return new NameImpl(namespaceURI, typeName);
    }

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        return getBounds(Query.ALL);
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return getBoundsInternal(query);
    }

    private ReferencedEnvelope getBoundsInternal(Query q) throws IOException {
        GeometryDescriptor descriptor = getGeometryDescriptor();
        if (descriptor != null) {
            q = new Query(q);
            q.setPropertyNames(descriptor.getLocalName());
        }
        try (FeatureReader<FeatureType, Feature> r = getReader(q)) {
            ReferencedEnvelope e = new ReferencedEnvelope();
            if (r.hasNext()) {
                Feature f = r.next();
                e.init(f.getBounds());
            }
            while (r.hasNext()) {
                e.include(r.next().getBounds());
            }
            return e;
        }
    }

    @Override
    public int getCount(Query query) throws IOException {
        return getCountInteral(query);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<RenderingHints.Key> getSupportedHints() {
        return (Set<RenderingHints.Key>) (Set<?>) hints;
    }

    public FeatureReader<FeatureType, Feature> getReader(Query query) {
        return getReaderInteranl(query);
    }

    protected abstract FeatureReader<FeatureType, Feature> getReaderInteranl(Query query);

    protected abstract int getCountInteral(Query query);

    protected boolean isAll(Filter f) {
        return f == null || f == Filter.INCLUDE;
    }

    protected void addHints(Set<Hints.Key> hints) {}
}
