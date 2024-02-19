/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import net.opengis.cat.csw20.ElementSetType;
import org.geoserver.csw.feature.MemoryFeatureCollection;
import org.geoserver.csw.store.AbstractCatalogStore;
import org.geotools.api.data.Query;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.FeatureTypeFactory;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.junit.Test;
import org.xml.sax.helpers.NamespaceSupport;

public class AbstractCatalogStoreTest {

    @Test
    public void testNamespaceSupport() throws IOException, URISyntaxException {
        AbstractCatalogStore store =
                new AbstractCatalogStore() {
                    {
                        support(CSWRecordDescriptor.getInstance());
                        support(GSRecordDescriptor.getInstance());
                    }

                    @Override
                    public FeatureCollection<FeatureType, Feature> getRecordsInternal(
                            RecordDescriptor rd, RecordDescriptor rdOutput, Query q, Transaction t)
                            throws IOException {
                        if (rd == GSRecordDescriptor.getInstance()) {
                            return new MemoryFeatureCollection(
                                    GSRecordDescriptor.getInstance().getFeatureType());
                        } else {
                            throw new RuntimeException(
                                    "Was expecting the geoserver record descriptor");
                        }
                    }
                };

        RecordDescriptor[] descriptors = store.getRecordDescriptors();
        assertEquals(2, descriptors.length);
        assertEquals(CSWRecordDescriptor.getInstance(), descriptors[0]);
        assertEquals(GSRecordDescriptor.getInstance(), descriptors[1]);
        Query query = new Query("Record");
        query.setNamespace(new URI(GSRecordDescriptor.GS_NAMESPACE));
        FeatureCollection records = store.getRecords(query, Transaction.AUTO_COMMIT, null);
        assertEquals(GSRecordDescriptor.getInstance().getFeatureType(), records.getSchema());
    }

    static class GSRecordDescriptor extends AbstractRecordDescriptor {
        static final String GS_NAMESPACE = "http://www.geoserver.org/csw";
        CSWRecordDescriptor delegate = CSWRecordDescriptor.getInstance();
        static final GSRecordDescriptor INSTANCE = new GSRecordDescriptor();

        public static GSRecordDescriptor getInstance() {
            return INSTANCE;
        }

        @Override
        public FeatureType getFeatureType() {
            FeatureType ft = delegate.getFeatureType();
            FeatureTypeFactory factory = new FeatureTypeFactoryImpl();
            FeatureType gsft =
                    factory.createFeatureType(
                            new NameImpl(GS_NAMESPACE, "Record"),
                            ft.getDescriptors(),
                            null,
                            false,
                            null,
                            ft.getSuper(),
                            null);
            return gsft;
        }

        @Override
        public AttributeDescriptor getFeatureDescriptor() {
            AttributeTypeBuilder builder = new AttributeTypeBuilder();
            AttributeDescriptor descriptor =
                    builder.buildDescriptor(
                            new NameImpl(GS_NAMESPACE, "Record"), delegate.getFeatureType());
            return descriptor;
        }

        @Override
        public String getOutputSchema() {
            return delegate.getOutputSchema();
        }

        @Override
        public List<Name> getPropertiesForElementSet(ElementSetType elementSet) {
            return delegate.getPropertiesForElementSet(elementSet);
        }

        @Override
        public NamespaceSupport getNamespaceSupport() {
            return delegate.getNamespaceSupport();
        }

        @Override
        public Query adaptQuery(Query query) {
            return delegate.adaptQuery(query);
        }

        @Override
        public String getBoundingBoxPropertyName() {
            return delegate.getBoundingBoxPropertyName();
        }

        @Override
        public List<Name> getQueryables() {
            return delegate.getQueryables();
        }

        @Override
        public String getQueryablesDescription() {
            return delegate.getQueryablesDescription();
        }

        @Override
        public List<PropertyName> translateProperty(Name name) {
            return delegate.translateProperty(name);
        }

        @Override
        public void verifySpatialFilters(Filter filter) {
            delegate.verifySpatialFilters(filter);
        }
    }
}
