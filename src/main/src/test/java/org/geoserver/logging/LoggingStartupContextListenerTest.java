/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.servlet.ServletContextEvent;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.mockrunner.mock.web.MockServletContext;

public class LoggingStartupContextListenerTest {

    @Before
    public void cleanupLoggers() {
        LogManager.resetConfiguration();
    }
    
    @Test
    public void testLogLocationFromServletContext() throws Exception {
        File tmp = File.createTempFile("log", "tmp", new File("target"));
        tmp.delete();
        tmp.mkdirs();

        File logs = new File(tmp, "logs");
        assertTrue(logs.mkdirs());

        FileUtils.copyURLToFile(getClass().getResource("logging.xml"), new File(tmp, "logging.xml"));
        
        MockServletContext context = new MockServletContext();
        context.setInitParameter("GEOSERVER_DATA_DIR", tmp.getPath());
        context.setInitParameter("GEOSERVER_LOG_LOCATION", new File(tmp, "foo.log").getAbsolutePath());

        Logger logger = Logger.getRootLogger();
        assertNull("Expected geoserverlogfile to be null.  But was: "+logger.getAppender("geoserverlogfile"), logger.getAppender("geoserverlogfile"));

        String rel = System.getProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL);
        System.setProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL, "false");
        try {
            new LoggingStartupContextListener().contextInitialized(new ServletContextEvent(context));
        }
        finally {
            System.setProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL, "rel");
        }

        Appender appender = logger.getAppender("geoserverlogfile"); 
        assertNotNull(appender);
        assertTrue(appender instanceof FileAppender);

        assertEquals(new File(tmp, "foo.log").getCanonicalPath().toLowerCase(), ((FileAppender)appender).getFile().toLowerCase());
    }
}
