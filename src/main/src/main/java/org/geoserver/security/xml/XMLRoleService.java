/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.FileBasedSecurityServiceConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.AbstractRoleService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.util.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLRoleService extends AbstractRoleService {

    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.xml");
    protected DocumentBuilder builder;
    protected Resource roleResource;

    /** Validate against schema on load/store, default = true; */
    private boolean validatingXMLSchema = true;

    public XMLRoleService() throws IOException {
        super();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setIgnoringComments(true);
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {

        super.initializeFromConfig(config);
        validatingXMLSchema = false;

        if (config instanceof XMLSecurityServiceConfig) {
            validatingXMLSchema = ((XMLSecurityServiceConfig) config).isValidating();

            // copy schema file
            Resource xsdFile = getConfigRoot().get(XMLConstants.FILE_RR_SCHEMA);
            if (xsdFile.getType() == Type.UNDEFINED) {
                IOUtils.copy(
                        getClass().getResourceAsStream(XMLConstants.FILE_RR_SCHEMA), xsdFile.out());
            }
        }

        if (config instanceof FileBasedSecurityServiceConfig) {
            String fileName = ((FileBasedSecurityServiceConfig) config).getFileName();

            File roleFile = new File(fileName);
            if (roleFile.isAbsolute()) {
                roleResource = Files.asResource(roleFile);
            } else {
                roleResource = getConfigRoot().get(fileName);
            }

            if (roleResource.getType() == Type.UNDEFINED) {
                IOUtils.copy(
                        getClass().getResourceAsStream("rolesTemplate.xml"), roleResource.out());
            }
        } else {
            throw new IOException("Cannot initialize from " + config.getClass().getName());
        }
        // load the data
        deserialize();
    }

    @Override
    public boolean canCreateStore() {
        return true;
    }

    @Override
    public GeoServerRoleStore createStore() throws IOException {
        XMLRoleStore store = new XMLRoleStore();
        store.initializeFromService(this);
        return store;
    }

    public boolean isValidatingXMLSchema() {
        return validatingXMLSchema;
    }

    public void setValidatingXMLSchema(boolean validatingXMLSchema) {
        this.validatingXMLSchema = validatingXMLSchema;
    }

    @Override
    protected void deserialize() throws IOException {

        try {
            Document doc = null;
            InputStream is = null;
            try {
                is = roleResource.in();
                doc = builder.parse(is);
            } catch (SAXException e) {
                throw new IOException(e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (isValidatingXMLSchema()) {
                XMLValidator.Singleton.validateRoleRegistry(doc);
            }

            XPathExpression expr = XMLXpathFactory.Singleton.getVersionExpressionRR();
            String versioNummer = expr.evaluate(doc);
            RoleXMLXpath xmlXPath = XMLXpathFactory.Singleton.getRoleXMLXpath(versioNummer);

            clearMaps();

            NodeList roleNodes =
                    (NodeList)
                            xmlXPath.getRoleListExpression().evaluate(doc, XPathConstants.NODESET);

            for (int i = 0; i < roleNodes.getLength(); i++) {
                Node roleNode = roleNodes.item(i);

                String roleName = xmlXPath.getRoleNameExpression().evaluate(roleNode);
                NodeList propertyNodes =
                        (NodeList)
                                xmlXPath.getRolePropertiesExpression()
                                        .evaluate(roleNode, XPathConstants.NODESET);
                Properties roleProps = new Properties();
                for (int j = 0; j < propertyNodes.getLength(); j++) {
                    Node propertyNode = propertyNodes.item(j);
                    String propertyName =
                            xmlXPath.getPropertyNameExpression().evaluate(propertyNode);
                    String propertyValue =
                            xmlXPath.getPropertyValueExpression().evaluate(propertyNode);
                    roleProps.put(propertyName, propertyValue);
                }
                GeoServerRole role = createRoleObject(roleName);

                role.getProperties().clear(); // set properties
                for (Object key : roleProps.keySet()) {
                    role.getProperties().put(key, roleProps.get(key));
                }
                helper.roleMap.put(roleName, role);
            }
            // second pass for hierarchy
            for (int i = 0; i < roleNodes.getLength(); i++) {
                Node roleNode = roleNodes.item(i);
                String roleName = xmlXPath.getRoleNameExpression().evaluate(roleNode);
                String parentName = xmlXPath.getParentExpression().evaluate(roleNode);
                if (parentName != null && parentName.length() > 0) {
                    helper.role_parentMap.put(
                            helper.roleMap.get(roleName), helper.roleMap.get(parentName));
                }
            }

            // user roles
            NodeList userRolesNodes =
                    (NodeList)
                            xmlXPath.getUserRolesExpression().evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < userRolesNodes.getLength(); i++) {
                Node userRolesNode = userRolesNodes.item(i);
                String userName = xmlXPath.getUserNameExpression().evaluate(userRolesNode);
                SortedSet<GeoServerRole> roleSet = new TreeSet<GeoServerRole>();
                helper.user_roleMap.put(userName, roleSet);
                NodeList userRolesRefNodes =
                        (NodeList)
                                xmlXPath.getUserRolRefsExpression()
                                        .evaluate(userRolesNode, XPathConstants.NODESET);
                for (int j = 0; j < userRolesRefNodes.getLength(); j++) {
                    Node userRolesRefNode = userRolesRefNodes.item(j);
                    String roleRef =
                            xmlXPath.getUserRolRefNameExpression().evaluate(userRolesRefNode);
                    roleSet.add(helper.roleMap.get(roleRef));
                }
            }

            // group roles
            NodeList groupRolesNodes =
                    (NodeList)
                            xmlXPath.getGroupRolesExpression()
                                    .evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < groupRolesNodes.getLength(); i++) {
                Node groupRolesNode = groupRolesNodes.item(i);
                String groupName = xmlXPath.getGroupNameExpression().evaluate(groupRolesNode);
                SortedSet<GeoServerRole> roleSet = new TreeSet<GeoServerRole>();
                helper.group_roleMap.put(groupName, roleSet);
                NodeList groupRolesRefNodes =
                        (NodeList)
                                xmlXPath.getGroupRolRefsExpression()
                                        .evaluate(groupRolesNode, XPathConstants.NODESET);
                for (int j = 0; j < groupRolesRefNodes.getLength(); j++) {
                    Node groupRolesRefNode = groupRolesRefNodes.item(j);
                    String roleRef =
                            xmlXPath.getGroupRolRefNameExpression().evaluate(groupRolesRefNode);
                    roleSet.add(helper.roleMap.get(roleRef));
                }
            }
        } catch (XPathExpressionException ex) {
            throw new IOException(ex);
        }
    }
}
