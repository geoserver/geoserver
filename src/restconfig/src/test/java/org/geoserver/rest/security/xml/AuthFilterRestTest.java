package org.geoserver.rest.security.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.geoserver.security.config.AnonymousAuthenticationFilterConfig;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.config.CredentialsFromRequestHeaderFilterConfig;
import org.geoserver.security.config.DigestAuthenticationFilterConfig;
import org.geoserver.security.config.ExceptionTranslationFilterConfig;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig;
import org.geoserver.security.config.LogoutFilterConfig;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.RememberMeAuthenticationFilterConfig;
import org.geoserver.security.config.RequestHeaderAuthenticationFilterConfig;
import org.geoserver.security.config.RoleFilterConfig;
import org.geoserver.security.config.SSLFilterConfig;
import org.geoserver.security.config.SecurityContextPersistenceFilterConfig;
import org.geoserver.security.config.SecurityInterceptorFilterConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerBasicAuthenticationFilter;
import org.junit.Ignore;
import org.junit.Test;

/** Unable to test the XML conversion so I wrote this to manually test them It requires a Geoserver running */
public class AuthFilterRestTest {
    public static final String TEST_FILTER_NAME = "Test Filter";
    private static final String TEST_FILTER_ID = "123";
    private ObjectMapper objectMapper = new ObjectMapper();
    /// ///////////////////////////////////////////////////////////////////////
    /// XML Tests
    @Test
    public void testRoundTrip_AnonymousAuthenticationFilterConfig_Xml() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(AuthFilter.class, AnonymousAuthenticationFilterConfig.class);

        // Create an instance of AnonymousAuthenticationFilterConfig
        AnonymousAuthenticationFilterConfig originalConfig = new AnonymousAuthenticationFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        AnonymousAuthenticationFilterConfig newConfig = (AnonymousAuthenticationFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    public void testRoundTrip_BasicAuthenticationFilterConfig_Xml() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(AuthFilter.class, BasicAuthenticationFilterConfig.class);

        // Create an instance of BasicAuthenticationFilterConfig
        BasicAuthenticationFilterConfig originalConfig = new BasicAuthenticationFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        BasicAuthenticationFilterConfig newConfig = (BasicAuthenticationFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    public void testRoundTrip_CredentialsFromRequestHeaderFilterConfig_Xml() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(AuthFilter.class, CredentialsFromRequestHeaderFilterConfig.class);

        // Create an instance of CredentialsFromRequestHeaderFilterConfig
        CredentialsFromRequestHeaderFilterConfig originalConfig = new CredentialsFromRequestHeaderFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        CredentialsFromRequestHeaderFilterConfig newConfig =
                (CredentialsFromRequestHeaderFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    public void testRoundTrip_DigestAuthenticationFilterConfig_Xml() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(AuthFilter.class, DigestAuthenticationFilterConfig.class);

        // Create an instance of DigestAuthenticationFilterConfig
        DigestAuthenticationFilterConfig originalConfig = new DigestAuthenticationFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        DigestAuthenticationFilterConfig newConfig = (DigestAuthenticationFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    public void testRoundTrip_ExceptionTranslationFilterConfig_Xml() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(AuthFilter.class, ExceptionTranslationFilterConfig.class);

        // Create an instance of ExceptionTranslationFilterConfig
        ExceptionTranslationFilterConfig originalConfig = new ExceptionTranslationFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        ExceptionTranslationFilterConfig newConfig = (ExceptionTranslationFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    public void testRoundTrip_LogoutFilterConfig_Xml() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(AuthFilter.class, LogoutFilterConfig.class);

        // Create an instance of LogoutFilterConfig
        LogoutFilterConfig originalConfig = new LogoutFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        LogoutFilterConfig newConfig = (LogoutFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    @Ignore
    public void testRoundTrip_J2eeAuthenticationBaseFilterConfig_Xml() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(AuthFilter.class, J2eeAuthenticationBaseFilterConfig.class);

        // Create an instance of J2eeAuthenticationBaseFilterConfig
        J2eeAuthenticationBaseFilterConfig originalConfig = new J2eeAuthenticationBaseFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        J2eeAuthenticationBaseFilterConfig newConfig = (J2eeAuthenticationBaseFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    @Ignore
    public void testRoundTrip_RequestHeaderAuthenticationFilterConfig_Xml() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(AuthFilter.class, RequestHeaderAuthenticationFilterConfig.class);

        // Create an instance of J2eeAuthenticationBaseFilterConfig
        RequestHeaderAuthenticationFilterConfig originalConfig = new RequestHeaderAuthenticationFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        RequestHeaderAuthenticationFilterConfig newConfig =
                (RequestHeaderAuthenticationFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    public void testRoundTrip_RememberMeAuthenticationFilterConfig_Xml() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(AuthFilter.class, RememberMeAuthenticationFilterConfig.class);

        // Create an instance of J2eeAuthenticationBaseFilterConfig
        RememberMeAuthenticationFilterConfig originalConfig = new RememberMeAuthenticationFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        RememberMeAuthenticationFilterConfig newConfig = (RememberMeAuthenticationFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    public void testRoundTrip_RoleFilterConfig_Xml() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(AuthFilter.class, RoleFilterConfig.class);

        // Create an instance of J2eeAuthenticationBaseFilterConfig
        RoleFilterConfig originalConfig = new RoleFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        RoleFilterConfig newConfig = (RoleFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    public void testRoundTrip_SecurityContextPersistenceFilterConfig_Xml() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(AuthFilter.class, SecurityContextPersistenceFilterConfig.class);

        // Create an instance of J2eeAuthenticationBaseFilterConfig
        SecurityContextPersistenceFilterConfig originalConfig = new SecurityContextPersistenceFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        SecurityContextPersistenceFilterConfig newConfig =
                (SecurityContextPersistenceFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    public void testRoundTrip_SecurityInterceptorFilterConfig_Xml() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(AuthFilter.class, SecurityInterceptorFilterConfig.class);

        // Create an instance of J2eeAuthenticationBaseFilterConfig
        SecurityInterceptorFilterConfig originalConfig = new SecurityInterceptorFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        SecurityInterceptorFilterConfig newConfig = (SecurityInterceptorFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    public void testRoundTrip_SSLFilterConfig_Xml() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(AuthFilter.class, SSLFilterConfig.class);

        // Create an instance of J2eeAuthenticationBaseFilterConfig
        SSLFilterConfig originalConfig = new SSLFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        SSLFilterConfig newConfig = (SSLFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    public void testRoundTrip_UsernamePasswordAuthenticationFilterConfig_Xml() throws JAXBException {
        JAXBContext context =
                JAXBContext.newInstance(AuthFilter.class, UsernamePasswordAuthenticationFilterConfig.class);

        // Create an instance of J2eeAuthenticationBaseFilterConfig
        UsernamePasswordAuthenticationFilterConfig originalConfig = new UsernamePasswordAuthenticationFilterConfig();
        originalConfig.setId("TEST_FILTER_ID");
        originalConfig.setName("TEST_FILTER_NAME");
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to XML
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(filter, writer);
        String xml = writer.toString();

        // Unmarshal back to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        AuthFilter newFilter = (AuthFilter) unmarshaller.unmarshal(reader);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());

        // Verify the configuration object
        UsernamePasswordAuthenticationFilterConfig newConfig =
                (UsernamePasswordAuthenticationFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    /// ///////////////////////////////////////////////////////////////////////
    /// Json Tests
    @Test
    public void testRoundTrip_AnonymousAuthenticationFilterConfig_Json() throws JsonProcessingException {
        // Create an instance of J2eeAuthenticationBaseFilterConfig
        AnonymousAuthenticationFilterConfig originalConfig = new AnonymousAuthenticationFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to JSON
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        AnonymousAuthenticationFilterConfig newConfig = (AnonymousAuthenticationFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    public void testRoundTrip_BasicAuthenticationFilterConfig_Json() throws JsonProcessingException {
        // Create an instance of BasicAuthenticationFilterConfig
        BasicAuthenticationFilterConfig originalConfig = new BasicAuthenticationFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        originalConfig.setUseRememberMe(true);
        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to JSON
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        BasicAuthenticationFilterConfig newConfig = (BasicAuthenticationFilterConfig) newFilter.getConfig();
        assertEquals(originalConfig.isUseRememberMe(), newConfig.isUseRememberMe());
    }

    @Test
    public void testRoundTrip_CredentialsFromRequestHeaderFilterConfig_Json() throws JsonProcessingException {
        String passwordRegex = "PasswordRegex";
        String passwordHeader = "PasswordHeader";
        String userHeader = "userHeader";
        String userRegex = "UserRegex";

        // Create an instance of CredentialsFromRequestHeaderFilterConfig
        CredentialsFromRequestHeaderFilterConfig originalConfig = new CredentialsFromRequestHeaderFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        originalConfig.setParseAsUriComponents(true);
        originalConfig.setPasswordRegex(passwordRegex);
        originalConfig.setPasswordHeaderName(passwordHeader);
        originalConfig.setUserNameRegex(userRegex);
        originalConfig.setUserNameHeaderName(userHeader);

        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to JSON
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        CredentialsFromRequestHeaderFilterConfig newConfig =
                (CredentialsFromRequestHeaderFilterConfig) newFilter.getConfig();
        assertEquals(originalConfig.isParseAsUriComponents(), newConfig.isParseAsUriComponents());
        assertEquals(originalConfig.getPasswordHeaderName(), newConfig.getPasswordHeaderName());
        assertEquals(originalConfig.getPasswordRegex(), newConfig.getPasswordRegex());
        assertEquals(originalConfig.getUserNameHeaderName(), newConfig.getUserNameHeaderName());
        assertEquals(originalConfig.getUserNameRegex(), newConfig.getUserNameRegex());
    }

    @Test
    public void testRoundTrip_DigestAuthenticationFilterConfig_Json() throws JsonProcessingException {
        int nonceValiditySeconds = 30;
        String userGroupService = "UserGroupService";

        // Create an instance of DigestAuthenticationFilterConfig
        DigestAuthenticationFilterConfig originalConfig = new DigestAuthenticationFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(DigestAuthenticationFilterConfig.class.getName());
        originalConfig.setNonceValiditySeconds(nonceValiditySeconds);
        originalConfig.setUserGroupServiceName(userGroupService);

        AuthFilter filter = new AuthFilter(originalConfig);
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        DigestAuthenticationFilterConfig newConfig = (DigestAuthenticationFilterConfig) newFilter.getConfig();
        assertEquals(originalConfig.getNonceValiditySeconds(), newConfig.getNonceValiditySeconds());
        assertEquals(originalConfig.getUserGroupServiceName(), newConfig.getUserGroupServiceName());
    }

    @Test
    public void testRoundTrip_ExceptionTranslationFilterConfig_Json() throws JsonProcessingException {
        // Create an instance of ExceptionTranslationFilterConfig
        ExceptionTranslationFilterConfig originalConfig = new ExceptionTranslationFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        originalConfig.setAuthenticationFilterName("Auth Filter Name");
        originalConfig.setAccessDeniedErrorPage("Access Denied Page");

        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to JSON
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        ExceptionTranslationFilterConfig newConfig = (ExceptionTranslationFilterConfig) newFilter.getConfig();
        assertEquals(originalConfig.getAuthenticationFilterName(), newConfig.getAuthenticationFilterName());
        assertEquals(originalConfig.getAccessDeniedErrorPage(), newConfig.getAccessDeniedErrorPage());
    }

    @Test
    public void testRoundTrip_LogoutFilterConfig_Json() throws JsonProcessingException {
        // Create an instance of LogoutFilterConfig
        LogoutFilterConfig originalConfig = new LogoutFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        originalConfig.setRedirectURL("URL");
        originalConfig.setFormLogoutChain("Chain");

        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to JSON
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        LogoutFilterConfig newConfig = (LogoutFilterConfig) newFilter.getConfig();
        assertEquals(originalConfig.getFormLogoutChain(), newConfig.getFormLogoutChain());
        assertEquals(originalConfig.getRedirectURL(), newConfig.getRedirectURL());
    }

    @Test
    public void testRoundTrip_J2eeAuthenticationBaseFilterConfig_Json() throws JsonProcessingException {
        // Create an instance of J2eeAuthenticationBaseFilterConfig
        J2eeAuthenticationBaseFilterConfig originalConfig = new J2eeAuthenticationBaseFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        originalConfig.setRoleSource(J2eeAuthenticationBaseFilterConfig.J2EERoleSource.Header);
        originalConfig.setRoleServiceName("Role Service Name");
        originalConfig.setRoleConverterName("Role Converter Name");
        originalConfig.setRolesHeaderAttribute("Roles Header Attribute");
        originalConfig.setUserGroupServiceName("User Group Service Name");

        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to JSON
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        PreAuthenticatedUserNameFilterConfig newConfig = (PreAuthenticatedUserNameFilterConfig) newFilter.getConfig();
        assertEquals(originalConfig.getRoleSource(), newConfig.getRoleSource());
        assertEquals(originalConfig.getRoleConverterName(), newConfig.getRoleConverterName());
        assertEquals(originalConfig.getRoleServiceName(), newConfig.getRoleServiceName());
        assertEquals(originalConfig.getRolesHeaderAttribute(), newConfig.getRolesHeaderAttribute());
    }

    @Test
    public void testRoundTrip_RequestHeaderAuthenticationFilterConfig_Json() throws JsonProcessingException {
        // Create an instance of RequestHeaderAuthenticationFilterConfig
        RequestHeaderAuthenticationFilterConfig originalConfig = new RequestHeaderAuthenticationFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        originalConfig.setRoleSource(PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource.Header);
        originalConfig.setRoleServiceName("Role Service Name");
        originalConfig.setRoleConverterName("Role Converter Name");
        originalConfig.setRolesHeaderAttribute("Roles Header Attribute");
        originalConfig.setUserGroupServiceName("User Group Service Name");

        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to JSON
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        RequestHeaderAuthenticationFilterConfig newConfig =
                (RequestHeaderAuthenticationFilterConfig) newFilter.getConfig();
        assertEquals(originalConfig.getRoleSource(), newConfig.getRoleSource());
        assertEquals(originalConfig.getRoleConverterName(), newConfig.getRoleConverterName());
        assertEquals(originalConfig.getRoleServiceName(), newConfig.getRoleServiceName());
        assertEquals(originalConfig.getRolesHeaderAttribute(), newConfig.getRolesHeaderAttribute());
    }

    @Test
    public void testRoundTrip_RememberMeAuthenticationFilterConfig_Json() throws JsonProcessingException {
        // Create an instance of RememberMeAuthenticationFilterConfig
        RememberMeAuthenticationFilterConfig originalConfig = new RememberMeAuthenticationFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());

        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to JSON
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        RememberMeAuthenticationFilterConfig newConfig = (RememberMeAuthenticationFilterConfig) newFilter.getConfig();
        assertNotNull(newConfig);
    }

    @Test
    public void testRoundTrip_RoleFilterConfig_Json() throws JsonProcessingException {
        // Create an instance of RoleFilterConfig
        RoleFilterConfig originalConfig = new RoleFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        originalConfig.setHttpResponseHeaderAttrForIncludedRoles("Header Attribute");
        originalConfig.setRoleConverterName("Role Converter Name");

        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to JSON
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        RoleFilterConfig newConfig = (RoleFilterConfig) newFilter.getConfig();
        assertEquals(originalConfig.getRoleConverterName(), newConfig.getRoleConverterName());
        assertEquals(
                originalConfig.getHttpResponseHeaderAttrForIncludedRoles(),
                newConfig.getHttpResponseHeaderAttrForIncludedRoles());
    }

    @Test
    public void testRoundTrip_SecurityContextPersistenceFilterConfig_Json() throws JsonProcessingException {
        // Create an instance of SecurityContextPersistenceFilterConfig
        SecurityContextPersistenceFilterConfig originalConfig = new SecurityContextPersistenceFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        originalConfig.setAllowSessionCreation(true);

        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to JSON
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        SecurityContextPersistenceFilterConfig newConfig =
                (SecurityContextPersistenceFilterConfig) newFilter.getConfig();
        assertEquals(originalConfig.isAllowSessionCreation(), newConfig.isAllowSessionCreation());
    }

    @Test
    public void testRoundTrip_SecurityInterceptorFilterConfig_Json() throws JsonProcessingException {
        // Create an instance of SecurityInterceptorFilterConfig
        SecurityInterceptorFilterConfig originalConfig = new SecurityInterceptorFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        originalConfig.setSecurityMetadataSource("Security Metadata Source");
        originalConfig.setAllowIfAllAbstainDecisions(true);

        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to JSON
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        SecurityInterceptorFilterConfig newConfig = (SecurityInterceptorFilterConfig) newFilter.getConfig();
        assertEquals(originalConfig.getSecurityMetadataSource(), newConfig.getSecurityMetadataSource());
        assertEquals(originalConfig.isAllowIfAllAbstainDecisions(), newConfig.isAllowIfAllAbstainDecisions());
    }

    @Test
    public void testRoundTrip_SSLFilterConfig_Json() throws JsonProcessingException {
        // Create an instance of SSLFilterConfig
        SSLFilterConfig originalConfig = new SSLFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        originalConfig.setSslPort(1234);

        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to JSON
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        SSLFilterConfig newConfig = (SSLFilterConfig) newFilter.getConfig();
        assertEquals(originalConfig.getSslPort(), newConfig.getSslPort());
    }

    @Test
    public void testRoundTrip_UsernamePasswordAuthenticationFilterConfig_Json() throws JsonProcessingException {
        // Create an instance of UsernamePasswordAuthenticationFilterConfig
        UsernamePasswordAuthenticationFilterConfig originalConfig = new UsernamePasswordAuthenticationFilterConfig();
        originalConfig.setId(TEST_FILTER_ID);
        originalConfig.setName(TEST_FILTER_NAME);
        originalConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        originalConfig.setPasswordParameterName("Password Parameter Name");
        originalConfig.setUsernameParameterName("Username Parameter Name");

        AuthFilter filter = new AuthFilter(originalConfig);

        // Marshal to JSON
        String json = objectMapper.writeValueAsString(filter);

        // Unmarshal back to object
        AuthFilter newFilter = objectMapper.readValue(json, AuthFilter.class);

        // Validate objects are identical
        assertEquals(filter.getId(), newFilter.getId());
        assertEquals(filter.getName(), newFilter.getName());
        assertEquals(filter.getConfig().getClassName(), newFilter.getConfig().getClassName());
        UsernamePasswordAuthenticationFilterConfig newConfig =
                (UsernamePasswordAuthenticationFilterConfig) newFilter.getConfig();
        assertEquals(originalConfig.getPasswordParameterName(), newConfig.getPasswordParameterName());
        assertEquals(originalConfig.getUsernameParameterName(), newConfig.getUsernameParameterName());
    }
}
