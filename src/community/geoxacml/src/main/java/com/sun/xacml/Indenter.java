/*
 * @(#)Indenter.java
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

package com.sun.xacml;

import java.util.Arrays;

/**
 * Provides flexible indenting for XML encoding. This class generates strings of spaces to be
 * prepended to lines of XML. The strings are formed according to a specified indent width and the
 * given depth.
 * 
 * @since 1.0
 * @author Marco Barreno
 * @author Seth Proctor
 */
public class Indenter {

    /**
     * The default indentation width
     */
    public static final int DEFAULT_WIDTH = 2;

    // The width of one indentation level
    private int width;

    // the current depth
    private int depth;

    /**
     * Constructs an <code>Indenter</code> with the default indent width.
     */
    public Indenter() {
        this(DEFAULT_WIDTH);
    }

    /**
     * Constructs an <code>Indenter</code> with a user-supplied indent width.
     * 
     * @param userWidth
     *            the number of spaces to use for each indentation level
     */
    public Indenter(int userWidth) {
        width = userWidth;
        depth = 0;
    }

    /**
     * Move in one width.
     */
    public void in() {
        depth += width;
    }

    /**
     * Move out one width.
     */
    public void out() {
        depth -= width;
    }

    /**
     * Create a <code>String</code> of spaces for indentation based on the current depth.
     * 
     * @return an indent string to prepend to lines of XML
     */
    public String makeString() {
        // Return quickly if no indenting
        if (depth <= 0) {
            return new String("");
        }

        // Make a char array and fill it with spaces
        char[] array = new char[depth];
        Arrays.fill(array, ' ');

        // Now return a string built from that char array
        return new String(array);
    }

}
