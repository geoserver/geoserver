/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import org.junit.BeforeClass;

public class MemoryMonitorDAOTest extends MonitorDAOTestSupport {

    @BeforeClass
    public static void createDAO() throws Exception {
        dao = new MemoryMonitorDAO();
        setUpData();
    }
}
