/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

/**
 * copied and modified from org.springframework.ldap.test.LdapTestUtils to allow anonymous access
 * (there was no alternative way)
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 * @author Mattias Hellborg Arthursson
 * @author Niels Charlier
 */
public class LDAPTestUtils {
    public static final int LDAP_SERVER_PORT = 10389;
    public static final String LDAP_SERVER_URL = "ldap://127.0.0.1:10389";
    public static final String LDAP_BASE_PATH = "dc=example,dc=com";
    public static final String DEFAULT_PRINCIPAL = "uid=admin,ou=system";
    public static final String DEFAULT_PASSWORD = "secret";
}
