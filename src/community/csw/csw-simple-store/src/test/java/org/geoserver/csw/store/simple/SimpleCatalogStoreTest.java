package org.geoserver.csw.store.simple;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.geoserver.csw.records.CSWRecordTypes;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public class SimpleCatalogStoreTest extends TestCase {

    File root = new File("./src/test/resources/org/geoserver/csw/store/simple");
    SimpleCatalogStore store = new SimpleCatalogStore(root);

    public void testCreationExceptions() throws IOException {
        try {
            new SimpleCatalogStore(new File("./pom.xml"));
            fail("Should have failed, the reference is not a directory");
        } catch(IllegalArgumentException e) {
            // fine
        }
        
        File f = new File("./target/notThere");
        if(f.exists()) {
            FileUtils.deleteDirectory(f);
        }
        try {
            new SimpleCatalogStore(f);
            fail("Should have failed, the reference is not there!");
        } catch(IllegalArgumentException e) {
            // fine
        }
    }
    
    public void testFeatureTypes() throws IOException {
        FeatureType[] fts = store.getRecordSchemas();
        assertEquals(1, fts.length);
        assertEquals(CSWRecordTypes.RECORD, fts[0]);
    }
    
    public void testReadAllRecords() throws IOException {
        FeatureCollection records = store.getRecords(Query.ALL, Transaction.AUTO_COMMIT);
        int fileCount = root.list(new RegexFileFilter("Record_.*\\.xml")).length;
        assertEquals(fileCount, records.size());
        
        FeatureIterator<Feature> fi = records.features();
        try {
            while(fi.hasNext()) {
                Feature f = fi.next();
                
                // check the id has be read and matches the expected format (given what we have in the files)
                ComplexAttribute ida = (ComplexAttribute) f.getProperty("identifier");
                String id = (String) ida.getProperty("value").getValue();
                assertNotNull(id);
                assertTrue(id.matches("urn:uuid:[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"));
                
                // the other thing we always have in these records is the type
                Attribute type = (Attribute) f.getProperty("type");
                assertNotNull(type);
                assertNotNull(type.getValue());
            }
        } finally {
            fi.close();
        }
    }
}
