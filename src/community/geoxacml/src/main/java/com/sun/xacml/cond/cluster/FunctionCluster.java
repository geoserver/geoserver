/*
 * @(#)FunctionCluster.java
 *
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.xacml.cond.cluster;

import java.util.Set;

import com.sun.xacml.cond.Function;

/**
 * Interface used by classes that support more than one function. It's a common design model to have
 * a single class support more than one XACML function. In those cases, you should provide a proxy
 * that implements <code>FunctionCluster</code> in addition to the <code>Function</code>. This is
 * particularly important for the run-time configuration system, which uses this interface to create
 * "clusters" of functions and therefore can use a smaller configuration file.
 * 
 * @since 1.2
 * @author Seth Proctor
 */
public interface FunctionCluster {

    /**
     * Returns a single instance of each of the functions supported by some class. The
     * <code>Set</code> must contain instances of <code>Function</code>, and it must be both
     * non-null and non-empty. It may contain only a single <code>Function</code>.
     * <p>
     * Note that this is only used to return concrete <code>Function</code>s. It may not be used to
     * report abstract functions.
     * 
     * @return the functions supported by this class
     * 
     *         Adding generic type support by Christian Mueller (geotools)
     */
    public Set<Function> getSupportedFunctions();

}
