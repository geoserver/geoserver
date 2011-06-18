/*
 * @(#)DoubleAttribute.java
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
 * Representation of an xsi:double value. This class supports parsing xsi:double values. All objects
 * of this class are immutable and all methods of the class are thread-safe.
 * 
 * @since 1.0
 * @author Marco Barreno
 * @author Seth Proctor
 * @author Steve Hanna
 */
public class DoubleAttribute extends AttributeValue {
    /**
     * Official name of this type
     */
    public static final String identifier = "http://www.w3.org/2001/XMLSchema#double";

    /**
     * URI version of name for this type
     */
    private static final URI identifierURI = URI.create(identifier);

    /**
     * The actual double value that this object represents.
     */
    private double value;

    /**
     * Creates a new <code>DoubleAttribute</code> that represents the double value supplied.
     * 
     * @param value
     *            the <code>double</code> value to be represented
     */
    public DoubleAttribute(double value) {
        super(identifierURI);
        this.value = value;
    }

    /**
     * Returns a new <code>DoubleAttribute</code> that represents the xsi:double at a particular DOM
     * node.
     * 
     * @param root
     *            the <code>Node</code> that contains the desired value
     * @return a new <code>DoubleAttribute</code> representing the appropriate value (null if there
     *         is a parsing error)
     * @throws NumberFormatException
     *             if the string form is not a double
     */
    public static DoubleAttribute getInstance(Node root) throws NumberFormatException {
        return getInstance(root.getFirstChild().getNodeValue());
    }

    /**
     * Returns a new <code>DoubleAttribute</code> that represents the xsi:double value indicated by
     * the string provided.
     * 
     * @param value
     *            a string representing the desired value
     * @return a new <code>DoubleAttribute</code> representing the desired value (null if there is a
     *         parsing error)
     * @throws NumberFormatException
     *             if the value is not a double
     */
    public static DoubleAttribute getInstance(String value) {
        // Convert "INF" to "Infinity"
        if (value.endsWith("INF")) {
            int infIndex = value.lastIndexOf("INF");
            value = value.substring(0, infIndex) + "Infinity";
        }

        return new DoubleAttribute(Double.parseDouble(value));
    }

    /**
     * Returns the <code>double</code> value represented by this object.
     * 
     * @return the <code>double</code> value
     */
    public double getValue() {
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
        if (!(o instanceof DoubleAttribute))
            return false;

        DoubleAttribute other = (DoubleAttribute) o;

        // Handle the NaN case, where Java says NaNs are never
        // equal and XML Query says they always are
        if (Double.isNaN(value)) {
            // this is a NaN, so see if the other is as well
            if (Double.isNaN(other.value)) {
                // they're both NaNs, so they're equal
                return true;
            } else {
                // they're not both NaNs, so they're not equal
                return false;
            }
        } else {
            // not NaNs, so we can do a normal comparison
            return (value == other.value);
        }
    }

    /**
     * Returns the hashcode value used to index and compare this object with others of the same
     * type. Typically this is the hashcode of the backing data object.
     * 
     * @return the object's hashcode value
     */
    public int hashCode() {
        long v = Double.doubleToLongBits(value);
        return (int) (v ^ (v >>> 32));
    }

    /**
     *
     */
    public String encode() {
        return String.valueOf(value);
    }

}
