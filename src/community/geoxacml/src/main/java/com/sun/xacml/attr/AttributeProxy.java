/*
 * @(#)AttributeProxy.java
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

import org.w3c.dom.Node;

/**
 * Used by the <code>AttributeFactory</code> to create new attributes. Typically a new proxy class
 * is created which in turn knows how to create a specific kind of attribute, and then this proxy
 * class is installed in the <code>AttributeFactory</code>.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public interface AttributeProxy {

    /**
     * Tries to create a new <code>AttributeValue</code> based on the given DOM root node.
     * 
     * @param root
     *            the DOM root of some attribute data
     * 
     * @return an <code>AttributeValue</code> representing the given data
     * 
     * @throws Exception
     *             if the data couldn't be used (the exception is typically wrapping some other
     *             exception)
     */
    public AttributeValue getInstance(Node root) throws Exception;

    /**
     * Tries to create a new <code>AttributeValue</code> based on the given String data.
     * 
     * @param value
     *            the text form of some attribute data
     * 
     * @return an <code>AttributeValue</code> representing the given data
     * 
     * @throws Exception
     *             if the data couldn't be used (the exception is typically wrapping some other
     *             exception)
     */
    public AttributeValue getInstance(String value) throws Exception;

}
