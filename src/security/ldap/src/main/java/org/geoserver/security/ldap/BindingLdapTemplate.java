/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import javax.naming.Name;
import javax.naming.directory.DirContext;
import org.springframework.ldap.core.AuthenticatedLdapEntryContextCallback;
import org.springframework.ldap.core.AuthenticationErrorCallback;
import org.springframework.ldap.core.ContextExecutor;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;

/**
 * Alternative SpringSecurityLdapTemplate, executing authentication without a prior search that
 * could raise errors by some LDAP servers.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 */
public class BindingLdapTemplate extends SpringSecurityLdapTemplate {

    public BindingLdapTemplate(ContextSource contextSource) {
        super(contextSource);
    }

    /** Alternative authenticate implementation, requiring a username instead of a filter. */
    @Override
    public boolean authenticate(
            Name base,
            String username,
            String password,
            final AuthenticatedLdapEntryContextCallback callback,
            AuthenticationErrorCallback errorCallback) {

        try {
            DirContext ctx = getContextSource().getContext(username, password);
            ContextExecutor ce =
                    new ContextExecutor() {
                        public Object executeWithContext(DirContext ctx)
                                throws javax.naming.NamingException {
                            callback.executeWithContext(ctx, null);
                            return null;
                        }
                    };
            try {
                ce.executeWithContext(ctx);
            } catch (javax.naming.NamingException e) {
                throw LdapUtils.convertLdapException(e);
            } catch (Exception e) {
                throw e;
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (Exception e) {
                        // Never mind this.
                    }
                }
            }

            return true;
        } catch (Exception e) {
            errorCallback.execute(e);
            return false;
        }
    }
}
