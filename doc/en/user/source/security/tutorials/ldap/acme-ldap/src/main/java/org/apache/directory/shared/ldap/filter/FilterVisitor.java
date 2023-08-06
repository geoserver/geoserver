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


import java.util.List;


/**
 * Filter expression tree node visitor interface. Note that this is a variation
 * of the extrinsic visitor variation. It has the following advantages over the
 * standard visitor pattern:
 * <ul>
 * <li>Visitor takes responsibility that a visitor can visit a node</li>
 * <li>Each visitor knows which types of concrete classes it can visit</li>
 * <li>New visitors can be created without changing the node class</li>
 * <li>New node classes can be added without having to change old visitors</li>
 * <li>Visitation order can be controled in every respect:</li>
 * <ul>
 * <li>Visitation rejection with canVisit() and/or getOrder()</li>
 * <li>Recursive visitation ordering with isPrefix()</li>
 * <li>Child visitation ordering with getOrder()</li>
 * </ul>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 664290 $
 */
public interface FilterVisitor
{
    /**
     * Visits a filter expression AST using a specific visitation order.
     * 
     * @param node the node to visit
     * @return node the resulting modified node
     */
    Object visit( ExprNode node );


    /**
     * Checks to see if a node can be visited.
     * 
     * @param node the node to be visited
     * @return whether or node the node should be visited
     */
    boolean canVisit( ExprNode node );


    /**
     * Determines whether the visitation order is prefix or postfix.
     * 
     * @return true if the visitation is in prefix order, false otherwise.
     */
    boolean isPrefix();


    /**
     * Get the array of children to visit sequentially to determine the order of
     * child visitations. Some children may not be returned at all if canVisit()
     * returns false on them.
     * 
     * @param node the parent branch node
     * @param children the child node array
     * @return the new reordered array of children
     */
    List<ExprNode> getOrder( BranchNode node, List<ExprNode> children );
}
