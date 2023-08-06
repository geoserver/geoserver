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

package org.apache.directory.shared.ldap.util;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.directory.shared.i18n.I18n;


public class MandatoryAndOptionalComponentsMonitor implements ComponentsMonitor
{
    private ComponentsMonitor mandatoryComponentsMonitor;

    private ComponentsMonitor optionalComponentsMonitor;


    public MandatoryAndOptionalComponentsMonitor(String[] mandatoryComponents, String[] optionalComponents)
        throws IllegalArgumentException
    {
        // check for common elements
        for ( int i = 0; i < mandatoryComponents.length; i++ )
        {
            for ( int j = 0; j < optionalComponents.length; j++ )
            {
                if ( mandatoryComponents[i].equals( optionalComponents[j] ) )
                {
                    throw new IllegalArgumentException( I18n.err( I18n.ERR_04415, mandatoryComponents[i] ) );
                }
            }
        }

        mandatoryComponentsMonitor = new MandatoryComponentsMonitor( mandatoryComponents );
        optionalComponentsMonitor = new OptionalComponentsMonitor( optionalComponents );
    }


    public ComponentsMonitor useComponent( String component )
    {
        try
        {
            mandatoryComponentsMonitor.useComponent( component );
        }
        catch ( IllegalArgumentException e1 )
        {
            try
            {
                optionalComponentsMonitor.useComponent( component );
            }
            catch ( IllegalArgumentException e2 )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_04416, component ) );
            }
        }

        return this;
    }


    public boolean allComponentsUsed()
    {
        return ( mandatoryComponentsMonitor.allComponentsUsed() && optionalComponentsMonitor.allComponentsUsed() );
    }


    public boolean finalStateValid()
    {
        return ( mandatoryComponentsMonitor.finalStateValid() && optionalComponentsMonitor.finalStateValid() );
    }


    public List getRemainingComponents()
    {
        List remainingComponents = new LinkedList();

        remainingComponents.addAll( mandatoryComponentsMonitor.getRemainingComponents() );
        remainingComponents.addAll( optionalComponentsMonitor.getRemainingComponents() );

        return Collections.unmodifiableList( remainingComponents );
    }

}
