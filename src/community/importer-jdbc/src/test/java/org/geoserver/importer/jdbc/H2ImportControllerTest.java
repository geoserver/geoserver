/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;

import org.geoserver.importer.rest.ImportControllerTest;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;

public class H2ImportControllerTest extends ImportControllerTest {

    @Test
    public void testJdbcImportStore() {
        assertThat(importer.getStore(), CoreMatchers.instanceOf(JDBCImportStore.class));
    }

    @Override
    @Ignore
    @Test
    public void testPutWithId() throws Exception {
        super.testPutWithId();
    }

    @Override
    @Ignore
    @Test
    public void testPutWithIdNoContentType() throws Exception {
        super.testPutWithIdNoContentType();
    }
}
