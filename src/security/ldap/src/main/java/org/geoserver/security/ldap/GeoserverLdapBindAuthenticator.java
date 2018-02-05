/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.ldap;

import java.text.MessageFormat;

import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControl;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControlExtractor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Extended BindAuthenticator using a filter to find user data as an alternative
 * to a direct dn access.
 * 
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 * 
 */
public class GeoserverLdapBindAuthenticator extends BindAuthenticator {

    private static final Log logger = LogFactory
            .getLog(GeoserverLdapBindAuthenticator.class);

    private String userFilter = "";

    private String userFormat = "";

    private String bindUsername = "";

    private String bindPassword = "";

    public GeoserverLdapBindAuthenticator(
            BaseLdapPathContextSource contextSource) {
        super(contextSource);
    }

    public void setUserFilter(String userFilter) {
        this.userFilter = userFilter;
    }

    public void setBindUsername(String bindUsername) { this.bindUsername = bindUsername; }

    public void setBindPassword(String bindPassword) { this.bindPassword = bindPassword; }

    @Override
    public DirContextOperations authenticate(Authentication authentication) {
        if (userFilter == null || userFilter.equals("")) {
            // authenticate using dn
            return super.authenticate(authentication);
        } else {
            return authenticateUsingFilter(authentication);
        }
    }

    /**
     * If userFilter is defined we extract user data using the filter and
     * dnPattern (if defined) to transform username for authentication.
     * 
     * @param authentication
     *
     */
    protected DirContextOperations authenticateUsingFilter(
            Authentication authentication) {
        DirContextOperations user = null;
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class,
                authentication,
                "Can only process UsernamePasswordAuthenticationToken objects");

        String username = authentication.getName();
        String originalUser = username;
        String password = (String) authentication.getCredentials();
        // format given username if required
        if (userFormat != null && !userFormat.equals("")) {
            username = MessageFormat.format(userFormat, username);
        }
        if (!StringUtils.hasLength(password)) {
            logger.debug("Rejecting empty password for user " + username);
            throw new BadCredentialsException(messages.getMessage(
                    "BindAuthenticator.emptyPassword", "Empty Password"));
        }

        DirContext ctx = null;
        String userDnStr = "";
        String fullUserDnStr = "";
        try {
            boolean useBindUsername = bindUsername != null && !bindUsername.equals("");
            if (useBindUsername) {
                // If a bind username has been specified, use it instead of using user credentials
                if (!StringUtils.hasLength(bindPassword)) {
                    logger.debug("Rejecting empty password for user " + bindUsername);
                    throw new BadCredentialsException(messages.getMessage(
                            "BindAuthenticator.emptyPassword", "Empty Password"));
                }
                ctx = getContextSource().getContext(bindUsername, bindPassword);
            } else {
                ctx = getContextSource().getContext(username, password);
            }

            // Check for password policy control
            PasswordPolicyControl ppolicy = PasswordPolicyControlExtractor
                    .extractControl(ctx);

            logger.debug("Retrieving user object using filter...");
            SearchControls searchCtls = new SearchControls();
            searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            user = SpringSecurityLdapTemplate.searchForSingleEntryInternal(ctx,
                    searchCtls, "", userFilter, new Object[] { username, originalUser });
            logger.debug("User filter: " + userFilter);
            userDnStr = user.getDn().toString();
            fullUserDnStr = user.getNameInNamespace();
            logger.debug("User DN: " + fullUserDnStr);
            if (ppolicy != null) {
                user.setAttributeValue(ppolicy.getID(), ppolicy);
            }
            // If a bind username has been specified, check user provided credentials against found DN
            if (useBindUsername) {
                LdapUtils.closeContext(ctx);
                try {
                    ctx = getContextSource().getContext(fullUserDnStr, password);
                } catch (Exception e) {
                    user = null;
                    throw e;
                }
            }

        } catch (NamingException e) {
            // This will be thrown if an invalid user name is used and the
            // method may
            // be called multiple times to try different names, so we trap the
            // exception
            // unless a subclass wishes to implement more specialized behaviour.
            if ((e instanceof org.springframework.ldap.AuthenticationException)
                    || (e instanceof org.springframework.ldap.OperationNotSupportedException)) {
                handleBindException(userDnStr, username, e);
            } else {
                throw e;
            }
        } catch (javax.naming.NamingException e) {
            throw LdapUtils.convertLdapException(e);
        } finally {
            LdapUtils.closeContext(ctx);
        }

        if (user == null) {
            throw new BadCredentialsException(messages.getMessage(
                    "BindAuthenticator.badCredentials", "Bad credentials"));
        }

        return user;
    }

    public void setUserFormat(String userFormat) {
        this.userFormat = userFormat;
    }

}
