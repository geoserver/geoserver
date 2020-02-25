/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;

import org.geoserver.importer.ImporterTest;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class H2ImporterTest extends ImporterTest {

    @Test
    public void testJdbcImportStore() {
        assertThat(importer.getStore(), CoreMatchers.instanceOf(JDBCImportStore.class));
    }
}
