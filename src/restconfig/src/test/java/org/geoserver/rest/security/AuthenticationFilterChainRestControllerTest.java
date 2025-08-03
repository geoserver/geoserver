/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.rest.security.xml.AuthFilterChainCollection;
import org.geoserver.rest.security.xml.AuthFilterChainFilters;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.HtmlLoginFilterChain;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

public class AuthenticationFilterChainRestControllerTest extends GeoServerTestSupport {

    private static final String DEFAULT_CHAIN_NAME = "default";
    private static final String TEST_CHAIN_NAME_PREFIX = "TEST-";
    private static final List<String> TEST_FILTERS = List.of("basic", "anonymous");
    private static final List<String> NEW_TEST_FILTERS = List.of("basic");
    private static final List<String> PATTERNS = List.of("/test/path1/*", "/test/path2/*");
    private static final List<String> NEW_PATTERNS = List.of("/test/path1/*");
    private static final boolean ALLOW_SESSION_CREATION_FLAG = true;
    private static final boolean DISABLED_FLAG = true;
    private static final boolean REQUIRE_SSL_FLAG = true;
    private static final boolean MATCH_HTTP_METHOD_FLAG = true;
    private static final String CLASS_NAME = HtmlLoginFilterChain.class.getName();

    private AuthenticationFilterChainRestController controller;

    @Override
    @Before
    public void oneTimeSetUp() throws Exception {
        setValidating(true);
        super.oneTimeSetUp();
        GeoServerSecurityManager securityManager = applicationContext.getBean(GeoServerSecurityManager.class);
        controller = new AuthenticationFilterChainRestController(securityManager);
    }

    private void setAdminUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearUser() {
        SecurityContextHolder.clearContext();
    }

    // ---------- helpers: XML marshalling identical to controller ----------

    private static void configureAliases(XStreamPersister xp) {
        XStream xs = xp.getXStream();

        xs.aliasSystemAttribute(null, "class");
        xs.aliasSystemAttribute(null, "resolves-to");

        xs.allowTypesByWildcard(new String[] {"org.geoserver.rest.security.xml.*"});

        xs.alias("filterChain", AuthFilterChainCollection.class);
        xs.addImplicitCollection(AuthFilterChainCollection.class, "chains", "filters", AuthFilterChainFilters.class);

        xs.alias("filters", AuthFilterChainFilters.class);
        xs.aliasField("class", AuthFilterChainFilters.class, "clazz");
        xs.aliasAttribute(AuthFilterChainFilters.class, "requireSSL", "ssl");

        xs.useAttributeFor(AuthFilterChainFilters.class, "name");
        xs.useAttributeFor(AuthFilterChainFilters.class, "clazz");
        xs.useAttributeFor(AuthFilterChainFilters.class, "path");
        xs.useAttributeFor(AuthFilterChainFilters.class, "disabled");
        xs.useAttributeFor(AuthFilterChainFilters.class, "allowSessionCreation");
        xs.useAttributeFor(AuthFilterChainFilters.class, "requireSSL");
        xs.useAttributeFor(AuthFilterChainFilters.class, "matchHTTPMethod");
        xs.useAttributeFor(AuthFilterChainFilters.class, "interceptorName");
        xs.useAttributeFor(AuthFilterChainFilters.class, "exceptionTranslationName");
        xs.useAttributeFor(AuthFilterChainFilters.class, "roleFilterName");

        xs.addImplicitCollection(AuthFilterChainFilters.class, "filters", "filter", String.class);
    }

    private static String toXml(AuthFilterChainFilters dto) throws Exception {
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        configureAliases(xp);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        xp.save(dto, bos);
        return bos.toString(StandardCharsets.UTF_8);
    }

    private static AuthFilterChainFilters fromXmlChain(String xml) throws Exception {
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        configureAliases(xp);
        return xp.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), AuthFilterChainFilters.class);
    }

    private static AuthFilterChainCollection fromXmlCollection(String xml) throws Exception {
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        configureAliases(xp);
        return xp.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), AuthFilterChainCollection.class);
    }

    private static String joinPatterns(List<String> patterns) {
        return String.join(",", patterns);
    }

    private static AuthFilterChainFilters newDTO(String name) {
        AuthFilterChainFilters dto = new AuthFilterChainFilters();
        dto.setName(name);
        dto.setClazz(CLASS_NAME);
        dto.setPath(joinPatterns(PATTERNS));
        dto.setAllowSessionCreation(ALLOW_SESSION_CREATION_FLAG);
        dto.setDisabled(DISABLED_FLAG);
        dto.setRequireSSL(REQUIRE_SSL_FLAG);
        dto.setMatchHTTPMethod(MATCH_HTTP_METHOD_FLAG);
        // typical subclass attributes
        dto.setInterceptorName("interceptor");
        dto.setExceptionTranslationName("exception");
        dto.setFilters(new ArrayList<>(TEST_FILTERS));
        return dto;
    }

    private static AuthFilterChainFilters updatedDTO(AuthFilterChainFilters base) {
        AuthFilterChainFilters dto = new AuthFilterChainFilters();
        dto.setName(base.getName());
        dto.setClazz(base.getClazz());
        dto.setPath(joinPatterns(NEW_PATTERNS));
        dto.setAllowSessionCreation(!ALLOW_SESSION_CREATION_FLAG);
        dto.setDisabled(!DISABLED_FLAG);
        dto.setRequireSSL(!REQUIRE_SSL_FLAG);
        dto.setMatchHTTPMethod(!MATCH_HTTP_METHOD_FLAG);
        dto.setInterceptorName("interceptor");
        dto.setExceptionTranslationName("exception");
        dto.setFilters(new ArrayList<>(NEW_TEST_FILTERS));
        return dto;
    }

    private static MockHttpServletRequest xmlRequest(String xml) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setContentType(MediaType.APPLICATION_XML_VALUE);
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        req.setContent(xml.getBytes(StandardCharsets.UTF_8));
        return req;
    }

    // ---------- tests ----------

    @Test
    public void testListFilterChains_AsXml() throws Exception {
        setAdminUser();
        try {
            ResponseEntity<String> resp = controller.getAllXml();
            assertEquals(200, resp.getStatusCodeValue());
            String xml = resp.getBody();
            assertNotNull(xml);

            AuthFilterChainCollection col = fromXmlCollection(xml);
            assertNotNull(col);
            boolean found = col.getChains().stream().anyMatch(c -> DEFAULT_CHAIN_NAME.equals(c.getName()));
            assertTrue("default chain should be present", found);
        } finally {
            clearUser();
        }
    }

    @Test
    public void testViewFilterChain_AsXml() throws Exception {
        setAdminUser();
        try {
            ResponseEntity<String> resp = controller.getOneXml(DEFAULT_CHAIN_NAME);
            assertEquals(200, resp.getStatusCodeValue());
            AuthFilterChainFilters dto = fromXmlChain(resp.getBody());
            assertNotNull(dto);
            assertEquals(DEFAULT_CHAIN_NAME, dto.getName());
        } finally {
            clearUser();
        }
    }

    @Test(expected = AuthenticationFilterChainRestController.FilterChainNotFound.class)
    public void testViewFilterChain_Unknown() {
        setAdminUser();
        try {
            controller.getOneXml("UnknownName");
        } finally {
            clearUser();
        }
    }

    @Test
    public void testCreateFilterChain_Xml() throws Exception {
        setAdminUser();
        try {
            String name = TEST_CHAIN_NAME_PREFIX + UUID.randomUUID();
            AuthFilterChainFilters dto = newDTO(name);
            String body = toXml(dto);

            UriComponentsBuilder b = UriComponentsBuilder.fromPath("");
            ResponseEntity<String> created = controller.createOneXml(xmlRequest(body), null, b);
            assertEquals(201, created.getStatusCodeValue());

            ResponseEntity<String> view = controller.getOneXml(name);
            AuthFilterChainFilters got = fromXmlChain(view.getBody());

            assertEquals(dto.getName(), got.getName());
            assertEquals(dto.getClazz(), got.getClazz());
            assertEquals(dto.getPath(), got.getPath());
            assertEquals(dto.getFilters(), got.getFilters());
            assertEquals(dto.getInterceptorName(), got.getInterceptorName());
            assertEquals(dto.getExceptionTranslationName(), got.getExceptionTranslationName());
            assertEquals(dto.getAllowSessionCreation(), got.getAllowSessionCreation());
            assertEquals(dto.getDisabled(), got.getDisabled());
            assertEquals(dto.getRequireSSL(), got.getRequireSSL());
            assertEquals(dto.getMatchHTTPMethod(), got.getMatchHTTPMethod());
        } finally {
            clearUser();
        }
    }

    @Test(expected = AuthenticationFilterChainRestController.DuplicateChainName.class)
    public void testCreateFilterChain_DuplicateName() throws Exception {
        setAdminUser();
        try {
            String name = TEST_CHAIN_NAME_PREFIX + UUID.randomUUID();
            AuthFilterChainFilters dto = newDTO(name);
            String body = toXml(dto);

            UriComponentsBuilder b = UriComponentsBuilder.fromPath("");
            controller.createOneXml(xmlRequest(body), null, b);
            controller.createOneXml(xmlRequest(body), null, b); // should throw DuplicateChainName
        } finally {
            clearUser();
        }
    }

    @Test
    public void testUpdateFilterChain_Xml() throws Exception {
        setAdminUser();
        try {
            String name = TEST_CHAIN_NAME_PREFIX + UUID.randomUUID();
            AuthFilterChainFilters dto = newDTO(name);
            controller.createOneXml(xmlRequest(toXml(dto)), null, UriComponentsBuilder.fromPath(""));

            AuthFilterChainFilters updated = updatedDTO(dto);
            ResponseEntity<String> updatedResp = controller.updateOneXml(name, xmlRequest(toXml(updated)), null);
            assertEquals(200, updatedResp.getStatusCodeValue());

            ResponseEntity<String> view = controller.getOneXml(name);
            AuthFilterChainFilters got = fromXmlChain(view.getBody());

            assertEquals(updated.getName(), got.getName());
            assertEquals(updated.getClazz(), got.getClazz());
            assertEquals(updated.getPath(), got.getPath());
            assertEquals(updated.getFilters(), got.getFilters());
            assertEquals(updated.getAllowSessionCreation(), got.getAllowSessionCreation());
            assertEquals(updated.getDisabled(), got.getDisabled());
            assertEquals(updated.getRequireSSL(), got.getRequireSSL());
            assertEquals(updated.getMatchHTTPMethod(), got.getMatchHTTPMethod());
        } finally {
            clearUser();
        }
    }

    @Test(expected = AuthenticationFilterChainRestController.BadRequest.class)
    public void testUpdateFilterChain_MismatchName() throws Exception {
        setAdminUser();
        try {
            String name = TEST_CHAIN_NAME_PREFIX + UUID.randomUUID();
            AuthFilterChainFilters dto = newDTO(name);
            controller.createOneXml(xmlRequest(toXml(dto)), null, UriComponentsBuilder.fromPath(""));

            // change DTO name but put with different path var
            AuthFilterChainFilters changedName = newDTO(TEST_CHAIN_NAME_PREFIX + UUID.randomUUID());
            controller.updateOneXml(name, xmlRequest(toXml(changedName)), null);
        } finally {
            clearUser();
        }
    }

    @Test
    public void testDeleteFilterChain() throws Exception {
        setAdminUser();
        try {
            String name = TEST_CHAIN_NAME_PREFIX + UUID.randomUUID();
            controller.createOneXml(xmlRequest(toXml(newDTO(name))), null, UriComponentsBuilder.fromPath(""));

            controller.deleteOne(name);

            try {
                controller.getOneXml(name);
                fail("Expected FilterChainNotFound after delete");
            } catch (AuthenticationFilterChainRestController.FilterChainNotFound expected) {
                // ok
            }
        } finally {
            clearUser();
        }
    }

    @Test(expected = AuthenticationFilterChainRestController.NothingToDelete.class)
    public void testDeleteFilterChain_Unknown() {
        setAdminUser();
        try {
            controller.deleteOne("UnknownName");
        } finally {
            clearUser();
        }
    }

    @Test(expected = AuthenticationFilterChainRestController.BadRequest.class)
    public void testDeleteFilterChain_cannotBeRemoved() {
        setAdminUser();
        try {
            controller.deleteOne("webLogout");
        } finally {
            clearUser();
        }
    }
}
