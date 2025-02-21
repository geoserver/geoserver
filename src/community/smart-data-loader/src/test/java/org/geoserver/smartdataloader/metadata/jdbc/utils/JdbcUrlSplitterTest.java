package org.geoserver.smartdataloader.metadata.jdbc.utils;

import org.junit.Assert;
import org.junit.Test;

public class JdbcUrlSplitterTest {

    @Test
    public void testJdbcUrlSplitter() {
        JdbcUrlSplitter jdbcUrlSplitter = new JdbcUrlSplitter("jdbc:postgresql://localhost:5432/geoserver");
        Assert.assertEquals("postgresql", jdbcUrlSplitter.driverName);
        Assert.assertEquals("localhost", jdbcUrlSplitter.host);
        Assert.assertEquals("5432", jdbcUrlSplitter.port);
        Assert.assertEquals("geoserver", jdbcUrlSplitter.database);
        Assert.assertNull(jdbcUrlSplitter.params);
    }

    @Test
    public void testJdbcUrlSplitterWithParams() {
        String jdbcUrl =
                "jdbc:postgresql://localhost:5432/mydatabase?reWriteBatchedInserts=false&amp;sslmode=DISABLE&amp;binaryTransferEnable=bytea";
        JdbcUrlSplitter splitter = new JdbcUrlSplitter(jdbcUrl);

        Assert.assertEquals("postgresql", splitter.driverName);
        Assert.assertEquals("localhost", splitter.host);
        Assert.assertEquals("5432", splitter.port);
        Assert.assertEquals("mydatabase", splitter.database);
        Assert.assertNotNull(splitter.params);
    }

    @Test
    public void testJdbcUrlSplitterWithParams2() {
        String jdbcUrl =
                "jdbc:postgresql://localhost:5432/mydatabase;reWriteBatchedInserts=false&amp;sslmode=DISABLE&amp;binaryTransferEnable=bytea";
        JdbcUrlSplitter splitter = new JdbcUrlSplitter(jdbcUrl);

        Assert.assertEquals("postgresql", splitter.driverName);
        Assert.assertEquals("localhost", splitter.host);
        Assert.assertEquals("5432", splitter.port);
        Assert.assertEquals("mydatabase", splitter.database);
        Assert.assertNotNull(splitter.params);
    }
}
