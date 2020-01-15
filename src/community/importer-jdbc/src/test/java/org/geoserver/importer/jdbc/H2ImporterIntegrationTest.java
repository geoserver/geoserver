/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;

import org.geoserver.importer.rest.ImporterIntegrationTest;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

/** Getting some real exercise */
public class H2ImporterIntegrationTest extends ImporterIntegrationTest {

    @Test
    public void testJdbcImportStore() {
        assertThat(importer.getStore(), CoreMatchers.instanceOf(JDBCImportStore.class));
    }
}
