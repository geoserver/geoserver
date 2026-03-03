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
package org.apache.directory.shared.ldap.util.tree;


/**
 * An interface for nodes in a tree designed to quickly lookup hierarchical DN.
 * Branch nodes in this tree contain other nodes.  Leaf nodes in the tree
 * contain a reference to an object  whose suffix is the path through the 
 * nodes of the tree from the root.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface DnNode<N>
{
    /**
     * Tells if the implementation is a leaf node. If it's a branch node
     * then false is returned.
     *
     * @return <code>true</code> if the class is a leaf node, false otherwise.
     */
    boolean isLeaf();
    
    
    /**
     * Returns the number of entries under this node. It includes
     * the node itself, plus the sum of all it children and descendents.
     *
     * @return The number of descendents
     */
    int size();
}
