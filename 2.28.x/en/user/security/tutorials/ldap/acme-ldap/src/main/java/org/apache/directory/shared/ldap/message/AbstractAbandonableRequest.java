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
package org.apache.directory.shared.ldap.message;


import java.util.Observable;
import java.util.Observer;

import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.internal.InternalAbandonableRequest;
import org.apache.directory.shared.ldap.message.internal.InternalAbstractRequest;


/**
 * The base abandonable request message class. All such requests have a reponse
 * type.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 910150 $
 */
public class AbstractAbandonableRequest extends InternalAbstractRequest implements InternalAbandonableRequest
{
    static final long serialVersionUID = -4511116249089399040L;

    /** Flag indicating whether or not this request returns a response. */
    private boolean abandoned = false;

    private RequestObservable o;


    /**
     * Subclasses must provide these parameters via a super constructor call.
     * 
     * @param id
     *            the sequential message identifier
     * @param type
     *            the request type enum
     */
    protected AbstractAbandonableRequest( final int id, final MessageTypeEnum type )
    {
        super( id, type, true );
    }


    public void abandon()
    {
        if ( abandoned )
        {
            return;
        }
        
        abandoned = true;
        if ( o == null )
        {
            o = new RequestObservable();
        }
        o.setChanged();
        o.notifyObservers();
        o.deleteObservers();
    }


    public boolean isAbandoned()
    {
        return abandoned;
    }


    public void addAbandonListener( final AbandonListener listener )
    {
        if ( o == null )
        {
            o = new RequestObservable();
        }
        
        o.addObserver( new Observer()
        {
            public void update( Observable o, Object arg )
            {
                listener.requestAbandoned( AbstractAbandonableRequest.this );
            }
        } );
    }

    
    class RequestObservable extends Observable
    {
        public void setChanged()
        {
            super.setChanged();
        }
    }
}
