/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import org.junit.Test;

public class SystemPropertiesStatusTest {

    String KEY = "TESTTESTTEST";

    String VALUE = "ABCDEF_TEST_TEST_TEST";

    @Test
    public void testSystemPropertiesStatus() {
        System.setProperty(KEY, VALUE);

        SystemPropertyStatus status = new SystemPropertyStatus() {
            @Override
            String getEnvironmentVariable(String envVar) {
                return "true";
            }
        };

        assertTrue(status.getMessage().isPresent());
        assertTrue(status.getMessage().get().contains(KEY));
        assertTrue(status.getMessage().get().contains(VALUE));
    }

    /** Tests the SystemPropertyStatusEnabledEnvironmentVar so it turns on/off the message (list of property vars). */
    @Test
    public void testEnabled() {
        final var VALUE = new ArrayList<String>();

        // create subclass of SystemPropertyStatus so we can change the value of the environment
        // variable.
        // VALUE empty -> null
        // otherwise its the first item in the VALUE
        // if the request is for a different environment var -> throw
        SystemPropertyStatus status = new SystemPropertyStatus() {
            @Override
            String getEnvironmentVariable(String envVar) {
                if (envVar.equals(SystemPropertyStatus.SystemPropertyStatusEnabledEnvironmentVar)) {
                    if (VALUE.isEmpty()) {
                        return null;
                    }
                    return VALUE.get(0);
                }
                throw new RuntimeException("bad var");
            }
        };

        VALUE.clear();
        VALUE.add("true");
        assertTrue(status.isShow());
        assertFalse(status.getMessage().isEmpty());

        VALUE.clear();
        VALUE.add("TRUE");
        assertTrue(status.isShow());
        assertFalse(status.getMessage().isEmpty());

        VALUE.clear();
        VALUE.add("FALSE");
        assertFalse(status.isShow());
        assertTrue(status.getMessage().get().startsWith("Java system properties hidden for security reasons."));

        VALUE.clear();
        VALUE.add("false");
        assertFalse(status.isShow());
        assertTrue(status.getMessage().get().startsWith("Java system properties hidden for security reasons."));

        // default -> false
        VALUE.clear();
        assertFalse(status.isShow());
        assertTrue(status.getMessage().get().startsWith("Java system properties hidden for security reasons."));

        // bad value -> false
        VALUE.clear();
        VALUE.add("maybe");
        assertFalse(status.isShow());
        assertTrue(status.getMessage().get().startsWith("Java system properties hidden for security reasons."));
    }
}
