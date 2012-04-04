/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.xml;

import static org.geoserver.security.xml.XMLConstants.A_GROUP_ENABLED_UR;
import static org.geoserver.security.xml.XMLConstants.A_GROUP_NAME_UR;
import static org.geoserver.security.xml.XMLConstants.A_MEMBER_NAME_UR;
import static org.geoserver.security.xml.XMLConstants.A_PROPERTY_NAME_UR;
import static org.geoserver.security.xml.XMLConstants.A_USER_ENABLED_UR;
import static org.geoserver.security.xml.XMLConstants.A_USER_NAME_UR;
import static org.geoserver.security.xml.XMLConstants.A_USER_PASSWORD_UR;
import static org.geoserver.security.xml.XMLConstants.A_VERSION_UR;
import static org.geoserver.security.xml.XMLConstants.E_GROUPS_UR;
import static org.geoserver.security.xml.XMLConstants.E_GROUP_UR;
import static org.geoserver.security.xml.XMLConstants.E_MEMBER_UR;
import static org.geoserver.security.xml.XMLConstants.E_PROPERTY_UR;
import static org.geoserver.security.xml.XMLConstants.E_USERREGISTRY_UR;
import static org.geoserver.security.xml.XMLConstants.E_USERS_UR;
import static org.geoserver.security.xml.XMLConstants.E_USER_UR;
import static org.geoserver.security.xml.XMLConstants.NS_UR;
import static org.geoserver.security.xml.XMLConstants.VERSION_UR_1_0;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.SortedSet;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.file.LockFile;
import org.geoserver.security.impl.AbstractUserGroupStore;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.PasswordPolicyException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author christian
 *
 */
public class XMLUserGroupStore extends AbstractUserGroupStore {
            

    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.security.xml");
    protected File userFile;    
    protected LockFile lockFile = null; 
    /**
     * Validate against schema on load/store,
     * default = true;
     */
    

    private boolean validatingXMLSchema = true;

    
    
    public boolean isValidatingXMLSchema() {
        return validatingXMLSchema;
    }

    public void setValidatingXMLSchema(boolean validatingXMLSchema) {
        this.validatingXMLSchema = validatingXMLSchema;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverUserGroupStore#initializeFromServer(org.geoserver.security.GeoserverUserGroupService)
     */
    public void initializeFromService(GeoServerUserGroupService service) throws IOException {
        this.userFile=((XMLUserGroupService) service).userFile;
        this.validatingXMLSchema=((XMLUserGroupService) service).isValidatingXMLSchema();
        super.initializeFromService(service);
    }

    

    /* (non-Javadoc)
     * @see org.geoserver.security.impl.AbstractUserGroupStore#serialize()
     */
    @Override
    protected void serialize() throws IOException {
        
        
        DocumentBuilder builder=null;
        try {
            DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
            builder = fac.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            throw new IOException(e1);
        }
        Document doc =builder.newDocument();
        
        Element userreg = doc.createElement(E_USERREGISTRY_UR);
        doc.appendChild(userreg);
        userreg.setAttribute(javax.xml.XMLConstants.XMLNS_ATTRIBUTE, NS_UR);
        userreg.setAttribute( A_VERSION_UR, VERSION_UR_1_0);
        
        
        Element users = doc.createElement(E_USERS_UR);
        userreg.appendChild(users);
        for (GeoServerUser userObject : helper.userMap.values()) {
            Element user = doc.createElement(E_USER_UR);
            users.appendChild(user);
            user.setAttribute( A_USER_NAME_UR, userObject.getUsername());
            if (userObject.getPassword() != null) {
                user.setAttribute( A_USER_PASSWORD_UR, userObject.getPassword());
            }
            user.setAttribute( A_USER_ENABLED_UR, String.valueOf(userObject.isEnabled()));

            for (Object key: userObject.getProperties().keySet()) {
                Element property = doc.createElement(E_PROPERTY_UR);
                user.appendChild(property);
                property.setAttribute(A_PROPERTY_NAME_UR, key.toString());
                property.setTextContent(userObject.getProperties().getProperty(key.toString()));
            }
        }
        
        Element groups = doc.createElement(E_GROUPS_UR);
        userreg.appendChild(groups);
        for (GeoServerUserGroup groupObject : helper.groupMap.values()) {
            Element group = doc.createElement(E_GROUP_UR);
            groups.appendChild(group);
            group.setAttribute( A_GROUP_NAME_UR, groupObject.getGroupname());
            group.setAttribute( A_GROUP_ENABLED_UR, groupObject.isEnabled() ? "true" : "false");
            SortedSet<GeoServerUser> userObjects = helper.group_userMap.get(groupObject);
            if (userObjects !=null) {
                for (GeoServerUser userObject : userObjects) {
                    Element member = doc.createElement(E_MEMBER_UR);
                    group.appendChild(member);
                    member.setAttribute( A_MEMBER_NAME_UR, userObject.getUsername());
                }
            }
        }            

        // serialize the dom
        try {
//            TODO, wait for JAVA 6
//            if (isValidatingXMLSchema()) {
//                XMLValidator.Singleton.validateUserGroupRegistry(doc);
//            }            
            
            OutputFormat of = 
                    new OutputFormat("XML","UTF-8",true);
             XMLSerializer serializer = 
                                    new XMLSerializer();
             serializer.setOutputFormat(of);
             serializer.setOutputByteStream(new              
                    FileOutputStream(userFile));
             serializer.serialize(doc);
            
            /* standard java, but there is no possibility to set 
             * the number of chars to indent, each line is starting at 
             * column 0            
            Source source = new DOMSource(doc);
            Result result = new StreamResult(
                    new OutputStreamWriter(new FileOutputStream(userFile),"UTF-8"));
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            xformer.transform(source, result);
            */
                        
        } catch (Exception e) {
            throw new IOException(e);
        } 
        
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.impl.AbstractUserGroupService#deserialize()
     */
    @Override
    protected void deserialize() throws IOException {
        super.deserialize();
        releaseLock();
    }
    

    
    protected void ensureLock() throws IOException {
        if (lockFile!=null) return; // we have one
        lockFile=new LockFile(userFile);
        try {
            lockFile.writeLock();
        } catch (IOException ex) { // cannot obtain lock
            lockFile=null; // assert lockFile == null
            throw ex; // throw again
        }
    }

    
    protected void releaseLock()  {
        if (lockFile==null) return; // we have none        
        lockFile.writeUnLock();
        lockFile=null;
    }
    
    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void store() throws IOException {
       ensureLock();
       super.store();       
       releaseLock();
    }

    @Override
    public void addUser(GeoServerUser user) throws IOException,PasswordPolicyException  {
        ensureLock();
        super.addUser(user);
    }

    @Override
    public void addGroup(GeoServerUserGroup group) throws IOException {
        ensureLock();
        super.addGroup(group);
    }

    @Override
    public void updateUser(GeoServerUser user) throws IOException,PasswordPolicyException {
        ensureLock();
        super.updateUser(user);
    }

    @Override
    public void updateGroup(GeoServerUserGroup group) throws IOException {
        ensureLock();
        super.updateGroup(group);
    }

    @Override
    public boolean removeUser(GeoServerUser user) throws IOException {
        ensureLock();
        return super.removeUser(user);
    }

    @Override
    public boolean removeGroup(GeoServerUserGroup group) throws IOException {
        ensureLock();
        return super.removeGroup(group);
    }

    @Override
    public void associateUserToGroup(GeoServerUser user, GeoServerUserGroup group)
            throws IOException {
        ensureLock();
        super.associateUserToGroup(user, group);
    }

    @Override
    public void disAssociateUserFromGroup(GeoServerUser user, GeoServerUserGroup group)
            throws IOException {
        ensureLock();
        super.disAssociateUserFromGroup(user, group);
    }

    @Override
    public void clear() throws IOException {
        ensureLock();
        super.clear();
    }


}
