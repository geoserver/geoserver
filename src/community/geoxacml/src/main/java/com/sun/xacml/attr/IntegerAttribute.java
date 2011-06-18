/*
 * @(#)IntegerAttribute.java
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
 * Representation of an xs:integer value. This class supports parsing xs:integer values. All objects
 * of this class are immutable and all methods of the class are thread-safe.
 * 
 * @since 1.0
 * @author Marco Barreno
 * @author Steve Hanna
 */
public class IntegerAttribute extends AttributeValue {
    /**
     * Official name of this type
     */
    public static final String identifier = "http://www.w3.org/2001/XMLSchema#integer";

    /**
     * URI version of name for this type
     */
    private static final URI identifierURI = URI.create(identifier);

    /**
     * The actual long value that this object represents.
     */
    private long value;

    /**
     * Creates a new <code>IntegerAttribute</code> that represents the long value supplied.
     * 
     * @param value
     *            the <code>long</code> value to be represented
     */
    public IntegerAttribute(long value) {
        super(identifierURI);
        this.value = value;
    }

    /**
     * Returns a new <code>IntegerAttribute</code> that represents the xs:integer at a particular
     * DOM node.
     * 
     * @param root
     *            the <code>Node</code> that contains the desired value
     * @return a new <code>IntegerAttribute</code> representing the appropriate value (null if there
     *         is a parsing error)
     * @throws NumberFormatException
     *             if the string form isn't a number
     */
    public static IntegerAttribute getInstance(Node root) throws NumberFormatException {
        return getInstance(root.getFirstChild().getNodeValue());
    }

    /**
     * Returns a new <code>IntegerAttribute</code> that represents the xs:integer value indicated by
     * the string provided.
     * 
     * @param value
     *            a string representing the desired value
     * @return a new <code>IntegerAttribute</code> representing the appropriate value (null if there
     *         is a parsing error)
     * @throws NumberFormatException
     *             if the string isn't a number
     */
    public static IntegerAttribute getInstance(String value) throws NumberFormatException {
        // Leading '+' is allowed per XML schema and not
        // by Long.parseLong. Strip it, if present.
        if ((value.length() >= 1) && (value.charAt(0) == '+'))
            value = value.substring(1);
        return new IntegerAttribute(Long.parseLong(value));
    }

    /**
     * Returns the <code>long</code> value represented by this object.
     * 
     * @return the <code>long</code> value
     */
    public long getValue() {
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
        if (!(o instanceof IntegerAttribute))
            return false;

        IntegerAttribute other = (IntegerAttribute) o;

        return (value == other.value);
    }

    /**
     * Returns the hashcode value used to index and compare this object with others of the same
     * type. Typically this is the hashcode of the backing data object.
     * 
     * @return the object's hashcode value
     */
    public int hashCode() {
        return (int) value;
    }

    /**
     *
     */
    public String encode() {
        return String.valueOf(value);
    }

}
