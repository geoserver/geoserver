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
package org.apache.directory.shared.ldap.aci;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;


/**
 * Defines a set of zero or more users the permissions apply to.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923667 $, $Date: 2010-03-16 12:22:36 +0200 (Tue, 16 Mar 2010) $
 */
public abstract class UserClass implements Serializable
{
    /**
     * Every directory user (with possible requirements for
     * authenticationLevel).
     */
    public static final AllUsers ALL_USERS = new AllUsers();

    /**
     * The user with the same distinguished name as the entry being accessed, or
     * if the entry is a member of a family, then additionally the user with the
     * distinguished name of the ancestor.
     */
    public static final ThisEntry THIS_ENTRY = new ThisEntry();
    
    /**
     * The user as parent (ancestor) of accessed entry.
     */
    public static final ParentOfEntry PARENT_OF_ENTRY = new ParentOfEntry();


    /**
     * Creates a new instance.
     */
    protected UserClass()
    {
    }
    

    /**
     * Every directory user (with possible requirements for
     * authenticationLevel).
     */
    public static class AllUsers extends UserClass
    {
        private static final long serialVersionUID = 8967984720792510292L;


        private AllUsers()
        {
        }


        public String toString()
        {
            return "allUsers";
        }
    }

    /**
     * The user with the same distinguished name as the entry being accessed, or
     * if the entry is a member of a family, then additionally the user with the
     * distinguished name of the ancestor.
     */
    public static class ThisEntry extends UserClass
    {
        private static final long serialVersionUID = -8189325270233754470L;


        private ThisEntry()
        {
        }


        public String toString()
        {
            return "thisEntry";
        }
    }
    
    /**
     * The user as parent (ancestor) of accessed entry.
     */
    public static class ParentOfEntry extends UserClass
    {
        private static final long serialVersionUID = 5247207736068086476L;
        
        private ParentOfEntry()
        {
        }
        
        public String toString()
        {
            return "parentOfEntry";
        }
        
    }
    
    /**
     * A base class for all user classes which has a set of DNs.
     */
    private static abstract class NamedUserClass extends UserClass
    {
        protected final Set<DN> names;


        /**
         * Creates a new instance.
         * 
         * @param names a set of names
         */
        protected NamedUserClass( Set<DN> names )
        {
            this.names = Collections.unmodifiableSet( new HashSet<DN>( names ) );
        }


        /**
         * Returns the set of all names.
         */
        public Set<DN> getNames()
        {
            return names;
        }


        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }

            if ( o == null )
            {
                return false;
            }

            if ( getClass().isAssignableFrom( o.getClass() ) )
            {
                Name that = ( Name ) o;
                return this.names.equals( that.names );
            }

            return false;
        }


        public String toString()
        {
            StringBuilder buffer = new StringBuilder();
            
            boolean isFirst = true;
            buffer.append( "{ " );
            
            for ( DN name:names )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    buffer.append( ", " );
                }
                
                buffer.append( '"' );
                buffer.append( name.toString() );
                buffer.append( '"' );
            }
            
            buffer.append( " }" );
            
            return buffer.toString();
        }
    }

    /**
     * The user with the specified distinguished name.
     */
    public static class Name extends NamedUserClass
    {
        private static final long serialVersionUID = -4168412030168359882L;


        /**
         * Creates a new instance.
         * 
         * @param usernames
         *            the set of user DNs.
         */
        public Name( Set<DN> usernames )
        {
            super( usernames );
        }


        public String toString()
        {
            return "name " + super.toString();
        }
    }

    /**
     * The set of users who are members of the groupOfUniqueNames entry,
     * identified by the specified distinguished name. Members of a group of
     * unique names are treated as individual object names, and not as the names
     * of other groups of unique names.
     */
    public static class UserGroup extends NamedUserClass
    {
        private static final long serialVersionUID = 8887107815072965807L;


        /**
         * Creates a new instance.
         * 
         * @param groupNames
         *            the set of group DNs.
         */
        public UserGroup( Set<DN> groupNames )
        {
            super( groupNames );
        }


        public String toString()
        {
            return "userGroup " + super.toString();
        }
    }

    /**
     * The set of users whose distinguished names fall within the definition of
     * the (unrefined) subtree.
     */
    public static class Subtree extends UserClass
    {
        private static final long serialVersionUID = 3949337699049701332L;

        protected final Collection<SubtreeSpecification> subtreeSpecifications;


        /**
         * Creates a new instance.
         * 
         * @param subtreeSpecs
         *            the collection of unrefined {@link SubtreeSpecification}s.
         */
        public Subtree( Collection<SubtreeSpecification> subtreeSpecs )
        {
            this.subtreeSpecifications = Collections.unmodifiableCollection( new ArrayList<SubtreeSpecification>( subtreeSpecs ) );
        }


        /**
         * Returns the collection of unrefined {@link SubtreeSpecification}s.
         */
        public Collection<SubtreeSpecification> getSubtreeSpecifications()
        {
            return subtreeSpecifications;
        }


        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }

            if ( o instanceof Subtree )
            {
                Subtree that = ( Subtree ) o;
                return this.subtreeSpecifications.equals( that.subtreeSpecifications );
            }

            return false;
        }


        public String toString()
        {
            StringBuilder buffer = new StringBuilder();
            
            boolean isFirst = true;
            buffer.append( "subtree { " );
            
            for ( SubtreeSpecification ss:subtreeSpecifications )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    buffer.append( ", " );
                }
                
                ss.printToBuffer( buffer );
            }
            
            buffer.append( " }" );
            
            return buffer.toString();
        }
    }
}
