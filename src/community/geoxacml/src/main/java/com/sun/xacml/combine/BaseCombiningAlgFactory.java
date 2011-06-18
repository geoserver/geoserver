/*
 * @(#)BaseCombiningAlgFactory.java
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

package com.sun.xacml.combine;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import com.sun.xacml.UnknownIdentifierException;

/**
 * This is a basic implementation of <code>CombiningAlgFactory</code>. It implements the insertion
 * and retrieval methods, but doesn't actually setup the factory with any algorithms.
 * <p>
 * Note that while this class is thread-safe on all creation methods, it is not safe to add support
 * for a new algorithm while creating an instance of an algorithm. This follows from the assumption
 * that most people will initialize these factories up-front, and then start processing without ever
 * modifying the factories. If you need these mutual operations to be thread-safe, then you should
 * write a wrapper class that implements the right synchronization.
 * 
 * @since 1.2
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class BaseCombiningAlgFactory extends CombiningAlgFactory {

    // the map of available combining algorithms
    private HashMap<String, CombiningAlgorithm> algMap;

    /**
     * Default constructor.
     */
    public BaseCombiningAlgFactory() {
        algMap = new HashMap<String, CombiningAlgorithm>();
    }

    /**
     * Constructor that configures this factory with an initial set of supported algorithms.
     * 
     * @param algorithms
     *            a <code>Set</code> of </code>CombiningAlgorithm</code>s
     * 
     * @throws IllegalArgumentException
     *             if any elements of the set are not </code>CombiningAlgorithm</code>s
     */
    public BaseCombiningAlgFactory(Set<CombiningAlgorithm> algorithms) {
        algMap = new HashMap<String, CombiningAlgorithm>();

        for (CombiningAlgorithm alg : algorithms)
            algMap.put(alg.getIdentifier().toString(), alg);
    }

    /**
     * Adds a combining algorithm to the factory. This single instance will be returned to anyone
     * who asks the factory for an algorithm with the id given here.
     * 
     * @param alg
     *            the combining algorithm to add
     * 
     * @throws IllegalArgumentException
     *             if the algId is already registered
     */
    public void addAlgorithm(CombiningAlgorithm alg) {
        String algId = alg.getIdentifier().toString();

        // check that the id doesn't already exist in the factory
        if (algMap.containsKey(algId))
            throw new IllegalArgumentException("algorithm already registered: " + algId);

        // add the algorithm
        algMap.put(algId, alg);
    }

    /**
     * Returns the algorithm identifiers supported by this factory.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public Set<String> getSupportedAlgorithms() {
        return Collections.unmodifiableSet(algMap.keySet());
    }

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
    public CombiningAlgorithm createAlgorithm(URI algId) throws UnknownIdentifierException {
        String id = algId.toString();

        if (algMap.containsKey(id)) {
            return (CombiningAlgorithm) (algMap.get(algId.toString()));
        } else {
            throw new UnknownIdentifierException("unknown combining algId: " + id);
        }
    }

}
