/*
 * @(#)StandardFunctionFactory.java
 *
 * Copyright 2004-2006 Sun Microsystems, Inc. All Rights Reserved.
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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.sun.xacml.UnknownIdentifierException;
import com.sun.xacml.cond.cluster.AbsFunctionCluster;
import com.sun.xacml.cond.cluster.AddFunctionCluster;
import com.sun.xacml.cond.cluster.ComparisonFunctionCluster;
import com.sun.xacml.cond.cluster.ConditionBagFunctionCluster;
import com.sun.xacml.cond.cluster.ConditionSetFunctionCluster;
import com.sun.xacml.cond.cluster.DateMathFunctionCluster;
import com.sun.xacml.cond.cluster.DivideFunctionCluster;
import com.sun.xacml.cond.cluster.EqualFunctionCluster;
import com.sun.xacml.cond.cluster.FloorFunctionCluster;
import com.sun.xacml.cond.cluster.GeneralBagFunctionCluster;
import com.sun.xacml.cond.cluster.GeneralSetFunctionCluster;
import com.sun.xacml.cond.cluster.HigherOrderFunctionCluster;
import com.sun.xacml.cond.cluster.LogicalFunctionCluster;
import com.sun.xacml.cond.cluster.MatchFunctionCluster;
import com.sun.xacml.cond.cluster.ModFunctionCluster;
import com.sun.xacml.cond.cluster.MultiplyFunctionCluster;
import com.sun.xacml.cond.cluster.NOfFunctionCluster;
import com.sun.xacml.cond.cluster.NotFunctionCluster;
import com.sun.xacml.cond.cluster.NumericConvertFunctionCluster;
import com.sun.xacml.cond.cluster.RoundFunctionCluster;
import com.sun.xacml.cond.cluster.StringFunctionCluster;
import com.sun.xacml.cond.cluster.StringNormalizeFunctionCluster;
import com.sun.xacml.cond.cluster.SubtractFunctionCluster;

/**
 * This factory supports the standard set of functions specified in XACML 1.x and 2.0. It is the
 * default factory used by the system, and imposes a singleton pattern insuring that there is only
 * ever one instance of this class.
 * <p>
 * Note that because this supports only the standard functions, this factory does not allow the
 * addition of any other functions. If you call <code>addFunction</code> on an instance of this
 * class, an exception will be thrown. If you need a standard factory that is modifiable, you can
 * either create a new <code>BaseFunctionFactory</code> (or some other implementation of
 * <code>FunctionFactory</code>) populated with the standard functions from
 * <code>getStandardFunctions</code> or you can use <code>getNewFactoryProxy</code> to get a proxy
 * containing a new, modifiable set of factories.
 * 
 * @since 1.2
 * @author Seth Proctor
 * 
 *         FunctionCluster,Set<Function>
 */

public class StandardFunctionFactory extends BaseFunctionFactory {

    // the three singleton instances
    private static StandardFunctionFactory targetFactory = null;

    private static StandardFunctionFactory conditionFactory = null;

    private static StandardFunctionFactory generalFactory = null;

    // the three function sets/maps that we use internally
    private static Set<Function> targetFunctions = null;

    private static Set<Function> conditionFunctions = null;

    private static Set<Function> generalFunctions = null;

    private static Map<URI, MapFunctionProxy> targetAbstractFunctions = null;

    private static Map<URI, MapFunctionProxy> conditionAbstractFunctions = null;

    private static Map<URI, MapFunctionProxy> generalAbstractFunctions = null;

    // the static sets of supported identifiers for each XACML version
    // private static Set<Function> supportedV1Functions;
    // private static Set<Function> supportedV2Functions;

    // the set/map used by each singleton factory instance
    // private Set<Function> supportedFunctions = null;
    // private Map supportedAbstractFunctions = null;

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(StandardFunctionFactory.class.getName());

    /**
     * Creates a new StandardFunctionFactory, making sure that the default maps are initialized
     * correctly. Standard factories can't be modified, so there is no notion of supersetting since
     * that's only used for correctly propagating new functions.
     */
    private StandardFunctionFactory(Set<Function> supportedFunctions,
            Map<URI, ? extends FunctionProxy> supportedAbstractFunctions) {
        super(supportedFunctions, supportedAbstractFunctions);

        // this.supportedFunctions = supportedFunctions;
        // this.supportedAbstractFunctions = supportedAbstractFunctions;
    }

    /**
     * Private initializer for the target functions. This is only ever called once.
     */
    private static void initTargetFunctions() {
        logger.config("Initializing standard Target functions");

        targetFunctions = new HashSet<Function>();

        // add EqualFunction
        targetFunctions.addAll((new EqualFunctionCluster()).getSupportedFunctions());
        // add LogicalFunction
        targetFunctions.addAll((new LogicalFunctionCluster()).getSupportedFunctions());
        // add NOfFunction
        targetFunctions.addAll((new NOfFunctionCluster()).getSupportedFunctions());
        // add NotFunction
        targetFunctions.addAll((new NotFunctionCluster()).getSupportedFunctions());
        // add ComparisonFunction
        targetFunctions.addAll((new ComparisonFunctionCluster()).getSupportedFunctions());
        // add MatchFunction
        targetFunctions.addAll((new MatchFunctionCluster()).getSupportedFunctions());

        targetAbstractFunctions = new HashMap<URI, MapFunctionProxy>();
    }

    /**
     * Private initializer for the condition functions. This is only ever called once.
     */
    private static void initConditionFunctions() {
        logger.config("Initializing standard Condition functions");

        if (targetFunctions == null)
            initTargetFunctions();

        conditionFunctions = new HashSet<Function>(targetFunctions);

        // add condition function TimeInRange
        conditionFunctions.add(new TimeInRangeFunction());
        // add condition functions from BagFunction
        conditionFunctions.addAll((new ConditionBagFunctionCluster()).getSupportedFunctions());
        // add condition functions from SetFunction
        conditionFunctions.addAll((new ConditionSetFunctionCluster()).getSupportedFunctions());
        // add condition functions from HigherOrderFunction
        conditionFunctions.addAll((new HigherOrderFunctionCluster()).getSupportedFunctions());

        conditionAbstractFunctions = new HashMap<URI, MapFunctionProxy>(targetAbstractFunctions);
    }

    /**
     * Private initializer for the general functions. This is only ever called once.
     */
    private static void initGeneralFunctions() {
        logger.config("Initializing standard General functions");

        if (conditionFunctions == null)
            initConditionFunctions();

        generalFunctions = new HashSet<Function>(conditionFunctions);

        // add AddFunction
        generalFunctions.addAll((new AddFunctionCluster()).getSupportedFunctions());
        // add SubtractFunction
        generalFunctions.addAll((new SubtractFunctionCluster()).getSupportedFunctions());
        // add MultiplyFunction
        generalFunctions.addAll((new MultiplyFunctionCluster()).getSupportedFunctions());
        // add DivideFunction
        generalFunctions.addAll((new DivideFunctionCluster()).getSupportedFunctions());
        // add ModFunction
        generalFunctions.addAll((new ModFunctionCluster()).getSupportedFunctions());
        // add AbsFunction
        generalFunctions.addAll((new AbsFunctionCluster()).getSupportedFunctions());
        // add RoundFunction
        generalFunctions.addAll((new RoundFunctionCluster()).getSupportedFunctions());
        // add FloorFunction
        generalFunctions.addAll((new FloorFunctionCluster()).getSupportedFunctions());
        // add DateMathFunction
        generalFunctions.addAll((new DateMathFunctionCluster()).getSupportedFunctions());
        // add general functions from BagFunction
        generalFunctions.addAll((new GeneralBagFunctionCluster()).getSupportedFunctions());
        // add NumericConvertFunction
        generalFunctions.addAll((new NumericConvertFunctionCluster()).getSupportedFunctions());
        // add StringNormalizeFunction
        generalFunctions.addAll((new StringNormalizeFunctionCluster()).getSupportedFunctions());
        // add general functions from SetFunction
        generalFunctions.addAll((new GeneralSetFunctionCluster()).getSupportedFunctions());
        // add the XACML 2.0 string functions
        generalFunctions.addAll((new StringFunctionCluster()).getSupportedFunctions());

        generalAbstractFunctions = new HashMap<URI, MapFunctionProxy>(conditionAbstractFunctions);

        // add the map function's proxy
        try {
            generalAbstractFunctions.put(new URI(MapFunction.NAME_MAP), new MapFunctionProxy());
        } catch (URISyntaxException e) {
            // this shouldn't ever happen, but just in case...
            throw new IllegalArgumentException("invalid function name");
        }
    }

    /**
     * Returns a FunctionFactory that will only provide those functions that are usable in Target
     * matching. This method enforces a singleton model, meaning that this always returns the same
     * instance, creating the factory if it hasn't been requested before. This is the default model
     * used by the <code>FunctionFactory</code>, ensuring quick access to this factory.
     * 
     * @return a <code>FunctionFactory</code> for target functions
     */
    public static StandardFunctionFactory getTargetFactory() {
        if (targetFactory == null) {
            synchronized (StandardFunctionFactory.class) {
                if (targetFunctions == null)
                    initTargetFunctions();
                if (targetFactory == null)
                    targetFactory = new StandardFunctionFactory(targetFunctions,
                            targetAbstractFunctions);
            }
        }

        return targetFactory;
    }

    /**
     * Returns a FuntionFactory that will only provide those functions that are usable in the root
     * of the Condition. These Functions are a superset of the Target functions. This method
     * enforces a singleton model, meaning that this always returns the same instance, creating the
     * factory if it hasn't been requested before. This is the default model used by the
     * <code>FunctionFactory</code>, ensuring quick access to this factory.
     * 
     * @return a <code>FunctionFactory</code> for condition functions
     */
    public static StandardFunctionFactory getConditionFactory() {
        if (conditionFactory == null) {
            synchronized (StandardFunctionFactory.class) {
                if (conditionFunctions == null)
                    initConditionFunctions();
                if (conditionFactory == null)
                    conditionFactory = new StandardFunctionFactory(conditionFunctions,
                            conditionAbstractFunctions);
            }
        }

        return conditionFactory;
    }

    /**
     * Returns a FunctionFactory that provides access to all the functions. These Functions are a
     * superset of the Condition functions. This method enforces a singleton model, meaning that
     * this always returns the same instance, creating the factory if it hasn't been requested
     * before. This is the default model used by the <code>FunctionFactory</code>, ensuring quick
     * access to this factory.
     * 
     * @return a <code>FunctionFactory</code> for all functions
     */
    public static StandardFunctionFactory getGeneralFactory() {
        if (generalFactory == null) {
            synchronized (StandardFunctionFactory.class) {
                if (generalFunctions == null) {
                    initGeneralFunctions();
                    generalFactory = new StandardFunctionFactory(generalFunctions,
                            generalAbstractFunctions);
                }
            }
        }

        return generalFactory;
    }

    /**
     * Returns the identifiers supported for the given version of XACML. Because this factory
     * supports identifiers from all versions of the XACML specifications, this method is useful for
     * getting a list of which specific identifiers are supported by a given version of XACML.
     * 
     * @param xacmlVersion
     *            a standard XACML identifier string, as provided in <code>PolicyMetaData</code>
     * 
     * @return a <code>Set</code> of identifiers
     * 
     * @throws UnknownIdentifierException
     *             if the version string is unknown
     */
    public static Set<String> getStandardFunctions(String xacmlVersion) {
        // FIXME: collecting the identifiers needs to be implemented..
        throw new RuntimeException("This method isn't implemented yet.");
    }

    /**
     * Returns the set of abstract functions that this standard factory supports as a mapping of
     * identifier to proxy.
     * 
     * @return a <code>Map</code> mapping <code>URI</code>s to <code>FunctionProxy</code>s
     */
    public static Map<URI, ? extends FunctionProxy> getStandardAbstractFunctions(String xacmlVersion) {
        // FIXME: collecting the identifiers needs to be implemented..
        throw new RuntimeException("This method isn't implemented yet.");
    }

    /**
     * A convenience method that returns a proxy containing newly created instances of
     * <code>BaseFunctionFactory</code>s that are correctly supersetted and contain the standard
     * functions and abstract functions. These factories allow adding support for new functions.
     * 
     * @return a new proxy containing new factories supporting the standard functions
     */
    public static FunctionFactoryProxy getNewFactoryProxy() {
        // first off, make sure everything's been initialized
        getGeneralFactory();

        // now create the new instances
        FunctionFactory newGeneral = new BaseFunctionFactory(generalFunctions,
                generalAbstractFunctions);

        FunctionFactory newCondition = new BaseFunctionFactory(newGeneral, conditionFunctions,
                conditionAbstractFunctions);

        FunctionFactory newTarget = new BaseFunctionFactory(newCondition, targetFunctions,
                targetAbstractFunctions);

        return new BasicFunctionFactoryProxy(newTarget, newCondition, newGeneral);
    }

    /**
     * Always throws an exception, since support for new functions may not be added to a standard
     * factory.
     * 
     * @param function
     *            the <code>Function</code> to add to the factory
     * 
     * @throws UnsupportedOperationException
     *             always
     */
    public void addFunction(Function function) throws IllegalArgumentException {
        throw new UnsupportedOperationException("a standard factory cannot "
                + "support new functions");
    }

    /**
     * Always throws an exception, since support for new functions may not be added to a standard
     * factory.
     * 
     * @param proxy
     *            the <code>FunctionProxy</code> to add to the factory
     * @param identity
     *            the function's identifier
     * 
     * @throws UnsupportedOperationException
     *             always
     */
    public void addAbstractFunction(FunctionProxy proxy, URI identity)
            throws IllegalArgumentException {
        throw new UnsupportedOperationException("a standard factory cannot "
                + "support new functions");
    }

}
