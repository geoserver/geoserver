/*
 * @(#)BooleanAttribute.java
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

import com.sun.xacml.ParsingException;

/**
 * Representation of an xs:boolean value. This class supports parsing xs:boolean values. All objects
 * of this class are immutable and all methods of the class are thread-safe.
 * 
 * @since 1.0
 * @author Marco Barreno
 * @author Steve Hanna
 */
public class BooleanAttribute extends AttributeValue {

    /**
     * Official name of this type
     */
    public static final String identifier = "http://www.w3.org/2001/XMLSchema#boolean";

    /**
     * URI version of name for this type
     */
    private static final URI identifierURI = URI.create(identifier);

    /**
     * Single instance of BooleanAttribute that represents true. Initialized by the static
     * initializer below.
     */
    private static BooleanAttribute trueInstance = new BooleanAttribute(true);

    /**
     * Single instance of BooleanAttribute that represents false. Initialized by the static
     * initializer below.
     */
    private static BooleanAttribute falseInstance = new BooleanAttribute(false);

    /**
     * The actual boolean value that this object represents.
     */
    private boolean value;

    /**
     * Creates a new <code>BooleanAttribute</code> that represents the boolean value supplied.
     * <p>
     * This constructor is private because it should not be used by anyone other than the static
     * initializer in this class. Instead, please use one of the getInstance methods, which will
     * ensure that only two BooleanAttribute objects are created, thus avoiding excess object
     * creation.
     */
    private BooleanAttribute(boolean value) {
        super(identifierURI);

        this.value = value;
    }

    /**
     * Returns a <code>BooleanAttribute</code> that represents the xs:boolean at a particular DOM
     * node.
     * 
     * @param root
     *            the <code>Node</code> that contains the desired value
     * @return a <code>BooleanAttribute</code> representing the appropriate value (null if there is
     *         a parsing error)
     */
    public static BooleanAttribute getInstance(Node root) throws ParsingException {
        return getInstance(root.getFirstChild().getNodeValue());
    }

    /**
     * Returns a <code>BooleanAttribute</code> that represents the xs:boolean value indicated by the
     * string provided.
     * 
     * @param value
     *            a string representing the desired value
     * @return a <code>BooleanAttribute</code> representing the appropriate value (null if there is
     *         a parsing error)
     */
    public static BooleanAttribute getInstance(String value) throws ParsingException {
        if (value.equals("true"))
            return trueInstance;
        if (value.equals("false"))
            return falseInstance;

        throw new ParsingException("Boolean string must be true or false");
    }

    /**
     * Returns a <code>BooleanAttribute</code> that represents the boolean value provided.
     * 
     * @param value
     *            a boolean representing the desired value
     * @return a <code>BooleanAttribute</code> representing the appropriate value
     */
    public static BooleanAttribute getInstance(boolean value) {

        if (value)
            return trueInstance;
        else
            return falseInstance;
    }

    /**
     * Returns a <code>BooleanAttribute</code> that represents a true value.
     * 
     * @return a <code>BooleanAttribute</code> representing a true value
     */
    public static BooleanAttribute getTrueInstance() {

        return trueInstance;
    }

    /**
     * Returns a <code>BooleanAttribute</code> that represents a false value.
     * 
     * @return a <code>BooleanAttribute</code> representing a false value
     */
    public static BooleanAttribute getFalseInstance() {

        return falseInstance;
    }

    /**
     * Returns the <code>boolean</code> value represented by this object.
     * 
     * @return the <code>boolean</code> value
     */
    public boolean getValue() {
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
        if (!(o instanceof BooleanAttribute))
            return false;

        BooleanAttribute other = (BooleanAttribute) o;

        return (value == other.value);
    }

    /**
     * Returns the hashcode value used to index and compare this object with others of the same
     * type. Typically this is the hashcode of the backing data object.
     * 
     * @return the object's hashcode value
     */
    public int hashCode() {
        // these numbers come from the javadoc for java.lang.Boolean...no,
        // really, they do. I can't imagine what they were thinking...
        return (value ? 1231 : 1237);
    }

    /**
     *
     */
    public String encode() {
        return (value ? "true" : "false");
    }

}
