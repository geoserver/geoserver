/* (c) 2014 - 2023 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.util.function.Supplier;
import javax.naming.directory.DirContext;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.AbstractContextSource;
import org.springframework.ldap.core.support.DefaultTlsDirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.authentication.SpringSecurityAuthenticationSource;

/**
 * LDAP utility class. Here are the LDAP access functionalities common to all LDAP security
 * services.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 */
public class LDAPUtils {

    /** Creates an LdapContext from a configuration object. */
    public static LdapContextSource createLdapContext(LDAPBaseSecurityServiceConfig ldapConfig) {
        LdapContextSource ldapContext =
                new DefaultSpringSecurityContextSource(ldapConfig.getServerURL());
        ldapContext.setCacheEnvironmentProperties(false);
        ldapContext.setAuthenticationSource(new SpringSecurityAuthenticationSource());

        if (ldapConfig.isUseTLS()) {
            // TLS does not play nicely with pooled connections
            ldapContext.setPooled(false);

            DefaultTlsDirContextAuthenticationStrategy tls =
                    new DefaultTlsDirContextAuthenticationStrategy();
            tls.setHostnameVerifier((hostname, session) -> true);

            ldapContext.setAuthenticationStrategy(tls);
        }
        return ldapContext;
    }

    /** Returns an LDAP template bounded to the given context, if not null. */
    public static SpringSecurityLdapTemplate getLdapTemplateInContext(
            final DirContext ctx, final SpringSecurityLdapTemplate template) {
        SpringSecurityLdapTemplate authTemplate;
        if (ctx == null) {
            authTemplate = template;
            ((AbstractContextSource) authTemplate.getContextSource()).setAnonymousReadOnly(true);
        } else {
            // if we have the authenticated context we build a new LdapTemplate
            // using it
            authTemplate =
                    new SpringSecurityLdapTemplate(
                            new ContextSource() {

                                @Override
                                public DirContext getReadOnlyContext() throws NamingException {
                                    return ctx;
                                }

                                @Override
                                public DirContext getReadWriteContext() throws NamingException {
                                    return ctx;
                                }

                                @Override
                                public DirContext getContext(String principal, String credentials)
                                        throws NamingException {
                                    return ctx;
                                }
                            });
        }
        return authTemplate;
    }

    /** Returns an LDAP template bounded to the given context supplier, if not null. */
    public static SpringSecurityLdapTemplate getLdapTemplateInContext(
            Supplier<DirContext> ctxSupplier, final SpringSecurityLdapTemplate template) {
        SpringSecurityLdapTemplate authTemplate;
        if (ctxSupplier == null) {
            authTemplate = template;
            ((AbstractContextSource) authTemplate.getContextSource()).setAnonymousReadOnly(true);
        } else {
            authTemplate =
                    new SpringSecurityLdapTemplate(
                            new ContextSource() {

                                @Override
                                public DirContext getReadOnlyContext() throws NamingException {
                                    return ctxSupplier.get();
                                }

                                @Override
                                public DirContext getReadWriteContext() throws NamingException {
                                    return ctxSupplier.get();
                                }

                                @Override
                                public DirContext getContext(String principal, String credentials)
                                        throws NamingException {
                                    return ctxSupplier.get();
                                }
                            });
        }
        return authTemplate;
    }
    /**
     * Escapes the "\" token in search strings for the Spring method template.search().
     *
     * <p>For the Spring method template.search(...) several characters are escaped with a "\". For
     * example, if you have user CNs with a comma such as "Smith, John", this comma is escaped with
     * a "\". This results in "Smith\, John". Characters other than commas are also escaped (see
     * https://ldap.com/ldap-dns-and-rdns).
     *
     * <p>In the new Spring version, the escape token must be "\\" or "\5C" to get correct results
     * when searching for users etc. For example the search string "Smith\, John" is escaped to
     * "Smith\\, John". Actually, there is a double escape.
     */
    public static String escapeSearchString(String searchString) {
        // Replace the escape token "\\" with "\\\\" to search.
        return searchString.replace("\\", "\\\\");
    }
}
