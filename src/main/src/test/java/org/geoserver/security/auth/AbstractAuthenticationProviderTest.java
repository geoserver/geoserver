/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.auth;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AbstractSecurityServiceTest;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.HtmlLoginFilterChain;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.impl.DigestAuthUtils;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.MemoryRoleService;
import org.geoserver.security.impl.MemoryUserGroupService;
import org.geoserver.security.password.PasswordValidator;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;

public abstract class AbstractAuthenticationProviderTest extends AbstractSecurityServiceTest {

    public static final String testUserName = "user1";
    public static final String testPassword = "pw1";
    public static final String rootRole = "RootRole";
    public static final String derivedRole = "DerivedRole";
    protected String pattern = "/foo/**";
    public static final String testProviderName = "testAuthenticationProvider";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        createServices();
    }

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
    }

    protected TestingAuthenticationCache getCache() {
        return (TestingAuthenticationCache) getSecurityManager().getAuthenticationCache();
    }

    protected void createServices() throws Exception {

        GeoServerRoleService rservice = createRoleService("rs1");
        GeoServerRoleStore rstore = rservice.createStore();
        GeoServerRole root, derived;
        rstore.addRole(root = rstore.createRoleObject(rootRole));
        rstore.addRole(derived = rstore.createRoleObject(derivedRole));
        rstore.setParentRole(derived, root);
        rstore.associateRoleToUser(derived, testUserName);
        rstore.associateRoleToUser(derived, "castest");
        rstore.store();

        SecurityManagerConfig mconfig = getSecurityManager().loadSecurityConfig();
        mconfig.setRoleServiceName("rs1");
        getSecurityManager().saveSecurityConfig(mconfig);

        GeoServerUserGroupService ugservice = createUserGroupService("ug1");
        GeoServerUserGroupStore ugstore = ugservice.createStore();
        GeoServerUser u1 = ugstore.createUserObject(testUserName, testPassword, true);
        ugstore.addUser(u1);
        GeoServerUser u2 = ugstore.createUserObject("abc@xyz.com", "abc", true);
        ugstore.addUser(u2);
        GeoServerUser u3 = ugstore.createUserObject("castest", "castest", true);
        ugstore.addUser(u3);

        ugstore.store();

        GeoServerAuthenticationProvider prov =
                createAuthProvider(testProviderName, ugservice.getName());
        prepareAuthProviders(prov.getName());
    }

    protected void insertAnonymousFilter() throws Exception {
        SecurityManagerConfig mconfig = getSecurityManager().loadSecurityConfig();
        mconfig.getFilterChain()
                .find(pattern)
                .getFilterNames()
                .add(GeoServerSecurityFilterChain.ANONYMOUS_FILTER);
        getSecurityManager().saveSecurityConfig(mconfig);
    }

    protected void removeAnonymousFilter() throws Exception {
        SecurityManagerConfig mconfig = getSecurityManager().loadSecurityConfig();
        mconfig.getFilterChain()
                .find(pattern)
                .getFilterNames()
                .remove(GeoServerSecurityFilterChain.ANONYMOUS_FILTER);
        getSecurityManager().saveSecurityConfig(mconfig);
    }

    public GeoServerAuthenticationProvider createAuthProvider(
            String name, String userGroupServiceName) throws Exception {
        UsernamePasswordAuthenticationProviderConfig config =
                new UsernamePasswordAuthenticationProviderConfig();
        config.setClassName(UsernamePasswordAuthenticationProvider.class.getName());
        config.setUserGroupServiceName(userGroupServiceName);
        config.setName(name);
        getSecurityManager().saveAuthenticationProvider(config);
        return getSecurityManager().loadAuthenticationProvider(name);
    }

    @Override
    public GeoServerRoleService createRoleService(String name) throws Exception {
        SecurityRoleServiceConfig config = getRoleConfig(name);
        getSecurityManager().saveRoleService(config);
        return getSecurityManager().loadRoleService(name);
    }

    public MemoryRoleServiceConfigImpl getRoleConfig(String name) {
        MemoryRoleServiceConfigImpl config = new MemoryRoleServiceConfigImpl();
        config.setName(name);
        config.setClassName(MemoryRoleService.class.getName());
        config.setToBeEncrypted("encryptme");
        return config;
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String name) throws Exception {
        return createUserGroupService(name, getPBEPasswordEncoder().getName());
    }

    public GeoServerUserGroupService createUserGroupService(String name, String passwordEncoderName)
            throws Exception {
        SecurityUserGroupServiceConfig config = getUserGroupConfg(name, passwordEncoderName);
        getSecurityManager().saveUserGroupService(config /*,isNewUGService(name)*/);
        return getSecurityManager().loadUserGroupService(name);
    }

    public MemoryUserGroupServiceConfigImpl getUserGroupConfg(
            String name, String passwordEncoderName) {
        MemoryUserGroupServiceConfigImpl config = new MemoryUserGroupServiceConfigImpl();
        config.setName(name);
        config.setClassName(MemoryUserGroupService.class.getName());
        config.setPasswordEncoderName(passwordEncoderName);
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        config.setToBeEncrypted("encryptme");
        return config;
    }

    public void checkForAuthenticatedRole(Authentication auth) {
        assertTrue(auth.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE));
    }

    protected void prepareAuthProviders(String... authProviderNames) throws Exception {
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        config.getAuthProviderNames().clear();
        for (String n : authProviderNames) config.getAuthProviderNames().add(n);
        getSecurityManager().saveSecurityConfig(config);
    }

    protected void prepareFilterChain(
            Class<?> filterChainClass, String pattern, String... filterNames) throws Exception {
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        GeoServerSecurityFilterChain filterChain = config.getFilterChain();

        filterChain.removeForPattern(pattern);

        Constructor<?> cons = filterChainClass.getConstructor(new Class[] {String[].class});
        String[] args = {pattern};
        RequestFilterChain requestChain =
                (RequestFilterChain) cons.newInstance(new Object[] {args});
        requestChain = new HtmlLoginFilterChain(pattern);
        requestChain.setName("testChain");
        requestChain.setFilterNames(filterNames);

        // insert before default
        filterChain.getRequestChains().add(filterChain.getRequestChains().size() - 2, requestChain);

        getSecurityManager().saveSecurityConfig(config);
    }

    protected void modifyChain(
            String pattern, boolean disabled, boolean allowSessionCreation, String roleFilterName)
            throws Exception {
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        RequestFilterChain chain = config.getFilterChain().find(pattern);
        chain.setDisabled(disabled);
        chain.setAllowSessionCreation(allowSessionCreation);
        chain.setRoleFilterName(roleFilterName);

        getSecurityManager().saveSecurityConfig(config);
    }

    protected void prepareFilterChain(String pattern, String... filterNames) throws Exception {
        prepareFilterChain(HtmlLoginFilterChain.class, pattern, filterNames);
    }

    protected void updateUser(String ugService, String userName, boolean enabled) throws Exception {
        GeoServerUserGroupService ugservice = getSecurityManager().loadUserGroupService(ugService);
        GeoServerUserGroupStore ugstore = ugservice.createStore();
        GeoServerUser u1 = ugstore.getUserByUsername(userName);
        u1.setEnabled(enabled);
        ugstore.updateUser(u1);
        ugstore.store();
    }

    protected GeoServerSecurityFilterChainProxy getProxy() {
        return GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
    }

    protected String clientDigestString(
            String serverDigestString, String username, String password, String method) {
        String section212response = serverDigestString.substring(7);
        String[] headerEntries = DigestAuthUtils.splitIgnoringQuotes(section212response, ',');
        Map<String, String> headerMap =
                DigestAuthUtils.splitEachArrayElementAndCreateMap(headerEntries, "=", "\"");

        String realm = headerMap.get("realm");
        String qop = headerMap.get("qop");
        String nonce = headerMap.get("nonce");

        String uri = "/foo/bar";
        String nc = "00000001";
        String cnonce = "0a4f113b";
        String opaque = "5ccc069c403ebaf9f0171e9517f40e41";

        String responseString =
                DigestAuthUtils.generateDigest(
                        false, username, realm, password, method, uri, qop, nonce, nc, cnonce);

        String template = "Digest username=\"{0}\",realm=\"{1}\"";
        template += ",nonce=\"{2}\",uri=\"{3}\"";
        template += ",qop=\"{4}\",nc=\"{5}\"";
        template += ",cnonce=\"{6}\",response=\"{7}\"";
        template += ",opaque=\"{8}\"";

        return MessageFormat.format(
                template, username, realm, nonce, uri, qop, nc, cnonce, responseString, opaque);
    }

    protected void setCertifacteForUser(final String userName, MockHttpServletRequest request) {
        X509Certificate x509 =
                new X509Certificate() {

                    @Override
                    public Set<String> getCriticalExtensionOIDs() {
                        return null;
                    }

                    @Override
                    public byte[] getExtensionValue(String arg0) {
                        return null;
                    }

                    @Override
                    public Set<String> getNonCriticalExtensionOIDs() {
                        return null;
                    }

                    @Override
                    public boolean hasUnsupportedCriticalExtension() {
                        return false;
                    }

                    @Override
                    public void checkValidity()
                            throws CertificateExpiredException, CertificateNotYetValidException {}

                    @Override
                    public void checkValidity(Date arg0)
                            throws CertificateExpiredException, CertificateNotYetValidException {}

                    @Override
                    public int getBasicConstraints() {
                        return 0;
                    }

                    @Override
                    public Principal getIssuerDN() {
                        return null;
                    }

                    @Override
                    public boolean[] getIssuerUniqueID() {
                        return null;
                    }

                    @Override
                    public boolean[] getKeyUsage() {
                        return null;
                    }

                    @Override
                    public Date getNotAfter() {
                        return null;
                    }

                    @Override
                    public Date getNotBefore() {
                        return null;
                    }

                    @Override
                    public BigInteger getSerialNumber() {
                        return null;
                    }

                    @Override
                    public String getSigAlgName() {
                        return null;
                    }

                    @Override
                    public String getSigAlgOID() {
                        return null;
                    }

                    @Override
                    public byte[] getSigAlgParams() {
                        return null;
                    }

                    @Override
                    public byte[] getSignature() {
                        return null;
                    }

                    @Override
                    public Principal getSubjectDN() {
                        return () -> "cn=" + userName + ",ou=ou1";
                    }

                    @Override
                    public boolean[] getSubjectUniqueID() {
                        return null;
                    }

                    @Override
                    public byte[] getTBSCertificate() throws CertificateEncodingException {
                        return null;
                    }

                    @Override
                    public int getVersion() {
                        return 0;
                    }

                    @Override
                    public byte[] getEncoded() throws CertificateEncodingException {
                        return null;
                    }

                    @Override
                    public PublicKey getPublicKey() {
                        return null;
                    }

                    @Override
                    public String toString() {
                        return null;
                    }

                    @Override
                    public void verify(PublicKey arg0)
                            throws CertificateException, NoSuchAlgorithmException,
                                    InvalidKeyException, NoSuchProviderException,
                                    SignatureException {}

                    @Override
                    public void verify(PublicKey arg0, String arg1)
                            throws CertificateException, NoSuchAlgorithmException,
                                    InvalidKeyException, NoSuchProviderException,
                                    SignatureException {}
                };
        request.setAttribute("javax.servlet.request.X509Certificate", new X509Certificate[] {x509});
    }

    //    @Override
    //    protected MockHttpServletRequest createRequest(String path) {
    //        MockHttpServletRequest req= super.createRequest(path);
    //        req.setServletPath("");
    //        req.setPathInfo(path);
    //        return req;
    //
    //    }
}
