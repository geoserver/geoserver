/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.store.CatalogStoreCapabilities;
import org.geoserver.csw.store.RepositoryItem;
import org.geoserver.platform.resource.Files;
import org.geotools.csw.CSW;
import org.geotools.csw.DC;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.filter.SortByImpl;
import org.geotools.util.factory.Hints;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

public class SimpleCatalogStoreTest {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    File root = new File("./src/test/resources/org/geoserver/csw/store/simple");
    SimpleCatalogStore store = new SimpleCatalogStore(Files.asResource(root));

    @BeforeClass
    public static void setUp() {
        Hints.putSystemDefault(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, true);
    }

    @Test
    public void testCapabilities() throws Exception {
        CatalogStoreCapabilities capabilities = store.getCapabilities();
        assertFalse(capabilities.supportsTransactions());
        Name cswRecordName = CSWRecordDescriptor.RECORD_DESCRIPTOR.getName();
        assertTrue(capabilities.supportsGetRepositoryItem(cswRecordName));
        assertTrue(
                capabilities
                        .getQueriables(cswRecordName)
                        .contains(new NameImpl(CSW.NAMESPACE, "AnyText")));
        assertTrue(
                capabilities
                        .getDomainQueriables(cswRecordName)
                        .contains(new NameImpl(DC.NAMESPACE, "title")));
    }

    @Test
    public void testCreationExceptions() throws IOException {
        try {
            new SimpleCatalogStore(Files.asResource(new File("./pom.xml")));
            fail("Should have failed, the reference is not a directory");
        } catch (IllegalArgumentException e) {
            // fine
        }
    }

    @Test
    public void testFeatureTypes() throws IOException {
        RecordDescriptor[] fts = store.getRecordDescriptors();
        assertEquals(1, fts.length);
        assertEquals(CSWRecordDescriptor.RECORD_DESCRIPTOR, fts[0].getFeatureDescriptor());
    }

    @Test
    public void testReadAllRecords() throws IOException {
        FeatureCollection records = store.getRecords(Query.ALL, Transaction.AUTO_COMMIT);
        int fileCount = root.list(new RegexFileFilter("Record_.*\\.xml")).length;
        assertEquals(fileCount, records.size());

        FeatureIterator<Feature> fi = records.features();
        try {
            while (fi.hasNext()) {
                Feature f = fi.next();

                // check the id has be read and matches the expected format (given what we have in
                // the files)
                String id = getSimpleLiteralValue(f, "identifier");
                assertNotNull(id);
                assertTrue(
                        id.matches(
                                "urn:uuid:[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"));

                // check the feature id is the same as the id attribute
                assertEquals(id, f.getIdentifier().getID());

                // the other thing we always have in these records is the type
                Attribute type = (Attribute) f.getProperty("type");
                assertNotNull(type);
                assertNotNull(type.getValue());
            }
        } finally {
            fi.close();
        }
    }

    private String getSimpleLiteralValue(Feature f, String name) {
        ComplexAttribute ca = (ComplexAttribute) f.getProperty(name);
        return (String) ca.getProperty("value").getValue();
    }

    private String getSimpleLiteralScheme(Feature f, String name) {
        ComplexAttribute ca = (ComplexAttribute) f.getProperty(name);
        return (String) ca.getProperty("scheme").getValue();
    }

    @Test
    public void testElementValueFilter() throws IOException {
        Filter filter =
                FF.equals(
                        FF.property("dc:identifier/dc:value", CSWRecordDescriptor.NAMESPACES),
                        FF.literal("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd"));
        FeatureCollection records =
                store.getRecords(new Query("Record", filter), Transaction.AUTO_COMMIT);
        assertEquals(1, records.size());
        Feature record = (Feature) records.toArray()[0];
        assertEquals(
                "urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd",
                getSimpleLiteralValue(record, "identifier"));
        assertEquals("http://purl.org/dc/dcmitype/Service", getSimpleLiteralValue(record, "type"));
        assertEquals(
                "Proin sit amet justo. In justo. Aenean adipiscing nulla id tellus.",
                getSimpleLiteralValue(record, "abstract"));
    }

    @Test
    public void testSpatialFilter() throws IOException {
        Filter filter =
                FF.bbox("", 60.042, 13.754, 68.410, 17.920, CSWRecordDescriptor.DEFAULT_CRS_NAME);
        FeatureCollection records =
                store.getRecords(new Query("Record", filter), Transaction.AUTO_COMMIT);
        assertEquals(1, records.size());
        Feature record = (Feature) records.toArray()[0];
        assertEquals(
                "urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd",
                getSimpleLiteralValue(record, "identifier"));
    }

    @Test
    public void testScheme() throws IOException {
        Filter filter =
                FF.equals(
                        FF.property("dc:identifier/dc:value", CSWRecordDescriptor.NAMESPACES),
                        FF.literal("urn:uuid:6a3de50b-fa66-4b58-a0e6-ca146fdd18d4"));
        FeatureCollection records =
                store.getRecords(new Query("Record", filter), Transaction.AUTO_COMMIT);
        assertEquals(1, records.size());
        Feature record = (Feature) records.toArray()[0];
        assertEquals("http://www.digest.org/2.1", getSimpleLiteralScheme(record, "subject"));
    }

    @Test
    public void testSpatialFilterWorld() throws IOException {
        Filter filter = FF.bbox("", -90, -180, 90, 180, CSWRecordDescriptor.DEFAULT_CRS_NAME);
        FeatureCollection records =
                store.getRecords(new Query("Record", filter), Transaction.AUTO_COMMIT);
        // there are only 3 records with a bbox
        assertEquals(3, records.size());
    }

    @Test
    public void testMaxFeatures() throws IOException {
        Query query = new Query("Record");
        query.setMaxFeatures(2);

        FeatureCollection records = store.getRecords(query, Transaction.AUTO_COMMIT);
        assertEquals(2, records.size());
    }

    @Test
    public void testOffsetFeatures() throws IOException {
        Query queryAll = new Query("Record");
        FeatureCollection allRecords = store.getRecords(queryAll, Transaction.AUTO_COMMIT);
        int size = allRecords.size();
        assertEquals(12, size);

        // with an offset
        Query queryOffset = new Query("Record");
        queryOffset.setStartIndex(1);
        FeatureCollection offsetRecords = store.getRecords(queryOffset, Transaction.AUTO_COMMIT);
        assertEquals(size - 1, offsetRecords.size());

        // paged one, but towards the end so that we won't get a full page
        Query queryPaged = new Query("Record");
        queryPaged.setStartIndex(10);
        queryPaged.setMaxFeatures(3);
        FeatureCollection pagedRecords = store.getRecords(queryPaged, Transaction.AUTO_COMMIT);
        assertEquals(2, pagedRecords.size());
    }

    @Test
    public void testSortAscend() throws IOException {
        Query queryImage = new Query("Record");
        queryImage.setFilter(
                FF.equals(
                        FF.property("dc:type/dc:value", CSWRecordDescriptor.NAMESPACES),
                        FF.literal("http://purl.org/dc/dcmitype/Image")));
        queryImage.setSortBy(
                new SortBy[] {
                    new SortByImpl(
                            FF.property("dc:title/dc:value", CSWRecordDescriptor.NAMESPACES),
                            SortOrder.ASCENDING)
                });

        FeatureCollection records = store.getRecords(queryImage, Transaction.AUTO_COMMIT);
        // there are only 3 records with Image type
        assertEquals(3, records.size());

        // check they were sorted
        final List<String> values = collectElement(records, "title");
        assertEquals(3, values.size());
        assertEquals("Lorem ipsum", values.get(0));
        assertEquals("Lorem ipsum dolor sit amet", values.get(1));
        assertEquals("Vestibulum massa purus", values.get(2));
    }

    @Test
    public void testSortDescend() throws IOException {
        Query queryImage = new Query("Record");
        queryImage.setFilter(
                FF.equals(
                        FF.property("dc:type/dc:value", CSWRecordDescriptor.NAMESPACES),
                        FF.literal("http://purl.org/dc/dcmitype/Image")));
        queryImage.setSortBy(
                new SortBy[] {
                    new SortByImpl(
                            FF.property("dc:title/dc:value", CSWRecordDescriptor.NAMESPACES),
                            SortOrder.DESCENDING)
                });

        FeatureCollection records = store.getRecords(queryImage, Transaction.AUTO_COMMIT);
        // there are only 3 records with Image type
        assertEquals(3, records.size());

        // check they were sorted
        final List<String> values = collectElement(records, "title");
        assertEquals(3, values.size());
        assertEquals("Vestibulum massa purus", values.get(0));
        assertEquals("Lorem ipsum dolor sit amet", values.get(1));
        assertEquals("Lorem ipsum", values.get(2));
    }

    @Test
    public void testSortNatural() throws IOException {
        Query queryImage = new Query("Record");
        queryImage.setSortBy(new SortBy[] {SortBy.NATURAL_ORDER});

        FeatureCollection records = store.getRecords(queryImage, Transaction.AUTO_COMMIT);
        assertEquals(12, records.size());

        // check they were sorted
        final List<String> values = collectElement(records, "identifier");
        List<String> sorted = new ArrayList<String>(values);
        Collections.sort(sorted);
        assertEquals(sorted, values);
    }

    private List<String> collectElement(FeatureCollection records, final String property)
            throws IOException {
        final List<String> values = new ArrayList<String>();
        records.accepts(
                new FeatureVisitor() {

                    @Override
                    public void visit(Feature feature) {
                        ComplexAttribute ca = (ComplexAttribute) feature.getProperty(property);
                        String value = (String) ca.getProperty("value").getValue();
                        values.add(value);
                    }
                },
                null);
        return values;
    }

    @Test
    public void testLimitAttributes() throws IOException {
        Query query = new Query("Record");
        Filter typeDataset =
                FF.equals(
                        FF.property("dc:type/dc:value", CSWRecordDescriptor.NAMESPACES),
                        FF.literal("http://purl.org/dc/dcmitype/Dataset"));
        query.setFilter(typeDataset);
        query.setSortBy(
                new SortBy[] {
                    new SortByImpl(
                            FF.property("dc:subject/dc:value", CSWRecordDescriptor.NAMESPACES),
                            SortOrder.ASCENDING)
                });
        // select some properties we did not use for filtering and sorting
        query.setProperties(
                Arrays.asList(FF.property("dc:identifier", CSWRecordDescriptor.NAMESPACES)));

        FeatureCollection records = store.getRecords(query, Transaction.AUTO_COMMIT);
        assertEquals(3, records.size());

        // check the properties and collect their identifier
        final List<String> values = new ArrayList<String>();
        records.accepts(
                new FeatureVisitor() {

                    @Override
                    public void visit(Feature feature) {
                        // has the id
                        ComplexAttribute id = (ComplexAttribute) feature.getProperty("identifier");
                        assertNotNull(id);
                        String value = (String) id.getProperty("value").getValue();
                        values.add(value);

                        // only has the id
                        assertEquals(1, feature.getProperties().size());
                    }
                },
                null);

        // if they were actually sorted by subject, here is the expected identifier order
        assertEquals("urn:uuid:9a669547-b69b-469f-a11f-2d875366bbdc", values.get(0));
        assertEquals("urn:uuid:88247b56-4cbc-4df9-9860-db3f8042e357", values.get(1));
        assertEquals("urn:uuid:94bc9c83-97f6-4b40-9eb8-a8e8787a5c63", values.get(2));
    }

    @Test
    public void testGetDomain() throws IOException {
        Name name = new NameImpl(DC.NAMESPACE, "type");
        CloseableIterator<String> domain =
                store.getDomain(new NameImpl(CSW.NAMESPACE, "Record"), name);
        assertTrue(domain.hasNext());
        assertEquals("http://purl.org/dc/dcmitype/Dataset", domain.next());
        assertEquals("http://purl.org/dc/dcmitype/Image", domain.next());
        assertEquals("http://purl.org/dc/dcmitype/Service", domain.next());
        assertEquals("http://purl.org/dc/dcmitype/Text", domain.next());
        assertFalse(domain.hasNext());
        domain.close();
    }

    @Test
    public void testGetRepositoryItem() throws IOException {
        RepositoryItem item = store.getRepositoryItem("foo");
        assertNull(item);

        item = store.getRepositoryItem("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");
        assertNotNull(item);
        assertEquals("application/xml", item.getMime());
        String contents = IOUtils.toString(item.getContents(), "UTF-8");
        String expected =
                "This is a random comment that will show up only when fetching the repository item";
        assertTrue(contents.contains(expected));
    }
}
