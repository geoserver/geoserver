/*
 * @(#)VariableManager.java
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

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.ParsingException;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.ProcessingException;

/**
 * This class is used by the parsing routines to handle the relationships between variable
 * references and definitions. Specifically, it takes care of the fact that definitions can be
 * placed after their first reference, and can use references to create circular or recursive
 * relationships. It keeps track of what's in the process of being parsed and will pre-parse
 * elements as needed.
 * <p>
 * Note that you should never have to use this class directly. It is really meant only as a utility
 * for the internal parsing routines. Also, note that the operations on this class are not
 * thread-safe. Typically this doesn't matter, since the code doesn't support using more than one
 * thread to parse a single Policy.
 * 
 * @since 2.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class VariableManager {

    // the map from identifiers to internal data
    private Map<Object, VariableState> idMap;

    // the meta-data for the containing policy
    private PolicyMetaData metaData;

    /**
     * Creates a manager with a fixed set of supported identifiers. For each of these identifiers,
     * the map supplies a cooresponding DOM node used to parse the definition. This is used if, in
     * the course of parsing one definition, a reference requires that you have information about
     * another definition available. All parsed definitions are cached so that each is only parsed
     * once. If a node is not provided, then the parsing code may throw an exception if out-of-order
     * or circular refereces are used.
     * <p>
     * Note that the use of a DOM node may change to an arbitrary interface, so that you could use
     * your own mechanism, but this is still being hashed out. This interface will be forzed before
     * a 2.0 release.
     * 
     * @param variableIds
     *            a <code>Map</code> from an identifier to the <code>Node</code> that is the root of
     *            the cooresponding variable definition, or null
     * @param metaData
     *            the meta-data associated with the containing policy
     */
    public VariableManager(Map<String, Node> variableIds, PolicyMetaData metaData) {
        idMap = new HashMap<Object, VariableState>();

        Iterator<String> it = variableIds.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            Node node = variableIds.get(key);
            idMap.put(key, new VariableState(null, node, null, false, false));
        }

        this.metaData = metaData;
    }

    /**
     * Returns the definition with the given identifier. If the definition is not available, then
     * this method will try to get the definition based on the DOM node given for this identifier.
     * If parsing the definition requires loading another definition (because of a reference) then
     * this method will be recursively invoked. This may make it slow to call this method once, but
     * all retrieved definitions are cached, and once this manager has started parsing a definition
     * it will never try parsing that definition again. If the definition cannot be retrieved, then
     * an exception is thrown.
     * 
     * @param variableId
     *            the definition's identifier
     * 
     * @return the identified definition
     * 
     * @throws ProcessingException
     *             if the definition cannot be resolved
     */
    public VariableDefinition getDefinition(String variableId) {
        VariableState state = (VariableState) (idMap.get(variableId));

        // make sure this is an identifier we handle
        if (state == null)
            throw new ProcessingException("variable is unsupported: " + variableId);

        // if we've resolved the definition before, then we're done
        if (state.definition != null)
            return state.definition;

        // we don't have the definition, so get the DOM node
        Node node = state.rootNode;

        // we can't keep going unless we have a node to work with
        if (node != null) {
            // if we've already started parsing this node before, then
            // don't start again
            if (state.handled)
                throw new ProcessingException("processing in progress");

            // keep track of the fact that we're parsing this node, and
            // also get the type (if it's an Apply node)
            state.handled = true;
            discoverApplyType(node, state);

            try {
                // now actually try parsing the definition...remember that
                // if its expression has a reference, we could end up
                // calling this manager method again
                state.definition = VariableDefinition.getInstance(state.rootNode, metaData, this);

                return state.definition;
            } catch (ParsingException pe) {
                // we failed to parse the definition for some reason
                throw new ProcessingException("failed to parse the definition", pe);
            }
        }

        // we couldn't figure out how to resolve the definition
        throw new ProcessingException("couldn't retrieve definition: " + variableId);
    }

    /**
     * Private helper method to get the type of an expression, but only if that expression is an
     * Apply. Basically, if there is a circular reference, then we'll need to know the types before
     * we're done parsing one of the definitions. But, a circular reference that requires
     * type-checking can only happen if the definition's expression is an Apply. So, we look here,
     * and if it's an Apply, we get the type information and store that for later use, just in case.
     * <p>
     * Note that we could wait until later to try this, or we could check first to see if there will
     * be a circular reference. Comparatively, however, this isn't too expensive, and it makes the
     * system much simpler. Still, it's worth re-examining this to see if there's a way that makes
     * more sense.
     */
    private void discoverApplyType(Node root, VariableState state) {
        // get the first element, which is the expression node
        NodeList nodes = root.getChildNodes();
        Node xprNode = nodes.item(0);
        int i = 1;
        while (xprNode.getNodeType() != Node.ELEMENT_NODE)
            xprNode = nodes.item(i++);

        // now see if the node is an Apply
        if (xprNode.getNodeName().equals("Apply")) {
            try {
                // get the function in the Apply...
                Function function = ExpressionHandler.getFunction(xprNode, metaData,
                        FunctionFactory.getGeneralInstance());

                // ...and store the type information in the variable state
                state.type = function.getReturnType();
                state.returnsBag = function.returnsBag();
            } catch (ParsingException pe) {
                // we can just ignore this...if there really is an error,
                // then it will come up during parsing in a code path that
                // can handle the error cleanly
            }
        }
    }

    /**
     * Returns the datatype that the identified definition's expression resolves to on evaluation.
     * Note that this method makes every attempt to discover this value, including parsing dependent
     * definitions if needed and possible.
     * 
     * @param variableId
     *            the identifier for the definition
     * 
     * @return the datatype that the identified definition's expression evaluates to
     * 
     * @throws ProcessingException
     *             if the identifier is not supported or if the result cannot be resolved
     */
    public URI getVariableType(String variableId) {
        VariableState state = (VariableState) (idMap.get(variableId));

        // make sure the variable is supported
        if (state == null)
            throw new ProcessingException("variable not supported: " + variableId);

        // if we've previously figured out the type, then return that
        if (state.type != null)
            return state.type;

        // we haven't figured out the type already, so see if we have or
        // can resolve the definition
        VariableDefinition definition = state.definition;
        if (definition == null)
            definition = getDefinition(variableId);

        // if we could get the definition, then ask it for the type
        if (definition != null)
            return definition.getExpression().getType();

        // we exhausted all our ways to get the right answer
        throw new ProcessingException("we couldn't establish the type: " + variableId);
    }

    /**
     * Returns true if the identified definition's expression resolves to a bag on evaluation. Note
     * that this method makes every attempt to discover this value, including parsing dependent
     * definitions if needed and possible.
     * 
     * @param variableId
     *            the identifier for the definition
     * 
     * @return true if the identified definition's expression evaluates to a bag
     * 
     * @throws ProcessingException
     *             if the identifier is not supported or if the result cannot be resolved
     */
    public boolean returnsBag(String variableId) {
        VariableState state = (VariableState) (idMap.get(variableId));

        // make sure the variable is supported
        if (state == null)
            throw new ProcessingException("variable not supported: " + variableId);

        // the flag is only valid if a type has also been determined
        if (state.type != null)
            return state.returnsBag;

        // we haven't figured out the type already, so see if we have or
        // can resolve the definition
        VariableDefinition definition = state.definition;
        if (definition == null)
            definition = getDefinition(variableId);

        // if we could get the definition, then ask it for the bag return
        if (definition != null)
            return definition.getExpression().returnsBag();

        // we exhausted all our ways to get the right answer
        throw new ProcessingException("couldn't establish bag return for " + variableId);
    }

    /**
     * Inner class that is used simply to manage fields associated with a given identifier.
     */
    class VariableState {

        // the resolved definition for the identifier
        public VariableDefinition definition;

        // the DOM node used to parse the definition
        public Node rootNode;

        // the datatype returned when evaluating the definition
        public URI type;

        // whether the definition's root evaluates to a Bag
        public boolean returnsBag;

        // whether the definition is being parsed and constructed
        public boolean handled;

        public VariableState() {
            this.definition = null;
            this.rootNode = null;
            this.type = null;
            this.returnsBag = false;
            this.handled = false;
        }

        public VariableState(VariableDefinition definition, Node rootNode, URI type,
                boolean returnsBag, boolean handled) {
            this.definition = definition;
            this.rootNode = rootNode;
            this.type = type;
            this.returnsBag = returnsBag;
            this.handled = handled;
        }
    }

}
