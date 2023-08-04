/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.name;


import java.util.List;

import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;


/**
 * This class parses a DN. The DN MUST respect this BNF grammar (as of RFC2253,
 * par. 3, and RFC1779, fig. 1) <br>
 * <p> - &lt;distinguishedName&gt; ::= &lt;name&gt; | e <br> - &lt;name&gt; ::=
 * &lt;name-component&gt; &lt;name-components&gt; <br> - &lt;name-components&gt;
 * ::= &lt;spaces&gt; &lt;separator&gt; &lt;spaces&gt; &lt;name-component&gt;
 * &lt;name-components&gt; | e <br> - &lt;name-component&gt; ::=
 * &lt;attributeType&gt; &lt;spaces&gt; '=' &lt;spaces&gt;
 * &lt;attributeValue&gt; &lt;attributeTypeAndValues&gt; <br> -
 * &lt;attributeTypeAndValues&gt; ::= &lt;spaces&gt; '+' &lt;spaces&gt;
 * &lt;attributeType&gt; &lt;spaces&gt; '=' &lt;spaces&gt;
 * &lt;attributeValue&gt; &lt;attributeTypeAndValues&gt; | e <br> -
 * &lt;attributeType&gt; ::= [a-zA-Z] &lt;keychars&gt; | &lt;oidPrefix&gt; [0-9]
 * &lt;digits&gt; &lt;oids&gt; | [0-9] &lt;digits&gt; &lt;oids&gt; <br> -
 * &lt;keychars&gt; ::= [a-zA-Z] &lt;keychars&gt; | [0-9] &lt;keychars&gt; | '-'
 * &lt;keychars&gt; | e <br> - &lt;oidPrefix&gt; ::= 'OID.' | 'oid.' | e <br> -
 * &lt;oids&gt; ::= '.' [0-9] &lt;digits&gt; &lt;oids&gt; | e <br> -
 * &lt;attributeValue&gt; ::= &lt;pairs-or-strings&gt; | '#' &lt;hexstring&gt;
 * |'"' &lt;quotechar-or-pairs&gt; '"' <br> - &lt;pairs-or-strings&gt; ::= '\'
 * &lt;pairchar&gt; &lt;pairs-or-strings&gt; | &lt;stringchar&gt;
 * &lt;pairs-or-strings&gt; | e <br> - &lt;quotechar-or-pairs&gt; ::=
 * &lt;quotechar&gt; &lt;quotechar-or-pairs&gt; | '\' &lt;pairchar&gt;
 * &lt;quotechar-or-pairs&gt; | e <br> - &lt;pairchar&gt; ::= ',' | '=' | '+' |
 * '&lt;' | '&gt;' | '#' | ';' | '\' | '"' | [0-9a-fA-F] [0-9a-fA-F] <br> -
 * &lt;hexstring&gt; ::= [0-9a-fA-F] [0-9a-fA-F] &lt;hexpairs&gt; <br> -
 * &lt;hexpairs&gt; ::= [0-9a-fA-F] [0-9a-fA-F] &lt;hexpairs&gt; | e <br> -
 * &lt;digits&gt; ::= [0-9] &lt;digits&gt; | e <br> - &lt;stringchar&gt; ::=
 * [0x00-0xFF] - [,=+&lt;&gt;#;\"\n\r] <br> - &lt;quotechar&gt; ::= [0x00-0xFF] -
 * [\"] <br> - &lt;separator&gt; ::= ',' | ';' <br> - &lt;spaces&gt; ::= ' '
 * &lt;spaces&gt; | e <br>
 * </p>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923667 $, $Date: 2010-03-16 12:22:36 +0200 (Tue, 16 Mar 2010) $
 */
public enum DnParser
{
    INSTANCE;

    /**
     * Get a reference to the NameParser. Needed to be compliant with the JNDI
     * API
     *
     * @return An instance of the NameParser
     */
    public static DnParser getNameParser()
    {
        return INSTANCE;
    }


    /**
     * Parse a DN.
     *
     * @param dn The DN to be parsed
     * @param rdns The list that will contain the RDNs
     * @throws LdapInvalidDnException If the DN is invalid
     */
    public static void parseInternal( String name, List<RDN> rdns ) throws LdapInvalidDnException
    {
        try
        {
            FastDnParser.INSTANCE.parseDn( name, rdns );
        }
        catch ( TooComplexException e )
        {
            rdns.clear();
            new ComplexDnParser().parseDn( name, rdns );
        }
    }


    /**
     * Validate a DN
     *
     * @param dn The DN to be parsed
     *            
     * @return <code>true</code> if the DN is valid
     */
    public static boolean validateInternal( String name )
    {
        DN dn = new DN();
        try
        {
            parseInternal( name, dn.rdns );
            return true;
        }
        catch ( LdapInvalidDnException e )
        {
            return false;
        }
    }


    /**
     * Parse a String and return a DN if the String is a valid DN
     *
     * @param dn The DN to parse
     * @return A DN
     * @throws LdapInvalidDnException If the String is not a valid DN
     */
    public DN parse( String dn ) throws LdapInvalidDnException
    {
        return new DN( dn );
    }
}
