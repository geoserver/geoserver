/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.xml;

import static org.geoserver.security.xml.XMLConstants.A_GROUPNAME_RR;
import static org.geoserver.security.xml.XMLConstants.A_PARENTID_RR;
import static org.geoserver.security.xml.XMLConstants.A_PROPERTY_NAME_RR;
import static org.geoserver.security.xml.XMLConstants.A_ROLEID_RR;
import static org.geoserver.security.xml.XMLConstants.A_ROLEREFID_RR;
import static org.geoserver.security.xml.XMLConstants.A_USERNAME_RR;
import static org.geoserver.security.xml.XMLConstants.A_VERSION_RR;
import static org.geoserver.security.xml.XMLConstants.E_GROUPLIST_RR;
import static org.geoserver.security.xml.XMLConstants.E_GROUPROLES_RR;
import static org.geoserver.security.xml.XMLConstants.E_PROPERTY_RR;
import static org.geoserver.security.xml.XMLConstants.E_ROLELIST_RR;
import static org.geoserver.security.xml.XMLConstants.E_ROLEREF_RR;
import static org.geoserver.security.xml.XMLConstants.E_ROLEREGISTRY_RR;
import static org.geoserver.security.xml.XMLConstants.E_ROLE_RR;
import static org.geoserver.security.xml.XMLConstants.E_USERLIST_RR;
import static org.geoserver.security.xml.XMLConstants.E_USERROLES_RR;
import static org.geoserver.security.xml.XMLConstants.NS_RR;
import static org.geoserver.security.xml.XMLConstants.VERSION_RR_1_0;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.SortedSet;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.file.LockFile;
import org.geoserver.security.impl.AbstractRoleStore;
import org.geoserver.security.impl.GeoServerRole;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLRoleStore extends AbstractRoleStore {

    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.xml");

    protected Resource roleResource;
    // TODO: use resource locking system
    protected LockFile lockFile = null;
    /** Validate against schema on load/store, default = true; */
    private boolean validatingXMLSchema = true;

    public boolean isValidatingXMLSchema() {
        return validatingXMLSchema;
    }

    public void setValidatingXMLSchema(boolean validatingXMLSchema) {
        this.validatingXMLSchema = validatingXMLSchema;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.impl.AbstractRoleStore#initializeFromService(org.geoserver.security.GeoserverRoleService)
     */
    public void initializeFromService(GeoServerRoleService service) throws IOException {
        this.roleResource = ((XMLRoleService) service).roleResource;
        this.validatingXMLSchema = ((XMLRoleService) service).isValidatingXMLSchema();
        super.initializeFromService(service);
    }

    @Override
    protected void serialize() throws IOException {

        DocumentBuilder builder = null;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            throw new IOException(e1);
        }
        Document doc = builder.newDocument();

        Element rolereg = doc.createElement(E_ROLEREGISTRY_RR);
        doc.appendChild(rolereg);
        rolereg.setAttribute(javax.xml.XMLConstants.XMLNS_ATTRIBUTE, NS_RR);
        rolereg.setAttribute(A_VERSION_RR, VERSION_RR_1_0);

        Element rolelist = doc.createElement(E_ROLELIST_RR);
        rolereg.appendChild(rolelist);

        for (GeoServerRole roleObject : helper.roleMap.values()) {
            Element role = doc.createElement(E_ROLE_RR);
            rolelist.appendChild(role);
            role.setAttribute(A_ROLEID_RR, roleObject.getAuthority());
            GeoServerRole parentObject = helper.role_parentMap.get(roleObject);
            if (parentObject != null) {
                role.setAttribute(A_PARENTID_RR, parentObject.getAuthority());
            }
            for (Object key : roleObject.getProperties().keySet()) {
                Element property = doc.createElement(E_PROPERTY_RR);
                role.appendChild(property);
                property.setAttribute(A_PROPERTY_NAME_RR, key.toString());
                property.setTextContent(roleObject.getProperties().getProperty(key.toString()));
            }
        }

        Element userList = doc.createElement(E_USERLIST_RR);
        rolereg.appendChild(userList);
        for (String userName : helper.user_roleMap.keySet()) {
            Element userroles = doc.createElement(E_USERROLES_RR);
            userList.appendChild(userroles);
            userroles.setAttribute(A_USERNAME_RR, userName);
            SortedSet<GeoServerRole> roleObjects = helper.user_roleMap.get(userName);
            for (GeoServerRole roleObject : roleObjects) {
                Element ref = doc.createElement(E_ROLEREF_RR);
                userroles.appendChild(ref);
                ref.setAttribute(A_ROLEREFID_RR, roleObject.getAuthority());
            }
        }

        Element groupList = doc.createElement(E_GROUPLIST_RR);
        rolereg.appendChild(groupList);

        for (String groupName : helper.group_roleMap.keySet()) {
            Element grouproles = doc.createElement(E_GROUPROLES_RR);
            groupList.appendChild(grouproles);
            grouproles.setAttribute(A_GROUPNAME_RR, groupName);
            SortedSet<GeoServerRole> roleObjects = helper.group_roleMap.get(groupName);
            for (GeoServerRole roleObject : roleObjects) {
                Element ref = doc.createElement(E_ROLEREF_RR);
                grouproles.appendChild(ref);
                ref.setAttribute(A_ROLEREFID_RR, roleObject.getAuthority());
            }
        }

        // serialize the dom
        try {
            //            TODO, wait for JAVA 6
            //            if (isValidatingXMLSchema()) {
            //                XMLValidator.Singleton.validateRoleRegistry(doc);
            //            }

            Transformer tx = TransformerFactory.newInstance().newTransformer();
            tx.setOutputProperty(OutputKeys.METHOD, "XML");
            tx.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tx.setOutputProperty(OutputKeys.INDENT, "yes");

            try (OutputStream out = new BufferedOutputStream(roleResource.out())) {
                tx.transform(new DOMSource(doc), new StreamResult(out));
                out.flush();
            }

            /* standard java, but there is no possibility to set
             * the number of chars to indent, each line is starting at
             * column 0
            Source source = new DOMSource(doc);
            // Prepare the output file
            Result result = new StreamResult(
                    new OutputStreamWriter(new FileOutputStream(roleFile),"UTF-8"));

            TransformerFactory fac = TransformerFactory.newInstance();
            Transformer xformer = fac.newTransformer();
            xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            xformer.transform(source, result);
            */

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void deserialize() throws IOException {
        super.deserialize();
        releaseLock();
    }

    @Override
    public String toString() {
        return getName();
    }

    protected void ensureLock() throws IOException {
        if (lockFile != null) return; // we have one
        lockFile = new LockFile(roleResource);
        try {
            lockFile.writeLock();
        } catch (IOException ex) { // cannot obtain lock
            lockFile = null; // assert lockFile == null
            throw ex; // throw again
        }
    }

    protected void releaseLock() {
        if (lockFile == null) return; // we have none
        lockFile.writeUnLock();
        lockFile = null;
    }

    @Override
    public void addRole(GeoServerRole role) throws IOException {
        ensureLock();
        super.addRole(role);
    }

    @Override
    public void updateRole(GeoServerRole role) throws IOException {
        ensureLock();
        super.updateRole(role);
    }

    @Override
    public boolean removeRole(GeoServerRole role) throws IOException {
        ensureLock();
        return super.removeRole(role);
    }

    @Override
    public void store() throws IOException {
        ensureLock();
        super.store();
        releaseLock();
    }

    @Override
    public void disAssociateRoleFromGroup(GeoServerRole role, String groupname) throws IOException {
        ensureLock();
        super.disAssociateRoleFromGroup(role, groupname);
    }

    @Override
    public void associateRoleToGroup(GeoServerRole role, String groupname) throws IOException {
        ensureLock();
        super.associateRoleToGroup(role, groupname);
    }

    @Override
    public void associateRoleToUser(GeoServerRole role, String username) throws IOException {
        ensureLock();
        super.associateRoleToUser(role, username);
    }

    @Override
    public void disAssociateRoleFromUser(GeoServerRole role, String username) throws IOException {
        ensureLock();
        super.disAssociateRoleFromUser(role, username);
    }

    @Override
    public void setParentRole(GeoServerRole role, GeoServerRole parentRole) throws IOException {
        ensureLock();
        super.setParentRole(role, parentRole);
    }

    @Override
    public void clear() throws IOException {
        ensureLock();
        super.clear();
    }
}
