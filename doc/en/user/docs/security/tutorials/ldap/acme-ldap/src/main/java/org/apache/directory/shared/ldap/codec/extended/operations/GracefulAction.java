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

package org.apache.directory.shared.ldap.codec.extended.operations;


import org.apache.directory.shared.asn1.AbstractAsn1Object;


/**
 * A common class for graceful Disconnect and Shutdown extended operations.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664290 $, $Date: 2008-06-07 09:28:06 +0300 (Sat, 07 Jun 2008) $, 
 */
public abstract class GracefulAction extends AbstractAsn1Object
{
    /** Undetermined value used for timeOffline */
    public static final int UNDETERMINED = 0;

    /** The shutdown is immediate */
    public static final int NOW = 0;

    /** offline Time after disconnection */
    protected int timeOffline = UNDETERMINED;

    /** Delay before disconnection */
    protected int delay = NOW;


    /**
     * Default constructor. The time offline will be set to UNDETERMINED and
     * there is no delay.
     */
    public GracefulAction()
    {
        super();
    }


    /**
     * Create a GracefulAction object, with a timeOffline and a delay
     * 
     * @param timeOffline The time the server will be offline
     * @param delay The delay before the disconnection
     */
    public GracefulAction( int timeOffline, int delay )
    {
        super();
        this.timeOffline = timeOffline;
        this.delay = delay;
    }


    public int getDelay()
    {
        return delay;
    }


    public void setDelay( int delay )
    {
        this.delay = delay;
    }


    public int getTimeOffline()
    {
        return timeOffline;
    }


    public void setTimeOffline( int timeOffline )
    {
        this.timeOffline = timeOffline;
    }
}
