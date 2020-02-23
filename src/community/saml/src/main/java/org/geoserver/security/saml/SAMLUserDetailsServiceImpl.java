/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.saml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.RoleCalculator;
import org.geotools.util.logging.Logging;
import org.joda.time.format.DateTimeFormatter;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSAny;
import org.opensaml.xml.schema.XSDateTime;
import org.opensaml.xml.schema.XSInteger;
import org.opensaml.xml.schema.XSString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.util.StringUtils;

/**
 * Assigns {@link GeoServerRole} to user after successful authentication
 *
 * @author Xandros
 */
public class SAMLUserDetailsServiceImpl implements SAMLUserDetailsService {

    static final Logger LOGGER = Logging.getLogger(SAMLUserDetailsServiceImpl.class);

    private RoleSource roleSource;

    private String rolesHeaderAttribute;

    private String userGroupServiceName;

    private String roleServiceName;

    private GeoServerRoleConverter converter;

    private GeoServerSecurityManager securityManager;

    private HttpServletRequest request;

    /**
     * Used to identify local account of user referenced by data in the SAML assertion and return
     * UserDetails object describing the user roles
     */
    @Override
    public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {

        String principal = credential.getNameID().getValue();
        Set<GeoServerRole> roles = new HashSet<GeoServerRole>();
        if (GeoServerUser.ROOT_USERNAME.equals(principal)) {
            roles = Collections.singleton(GeoServerRole.ADMIN_ROLE);
        } else {
            try {
                roles.addAll(getRoles(principal));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (roles.contains(GeoServerRole.AUTHENTICATED_ROLE) == false)
                roles.add(GeoServerRole.AUTHENTICATED_ROLE);
        }
        GeoServerUser user = new GeoServerUser(principal);
        // , "", true, true, true, true, roles);
        for (Attribute attr : credential.getAttributes()) {
            String attrName =
                    attr.getFriendlyName() == null ? attr.getName() : attr.getFriendlyName();
            String attrValue = "";
            if (attr.getAttributeValues() != null && attr.getAttributeValues().size() > 0) {
                XMLObject xmlValue = attr.getAttributeValues().get(0);
                if (xmlValue instanceof XSAny) {
                    attrValue = ((XSAny) xmlValue).getTextContent();
                }
                if (xmlValue instanceof XSString) {
                    attrValue =
                            ((XSString) xmlValue).getValue() != null
                                    ? ((XSString) xmlValue).getValue()
                                    : "";
                    ;
                }
                if (xmlValue instanceof XSInteger) {
                    attrValue =
                            ((XSInteger) xmlValue).getValue() != null
                                    ? ((XSInteger) xmlValue).getValue().toString()
                                    : "";
                }
                if (xmlValue instanceof XSDateTime) {
                    DateTimeFormatter formatter = ((XSDateTime) xmlValue).getDateTimeFormatter();
                    attrValue =
                            ((XSDateTime) xmlValue).getValue() != null
                                    ? formatter.print(((XSDateTime) xmlValue).getValue())
                                    : "";
                    ;
                }
            }
            user.getProperties().setProperty(attrName, attrValue);
        }
        user.setAuthorities(roles);
        return user;
    }

    protected Collection<GeoServerRole> getRoles(String principal) throws IOException {

        if (PreAuthenticatedUserNameRoleSource.RoleService.equals(roleSource))
            return getRolesFromRoleService(principal);
        if (PreAuthenticatedUserNameRoleSource.UserGroupService.equals(roleSource))
            return getRolesFromUserGroupService(principal);
        if (PreAuthenticatedUserNameRoleSource.Header.equals(roleSource))
            return getRolesFromHttpAttribute(principal);

        throw new RuntimeException("Never should reach this point");
    }

    /**
     * Calculates roles from a {@link GeoServerRoleService} The default service is {@link
     * GeoServerSecurityManager#getActiveRoleService()}
     *
     * <p>The result contains all inherited roles, but no personalized roles
     */
    protected Collection<GeoServerRole> getRolesFromRoleService(String principal)
            throws IOException {
        boolean useActiveService = roleServiceName == null || roleServiceName.trim().length() == 0;

        GeoServerRoleService service =
                useActiveService
                        ? securityManager.getActiveRoleService()
                        : securityManager.loadRoleService(roleServiceName);

        RoleCalculator calc = new RoleCalculator(service);
        return calc.calculateRoles(principal);
    }

    /**
     * Calculates roles using a {@link GeoServerUserGroupService} if the principal is not found, an
     * empty collection is returned
     */
    protected Collection<GeoServerRole> getRolesFromUserGroupService(String principal)
            throws IOException {
        Collection<GeoServerRole> roles = new ArrayList<GeoServerRole>();

        GeoServerUserGroupService service =
                securityManager.loadUserGroupService(userGroupServiceName);
        UserDetails details = null;
        try {
            details = service.loadUserByUsername(principal);
        } catch (UsernameNotFoundException ex) {
            LOGGER.log(
                    Level.WARNING, "User " + principal + " not found in " + userGroupServiceName);
        }

        if (details != null) {
            for (GrantedAuthority auth : details.getAuthorities()) roles.add((GeoServerRole) auth);
        }
        return roles;
    }

    /**
     * Calculates roles using the String found in the http header attribute if no role string is
     * found, anempty collection is returned
     *
     * <p>The result contains personalized roles
     */
    protected Collection<GeoServerRole> getRolesFromHttpAttribute(String principal)
            throws IOException {
        Collection<GeoServerRole> roles = new ArrayList<GeoServerRole>();

        if (request != null) {
            String rolesString = request.getHeader(rolesHeaderAttribute);
            if (rolesString == null || rolesString.trim().length() == 0) {
                LOGGER.log(Level.WARNING, "No roles in header attribute: " + rolesHeaderAttribute);
                return roles;
            }

            roles.addAll(converter.convertRolesFromString(rolesString, principal));
            LOGGER.log(
                    Level.FINE,
                    "for principal "
                            + principal
                            + " found roles "
                            + StringUtils.collectionToCommaDelimitedString(roles)
                            + " in header "
                            + rolesHeaderAttribute);
        }
        return roles;
    }

    public void setRoleSource(RoleSource roleSource) {
        this.roleSource = roleSource;
    }

    public void setRolesHeaderAttribute(String rolesHeaderAttribute) {
        this.rolesHeaderAttribute = rolesHeaderAttribute;
    }

    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    public void setRoleServiceName(String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }

    public void setConverter(GeoServerRoleConverter converter) {
        this.converter = converter;
    }

    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }
}
