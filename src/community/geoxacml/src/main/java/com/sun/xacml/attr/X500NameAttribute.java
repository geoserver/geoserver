/*
 * @(#)X500NameAttribute.java
 *
 * Copyright 2003-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistribution of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *   2. Redistribution in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use in
 * the design, construction, operation or maintenance of any nuclear facility.
 */

package com.sun.xacml.attr;

import java.net.URI;

import javax.security.auth.x500.X500Principal;

import org.w3c.dom.Node;

/**
 * Representation of an X500 Name.
 * 
 * @since 1.0
 * @author Marco Barreno
 * @author Seth Proctor
 */
public class X500NameAttribute extends AttributeValue {

    /**
     * Official name of this type
     */
    public static final String identifier = "urn:oasis:names:tc:xacml:1.0:data-type:x500Name";

    // the actual value being stored
    private X500Principal value;

    /**
     * URI version of name for this type
     */
    private static final URI identifierURI = URI.create(identifier);

    /**
     * Creates a new <code>X500NameAttribute</code> that represents the value supplied.
     * 
     * @param value
     *            the X500 Name to be represented
     */
    public X500NameAttribute(X500Principal value) {
        super(identifierURI);
        this.value = value;
    }

    /**
     * Returns a new <codeX500NameAttribute</code> that represents the X500 Name at a particular DOM
     * node.
     * 
     * @param root
     *            the <code>Node</code> that contains the desired value
     * @return a new <code>X500NameAttribute</code> representing the appropriate value
     * @throws IllegalArgumentException
     *             if value is improperly specified
     */
    public static X500NameAttribute getInstance(Node root) throws IllegalArgumentException {
        return getInstance(root.getFirstChild().getNodeValue());
    }

    /**
     * Returns a new <code>X500NameAttribute</code> that represents the X500 Name value indicated by
     * the string provided.
     * 
     * @param value
     *            a string representing the desired value
     * @return a new <code>X500NameAttribute</code> representing the appropriate value
     * @throws IllegalArgumentException
     *             if value is improperly specified
     */
    public static X500NameAttribute getInstance(String value) throws IllegalArgumentException {
        return new X500NameAttribute(new X500Principal(value));
    }

    /**
     * Returns the name value represented by this object
     * 
     * @return the name
     */
    public X500Principal getValue() {
        return value;
    }

    /**
     * Returns true if the input is an instance of this class and if its value equals the value
     * contained in this class. This method deviates slightly from the XACML spec in the way that it
     * handles RDNs with multiple attributeTypeAndValue pairs and some additional canonicalization
     * steps. This method uses the procedure used by
     * <code>javax.security.auth.x500.X500Principal.equals()</code>, while the XACML spec uses a
     * slightly different procedure. In practice, it is expected that this difference will not be
     * noticeable. For more details, refer to the javadoc for <code>X500Principal.equals()</code>
     * and the XACML specification.
     * 
     * @param o
     *            the object to compare
     * 
     * @return true if this object and the input represent the same value
     */
    public boolean equals(Object o) {
        if (!(o instanceof X500NameAttribute))
            return false;

        X500NameAttribute other = (X500NameAttribute) o;

        return value.equals(other.value);
    }

    /**
     * Returns the hashcode value used to index and compare this object with others of the same
     * type. Typically this is the hashcode of the backing data object.
     * 
     * @return the object's hashcode value
     */
    public int hashCode() {
        return value.hashCode();
    }

    /**
     *
     */
    public String encode() {
        return value.getName();
    }

}
