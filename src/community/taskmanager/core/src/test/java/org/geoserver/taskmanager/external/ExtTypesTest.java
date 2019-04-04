/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geotools.feature.NameImpl;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ExtTypesTest extends AbstractTaskManagerTest {

    @Autowired ExtTypes extTypes;

    protected boolean setupDataDirectory() throws Exception {
        DATA_DIRECTORY.addWcs11Coverages();
        return true;
    }

    @Test
    public void testDbName() {
        List<String> domain = extTypes.dbName.getDomain(null);
        assertEquals(4, domain.size());
        assertEquals("myjndidb", domain.get(0));
        assertEquals("mypostgresdb", domain.get(1));
        assertEquals("testsourcedb", domain.get(2));
        assertEquals("testtargetdb", domain.get(3));
        assertFalse(extTypes.dbName.validate("doesntexist", null));
        assertTrue(extTypes.dbName.validate("myjndidb", null));
        assertTrue(extTypes.dbName.parse("myjndidb", null) instanceof DbSource);
        assertNull(extTypes.dbName.parse("doesntexist", null));
    }

    @Test
    public void testDbNameWithLogin() {
        login("someone", "pw", "ROLE_SOMEONE");
        List<String> domain = extTypes.dbName.getDomain(null);
        assertEquals(3, domain.size());
        assertEquals("mypostgresdb", domain.get(0));
        assertEquals("testsourcedb", domain.get(1));
        assertEquals("testtargetdb", domain.get(2));
        assertFalse(extTypes.dbName.validate("myjndidb", null));
        assertNull(extTypes.dbName.parse("myjndidb", null));

        login("someone_else", "pw", "ROLE_SOMEONE_ELSE");
        domain = extTypes.dbName.getDomain(null);
        assertEquals(3, domain.size());
        assertEquals("myjndidb", domain.get(0));
        assertEquals("mypostgresdb", domain.get(1));
        assertEquals("testsourcedb", domain.get(2));
        assertFalse(extTypes.dbName.validate("testtargetdb", null));
        assertNull(extTypes.dbName.parse("testtargetdb", null));

        logout();
    }

    @Test
    public void testTableName() {
        List<String> domain = extTypes.tableName.getDomain(Collections.singletonList("myjndidb"));
        assertEquals("", domain.get(0));

        final String tableName = "grondwaterlichamen_new";

        assertTrue(
                extTypes.tableName.validate("doesntexist", Collections.singletonList("myjndidb")));
        assertTrue(extTypes.tableName.validate(tableName, Collections.singletonList("myjndidb")));

        DbSource source = (DbSource) extTypes.dbName.parse("myjndidb", null);
        try (Connection conn = source.getDataSource().getConnection()) {
            try (ResultSet res = conn.getMetaData().getTables(null, null, tableName, null)) {
                Assume.assumeTrue(res.next());
            }
        } catch (SQLException e) {
            Assume.assumeTrue(false);
        }
        assertTrue(domain.contains(tableName));

        assertTrue(
                extTypes.tableName.parse(tableName, Collections.singletonList("myjndidb"))
                        instanceof DbTable);
        assertTrue(
                extTypes.tableName.parse("doesntexist", Collections.singletonList("myjndidb"))
                        instanceof DbTable);
    }

    @Test
    public void testExtGeoserver() {
        List<String> domain = extTypes.extGeoserver.getDomain(null);
        assertEquals(1, domain.size());
        assertEquals("mygs", domain.get(0));
        assertTrue(extTypes.extGeoserver.validate("mygs", null));
        assertFalse(extTypes.extGeoserver.validate("doesntexist", null));
        assertTrue(extTypes.extGeoserver.parse("mygs", null) instanceof ExternalGS);
        assertNull(extTypes.extGeoserver.parse("doesntexist", null));
    }

    @Test
    public void testInternalLayer() {
        List<String> domain = extTypes.internalLayer.getDomain(null);
        assertEquals(4, domain.size());
        assertEquals("wcs:BlueMarble", domain.get(0));
        assertEquals("wcs:DEM", domain.get(1));
        assertEquals("wcs:RotatedCad", domain.get(2));
        assertEquals("wcs:World", domain.get(3));
        assertTrue(extTypes.internalLayer.validate("wcs:BlueMarble", null));
        assertFalse(extTypes.internalLayer.validate("doesntexist", null));
        assertTrue(extTypes.internalLayer.parse("wcs:BlueMarble", null) instanceof LayerInfo);
        assertNull(extTypes.internalLayer.parse("doesntexist", null));
    }

    @Test
    public void testNames() {
        assertTrue(extTypes.name.validate("bla", null));
        assertTrue(extTypes.name.validate("gs:bla", null));
        assertFalse(extTypes.name.validate("doesntexist:bla", null));
        assertEquals(new NameImpl("http://geoserver.org", "bla"), extTypes.name.parse("bla", null));
        assertEquals(
                new NameImpl("http://geoserver.org", "bla"), extTypes.name.parse("gs:bla", null));
    }

    @Test
    public void testFileServices() {
        List<String> domain = extTypes.fileService.getDomain(null);
        assertEquals(2, domain.size());
        assertEquals("data-directory", domain.get(0));
        assertEquals("temp-directory", domain.get(1));
        assertTrue(extTypes.fileService.validate("data-directory", null));
        assertFalse(extTypes.fileService.validate("doesntexist", null));
        assertTrue(extTypes.fileService.parse("data-directory", null) instanceof FileService);
        assertNull(extTypes.fileService.parse("doesntexist", null));
    }

    @Test
    public void testFile() throws IOException {
        FileService service = (FileService) extTypes.fileService.parse("data-directory", null);
        try (InputStream is = new ByteArrayInputStream("test".getBytes())) {
            service.create("temp", is);
        }

        assertTrue(
                extTypes.file(true, false)
                        .validate("temp", Collections.singletonList("data-directory")));
        assertTrue(
                extTypes.file(true, false)
                        .validate("doesntexist", Collections.singletonList("data-directory")));
        assertTrue(
                extTypes.file(true, false)
                                .parse("temp", Collections.singletonList("data-directory"))
                        instanceof FileReference);
        assertNull(
                extTypes.file(true, false)
                        .parse("doesntexist", Collections.singletonList("data-directory")));
        assertTrue(
                extTypes.file(false, false)
                                .parse("doesntexist", Collections.singletonList("data-directory"))
                        instanceof FileReference);

        assertEquals(
                "temp.1",
                ((FileReference)
                                extTypes.file(false, false)
                                        .parse(
                                                "temp",
                                                Lists.newArrayList("data-directory", "true")))
                        .getNextVersion());
    }
}
