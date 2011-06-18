/*
 * @(#)IPv6AddressAttribute.java
 *
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
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
import java.net.UnknownHostException;

/**
 * Subclass of <code>IPAddressAttribute</code> that handles the specifics of IPv6. In general, you
 * shouldn't need to interact with this class except to create an instance directly.
 * 
 * @since 2.0
 * @author Seth Proctor
 */
public class IPv6AddressAttribute extends IPAddressAttribute {

    /**
     * Creates the new <code>IPv6AddressAttribute</code> with just the required address component.
     * 
     * @param address
     *            a non-null <code>InetAddress</code>
     */
    public IPv6AddressAttribute(InetAddress address) {
        this(address, null, new PortRange());
    }

    /**
     * Creates the new <code>IPv6AddressAttribute</code> with the optional address mask.
     * 
     * @param address
     *            a non-null <code>InetAddress</code>
     * @param mask
     *            an <code>InetAddress</code> or null if there is no mask
     */
    public IPv6AddressAttribute(InetAddress address, InetAddress mask) {
        this(address, mask, new PortRange());
    }

    /**
     * Creates the new <code>IPv6AddressAttribute</code> with the optional port range.
     * 
     * @param address
     *            a non-null <code>InetAddress</code>
     * @param portRange
     *            a non-null <code>PortRange</code>
     */
    public IPv6AddressAttribute(InetAddress address, PortRange range) {
        this(address, null, range);
    }

    /**
     * Creates the new <code>IPv6AddressAttribute</code> with all the optional components.
     * 
     * @param address
     *            a non-null <code>InetAddress</code>
     * @param mask
     *            an <code>InetAddress</code> or null if there is no mask
     * @param portRange
     *            a non-null <code>PortRange</code>
     */
    public IPv6AddressAttribute(InetAddress address, InetAddress mask, PortRange range) {
        super(address, mask, range);
    }

    /**
     * Returns a new <code>IPv6AddressAttribute</code> that represents the name indicated by the
     * <code>String</code> provided. This is a protected method because you should never call it
     * directly. Instead, you should call <code>getInstance</code> on
     * <code>IPAddressAttribute</code> which provides versions that take both a <code>String</code>
     * and a <code>Node</code> and will determine the protocol version correctly.
     * 
     * @param value
     *            a string representing the address
     * 
     * @return a new <code>IPAddressAttribute</code>
     * 
     * @throws UnknownHostException
     *             if the address components is invalid
     * @throws ParsingException
     *             if any of the address components is invalid
     */
    protected static IPAddressAttribute getV6Instance(String value) throws UnknownHostException {
        InetAddress address = null;
        InetAddress mask = null;
        PortRange range = null;
        int len = value.length();

        // get the required address component
        int endIndex = value.indexOf(']');
        address = InetAddress.getByName(value.substring(1, endIndex));

        // see if there's anything left in the string
        if (endIndex != (len - 1)) {
            // if there's a mask, it's also an IPv6 address
            if (value.charAt(endIndex + 1) == '/') {
                int startIndex = endIndex + 3;
                endIndex = value.indexOf(']', startIndex);
                mask = InetAddress.getByName(value.substring(startIndex, endIndex));
            }

            // finally, see if there's a port range, if we're not finished
            if ((endIndex != (len - 1)) && (value.charAt(endIndex + 1) == ':'))
                range = PortRange.getInstance(value.substring(endIndex + 2, len));
        }

        // if the range is null, then create it as unbound
        range = new PortRange();

        return new IPv6AddressAttribute(address, mask, range);
    }

    /**
     *
     */
    public String encode() {
        String str = "[" + getAddress().getHostAddress() + "]";

        if (getMask() != null)
            str += "/[" + getMask().getHostAddress() + "]";

        if (!getRange().isUnbound())
            str += ":" + getRange().encode();

        return str;
    }

}
