/*
 * @(#)IPAddressAttribute.java
 *
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
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

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import org.w3c.dom.Node;

import com.sun.xacml.ParsingException;

/**
 * Represents the IPAddress datatype introduced in XACML 2.0. All objects of this class are
 * immutable and all methods of the class are thread-safe.
 * <p>
 * To create an instance of an ipAddress from an encoded String or a DOM Node you should use the
 * <code>getInstance</code> methods provided by this class. To construct an ipAddress instance
 * directly, you must use the constructors provided by <code>IPv4AddressAttribute</code> and
 * <code>IPv6AddressAttribute</code>. These will both create an attribute of XACML type ipAddress,
 * but will handle the differences in these two representations correctly.
 * 
 * @since 2.0
 * @author Seth Proctor
 */
public abstract class IPAddressAttribute extends AttributeValue {

    /**
     * Official name of this type
     */
    public static final String identifier = "urn:oasis:names:tc:xacml:2.0:data-type:ipAddress";

    /**
     * URI version of name for this type
     */
    private static final URI identifierURI = URI.create(identifier);

    // the required address
    private InetAddress address;

    // the optional mask
    private InetAddress mask;

    // this is the optional port-range
    private PortRange range;

    /**
     * Creates the new <code>IPAddressAttribute</code> with all the optional components.
     * 
     * @param address
     *            a non-null <code>InetAddress</code>
     * @param mask
     *            an <code>InetAddress</code> or null if there is no mask
     * @param portRange
     *            a non-null <code>PortRange</code>
     */
    protected IPAddressAttribute(InetAddress address, InetAddress mask, PortRange range) {
        super(identifierURI);
        this.address = address;
        this.mask = mask;
        this.range = range;
    }

    /**
     * Returns a new <code>IPAddressAttribute</code> that represents the name at a particular DOM
     * node.
     * 
     * @param root
     *            the <code>Node</code> that contains the desired value
     * 
     * @return a new <code>IPAddressAttribute</code> representing the appropriate value (null if
     *         there is a parsing error)
     * 
     * @throws ParsingException
     *             if any of the address components is invalid
     */
    public static IPAddressAttribute getInstance(Node root) throws ParsingException {
        return getInstance(root.getFirstChild().getNodeValue());
    }

    /**
     * Returns a new <code>IPAddressAttribute</code> that represents the name indicated by the
     * <code>String</code> provided.
     * 
     * @param value
     *            a string representing the address
     * 
     * @return a new <code>IPAddressAttribute</code>
     * 
     * @throws ParsingException
     *             if any of the address components is invalid
     */
    public static IPAddressAttribute getInstance(String value) throws ParsingException {
        try {
            // an IPv6 address starts with a '['
            if (value.indexOf('[') == 0)
                return IPv6AddressAttribute.getV6Instance(value);
            else
                return IPv4AddressAttribute.getV4Instance(value);
        } catch (UnknownHostException uhe) {
            throw new ParsingException("Failed to parse an IPAddress", uhe);
        }
    }

    /**
     * Returns the address represented by this object.
     * 
     * @return the address
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Returns the mask represented by this object, or null if there is no mask.
     * 
     * @return the mask or null
     */
    public InetAddress getMask() {
        return mask;
    }

    /**
     * Returns the port range represented by this object which will be unbound if no range was
     * specified.
     * 
     * @return the range
     */
    public PortRange getRange() {
        return range;
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
        if (!(o instanceof IPAddressAttribute))
            return false;

        IPAddressAttribute other = (IPAddressAttribute) o;

        if (!address.equals(other.address))
            return false;

        if (mask != null) {
            if (other.mask == null)
                return false;

            if (!mask.equals(other.mask))
                return false;
        } else {
            if (other.mask != null)
                return false;
        }

        if (!range.equals(other.range))
            return false;

        return true;
    }

    /**
     * Returns the hashcode value used to index and compare this object with others of the same
     * type.
     * 
     * @return the object's hashcode value
     */
    public int hashCode() {

        // added by Mueller Christian
        
        int hash = 1;
        hash = hash * 31 + address.hashCode();
        hash = hash * 31 + (range== null ? 0 : range.hashCode());
        hash = hash * 31 + (mask== null ? 0 : mask.hashCode());
        return hash;
    }

    /**
     * Converts to a String representation.
     * 
     * @return the String representation
     */
    public String toString() {
        return "IPAddressAttribute: \"" + encode() + "\"";
    }

}
