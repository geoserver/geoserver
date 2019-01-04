/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** @author Ian Schneider <ischneider@boundlessgeo.com> */
@RunWith(Parameterized.class)
public class InitDropTest {
    JDBCConfigTestSupport.DBConfig dbConfig;

    JDBCConfigTestSupport testSupport;

    public InitDropTest(JDBCConfigTestSupport.DBConfig dbConfig) {
        this.dbConfig = dbConfig;
        testSupport = new JDBCConfigTestSupport(dbConfig);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return JDBCConfigTestSupport.parameterizedDBConfigs();
    }

    public void assertScript(String script) throws IOException {
        Logger logger = Logger.getAnonymousLogger();
        logger.setLevel(Level.WARNING);
        logger.addHandler(
                new Handler() {

                    {
                        setLevel(Level.WARNING);
                    }

                    @Override
                    public void close() throws SecurityException {}

                    @Override
                    public void flush() {}

                    @Override
                    public void publish(LogRecord lr) {
                        Assert.fail(lr.getMessage());
                    }
                });
        try {
            logger.warning("testing123");
            Assert.fail("test assumption failure");
        } catch (AssertionError expected) {
            // pass
        }
        testSupport.runScript(script, logger, true);
    }

    @Test
    public void testInitDrop() throws Exception {
        testSupport.setUp(); // clear state
        assertScript(dbConfig.getDropScript());
        assertScript(dbConfig.getInitScript());
    }
}
