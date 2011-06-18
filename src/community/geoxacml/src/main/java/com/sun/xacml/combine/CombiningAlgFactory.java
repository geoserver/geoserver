/*
 * @(#)CombiningAlgFactory.java
 *
 * Copyright 2003-2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.xacml.combine;

import java.net.URI;
import java.util.HashMap;
import java.util.Set;

import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.UnknownIdentifierException;

/**
 * Provides a factory mechanism for installing and retrieving combining algorithms.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public abstract class CombiningAlgFactory {

    // the proxy used to get the default factory
    private static CombiningAlgFactoryProxy defaultFactoryProxy;

    // the map of registered factories
    private static HashMap<String, CombiningAlgFactoryProxy> registeredFactories;

    /**
     * static intialiazer that sets up the default factory proxy and registers the standard
     * namespaces
     */
    static {
        CombiningAlgFactoryProxy proxy = new CombiningAlgFactoryProxy() {
            public CombiningAlgFactory getFactory() {
                return StandardCombiningAlgFactory.getFactory();
            }
        };

        registeredFactories = new HashMap<String, CombiningAlgFactoryProxy>();
        registeredFactories.put(PolicyMetaData.XACML_1_0_IDENTIFIER, proxy);
        registeredFactories.put(PolicyMetaData.XACML_2_0_IDENTIFIER, proxy);

        defaultFactoryProxy = proxy;
    };

    /**
     * Default constructor. Used only by subclasses.
     */
    protected CombiningAlgFactory() {

    }

    /**
     * Returns the default factory. Depending on the default factory's implementation, this may
     * return a singleton instance or new instances with each invokation.
     * 
     * @return the default <code>CombiningAlgFactory</code>
     */
    public static final CombiningAlgFactory getInstance() {
        return defaultFactoryProxy.getFactory();
    }

    /**
     * Returns a factory based on the given identifier. You may register as many factories as you
     * like, and then retrieve them through this interface, but a factory may only be registered
     * once using a given identifier. By default, the standard XACML 1.0 and 2.0 identifiers are
     * regsietered to provide the standard factory.
     * 
     * @param identifier
     *            the identifier for a factory
     * 
     * @return a <code>CombiningAlgFactory</code>
     * 
     * @throws UnknownIdentifierException
     *             if the given identifier isn't registered
     */
    public static final CombiningAlgFactory getInstance(String identifier)
            throws UnknownIdentifierException {
        CombiningAlgFactoryProxy proxy = (CombiningAlgFactoryProxy) (registeredFactories
                .get(identifier));

        if (proxy == null)
            throw new UnknownIdentifierException("Uknown CombiningAlgFactory " + "identifier: "
                    + identifier);

        return proxy.getFactory();
    }

    /**
     * Sets the default factory. This does not register the factory proxy as an identifiable
     * factory.
     * 
     * @param proxy
     *            the <code>CombiningAlgFactoryProxy</code> to set as the new default factory proxy
     */
    public static final void setDefaultFactory(CombiningAlgFactoryProxy proxy) {
        defaultFactoryProxy = proxy;
    }

    /**
     * Registers the given factory proxy with the given identifier. If the identifier is already
     * used, then this throws an exception. If the identifier is not already used, then it will
     * always be bound to the given proxy.
     * 
     * @param identifier
     *            the identifier for the proxy
     * @param proxy
     *            the <code>CombiningAlgFactoryProxy</code> to register with the given identifier
     * 
     * @throws IllegalArgumentException
     *             if the identifier is already used
     */
    public static final void registerFactory(String identifier, CombiningAlgFactoryProxy proxy)
            throws IllegalArgumentException {
        synchronized (registeredFactories) {
            if (registeredFactories.containsKey(identifier))
                throw new IllegalArgumentException("Identifier is already " + "registered as "
                        + "CombiningAlgFactory: " + identifier);

            registeredFactories.put(identifier, proxy);
        }
    }

    /**
     * Adds a combining algorithm to the factory. This single instance will be returned to anyone
     * who asks the factory for an algorithm with the id given here.
     * 
     * @param alg
     *            the combining algorithm to add
     * 
     * @throws IllegalArgumentException
     *             if the algorithm is already registered
     */
    public abstract void addAlgorithm(CombiningAlgorithm alg);

    /**
     * Adds a combining algorithm to the factory. This single instance will be returned to anyone
     * who asks the factory for an algorithm with the id given here.
     * 
     * @deprecated As of version 1.2, replaced by {@link #addAlgorithm(CombiningAlgorithm)}. The new
     *             factory system requires you to get a factory instance and then call the
     *             non-static methods on that factory. The static versions of these methods have
     *             been left in for now, but are slower and will be removed in a future version.
     * 
     * @param alg
     *            the combining algorithm to add
     * 
     * @throws IllegalArgumentException
     *             if the algorithm is already registered
     */
    public static void addCombiningAlg(CombiningAlgorithm alg) {
        getInstance().addAlgorithm(alg);
    }

    /**
     * Returns the algorithm identifiers supported by this factory.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public abstract Set<String> getSupportedAlgorithms();

    /**
     * Tries to return the correct combinging algorithm based on the given algorithm ID.
     * 
     * @param algId
     *            the identifier by which the algorithm is known
     * 
     * @return a combining algorithm
     * 
     * @throws UnknownIdentifierException
     *             algId is unknown
     */
    public abstract CombiningAlgorithm createAlgorithm(URI algId) throws UnknownIdentifierException;

    /**
     * Tries to return the correct combinging algorithm based on the given algorithm ID.
     * 
     * @deprecated As of version 1.2, replaced by {@link #createAlgorithm(URI)}. The new factory
     *             system requires you to get a factory instance and then call the non-static
     *             methods on that factory. The static versions of these methods have been left in
     *             for now, but are slower and will be removed in a future version.
     * 
     * @param algId
     *            the identifier by which the algorithm is known
     * 
     * @return a combining algorithm
     * 
     * @throws UnknownIdentifierException
     *             algId is unknown
     */
    public static CombiningAlgorithm createCombiningAlg(URI algId)
            throws UnknownIdentifierException {
        return getInstance().createAlgorithm(algId);
    }

}
