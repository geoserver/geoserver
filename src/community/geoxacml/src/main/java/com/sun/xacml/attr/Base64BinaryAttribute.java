/*
 * @(#)Base64BinaryAttribute.java
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

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import org.w3c.dom.Node;

import com.sun.xacml.ParsingException;

/**
 * Representation of an xsi:base64Binary value. This class supports parsing xsi:base64Binary values.
 * All objects of this class are immutable and all methods of the class are thread-safe.
 * 
 * @since 1.0
 * @author Steve Hanna
 */
public class Base64BinaryAttribute extends AttributeValue {
    /**
     * Official name of this type
     */
    public static final String identifier = "http://www.w3.org/2001/XMLSchema#base64Binary";

    /**
     * URI version of name for this type
     */
    private static final URI identifierURI = URI.create(identifier);

    /**
     * The actual binary value that this object represents.
     */
    private byte[] value;

    /**
     * The value returned by toString(). Cached, but only generated if needed.
     */
    private String strValue;

    /**
     * Creates a new <code>Base64BinaryAttribute</code> that represents the byte [] value supplied.
     * 
     * @param value
     *            the <code>byte []</code> value to be represented
     */
    public Base64BinaryAttribute(byte[] value) {
        super(identifierURI);

        // This will throw a NullPointerException if value == null.
        // That's what we want in that case.
        this.value = (byte[]) value.clone();
    }

    /**
     * Returns a new <code>Base64BinaryAttribute</code> that represents the xsi:base64Binary at a
     * particular DOM node.
     * 
     * @param root
     *            the <code>Node</code> that contains the desired value
     * @return a new <code>Base64BinaryAttribute</code> representing the appropriate value
     * @exception ParsingException
     *                if a parsing error occurs
     */
    public static Base64BinaryAttribute getInstance(Node root) throws ParsingException {
        return getInstance(root.getFirstChild().getNodeValue());
    }

    /**
     * Returns a new <code>Base64BinaryAttribute</code> that represents the xsi:base64Binary value
     * indicated by the string provided.
     * 
     * @param value
     *            a string representing the desired value
     * @return a new <code>Base64BinaryAttribute</code> representing the desired value
     * @exception ParsingException
     *                if a parsing error occurs
     */
    public static Base64BinaryAttribute getInstance(String value) throws ParsingException {
        byte[] bytes = null;

        try {
            bytes = Base64.decode(value, false);
        } catch (IOException e) {
            throw new ParsingException("Couldn't parse purported " + "Base64 string: " + value, e);
        }

        return new Base64BinaryAttribute(bytes);
    }

    /**
     * Returns the <code>byte []</code> value represented by this object. Note that this value is
     * cloned before returning to prevent unauthorized modifications.
     * 
     * @return the <code>byte []</code> value
     */
    public byte[] getValue() {
        return (byte[]) value.clone();
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
        if (!(o instanceof Base64BinaryAttribute))
            return false;

        Base64BinaryAttribute other = (Base64BinaryAttribute) o;

        return Arrays.equals(value, other.value);
    }

    /**
     * Returns the hashcode value used to index and compare this object with others of the same
     * type. Typically this is the hashcode of the backing data object.
     * 
     * @return the object's hashcode value
     */
    public int hashCode() {
        int code = (int) (value[0]);

        for (int i = 1; i < value.length; i++) {
            code *= 31;
            code += (int) (value[i]);
        }

        return code;
    }

    /**
     * Make the String representation of this object.
     * 
     * @return the String representation
     */
    private String makeStringRep() {
        return Base64.encode(value);
    }

    /**
     * Returns a String representation.
     * 
     * @return the String representation
     */
    public String toString() {
        if (strValue == null)
            strValue = makeStringRep();

        return "Base64BinaryAttribute: [\n" + strValue + "]\n";
    }

    /**
     *
     */
    public String encode() {
        if (strValue == null)
            strValue = makeStringRep();

        return strValue;
    }

}
