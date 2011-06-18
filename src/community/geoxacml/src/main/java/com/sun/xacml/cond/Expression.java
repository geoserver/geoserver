/*
 * @(#)Expression.java
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

package com.sun.xacml.cond;

import java.io.OutputStream;
import java.net.URI;

import com.sun.xacml.Indenter;

/**
 * This interface represents the expression type in the XACML 2.0 schema.
 * 
 * @since 2.0
 * @author Seth Proctor
 */
public interface Expression {

    /**
     * Returns the type of the expression. This may be the data type of an
     * <code>AttributeValue</code>, the return type of a <code>Function</code>, etc.
     * 
     * @return the attribute type of the referenced expression
     */
    public URI getType();

    /**
     * Returns whether or not this expression returns, or evaluates to a Bag. Note that
     * <code>Evaluatable</code>, which extends this interface, defines <code>evaluatesToBag</code>
     * which is essentially the same function. This method has been deprecated, and
     * <code>returnsBag</code> is now the preferred way to query all <code>Expression</code>s.
     */
    public boolean returnsBag();

    /**
     * Encodes this <code>Expression</code> into its XML representation and writes this encoding to
     * the given <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output);

    /**
     * Encodes this <code>Expression</code> into its XML representation and writes this encoding to
     * the given <code>OutputStream</code> with indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter);

}
