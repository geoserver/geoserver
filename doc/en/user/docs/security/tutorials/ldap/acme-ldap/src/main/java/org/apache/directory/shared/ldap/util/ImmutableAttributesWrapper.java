/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.shared.ldap.util;


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.shared.i18n.I18n;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ImmutableAttributesWrapper implements Attributes
{
    private final Attributes wrapped;


    public ImmutableAttributesWrapper( Attributes wrapped )
    {
        this.wrapped = wrapped;
    }


    public boolean isCaseIgnored()
    {
        return wrapped.isCaseIgnored();
    }


    public int size()
    {
        return wrapped.size();
    }


    public Attribute get( String attrID )
    {
        return new ImmutableAttributeWrapper( wrapped.get( attrID ) );
    }


    public NamingEnumeration<? extends Attribute> getAll()
    {
        return new ImmutableEnumeration( wrapped.getAll() );
    }


    public NamingEnumeration<String> getIDs()
    {
        return wrapped.getIDs();
    }


    public Attribute put( String attrID, Object val )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04389 ) );
    }


    public Attribute put( Attribute attr )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04389 ) );
    }


    public Attribute remove( String attrID )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04390 ) );
    }


    public Object clone()
    {
        throw new IllegalStateException( I18n.err( I18n.ERR_04391 ) );
    }


    class ImmutableEnumeration implements NamingEnumeration
    {
        private NamingEnumeration wrappedEnum;


        public ImmutableEnumeration( NamingEnumeration<? extends Attribute> all )
        {
            wrappedEnum = all;
        }


        public Attribute next() throws NamingException
        {
            return new ImmutableAttributeWrapper( ( Attribute ) wrappedEnum.next() );
        }


        public boolean hasMore() throws NamingException
        {
            return wrappedEnum.hasMore();
        }


        public void close() throws NamingException
        {
            wrappedEnum.close();
        }


        public boolean hasMoreElements()
        {
            return wrappedEnum.hasMoreElements();
        }


        public Attribute nextElement()
        {
            return new ImmutableAttributeWrapper( ( Attribute ) wrappedEnum.nextElement() );
        }
    }
}
