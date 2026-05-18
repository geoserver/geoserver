/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.util;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

/** Tests for {@link ConfidentialLogger} */
public class ConfidentialLoggerTest {

    /** Ensure adjusting Spring levels does not cause errors */
    @Test
    public void testEnableDisable() throws Exception {
        Logger logger = LogManager.getLogger("org.springframework.web.HttpLogging");
        // ConfidentialLogger.SPRING_ORG_LEVELS is captured at class-init time, which may be earlier
        // than this test (if another test loaded ConfidentialLogger first — surefire ordering is
        // not deterministic across package layouts). Read the cached level so the restore assertion
        // matches what disable() actually puts back, not what the level happens to be right now.
        @SuppressWarnings("unchecked")
        Map<String, Level> cachedOrgLevels = (Map<String, Level>)
                FieldUtils.readDeclaredStaticField(ConfidentialLogger.class, "SPRING_ORG_LEVELS", true);
        Level cachedOrg = cachedOrgLevels.get(logger.getName());
        ConfidentialLogger.setEnabled(true);
        assertEquals(Level.DEBUG, logger.getLevel());
        ConfidentialLogger.setEnabled(false);
        assertEquals(cachedOrg, logger.getLevel());
    }
}
