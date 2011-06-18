/*
 * @(#)BasicFunctionFactoryProxy.java
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

package com.sun.xacml.cond;

/**
 * A simple utility class that manages triples of function factories.
 * 
 * @since 1.2
 * @author Seth Proctor
 */
public class BasicFunctionFactoryProxy implements FunctionFactoryProxy {

    // the triple of factories
    private FunctionFactory targetFactory;

    private FunctionFactory conditionFactory;

    private FunctionFactory generalFactory;

    /**
     * Creates a new proxy.
     * 
     * @param targetFactory
     *            the target factory provided by this proxy
     * @param conditionFactory
     *            the target condition provided by this proxy
     * @param generalFactory
     *            the general factory provided by this proxy
     */
    public BasicFunctionFactoryProxy(FunctionFactory targetFactory,
            FunctionFactory conditionFactory, FunctionFactory generalFactory) {
        this.targetFactory = targetFactory;
        this.conditionFactory = conditionFactory;
        this.generalFactory = generalFactory;
    }

    public FunctionFactory getTargetFactory() {
        return targetFactory;
    }

    public FunctionFactory getConditionFactory() {
        return conditionFactory;
    }

    public FunctionFactory getGeneralFactory() {
        return generalFactory;
    }

}
