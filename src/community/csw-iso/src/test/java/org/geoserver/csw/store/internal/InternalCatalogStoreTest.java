/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import org.geoserver.csw.CSWTestSupport;
import org.junit.Test;

public class InternalCatalogStoreTest extends CSWTestSupport {

    @Test
    public void testMappingFiles() throws IOException, InterruptedException {

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
        File md = new File(csw, "MD_Metadata.properties");
        assertTrue(md.exists());

        assertNotNull(store.getMapping("Record"));
        assertNotNull(store.getMapping("MD_Metadata"));
        assertNotNull(store.getMapping("MD_Metadata").getElement("fileIdentifier.CharacterString"));
    }
}
