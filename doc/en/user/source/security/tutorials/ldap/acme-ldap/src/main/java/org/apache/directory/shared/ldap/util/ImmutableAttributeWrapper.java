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
import javax.naming.directory.DirContext;

import org.apache.directory.shared.i18n.I18n;


/**
 * A read only wrapper around an Attributes object.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ImmutableAttributeWrapper implements Attribute
{
    private final Attribute wrapped;


    public ImmutableAttributeWrapper( Attribute wrapped )
    {
        this.wrapped = wrapped;
    }


    public NamingEnumeration<?> getAll() throws NamingException
    {
        return wrapped.getAll();
    }


    public Object get() throws NamingException
    {
        return wrapped.get();
    }


    public int size()
    {
        return wrapped.size();
    }


    public String getID()
    {
        return wrapped.getID();
    }


    public boolean contains( Object attrVal )
    {
        return wrapped.contains( attrVal );
    }


    public boolean add( Object attrVal )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04392 ) );
    }


    public boolean remove( Object attrval )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04393 ) );
    }


    public void clear()
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04394 ) );
    }


    public DirContext getAttributeSyntaxDefinition() throws NamingException
    {
        return wrapped.getAttributeSyntaxDefinition();
    }


    public DirContext getAttributeDefinition() throws NamingException
    {
        return wrapped.getAttributeDefinition();
    }


    public Object clone()
    {
        throw new IllegalStateException( I18n.err( I18n.ERR_04395 ) );
    }


    public boolean isOrdered()
    {
        return wrapped.isOrdered();
    }


    public Object get( int ix ) throws NamingException
    {
        return wrapped.get( ix );
    }


    public Object remove( int ix )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04393 ) );
    }


    public void add( int ix, Object attrVal )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04392 ) );
    }


    public Object set( int ix, Object attrVal )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04396 ) );
    }
}
