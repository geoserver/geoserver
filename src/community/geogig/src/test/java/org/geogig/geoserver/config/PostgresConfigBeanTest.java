/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Test;

public class PostgresConfigBeanTest {

    private static final String UTF8 = StandardCharsets.UTF_8.name();

    private void verifyURI(
            String expectedHost,
            @Nullable Integer expectedPort,
            String expectedDb,
            @Nullable String expectedSchema,
            String expectedRepoId,
            String expectedUser,
            String expectedPassword,
            final URI uri)
            throws UnsupportedEncodingException {
        // assert the URI is built correctly
        assertEquals("Unexpected HOST value", expectedHost, uri.getHost());
        if (expectedPort != null) {
            assertEquals("Unexpected PORT value", expectedPort.intValue(), uri.getPort());
        } else {
            assertEquals("Unexpected PORT value", 5432, uri.getPort());
        }
        // path component has Database name, schema, and repository ID pieces
        String path = uri.getPath();
        assertNotNull("PATH value from URI should not be bull", path);
        // path value of URI should be "/databaseName/schema/repoId"
        String[] paths = path.split("/");
        assertEquals("Unexpected number of path elements", 4, paths.length);
        assertTrue("Unexpected leading URI Path element", paths[0].isEmpty());
        assertEquals("Unexpected DATABASE value", expectedDb, paths[1]);
        if (expectedSchema != null) {
            assertEquals("Unexpected SCHEMA value", expectedSchema, paths[2]);
        } else {
            assertEquals("Unexpected SCHEMA value", "public", paths[2]);
        }
        assertEquals("Unexpected REPOSITORY ID value", expectedRepoId, paths[3]);
        // query component has the username and password values
        String rawQuery = uri.getRawQuery();
        assertTrue("URI missing USER query parameter", rawQuery.contains("user="));
        assertTrue("URI missing PASSWORD queyr parameter", rawQuery.contains("&password="));
        // should only have a single `&` in the raw query string.
        String[] queryParams = rawQuery.split("&");
        assertEquals("Unexpected number of query parameters", 2, queryParams.length);
        // now, parse out the 2 parameters and decode the values
        String userPair, passwordPair;
        if (queryParams[0].startsWith("user=")) {
            userPair = queryParams[0];
            passwordPair = queryParams[1];
        } else {
            passwordPair = queryParams[0];
            userPair = queryParams[0];
        }
        assertTrue("No parsable USER query parameter found", userPair.startsWith("user="));
        assertTrue(
                "No parsable PASSWORD query parameter found", passwordPair.startsWith("password="));
        // user and password values should be URLEncoded in the raw query, let's decode them and
        // verify
        String[] userParts = userPair.split("=");
        assertEquals("Malformed USER query parameter", 2, userParts.length);
        String[] passwordParts = passwordPair.split("=");
        assertEquals("Malformed PASSWORD query parameter", 2, passwordParts.length);
        String decodedUser = URLDecoder.decode(userParts[1], UTF8);
        assertEquals(
                "Unexpected URL decoded value for USER query parameter", expectedUser, decodedUser);
        String decodedPassword = URLDecoder.decode(passwordParts[1], UTF8);
        assertEquals(
                "Unexpected URL decoded value for PASSWORD query parameter",
                expectedPassword,
                decodedPassword);
    }

    private URI buildURIFromBean(
            String expectedHost,
            @Nullable Integer expectedPort,
            String expectedDb,
            @Nullable String expectedSchema,
            String expectedRepoId,
            String expectedUser,
            String expectedPassword)
            throws UnsupportedEncodingException, URISyntaxException {
        // build the bean
        PostgresConfigBean bean = PostgresConfigBean.newInstance();
        bean.setHost(expectedHost);
        bean.setPort(expectedPort);
        bean.setDatabase(expectedDb);
        bean.setSchema(expectedSchema);
        bean.setUsername(expectedUser);
        bean.setPassword(expectedPassword);
        // build the URI with a testRepo
        URI uri = bean.buildUriForRepo(expectedRepoId);
        return uri;
    }

    private void test(
            String expectedHost,
            @Nullable Integer expectedPort,
            String expectedDb,
            @Nullable String expectedSchema,
            String expectedRepoId,
            String expectedUser,
            String expectedPassword)
            throws UnsupportedEncodingException, URISyntaxException {
        // build the bean and get the URI
        URI uri =
                buildURIFromBean(
                        expectedHost,
                        expectedPort,
                        expectedDb,
                        expectedSchema,
                        expectedRepoId,
                        expectedUser,
                        expectedPassword);
        // assert the URI is built correctly
        verifyURI(
                expectedHost,
                expectedPort,
                expectedDb,
                expectedSchema,
                expectedRepoId,
                expectedUser,
                expectedPassword,
                uri);
    }

    private void verifyBean(
            PostgresConfigBean bean,
            String expectedHost,
            @Nullable Integer expectedPort,
            String expectedDb,
            @Nullable String expectedSchema,
            String expectedUser,
            String expectedPassword) {
        assertEquals("Unexpected HOST value", expectedHost, bean.getHost());
        if (expectedPort != null) {
            assertEquals("Unexpected PORT value", expectedPort, bean.getPort());
        } else {
            assertEquals("Unexpected PORT value", Integer.valueOf(5432), bean.getPort());
        }
        assertEquals("Unexpected DATABASE NAME value", expectedDb, bean.getDatabase());
        if (expectedSchema != null) {
            assertEquals("Unexpected SCHEMA value", expectedSchema, bean.getSchema());
        } else {
            assertEquals("Unexpected SCHEMA value", "public", bean.getSchema());
        }
        assertEquals("Unexpected USER value", expectedUser, bean.getUsername());
        assertEquals("Unexpected PASSWORD value", expectedPassword, bean.getPassword());
    }

    @Test
    public void testBuildUriForRepo() throws UnsupportedEncodingException, URISyntaxException {
        // set some typical values
        final String expectedHost = "testHost";
        final Integer expectedPort = Integer.valueOf(5432);
        final String expectedDb = "testDb";
        final String expectedSchema = "testSchema";
        final String expectedUser = "testUser";
        final String expectedPassword = "testPassword";
        final String expectedRepoId = "testRepoId";
        // test it
        test(
                expectedHost,
                expectedPort,
                expectedDb,
                expectedSchema,
                expectedRepoId,
                expectedUser,
                expectedPassword);
    }

    @Test
    public void testBuildUriForRepo_specialCharacters_hash()
            throws UnsupportedEncodingException, URISyntaxException {
        final String expectedHost = "testHost";
        final Integer expectedPort = Integer.valueOf(5432);
        final String expectedDb = "testDb";
        final String expectedSchema = "testSchema";
        final String expectedUser = "testUser";
        final String expectedPassword = "pass#word";
        final String expectedRepoId = "testRepoId";
        // test it
        test(
                expectedHost,
                expectedPort,
                expectedDb,
                expectedSchema,
                expectedRepoId,
                expectedUser,
                expectedPassword);
    }

    @Test
    public void testBuildUriForRepo_specialCharacters_ampersand()
            throws UnsupportedEncodingException, URISyntaxException {
        final String expectedHost = "testHost";
        final Integer expectedPort = Integer.valueOf(5432);
        final String expectedDb = "testDb";
        final String expectedSchema = "testSchema";
        final String expectedUser = "testUser";
        final String expectedPassword = "pass&word";
        final String expectedRepoId = "testRepoId";
        // test it
        test(
                expectedHost,
                expectedPort,
                expectedDb,
                expectedSchema,
                expectedRepoId,
                expectedUser,
                expectedPassword);
    }

    @Test
    public void testBuildUriForRepo_specialCharacters_multi()
            throws UnsupportedEncodingException, URISyntaxException {
        final String expectedHost = "testHost";
        final Integer expectedPort = Integer.valueOf(5432);
        final String expectedDb = "testDb";
        final String expectedSchema = "testSchema";
        final String expectedUser = "testUser";
        final String expectedPassword = "!@#$%^&*()";
        final String expectedRepoId = "testRepoId";
        // test it
        test(
                expectedHost,
                expectedPort,
                expectedDb,
                expectedSchema,
                expectedRepoId,
                expectedUser,
                expectedPassword);
    }

    @Test
    public void testBeanFromURI() throws UnsupportedEncodingException, URISyntaxException {
        final String expectedHost = "testHost";
        final Integer expectedPort = Integer.valueOf(5432);
        final String expectedDb = "testDb";
        final String expectedSchema = "testSchema";
        final String expectedUser = "testUser";
        final String expectedPassword = "password";
        final String expectedRepoId = "testRepoId";
        // build a URI
        URI uri =
                buildURIFromBean(
                        expectedHost,
                        expectedPort,
                        expectedDb,
                        expectedSchema,
                        expectedRepoId,
                        expectedUser,
                        expectedPassword);
        // get a bean
        PostgresConfigBean bean = PostgresConfigBean.from(uri);
        verifyBean(
                bean,
                expectedHost,
                expectedPort,
                expectedDb,
                expectedSchema,
                expectedUser,
                expectedPassword);
    }

    @Test
    public void testBeanFromURI_specialCharacters_hash()
            throws UnsupportedEncodingException, URISyntaxException {
        final String expectedHost = "testHost";
        final Integer expectedPort = Integer.valueOf(5432);
        final String expectedDb = "testDb";
        final String expectedSchema = "testSchema";
        final String expectedUser = "testUser";
        final String expectedPassword = "pass#word";
        final String expectedRepoId = "testRepoId";
        // build a URI
        URI uri =
                buildURIFromBean(
                        expectedHost,
                        expectedPort,
                        expectedDb,
                        expectedSchema,
                        expectedRepoId,
                        expectedUser,
                        expectedPassword);
        // get a bean
        PostgresConfigBean bean = PostgresConfigBean.from(uri);
        verifyBean(
                bean,
                expectedHost,
                expectedPort,
                expectedDb,
                expectedSchema,
                expectedUser,
                expectedPassword);
    }

    @Test
    public void testBeanFromURI_specialCharacters_ampersand()
            throws UnsupportedEncodingException, URISyntaxException {
        final String expectedHost = "testHost";
        final Integer expectedPort = Integer.valueOf(5432);
        final String expectedDb = "testDb";
        final String expectedSchema = "testSchema";
        final String expectedUser = "testUser";
        final String expectedPassword = "pass&word";
        final String expectedRepoId = "testRepoId";
        // build a URI
        URI uri =
                buildURIFromBean(
                        expectedHost,
                        expectedPort,
                        expectedDb,
                        expectedSchema,
                        expectedRepoId,
                        expectedUser,
                        expectedPassword);
        // get a bean
        PostgresConfigBean bean = PostgresConfigBean.from(uri);
        verifyBean(
                bean,
                expectedHost,
                expectedPort,
                expectedDb,
                expectedSchema,
                expectedUser,
                expectedPassword);
    }

    @Test
    public void testBeanFromURI_specialCharacters_multi()
            throws UnsupportedEncodingException, URISyntaxException {
        final String expectedHost = "testHost";
        final Integer expectedPort = Integer.valueOf(5432);
        final String expectedDb = "testDb";
        final String expectedSchema = "testSchema";
        final String expectedUser = "testUser";
        final String expectedPassword = "!@#$%^&*()";
        final String expectedRepoId = "testRepoId";
        // build a URI
        URI uri =
                buildURIFromBean(
                        expectedHost,
                        expectedPort,
                        expectedDb,
                        expectedSchema,
                        expectedRepoId,
                        expectedUser,
                        expectedPassword);
        // get a bean
        PostgresConfigBean bean = PostgresConfigBean.from(uri);
        verifyBean(
                bean,
                expectedHost,
                expectedPort,
                expectedDb,
                expectedSchema,
                expectedUser,
                expectedPassword);
    }
}
