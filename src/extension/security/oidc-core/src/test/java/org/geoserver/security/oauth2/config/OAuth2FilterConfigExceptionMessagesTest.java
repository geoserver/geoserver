/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.geoserver.platform.exception.GeoServerExceptions;
import org.junit.Test;

/**
 * Every error id declared on {@link OAuth2FilterConfigException} must resolve to a human-readable message.
 *
 * <p>Nothing else checks this. {@code GeoServerExceptions} looks messages up under the throwing class's canonical or
 * simple name, so a key prefix that names no existing class silently yields no message at all and the administrator is
 * shown the bare id — {@code MS_TENANT_ID_INVALID} rather than a sentence. The module's message bundle carried such a
 * prefix ({@code GeoServerOAuth2FilterConfigException}, a class that exists only in the 2.28.x community module) for
 * every one of its keys, so the validator's entire vocabulary was unreachable.
 */
public class OAuth2FilterConfigExceptionMessagesTest {

    @Test
    public void everyErrorIdHasAMessage() throws Exception {
        List<String> unresolved = new ArrayList<>();
        List<String> ids = declaredErrorIds();
        assertFalse("no error ids found by reflection — the test is not exercising anything", ids.isEmpty());

        for (String id : ids) {
            String message = GeoServerExceptions.localize(new OAuth2FilterConfigException(id), Locale.ENGLISH);
            if (message == null || message.isBlank() || message.equals(id)) {
                unresolved.add(id);
            }
        }
        assertEquals("error ids with no message in GeoServerException.properties", List.of(), unresolved);
    }

    @Test
    public void tenantIdMessageNamesTheField() {
        String message = GeoServerExceptions.localize(
                new OAuth2FilterConfigException(OAuth2FilterConfigException.MS_TENANT_ID_INVALID), Locale.ENGLISH);

        assertNotNull(message);
        assertTrue(
                "the message an administrator sees should name the field, but was: " + message,
                message.contains("Tenant ID"));
    }

    private static List<String> declaredErrorIds() throws Exception {
        List<String> ids = new ArrayList<>();
        for (Field f : OAuth2FilterConfigException.class.getDeclaredFields()) {
            if (Modifier.isPublic(f.getModifiers())
                    && Modifier.isStatic(f.getModifiers())
                    && f.getType() == String.class) {
                ids.add((String) f.get(null));
            }
        }
        return ids;
    }
}
