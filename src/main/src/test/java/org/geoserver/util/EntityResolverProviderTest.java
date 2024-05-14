/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import org.junit.Test;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EntityResolverProviderTest {

    @Test
    public void testAllowListDefaults() throws Exception {
        // include defaults if allow list is empty
        String allowed[] = EntityResolverProvider.entityResolutionAllowlist("");
        assertNotNull("defaults for empty", allowed);
        assertEquals(4, allowed.length);
        assertTrue(Arrays.stream(allowed).anyMatch(AllowListEntityResolver.W3C::equals));
        assertTrue(Arrays.stream(allowed).anyMatch(AllowListEntityResolver.INSPIRE::equals));
        assertTrue(Arrays.stream(allowed).anyMatch(AllowListEntityResolver.OGC1::equals));
        assertTrue(Arrays.stream(allowed).anyMatch(AllowListEntityResolver.OGC2::equals));
    }

    @Test
    public void testAllowListUnrestriced() throws Exception {
        String allowed[] = EntityResolverProvider.entityResolutionAllowlist("*");
        assertNull("* for Unrestricted", allowed);

        allowed = EntityResolverProvider.entityResolutionAllowlist(null);
        assertNull("null for Unrestricted", allowed);
    }

    @Test
    public void testAllowListDomains() throws Exception {
        String allowed[] = EntityResolverProvider.entityResolutionAllowlist("how2map.com");
        // confirm allowed includes the defaults
        assertNotNull(allowed);
        assertEquals(5, allowed.length);
        assertTrue(Arrays.stream(allowed).anyMatch(AllowListEntityResolver.W3C::equals));
        assertTrue(Arrays.stream(allowed).anyMatch(AllowListEntityResolver.INSPIRE::equals));
        assertTrue(Arrays.stream(allowed).anyMatch(AllowListEntityResolver.OGC1::equals));
        assertTrue(Arrays.stream(allowed).anyMatch(AllowListEntityResolver.OGC2::equals));
        // in addition to the provided domain
        assertTrue(Arrays.stream(allowed).anyMatch("how2map.com"::equals));
    }

    @Test
    public void testNoWildcard() throws Exception {
        // AllowListEntityResolver uses '*' to allow all http content (null returned)
        String everything[] =
                EntityResolverProvider.entityResolutionAllowlist(
                        AllowListEntityResolver.UNRESTRICTED);
        assertNull("* allows everything", everything);

        // but wild cards such as `foo*bar` are not supported, strings are quoted and not intended
        // to be a RegEx
        String allowed[] = EntityResolverProvider.entityResolutionAllowlist("foo*bar");
        assertNotNull(allowed);
        assertTrue(Arrays.stream(allowed).anyMatch("foo*bar"::equals));
    }

    /**
     * Test behaviour of EntityResolveProvider in response to configuration.
     *
     * <p>EntityResolver returns {@code null} when the provided URI is *allowed*, Returns an
     * Inputstream if the content is provided, or throws an Exception if the URI is not allowed.
     */
    @Test
    public void testEntityResolverDefaultBehaviour() throws Exception {
        EntityResolverProvider provider = new EntityResolverProvider(null);
        provider.setEntityResolver(new AllowListEntityResolver(null));
        EntityResolver resolver = provider.getEntityResolver();

        // Confirm schema is available from public location
        // (this is a default from AllowListEntiryResolver)
        InputSource filter =
                resolver.resolveEntity(null, "http://schemas.opengis.net/filter/1.1.0/filter.xsd");
        assertNull("Public Filter 1.1.0 connection allowed", filter);

        // Confirm schema is available from jars, as is the case for those included in GeoTools
        InputSource filterJar =
                resolver.resolveEntity(
                        null, "jar:file:/some/path/gs-main.jar!schemas/filter/1.1.0/filter.xsd");
        assertNull("JAR Filter 1.1.0 connection allowed", filterJar);

        // Confirm schema is available when war is unpacked into JBoss virtual filesystem
        InputSource filterJBoss =
                resolver.resolveEntity(
                        null,
                        "vfsfile:/home/userone/jboss-eap-5.1/jboss-as/server/default_WAR/deploy/geoserver.war/WEB-INF/lib/gs-main.jar!/filter/1.1.0/filter.xsd");
        assertNull("JBoss Virtual File System Filter 1.1.0 connection allowed", filterJBoss);

        // confirm schema CANNOT be accessed from a random website http address
        // (such as an external geoserver location mentioned below)
        try {
            InputSource external =
                    resolver.resolveEntity(
                            null,
                            "https://how2map.geocat.live/geoserver/schemas/wfs/1.0.0/WFS-basic.xsd");
            assertNotNull("Website Filter 1.1.0 not allowed", external);
            fail("Filter 1.1.0 is should not be provided built-in");
        } catch (SAXException e) {
            // Confirm the exception is clear, and contains the URI for folks to troubleshoot their
            // xml document
            assertTrue(
                    "External XSD not allowed",
                    e.getMessage().startsWith("Entity resolution disallowed for"));
            assertTrue(
                    "External XSD not allowed",
                    e.getMessage()
                            .contains(
                                    "https://how2map.geocat.live/geoserver/schemas/wfs/1.0.0/WFS-basic.xsd"));
        }
    }
}
