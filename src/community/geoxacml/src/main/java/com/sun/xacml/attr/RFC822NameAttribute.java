/*
 * @(#)RFC822NameAttribute.java
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

import org.w3c.dom.Node;

/**
 * Representation of an rfc822Name (ie, an email address).
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public class RFC822NameAttribute extends AttributeValue {

    /**
     * Official name of this type
     */
    public static final String identifier = "urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name";

    /**
     * URI version of name for this type
     */
    private static final URI identifierURI = URI.create(identifier);

    // the actual value being stored
    private String value;

    /**
     * Creates a new <code>RFC822NameAttribute</code> that represents the value supplied.
     * 
     * @param value
     *            the email address to be represented
     */
    public RFC822NameAttribute(String value) {
        super(identifierURI);

        // check that the string is an address, ie, that it has one and only
        // one '@' character in it
        String[] parts = value.split("@");
        if (parts.length != 2) {
            // this is malformed input
            throw new IllegalArgumentException("invalid RFC822Name: " + value);
        }

        // cannonicalize the name
        this.value = parts[0] + "@" + parts[1].toLowerCase();
    }

    /**
     * Returns a new <code>RFC822NameAttribute</code> that represents the email address at a
     * particular DOM node.
     * 
     * @param root
     *            the <code>Node</code> that contains the desired value
     * @return a new <code>RFC822NameAttribute</code> representing the appropriate value
     */
    public static RFC822NameAttribute getInstance(Node root) {
        return getInstance(root.getFirstChild().getNodeValue());
    }

    /**
     * Returns a new <code>RFC822NameAttribute</code> that represents the email address value
     * indicated by the string provided.
     * 
     * @param value
     *            a string representing the desired value
     * @return a new <code>RFC822NameAttribute</code> representing the appropriate value
     */
    public static RFC822NameAttribute getInstance(String value) {
        return new RFC822NameAttribute(value);
    }

    /**
     * Returns the name value represented by this object
     * 
     * @return the name
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns true if the input is an instance of this class and if its value equals the value
     * contained in this class.
     * 
     * @param o
     *            the object to compare
     * 
     * @return true if this object and the input represent the same value
     */
    public boolean equals(Object o) {
        if (!(o instanceof RFC822NameAttribute))
            return false;

        RFC822NameAttribute other = (RFC822NameAttribute) o;

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
        return value;
    }

}
