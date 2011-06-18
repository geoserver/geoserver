package org.geoserver.monitor;

import org.junit.BeforeClass;

public class MemoryMonitorDAOTest extends MonitorDAOTestSupport {

    @BeforeClass
    public static void createDAO() throws Exception {
        dao = new MemoryMonitorDAO();
        setUpData();
    }
}
