/*
 * @(#)FunctionFactory.java
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

package com.sun.xacml.cond;

import java.net.URI;
import java.util.HashMap;
import java.util.Set;

import org.w3c.dom.Node;

import com.sun.xacml.ParsingException;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.UnknownIdentifierException;

/**
 * Factory used to create all functions. There are three kinds of factories: general, condition, and
 * target. These provide functions that can be used anywhere, only in a condition's root and only in
 * a target (respectively).
 * <p>
 * Note that all functions, except for abstract functions, are singletons, so any instance that is
 * added to a factory will be the same one returned from the create methods. This is done because
 * most functions don't have state, so there is no need to have more than one, or to spend the time
 * creating multiple instances that all do the same thing.
 * 
 * @since 1.0
 * @author Marco Barreno
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public abstract class FunctionFactory {

    // the proxies used to get the default factorys
    private static FunctionFactoryProxy defaultFactoryProxy;

    // the map of registered factories
    private static HashMap<String, FunctionFactoryProxy> registeredFactories;

    /**
     * static intialiazer that sets up the default factory proxies and registers the standard
     * namespaces
     */
    static {
        FunctionFactoryProxy proxy = new FunctionFactoryProxy() {
            public FunctionFactory getTargetFactory() {
                return StandardFunctionFactory.getTargetFactory();
            }

            public FunctionFactory getConditionFactory() {
                return StandardFunctionFactory.getConditionFactory();
            }

            public FunctionFactory getGeneralFactory() {
                return StandardFunctionFactory.getGeneralFactory();
            }
        };

        registeredFactories = new HashMap<String, FunctionFactoryProxy>();
        registeredFactories.put(PolicyMetaData.XACML_1_0_IDENTIFIER, proxy);
        registeredFactories.put(PolicyMetaData.XACML_2_0_IDENTIFIER, proxy);

        defaultFactoryProxy = proxy;
    };

    /**
     * Default constructor. Used only by subclasses.
     */
    protected FunctionFactory() {

    }

    /**
     * Returns the default FunctionFactory that will only provide those functions that are usable in
     * Target matching.
     * 
     * @return a <code>FunctionFactory</code> for target functions
     */
    public static final FunctionFactory getTargetInstance() {
        return defaultFactoryProxy.getTargetFactory();
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
     * @return a <code>FunctionFactory</code> that supports Target functions
     * 
     * @throws UnknownIdentifierException
     *             if the given identifier isn't registered
     */
    public static final FunctionFactory getTargetInstance(String identifier)
            throws UnknownIdentifierException {
        return getRegisteredProxy(identifier).getTargetFactory();
    }

    /**
     * Returns the default FuntionFactory that will only provide those functions that are usable in
     * the root of the Condition. These Functions are a superset of the Target functions.
     * 
     * @return a <code>FunctionFactory</code> for condition functions
     */
    public static final FunctionFactory getConditionInstance() {
        return defaultFactoryProxy.getConditionFactory();
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
     * @return a <code>FunctionFactory</code> that supports Condition functions
     * 
     * @throws UnknownIdentifierException
     *             if the given identifier isn't registered
     */
    public static final FunctionFactory getConditionInstance(String identifier)
            throws UnknownIdentifierException {
        return getRegisteredProxy(identifier).getConditionFactory();
    }

    /**
     * Returns the default FunctionFactory that provides access to all the functions. These
     * Functions are a superset of the Condition functions.
     * 
     * @return a <code>FunctionFactory</code> for all functions
     */
    public static final FunctionFactory getGeneralInstance() {
        return defaultFactoryProxy.getGeneralFactory();
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
     * @return a <code>FunctionFactory</code> that supports General functions
     * 
     * @throws UnknownIdentifierException
     *             if the given identifier isn't registered
     */
    public static final FunctionFactory getGeneralInstance(String identifier)
            throws UnknownIdentifierException {
        return getRegisteredProxy(identifier).getGeneralFactory();
    }

    /**
     * Returns the default FunctionFactoryProxy that provides access to all the functions.
     * 
     * @return a <code>FunctionFactoryProxy</code> for all functions
     */
    public static final FunctionFactoryProxy getInstance() {
        return defaultFactoryProxy;
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
     * @return a <code>FunctionFactoryProxy</code>
     * 
     * @throws UnknownIdentifierException
     *             if the given identifier isn't registered
     */
    public static final FunctionFactoryProxy getInstance(String identifier)
            throws UnknownIdentifierException {
        return getRegisteredProxy(identifier);
    }

    /**
     * Private helper that resolves the proxy for the given identifier, or throws an exception if no
     * proxy is registered for that identifier.
     */
    private static FunctionFactoryProxy getRegisteredProxy(String identifier)
            throws UnknownIdentifierException {
        FunctionFactoryProxy proxy = (FunctionFactoryProxy) (registeredFactories.get(identifier));

        if (proxy == null)
            throw new UnknownIdentifierException("Uknown FunctionFactory " + "identifier: "
                    + identifier);

        return proxy;
    }

    /**
     * Sets the default factory. This does not register the factory proxy as an identifiable
     * factory.
     * 
     * @param proxy
     *            the <code>FunctionFactoryProxy</code> to set as the new default factory proxy
     */
    public static final void setDefaultFactory(FunctionFactoryProxy proxy) {
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
     *            the <code>FunctionFactoryProxy</code> to register with the given identifier
     * 
     * @throws IllegalArgumentException
     *             if the identifier is already used
     */
    public static final void registerFactory(String identifier, FunctionFactoryProxy proxy)
            throws IllegalArgumentException {
        synchronized (registeredFactories) {
            if (registeredFactories.containsKey(identifier))
                throw new IllegalArgumentException("Identifier is already " + "registered as "
                        + "FunctionFactory: " + identifier);

            registeredFactories.put(identifier, proxy);
        }
    }

    /**
     * Adds the function to the factory. Most functions have no state, so the singleton model used
     * here is typically desireable. The factory will not enforce the requirement that a Target or
     * Condition matching function must be boolean.
     * 
     * @param function
     *            the <code>Function</code> to add to the factory
     * 
     * @throws IllegalArgumentException
     *             if the function's identifier is already used
     */
    public abstract void addFunction(Function function);

    /**
     * Adds the abstract function proxy to the factory. This is used for those functions which have
     * state, or change behavior (for instance the standard map function, which changes its return
     * type based on how it is used).
     * 
     * @param proxy
     *            the <code>FunctionProxy</code> to add to the factory
     * @param identity
     *            the function's identifier
     * 
     * @throws IllegalArgumentException
     *             if the function's identifier is already used
     */
    public abstract void addAbstractFunction(FunctionProxy proxy, URI identity);

    /**
     * Adds a target function.
     * 
     * @deprecated As of version 1.2, replaced by {@link #addFunction(Function)}. The new factory
     *             system requires you to get a factory instance and then call the non-static
     *             methods on that factory. The static versions of these methods have been left in
     *             for now, but are slower and will be removed in a future version.
     * 
     * @param function
     *            the function to add
     * 
     * @throws IllegalArgumentException
     *             if the name is already in use
     */
    public static void addTargetFunction(Function function) {
        getTargetInstance().addFunction(function);
    }

    /**
     * Adds an abstract target function.
     * 
     * @deprecated As of version 1.2, replaced by {@link #addAbstractFunction(FunctionProxy,URI)}.
     *             The new factory system requires you to get a factory instance and then call the
     *             non-static methods on that factory. The static versions of these methods have
     *             been left in for now, but are slower and will be removed in a future version.
     * 
     * @param proxy
     *            the function proxy to add
     * @param identity
     *            the name of the function
     * 
     * @throws IllegalArgumentException
     *             if the name is already in use
     */
    public static void addAbstractTargetFunction(FunctionProxy proxy, URI identity) {
        getTargetInstance().addAbstractFunction(proxy, identity);
    }

    /**
     * Adds a condition function.
     * 
     * @deprecated As of version 1.2, replaced by {@link #addFunction(Function)}. The new factory
     *             system requires you to get a factory instance and then call the non-static
     *             methods on that factory. The static versions of these methods have been left in
     *             for now, but are slower and will be removed in a future version.
     * 
     * @param function
     *            the function to add
     * 
     * @throws IllegalArgumentException
     *             if the name is already in use
     */
    public static void addConditionFunction(Function function) {
        getConditionInstance().addFunction(function);
    }

    /**
     * Adds an abstract condition function.
     * 
     * @deprecated As of version 1.2, replaced by {@link #addAbstractFunction(FunctionProxy,URI)}.
     *             The new factory system requires you to get a factory instance and then call the
     *             non-static methods on that factory. The static versions of these methods have
     *             been left in for now, but are slower and will be removed in a future version.
     * 
     * @param proxy
     *            the function proxy to add
     * @param identity
     *            the name of the function
     * 
     * @throws IllegalArgumentException
     *             if the name is already in use
     */
    public static void addAbstractConditionFunction(FunctionProxy proxy, URI identity) {
        getConditionInstance().addAbstractFunction(proxy, identity);
    }

    /**
     * Adds a general function.
     * 
     * @deprecated As of version 1.2, replaced by {@link #addFunction(Function)}. The new factory
     *             system requires you to get a factory instance and then call the non-static
     *             methods on that factory. The static versions of these methods have been left in
     *             for now, but are slower and will be removed in a future version.
     * 
     * @param function
     *            the function to add
     * 
     * @throws IllegalArgumentException
     *             if the name is already in use
     */
    public static void addGeneralFunction(Function function) {
        getGeneralInstance().addFunction(function);
    }

    /**
     * Adds an abstract general function.
     * 
     * @deprecated As of version 1.2, replaced by {@link #addAbstractFunction(FunctionProxy,URI)}.
     *             The new factory system requires you to get a factory instance and then call the
     *             non-static methods on that factory. The static versions of these methods have
     *             been left in for now, but are slower and will be removed in a future version.
     * 
     * @param proxy
     *            the function proxy to add
     * @param identity
     *            the name of the function
     * 
     * @throws IllegalArgumentException
     *             if the name is already in use
     */
    public static void addAbstractGeneralFunction(FunctionProxy proxy, URI identity) {
        getGeneralInstance().addAbstractFunction(proxy, identity);
    }

    /**
     * Returns the function identifiers supported by this factory.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public abstract Set<String> getSupportedFunctions();

    /**
     * Tries to get an instance of the specified function.
     * 
     * @param identity
     *            the name of the function
     * 
     * @throws UnknownIdentifierException
     *             if the name isn't known
     * @throws FunctionTypeException
     *             if the name is known to map to an abstract function, and should therefore be
     *             created through createAbstractFunction
     */
    public abstract Function createFunction(URI identity) throws UnknownIdentifierException,
            FunctionTypeException;

    /**
     * Tries to get an instance of the specified function.
     * 
     * @param identity
     *            the name of the function
     * 
     * @throws UnknownIdentifierException
     *             if the name isn't known
     * @throws FunctionTypeException
     *             if the name is known to map to an abstract function, and should therefore be
     *             created through createAbstractFunction
     */
    public abstract Function createFunction(String identity) throws UnknownIdentifierException,
            FunctionTypeException;

    /**
     * Tries to get an instance of the specified abstract function.
     * 
     * @param identity
     *            the name of the function
     * @param root
     *            the DOM root containing info used to create the function
     * 
     * @throws UnknownIdentifierException
     *             if the name isn't known
     * @throws FunctionTypeException
     *             if the name is known to map to a concrete function, and should therefore be
     *             created through createFunction
     * @throws ParsingException
     *             if the function can't be created with the given inputs
     */
    public abstract Function createAbstractFunction(URI identity, Node root)
            throws UnknownIdentifierException, ParsingException, FunctionTypeException;

    /**
     * Tries to get an instance of the specified abstract function.
     * 
     * @param identity
     *            the name of the function
     * @param root
     *            the DOM root containing info used to create the function
     * @param xpathVersion
     *            the version specified in the contianing policy, or null if no version was
     *            specified
     * 
     * @throws UnknownIdentifierException
     *             if the name isn't known
     * @throws FunctionTypeException
     *             if the name is known to map to a concrete function, and should therefore be
     *             created through createFunction
     * @throws ParsingException
     *             if the function can't be created with the given inputs
     */
    public abstract Function createAbstractFunction(URI identity, Node root, String xpathVersion)
            throws UnknownIdentifierException, ParsingException, FunctionTypeException;

    /**
     * Tries to get an instance of the specified abstract function.
     * 
     * @param identity
     *            the name of the function
     * @param root
     *            the DOM root containing info used to create the function
     * 
     * @throws UnknownIdentifierException
     *             if the name isn't known
     * @throws FunctionTypeException
     *             if the name is known to map to a concrete function, and should therefore be
     *             created through createFunction
     * @throws ParsingException
     *             if the function can't be created with the given inputs
     */
    public abstract Function createAbstractFunction(String identity, Node root)
            throws UnknownIdentifierException, ParsingException, FunctionTypeException;

    /**
     * Tries to get an instance of the specified abstract function.
     * 
     * @param identity
     *            the name of the function
     * @param root
     *            the DOM root containing info used to create the function
     * @param xpathVersion
     *            the version specified in the contianing policy, or null if no version was
     *            specified
     * 
     * @throws UnknownIdentifierException
     *             if the name isn't known
     * @throws FunctionTypeException
     *             if the name is known to map to a concrete function, and should therefore be
     *             created through createFunction
     * @throws ParsingException
     *             if the function can't be created with the given inputs
     */
    public abstract Function createAbstractFunction(String identity, Node root, String xpathVersion)
            throws UnknownIdentifierException, ParsingException, FunctionTypeException;

}
