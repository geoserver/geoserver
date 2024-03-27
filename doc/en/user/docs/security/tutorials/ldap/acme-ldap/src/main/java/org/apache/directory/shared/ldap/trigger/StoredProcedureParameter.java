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
package org.apache.directory.shared.ldap.trigger;


import org.apache.directory.shared.ldap.name.DN;


/**
 * An entity that represents a stored procedure parameter which can be
 * specified in an LDAP Trigger Specification.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date:$
 */
public abstract class StoredProcedureParameter
{
    public static class Generic_LDAP_CONTEXT extends StoredProcedureParameter
    {
        private DN ctxName;
        
        private Generic_LDAP_CONTEXT( DN ctxName )
        {
            super( "$ldapContext" );
            this.ctxName = ctxName;
        }
        
        public static StoredProcedureParameter instance( DN ctxName )
        {
            return new Generic_LDAP_CONTEXT( ctxName );
        }
        
        public DN getCtxName()
        {
            return ctxName;
        }
        
        public String toString()
        {
            return name + " \"" + ctxName.getName() + "\"";
        }
    }

    
    public static class Generic_OPERATION_PRINCIPAL extends StoredProcedureParameter
    {
        private static Generic_OPERATION_PRINCIPAL instance = new Generic_OPERATION_PRINCIPAL( "$operationPrincipal" );
        
        private Generic_OPERATION_PRINCIPAL( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }

    
    protected final String name;


    protected StoredProcedureParameter( String name )
    {
        this.name = name;
    }


    /**
     * Returns the name of this Stored Procedure Parameter.
     */
    public String getName()
    {
        return name;
    }


    public String toString()
    {
        return name;
    }
    

    /**
     * @see java.lang.Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int h = 37;
        
        h = h*17 + ( ( name == null ) ? 0 : name.hashCode() );
        
        return h;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        final StoredProcedureParameter other = ( StoredProcedureParameter ) obj;
        if ( name == null )
        {
            if ( other.name != null )
                return false;
        }
        else if ( !name.equals( other.name ) )
            return false;
        return true;
    }

    
    public static class Modify_OBJECT extends StoredProcedureParameter
    {
        private static Modify_OBJECT instance = new Modify_OBJECT( "$object" );
        
        private Modify_OBJECT( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }
    
    
    public static class Modify_MODIFICATION extends StoredProcedureParameter
    {
        private static Modify_MODIFICATION instance = new Modify_MODIFICATION( "$modification" );
        
        private Modify_MODIFICATION( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }
    
    
    public static class Modify_OLD_ENTRY extends StoredProcedureParameter
    {
        private static Modify_OLD_ENTRY instance = new Modify_OLD_ENTRY( "$oldEntry" );
        
        private Modify_OLD_ENTRY( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }
    
    
    public static class Modify_NEW_ENTRY extends StoredProcedureParameter
    {
        private static Modify_NEW_ENTRY instance = new Modify_NEW_ENTRY( "$newEntry" );
        
        private Modify_NEW_ENTRY( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }

    
    public static class Add_ENTRY extends StoredProcedureParameter
    {
        private static Add_ENTRY instance = new Add_ENTRY( "$entry" );
        
        private Add_ENTRY( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }
    
    
    public static class Add_ATTRIBUTES extends StoredProcedureParameter
    {
        private static Add_ATTRIBUTES instance = new Add_ATTRIBUTES( "$attributes" );
        
        private Add_ATTRIBUTES( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }

    
    public static class Delete_NAME extends StoredProcedureParameter
    {
        private static Delete_NAME instance = new Delete_NAME( "$name" );
        
        private Delete_NAME( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }
    
    
    public static class Delete_DELETED_ENTRY extends StoredProcedureParameter
    {
        private static Delete_DELETED_ENTRY instance = new Delete_DELETED_ENTRY( "$deletedEntry" );
        
        private Delete_DELETED_ENTRY( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }

    
    public static class ModifyDN_ENTRY extends StoredProcedureParameter
    {
        private static ModifyDN_ENTRY instance = new ModifyDN_ENTRY( "$entry" );
        
        private ModifyDN_ENTRY( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }
    
    
    public static class ModifyDN_NEW_RDN extends StoredProcedureParameter
    {
        private static ModifyDN_NEW_RDN instance = new ModifyDN_NEW_RDN( "$newrdn" );
        
        private ModifyDN_NEW_RDN( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }
    
    
    public static class ModifyDN_DELETE_OLD_RDN extends StoredProcedureParameter
    {
        private static ModifyDN_DELETE_OLD_RDN instance = new ModifyDN_DELETE_OLD_RDN( "$deleteoldrdn" );
        
        private ModifyDN_DELETE_OLD_RDN( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }
    
    
    public static class ModifyDN_NEW_SUPERIOR extends StoredProcedureParameter
    {
        private static ModifyDN_NEW_SUPERIOR instance = new ModifyDN_NEW_SUPERIOR( "$newSuperior" );
        
        private ModifyDN_NEW_SUPERIOR( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }
    
    
    public static class ModifyDN_OLD_RDN extends StoredProcedureParameter
    {
        private static ModifyDN_OLD_RDN instance = new ModifyDN_OLD_RDN( "$oldRDN" );
        
        private ModifyDN_OLD_RDN( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }
    
    
    public static class ModifyDN_OLD_SUPERIOR_DN extends StoredProcedureParameter
    {
        private static ModifyDN_OLD_SUPERIOR_DN instance = new ModifyDN_OLD_SUPERIOR_DN( "$oldRDN" );
        
        private ModifyDN_OLD_SUPERIOR_DN( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }
    
    
    public static class ModifyDN_NEW_DN extends StoredProcedureParameter
    {
        private static ModifyDN_NEW_DN instance = new ModifyDN_NEW_DN( "$oldRDN" );
        
        private ModifyDN_NEW_DN( String identifier )
        {
            super( identifier );
        }
        
        public static StoredProcedureParameter instance()
        {
            return instance;
        }
    }
}
