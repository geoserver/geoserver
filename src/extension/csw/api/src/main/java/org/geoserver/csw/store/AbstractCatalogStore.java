/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.csw.records.AbstractRecordDescriptor;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordDescriptor;
import org.geotools.api.data.Query;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;

/**
 * @author Andrea Aime - GeoSolutions
 * @author Niels Charlier
 */
public abstract class AbstractCatalogStore implements CatalogStore {

    protected Map<Name, RecordDescriptor> descriptorByType = new RecordDescriptorsMap();

    protected void support(RecordDescriptor descriptor) {
        descriptorByType.put(descriptor.getFeatureDescriptor().getName(), descriptor);
    }

    @Override
    public RecordDescriptor[] getRecordDescriptors() throws IOException {
        ArrayList<RecordDescriptor> ft = new ArrayList<>(descriptorByType.values());
        return ft.toArray(new RecordDescriptor[ft.size()]);
    }

    @Override
    public CloseableIterator<String> getDomain(Name typeName, final Name attributeName)
            throws IOException {
        final RecordDescriptor rd = descriptorByType.get(typeName);

        if (rd == null) {
            throw new IOException(typeName + " is not a supported type");
        }
        final List<PropertyName> properties = rd.translateProperty(attributeName);

        // do we have these properties?
        if (!hasProperties(rd.getFeatureType(), properties)) {
            return new CloseableIteratorAdapter<>(new ArrayList<String>().iterator());
        }

        // build the query against csw:record
        Query q = new Query(typeName.getLocalPart());

        q.setProperties(translateToPropertyNames(rd, attributeName));

        // collect the values without duplicates
        final Set<String> values = new HashSet<>();
        getRecords(q, Transaction.AUTO_COMMIT, rd)
                .accepts(
                        feature -> {
                            for (PropertyName property : properties) {
                                Property prop = (Property) property.evaluate(feature);
                                if (prop != null) {
                                    values.add(
                                            new String(
                                                    ((String) prop.getValue()).getBytes(ISO_8859_1),
                                                    UTF_8));
                                    break;
                                }
                            }
                        },
                        null);

        // sort and return
        List<String> result = new ArrayList<>(values);
        Collections.sort(result);
        return new CloseableIteratorAdapter<>(result.iterator());
    }

    protected boolean hasProperties(FeatureType featureType, List<PropertyName> properties) {
        boolean hasProperty = false;
        for (PropertyName property : properties) {
            AttributeDescriptor ad = (AttributeDescriptor) property.evaluate(featureType);
            hasProperty |= ad != null;
        }
        return hasProperty;
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getRecords(
            Query q, Transaction t, RecordDescriptor rdOutput) throws IOException {
        Name typeName = null;
        if (q.getTypeName() == null) {
            typeName = CSWRecordDescriptor.RECORD_DESCRIPTOR.getName();
        } else if (q.getNamespace() != null) {
            typeName = new NameImpl(q.getNamespace().toString(), q.getTypeName());
        } else {
            typeName = new NameImpl(q.getTypeName());
        }
        RecordDescriptor rd = descriptorByType.get(typeName);

        if (rd == null) {
            throw new IOException(q.getTypeName() + " is not a supported type");
        }

        return getRecordsInternal(rd, rdOutput, q, t);
    }

    public abstract FeatureCollection<FeatureType, Feature> getRecordsInternal(
            RecordDescriptor rd, RecordDescriptor rdOutput, Query q, Transaction t)
            throws IOException;

    @Override
    public RepositoryItem getRepositoryItem(String recordId) throws IOException {
        // not supported
        return null;
    }

    @Override
    public int getRecordsCount(Query q, Transaction t, RecordDescriptor rdOutput)
            throws IOException {
        // simply delegate to the feature collection, we have no optimizations
        // available for the time being (even counting the files in case of no filtering
        // would be wrong as we have to
        return getRecords(q, t, rdOutput).size();
    }

    @Override
    public List<FeatureId> addRecord(Feature f, Transaction t) throws IOException {
        throw new UnsupportedOperationException("This store does not support transactions yet");
    }

    @Override
    public void deleteRecord(Filter f, Transaction t) throws IOException {
        throw new UnsupportedOperationException("This store does not support transactions yet");
    }

    @Override
    public void updateRecord(
            Name typeName,
            Name[] attributeNames,
            Object[] attributeValues,
            Filter filter,
            Transaction t)
            throws IOException {
        throw new UnsupportedOperationException("This store does not support transactions yet");
    }

    @Override
    public CatalogStoreCapabilities getCapabilities() {
        return new CatalogStoreCapabilities(descriptorByType);
    }

    @Override
    public List<PropertyName> translateToPropertyNames(RecordDescriptor rd, Name name) {
        return Collections.singletonList(
                AbstractRecordDescriptor.buildPropertyName(rd.getNamespaceSupport(), name));
    }
}
