/*
/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.junit.Assert.*;

import org.junit.Test;

/** @author Ian Schneider <ischneider@opengeo.org> */
public class MemoryImportStoreTest {

    @Test
    public void testIDManagement() throws Exception {
        ImportStore store = new MemoryImportStore();

        // verify base - first one is zero
        ImportContext zero = new ImportContext();
        store.add(zero);
        assertEquals(Long.valueOf(0), zero.getId());

        // try for zero again (less than current case - client out of sync)
        Long advanceId = store.advanceId(0L);
        assertEquals(Long.valueOf(1), advanceId);

        // and again for current (equals current case - normal mode)
        advanceId = store.advanceId(2L);
        assertEquals(Long.valueOf(2), advanceId);

        // now jump ahead (client advances case - server out of sync)
        advanceId = store.advanceId(666L);
        assertEquals(Long.valueOf(666), advanceId);

        // the next created import should be one higher
        ImportContext dumby = new ImportContext();
        store.add(dumby);
        assertEquals(Long.valueOf(667), dumby.getId());
    }
}
