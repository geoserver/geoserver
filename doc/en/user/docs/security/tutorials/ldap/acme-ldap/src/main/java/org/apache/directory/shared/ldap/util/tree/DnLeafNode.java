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
 * A leaf node which stores an element. These objects are stored in BranchNodes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DnLeafNode<N> implements DnNode<N>
{
    /** The stored partition */
    private N element;

    
    /**
     * Creates a new instance of DnLeafNode.
     *
     * @param element the element to store
     */
    public DnLeafNode( N element )
    {
        this.element = element;
    }

    
    /**
     * @see DnNode#isLeaf()
     */
    public boolean isLeaf()
    {
        return true;
    }
    

    /**
     * @return Return the stored element
     */
    public N getElement()
    {
        return element;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return 1;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return element.toString();
    }
}
