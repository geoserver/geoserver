/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerConfigPersister;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.password.DecodingUserDetailsService;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.security.core.userdetails.UserDetails;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Category(SystemTest.class)
public class MemoryUserDetailsServiceTest extends AbstractUserDetailsServiceTest {

    static final String plainTextRole = "plainrole";
    static final String plainTextUserGroup = "plainuserGroup";

    @Override
    public GeoServerRoleService createRoleService(String name) throws Exception {
        MemoryRoleServiceConfigImpl config = getRoleConfig(name);
        GeoServerRoleService service = new MemoryRoleService();
        service.setSecurityManager(GeoServerExtensions.bean(GeoServerSecurityManager.class));
        service.initializeFromConfig(config);
        getSecurityManager().saveRoleService(config /*,isNewRoleService(name)*/);
        return service;
    }

    public MemoryRoleServiceConfigImpl getRoleConfig(String name) {
        MemoryRoleServiceConfigImpl config = new MemoryRoleServiceConfigImpl();
        config.setName(name);
        config.setClassName(MemoryRoleService.class.getName());
        config.setToBeEncrypted(plainTextRole);
        return config;
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String name) throws Exception {
        return createUserGroupService(name, getPBEPasswordEncoder().getName());
    }

    public GeoServerUserGroupService createUserGroupService(String name, String passwordEncoderName)
            throws Exception {
        MemoryUserGroupServiceConfigImpl config = getUserGroupConfg(name, passwordEncoderName);
        GeoServerUserGroupService service = new MemoryUserGroupService();
        service.setSecurityManager(GeoServerExtensions.bean(GeoServerSecurityManager.class));
        service.initializeFromConfig(config);
        getSecurityManager().saveUserGroupService(config /*,isNewUGService(name)*/);
        return service;
    }

    public MemoryUserGroupServiceConfigImpl getUserGroupConfg(
            String name, String passwordEncoderName) {
        MemoryUserGroupServiceConfigImpl config = new MemoryUserGroupServiceConfigImpl();
        config.setName(name);
        config.setClassName(MemoryUserGroupService.class.getName());
        config.setPasswordEncoderName(passwordEncoderName);
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        config.setToBeEncrypted(plainTextUserGroup);
        return config;
    }

    @Test
    public void testDecodingUserDetailsService() throws Exception {
        GeoServerUserGroupService service = createUserGroupService("test");
        DecodingUserDetailsService decService = DecodingUserDetailsService.newInstance(service);
        GeoServerUserGroupStore store = createStore(service);
        insertValues(store);
        store.store();

        String plainpassword = "geoserver";
        UserDetails admin = service.loadUserByUsername(GeoServerUser.ADMIN_USERNAME);
        assertFalse(plainpassword.equals(admin.getPassword()));
        UserDetails admin2 = decService.loadUserByUsername(GeoServerUser.ADMIN_USERNAME);
        assertTrue(plainpassword.equals(admin2.getPassword()));
    }

    @Test
    public void testCopyFrom() throws Exception {
        // from crypt tp crytp
        GeoServerUserGroupService service1 = createUserGroupService("copyFrom");
        GeoServerUserGroupService service2 = createUserGroupService("copyTo");
        copyFrom(service1, service2);

        // from plain to plain
        service1 = createUserGroupService("copyFrom1", getPlainTextPasswordEncoder().getName());
        service2 = createUserGroupService("copyTo1", getPlainTextPasswordEncoder().getName());
        copyFrom(service1, service2);

        // cypt to digest
        service1 = createUserGroupService("copyFrom2");
        service2 = createUserGroupService("copyTo2", getDigestPasswordEncoder().getName());
        copyFrom(service1, service2);

        // digest to digest
        service1 = createUserGroupService("copyFrom3", getDigestPasswordEncoder().getName());
        service2 = createUserGroupService("copyTo3", getDigestPasswordEncoder().getName());
        copyFrom(service1, service2);

        // digest to crypt
        service1 = createUserGroupService("copyFrom4", getDigestPasswordEncoder().getName());
        service2 = createUserGroupService("copyTo4");
        copyFrom(service1, service2);
    }

    protected void copyFrom(GeoServerUserGroupService service1, GeoServerUserGroupService service2)
            throws Exception {
        GeoServerUserGroupStore store1 = createStore(service1);
        GeoServerUserGroupStore store2 = createStore(service2);

        store1.clear();
        checkEmpty(store1);
        insertValues(store1);

        Util.copyFrom(store1, store2);
        store1.clear();
        checkEmpty(store1);
        checkValuesInserted(store2);
        store2.clear();
        checkEmpty(store2);
    }

    @Test
    public void testEncryption() throws Exception {
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        GeoServerPasswordEncoder encoder = getPlainTextPasswordEncoder();
        String plainprefix = encoder.getPrefix() + GeoServerPasswordEncoder.PREFIX_DELIMTER;
        config.setConfigPasswordEncrypterName(encoder.getName());

        getSecurityManager().saveSecurityConfig(config);

        String serviceName = "testEncrypt";
        String cryptprefix =
                getPBEPasswordEncoder().getPrefix() + GeoServerPasswordEncoder.PREFIX_DELIMTER;

        MemoryRoleServiceConfigImpl roleConfig = getRoleConfig(serviceName);
        MemoryUserGroupServiceConfigImpl ugConfig =
                getUserGroupConfg(serviceName, getPlainTextPasswordEncoder().getName());

        getSecurityManager().saveRoleService(roleConfig);
        getSecurityManager().saveUserGroupService(ugConfig);

        File roleDir = new File(getSecurityManager().get("security/role").dir(), serviceName);
        File ugDir = new File(getSecurityManager().get("security/usergroup").dir(), serviceName);
        File roleFile = new File(roleDir, GeoServerSecurityManager.CONFIG_FILENAME);
        File ugFile = new File(ugDir, GeoServerSecurityManager.CONFIG_FILENAME);

        assertTrue(roleFile.exists());
        assertTrue(ugFile.exists());

        Document ugDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ugFile);
        Document roleDoc =
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(roleFile);
        Element roleElem =
                (Element)
                        roleDoc.getDocumentElement().getElementsByTagName("toBeEncrypted").item(0);
        Element ugElem =
                (Element) ugDoc.getDocumentElement().getElementsByTagName("toBeEncrypted").item(0);

        // check file
        assertEquals(plainprefix + plainTextRole, roleElem.getTextContent());
        assertEquals(plainprefix + plainTextUserGroup, ugElem.getTextContent());

        // reload and check
        MemoryRoleService roleService =
                (MemoryRoleService) getSecurityManager().loadRoleService(serviceName);
        assertEquals(plainTextRole, roleService.getToBeEncrypted());
        MemoryUserGroupService ugService =
                (MemoryUserGroupService) getSecurityManager().loadUserGroupService(serviceName);
        assertEquals(plainTextUserGroup, ugService.getToBeEncrypted());

        // SWITCH TO ENCRYPTION
        config = getSecurityManager().getSecurityConfig();
        config.setConfigPasswordEncrypterName(getPBEPasswordEncoder().getName());
        getSecurityManager().saveSecurityConfig(config);
        getSecurityManager().updateConfigurationFilesWithEncryptedFields();

        ugDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ugFile);
        roleDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(roleFile);
        roleElem =
                (Element)
                        roleDoc.getDocumentElement().getElementsByTagName("toBeEncrypted").item(0);
        ugElem = (Element) ugDoc.getDocumentElement().getElementsByTagName("toBeEncrypted").item(0);

        // check file
        assertTrue(roleElem.getTextContent().startsWith(cryptprefix));
        assertTrue(ugElem.getTextContent().startsWith(cryptprefix));

        roleService = (MemoryRoleService) getSecurityManager().loadRoleService(serviceName);
        assertEquals(plainTextRole, roleService.getToBeEncrypted());
        ugService = (MemoryUserGroupService) getSecurityManager().loadUserGroupService(serviceName);
        assertEquals(plainTextUserGroup, ugService.getToBeEncrypted());
    }

    @Test
    public void testEncryption2() throws Exception {
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        config.setConfigPasswordEncrypterName(getPBEPasswordEncoder().getName());
        getSecurityManager().saveSecurityConfig(config);
        String serviceName = "testEncrypt2";
        String prefix =
                getPBEPasswordEncoder().getPrefix() + GeoServerPasswordEncoder.PREFIX_DELIMTER;

        MemoryRoleServiceConfigImpl roleConfig = getRoleConfig(serviceName);
        MemoryUserGroupServiceConfigImpl ugConfig =
                getUserGroupConfg(serviceName, getPlainTextPasswordEncoder().getName());

        getSecurityManager().saveRoleService(roleConfig);
        getSecurityManager().saveUserGroupService(ugConfig);

        File roleDir = new File(getSecurityManager().get("security/role").dir(), serviceName);
        File ugDir = new File(getSecurityManager().get("security/usergroup").dir(), serviceName);
        File roleFile = new File(roleDir, GeoServerSecurityManager.CONFIG_FILENAME);
        File ugFile = new File(ugDir, GeoServerSecurityManager.CONFIG_FILENAME);

        assertTrue(roleFile.exists());
        assertTrue(ugFile.exists());

        Document ugDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ugFile);
        Document roleDoc =
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(roleFile);
        Element roleElem =
                (Element)
                        roleDoc.getDocumentElement().getElementsByTagName("toBeEncrypted").item(0);
        Element ugElem =
                (Element) ugDoc.getDocumentElement().getElementsByTagName("toBeEncrypted").item(0);

        // check file
        assertTrue(roleElem.getTextContent().startsWith(prefix));
        assertTrue(ugElem.getTextContent().startsWith(prefix));

        // reload and check
        MemoryRoleService roleService =
                (MemoryRoleService) getSecurityManager().loadRoleService(serviceName);
        assertEquals(plainTextRole, roleService.getToBeEncrypted());
        MemoryUserGroupService ugService =
                (MemoryUserGroupService) getSecurityManager().loadUserGroupService(serviceName);
        assertEquals(plainTextUserGroup, ugService.getToBeEncrypted());

        // SWITCH TO PLAINTEXT

        config.setConfigPasswordEncrypterName(getPlainTextPasswordEncoder().getName());
        String plainprefix =
                getPlainTextPasswordEncoder().getPrefix()
                        + GeoServerPasswordEncoder.PREFIX_DELIMTER;
        getSecurityManager().saveSecurityConfig(config);
        getSecurityManager().updateConfigurationFilesWithEncryptedFields();

        ugDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ugFile);
        roleDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(roleFile);
        roleElem =
                (Element)
                        roleDoc.getDocumentElement().getElementsByTagName("toBeEncrypted").item(0);
        ugElem = (Element) ugDoc.getDocumentElement().getElementsByTagName("toBeEncrypted").item(0);

        // check file
        // check file
        assertEquals(plainprefix + plainTextRole, roleElem.getTextContent());
        assertEquals(plainprefix + plainTextUserGroup, ugElem.getTextContent());

        roleService = (MemoryRoleService) getSecurityManager().loadRoleService(serviceName);
        assertEquals(plainTextRole, roleService.getToBeEncrypted());
        ugService = (MemoryUserGroupService) getSecurityManager().loadUserGroupService(serviceName);
        assertEquals(plainTextUserGroup, ugService.getToBeEncrypted());
    }

    @Test
    public void testPasswordPersistence() throws Exception {
        Catalog cat = getCatalog();
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        GeoServerPasswordEncoder encoder = getPlainTextPasswordEncoder();
        String prefix = encoder.getPrefix() + GeoServerPasswordEncoder.PREFIX_DELIMTER;
        config.setConfigPasswordEncrypterName(encoder.getName());
        getSecurityManager().saveSecurityConfig(config);

        GeoServerConfigPersister cp =
                new GeoServerConfigPersister(
                        getResourceLoader(), new XStreamPersisterFactory().createXMLPersister());
        GeoServerResourcePersister rp = new GeoServerResourcePersister(cat);
        cat.addListener(cp);
        cat.addListener(rp);

        WorkspaceInfo ws = cat.getFactory().createWorkspace();
        ws.setName("password");
        cat.add(ws);

        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("password");
        ds.getConnectionParameters().put("user", "testuser");
        ds.getConnectionParameters().put("passwd", "secret");
        ds.getConnectionParameters().put("host", "localhost");
        ds.getConnectionParameters().put("port", "5432");
        ds.getConnectionParameters().put("database", "testdb");
        ds.getConnectionParameters().put("dbtype", "postgisng");
        ds.setWorkspace(ws);
        cat.add(ds);

        // TODO Justin, this does not work ?
        //        DataStore dataStore = DataStoreFinder.getDataStore(ds.getConnectionParameters());
        //        assertNotNull(dataStore);
        //        dataStore.dispose();

        // MockData data = getTestData();
        File store =
                new File(getDataDirectory().root(), "workspaces/password/password/datastore.xml");
        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(store);
        XPath xpath = XPathFactory.newInstance().newXPath();
        String encrypted = xpath.evaluate("//entry[@key='passwd']", dom.getDocumentElement());
        assertThat(encrypted, equalTo(prefix + "secret"));
        XStreamPersister xs = new XStreamPersisterFactory().createXMLPersister();

        FileInputStream fin = new FileInputStream(store);
        DataStoreInfo load = xs.load(fin, DataStoreInfo.class);
        fin.close();

        assertEquals("secret", load.getConnectionParameters().get("passwd"));

        // now encrypt
        config.setConfigPasswordEncrypterName(getPBEPasswordEncoder().getName());
        getSecurityManager().saveSecurityConfig(config);
        getSecurityManager().updateConfigurationFilesWithEncryptedFields();

        //        FileInputStream fi = new FileInputStream(store);
        //        BufferedReader r = new BufferedReader(new InputStreamReader(fi));
        //        String line;
        //        while ((line= r.readLine())!=null)
        //            System.out.println(line);
        //        fi.close();

        dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(store);
        xpath = XPathFactory.newInstance().newXPath();
        encrypted = xpath.evaluate("//entry[@key='passwd']", dom.getDocumentElement());

        // TODO, assertion does not pass with mvn clean install
        // but it passes with  mvn test
        // -Dtest=org.geoserver.security.impl.MemoryUserDetailsServiceTest
        // ???????

        // assertFalse("secret".equals(encrypted));

        xs = new XStreamPersisterFactory().createXMLPersister();

        fin = new FileInputStream(store);

        load = xs.load(fin, DataStoreInfo.class);
        assertEquals("secret", load.getConnectionParameters().get("passwd"));
        fin.close();
    }
}
