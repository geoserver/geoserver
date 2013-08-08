package org.geoserver.csw.store.internal;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.geoserver.csw.CSWTestSupport;
import org.junit.Test;

public class GeoServerInternalCatalogStoreTest extends CSWTestSupport {
        
    @Test
    public void testModifyMappingFiles() throws IOException, InterruptedException {
        
        //test empty mappings
        InternalCatalogStore store = new GeoServerInternalCatalogStore(this.getGeoServer());
        
        assertNull(store.getMapping("MD_Metadata").getElement("fileIdentifier.CharacterString"));
        assertNull(store.getMapping("Record").getElement("identifier.value"));        
        
        // copy all mappings into the data directory
        File root = testData.getDataDirectoryRoot();
        File csw = new File(root, "csw");
        File records = new File("./src/main/resources/org/geoserver/csw/store/internal");
        FileUtils.copyDirectory(records, csw); 
        
        //wait a second, that is exactly what it takes FileWatcher to update
        Thread.sleep(1001);
        
        //now there should be mappings
        
        assertNotNull(store.getMapping("MD_Metadata").getElement("fileIdentifier.CharacterString"));
        assertNotNull(store.getMapping("Record").getElement("identifier.value")); 
        
        assertNull(store.getMapping("Record").getElement("format.value"));
        
        //modify mapping file
        
        File record = new File(csw, "Record.properties");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(record, true)));
        out.println("\nformat.value='img/jpeg'");
        out.close();     

        //wait a second, that is exactly what it takes FileWatcher to update
        Thread.sleep(1001);
                
        //mapping should be automatically reloaded
        
        assertEquals( "img/jpeg", store.getMapping("Record").getElement("format.value").getContent().toString());
    }
}
