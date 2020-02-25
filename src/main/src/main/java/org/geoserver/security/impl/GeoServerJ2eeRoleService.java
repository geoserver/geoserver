/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.event.RoleLoadedEvent;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.filter.GeoServerJ2eeAuthenticationFilter;
import org.geotools.util.logging.Logging;
import org.springframework.util.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Implementation for {@link GeoServerRoleService} obtaining roles from <b>role-name</b> elements
 * contained in WEB-INF/web.xml
 *
 * <p>This implementation could be used in combination with {@link
 * GeoServerJ2eeAuthenticationFilter} objects.
 *
 * @author Christian
 */
public class GeoServerJ2eeRoleService extends AbstractGeoServerSecurityService
        implements GeoServerRoleService {

    public class WebXMLContentHandler implements ContentHandler {

        public static final String SECURITY_ROLE_REF = "security-role-ref";
        public static final String AUTH_CONSTRAINT = "auth-constraint";
        public static final String SECURITY_ROLE = "security-role";
        public static final String ROLE_NAME = "role-name";
        public static final String ROLE_LINK = "role-link";

        private String currentValue;
        private String roleName;
        private boolean inSecRoleRef, inAuthConstraint, inSecRole;
        private Map<String, String> inSecRoleRefRoles = new HashMap<String, String>();
        private List<String> inAuthConstraintRoles = new ArrayList<String>();
        private List<String> inSecRoleRoles = new ArrayList<String>();

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            currentValue = new String(ch, start, length);
        }

        @Override
        public void endDocument() throws SAXException {}

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (SECURITY_ROLE_REF.equals(localName)) {
                inSecRoleRef = false;
            }
            if (AUTH_CONSTRAINT.equals(localName)) {
                inAuthConstraint = false;
            }
            if (SECURITY_ROLE.equals(localName)) {
                inSecRole = false;
            }

            if (ROLE_NAME.endsWith(localName)) {
                if (inSecRoleRef) roleName = currentValue.trim();
                if (inAuthConstraint) inAuthConstraintRoles.add(currentValue.trim());
                if (inSecRole) inSecRoleRoles.add(currentValue.trim());
            }

            if (ROLE_LINK.endsWith(localName)) {
                inSecRoleRefRoles.put(roleName, currentValue.trim());
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs)
                throws SAXException {
            if (SECURITY_ROLE_REF.equals(localName)) {
                inSecRoleRef = true;
            }
            if (AUTH_CONSTRAINT.equals(localName)) {
                inAuthConstraint = true;
            }
            if (SECURITY_ROLE.equals(localName)) {
                inSecRole = true;
            }
        }

        @Override
        public void endPrefixMapping(String arg0) throws SAXException {}

        @Override
        public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {}

        @Override
        public void processingInstruction(String arg0, String arg1) throws SAXException {}

        @Override
        public void setDocumentLocator(Locator arg0) {}

        @Override
        public void skippedEntity(String arg0) throws SAXException {}

        @Override
        public void startDocument() throws SAXException {}

        @Override
        public void startPrefixMapping(String arg0, String arg1) throws SAXException {}

        public Map<String, String> getInSecRoleRefRoles() {
            return inSecRoleRefRoles;
        }

        public List<String> getInAuthConstraintRoles() {
            return inAuthConstraintRoles;
        }

        public List<String> getInSecRoleRoles() {
            return inSecRoleRoles;
        }
    };

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    protected String adminRoleName, groupAdminRoleName;
    protected SortedSet<GeoServerRole> emptySet;
    protected SortedSet<String> emptyStringSet;
    protected Map<String, String> parentMappings;
    protected HashMap<String, GeoServerRole> roleMap;
    protected SortedSet<GeoServerRole> roleSet;

    protected Set<RoleLoadedListener> listeners =
            Collections.synchronizedSet(new HashSet<RoleLoadedListener>());

    protected GeoServerJ2eeRoleService() throws IOException {
        emptySet = Collections.unmodifiableSortedSet(new TreeSet<GeoServerRole>());
        emptyStringSet = Collections.unmodifiableSortedSet(new TreeSet<String>());
        parentMappings = new HashMap<String, String>();
        load();
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        adminRoleName = ((SecurityRoleServiceConfig) config).getAdminRoleName();
        groupAdminRoleName = ((SecurityRoleServiceConfig) config).getGroupAdminRoleName();
        load();
    }

    @Override
    public GeoServerRole getAdminRole() {
        if (StringUtils.hasLength(adminRoleName) == false) return null;
        try {
            return getRoleByName(adminRoleName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GeoServerRole getGroupAdminRole() {
        if (StringUtils.hasLength(groupAdminRoleName) == false) return null;
        try {
            return getRoleByName(groupAdminRoleName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GeoServerRoleStore createStore() throws IOException {
        return null;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#registerRoleLoadedListener(org.geoserver.security.event.RoleLoadedListener)
     */
    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#unregisterRoleLoadedListener(org.geoserver.security.event.RoleLoadedListener)
     */
    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        listeners.remove(listener);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getRoles()
     */
    public SortedSet<GeoServerRole> getRoles() throws IOException {
        if (roleSet != null) return roleSet;
        return emptySet;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#load()
     */
    public synchronized void load() throws IOException {

        // parse web.xml only once because it cannot change during runtime
        if (roleMap != null) return;

        LOGGER.info("Start reloading roles for service named " + getName());
        File webXML = GeoServerExtensions.file("WEB-INF/web.xml");

        if (webXML == null) {
            throw new IOException("Cannot open /WEB-INF/web.xml");
        }

        LOGGER.info("Extracting roles from: " + webXML.getCanonicalPath());

        Set<String> roles = parseWebXML(webXML);
        roleMap = new HashMap<String, GeoServerRole>();

        for (String role : roles) {
            roleMap.put(role, createRoleObject(role));
            parentMappings.put(role, null);
        }
        roleSet = new TreeSet<GeoServerRole>();
        roleSet.addAll(roleMap.values());

        LOGGER.info("Reloading roles successful for service named " + getName());
        fireRoleLoadedEvent();
    }

    protected Set<String> parseWebXML(File file) throws IOException {

        WebXMLContentHandler handler = new WebXMLContentHandler();
        Set<String> result = new HashSet<String>();

        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setNamespaceAware(true);
            SAXParser parser = parserFactory.newSAXParser();
            XMLReader xmlReader = parser.getXMLReader();

            InputSource inputSource = new InputSource(new FileInputStream(file));

            xmlReader.setContentHandler(handler);
            // suppress validation
            xmlReader.setEntityResolver(
                    new EntityResolver() {
                        @Override
                        public InputSource resolveEntity(String publicId, String systemId)
                                throws SAXException, IOException {
                            return new InputSource(new StringReader(""));
                        }
                    });
            xmlReader.parse(inputSource);
        } catch (Exception e) {
            throw new IOException(e);
        }

        result.addAll(handler.getInAuthConstraintRoles());
        result.addAll(handler.getInSecRoleRoles());
        result.addAll(handler.getInSecRoleRefRoles().keySet());
        result.addAll(handler.getInSecRoleRefRoles().values());

        return result;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getRolesForUser(java.lang.String)
     */
    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        return emptySet;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getRolesForGroup(java.lang.String)
     */
    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        return emptySet;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#createRoleObject(java.lang.String)
     */
    public GeoServerRole createRoleObject(String role) throws IOException {
        return new GeoServerRole(role);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getParentRole(org.geoserver.security.impl.GeoserverRole)
     */
    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        return null;
    }

    //    protected void checkRole(GeoserverRole role) {
    //        if (roleMap.containsKey(role.getAuthority())==false)
    //            throw new IllegalArgumentException("Role: " +  role.getAuthority()+ " does not
    // exist");
    //    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getRoleByName(java.lang.String)
     */
    public GeoServerRole getRoleByName(String role) throws IOException {
        if (roleMap != null) return roleMap.get(role);
        return null;
    }

    /** Fire {@link RoleLoadedEvent} for all listeners */
    protected void fireRoleLoadedEvent() {
        RoleLoadedEvent event = new RoleLoadedEvent(this);
        for (RoleLoadedListener listener : listeners) {
            listener.rolesChanged(event);
        }
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getGroupNamesForRole(org.geoserver.security.impl.GeoserverRole)
     */
    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        return emptyStringSet;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getUserNamesForRole(org.geoserver.security.impl.GeoserverRole)
     */
    public SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException {
        return emptyStringSet;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getParentMappings()
     */
    public Map<String, String> getParentMappings() throws IOException {
        return parentMappings;
    }

    /**
     * (non-Javadoc)
     *
     * @see org.geoserver.security.GeoServerRoleService#personalizeRoleParams(java.lang.String,
     *     java.util.Properties, java.lang.String, java.util.Properties)
     *     <p>Do nothing, J2EE roles have no role params
     */
    public Properties personalizeRoleParams(
            String roleName, Properties roleParams, String userName, Properties userProps)
            throws IOException {
        return null;
    }

    /** The root configuration for the role service. */
    public Resource getConfigRoot() throws IOException {
        return getSecurityManager().role().get(getName());
    }

    public int getRoleCount() throws IOException {
        if (roleSet != null) return roleSet.size();
        return 0;
    }
}
