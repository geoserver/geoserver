/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

/** Tests for {@link ConfidentialLogger} */
public class ConfidentialLoggerTest {

    /** Ensure adjusting Spring levels does not cause errors */
    @Test
    public void testEnableDisable() {
        Logger logger = LogManager.getLogger("org.springframework.web.HttpLogging");
        Level lOrgLevel = logger.getLevel();
        ConfidentialLogger.setEnabled(true);
        assertEquals(logger.getLevel(), Level.DEBUG);
        ConfidentialLogger.setEnabled(false);
        assertEquals(logger.getLevel(), lOrgLevel);
    }
}
