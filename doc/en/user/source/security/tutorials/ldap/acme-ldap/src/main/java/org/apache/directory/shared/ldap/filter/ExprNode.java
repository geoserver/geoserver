/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */

package org.apache.directory.shared.ldap.filter;


/**
 * Root expression node interface which all expression nodes in the filter
 * expression tree implement.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 746607 $
 */
public interface ExprNode extends Cloneable
{
    /**
     * Gets an annotation on the tree by key.
     * 
     * @param key the annotation key.
     * @return the annotation value.
     */
    Object get( Object key );


    /**
     * Sets a annotation key to a value.
     * 
     * @param key the annotation key.
     * @param value the annotation value.
     */
    void set( String key, Object value );


    /**
     * Tests to see if this node is a leaf or branch node.
     * 
     * @return true if the node is a leaf,false otherwise
     */
    boolean isLeaf();

    
    /**
     * Gets the assertion type of this node. Make it possible to use switch
     * statements on the node type.
     * 
     * @return the assertion type
     */
    AssertionType getAssertionType();

    /**
     * Recursively appends the refinement string representation of this node and its
     * descendants in prefix notation to a buffer.
     *
     * TODO - Why is this here? Why not put it in some utility class?
     * 
     * @param buf the buffer to append to.
     * @return The buffer in which the refinement has been appended
     * @throws UnsupportedOperationException if this node isn't a part of a refinement.
     * @return the refinement buffer
     */
    StringBuilder printRefinementToBuffer( StringBuilder buf );
    
    
    /**
     * Element/node accept method for visitor pattern.
     * 
     * @param visitor the filter expression tree structure visitor
     * TODO - what is this modified element ?
     * @return the modified element
     */
    Object accept( FilterVisitor visitor );
    
    
    /**
     * Clone the object
     * @return
     */
    ExprNode clone();
}
