/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.geoserver.csw.CSWTestSupport;
import org.junit.Test;

public class InternalCatalogStoreTest extends CSWTestSupport {

    @Test
    public void testModifyMappingFiles() throws IOException, InterruptedException {

        // clear any existing mappings
        File root = testData.getDataDirectoryRoot();
        File csw = new File(root, "csw");
        if (csw.exists()) {
            csw.delete();
        }

        // get the store
        InternalCatalogStore store =
                applicationContext.getBean(
                        InternalCatalogStore
                                .class); // new InternalCatalogStore(this.getGeoServer());
        assertNotNull(store);

        // test if we have default mapping
        File record = new File(csw, "Record.properties");
        assertTrue(record.exists());

        assertNotNull(store.getMapping("Record"));
        assertNotNull(store.getMapping("Record").getElement("identifier.value"));

        assertNull(store.getMapping("Record").getElement("format.value"));

        // modify mapping file

        // wait one second, so the modification time will change and change is detected (on linux
        // the resolution is 1s instead of 1ms)
        Thread.sleep(1001);

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(record, true)));
        out.println("\nformat.value='img/jpeg'");
        out.close();

        // wait one second, that is exactly what it takes FileWatcher to update
        Thread.sleep(1001);

        // mapping should be automatically reloaded

        assertEquals(
                "img/jpeg",
                store.getMapping("Record").getElement("format.value").getContent().toString());
    }
}
