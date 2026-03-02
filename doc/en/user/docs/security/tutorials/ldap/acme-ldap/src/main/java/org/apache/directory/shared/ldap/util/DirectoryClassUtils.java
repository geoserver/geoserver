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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class DirectoryClassUtils
{
    private static final Logger LOG = LoggerFactory.getLogger( DirectoryClassUtils.class );
    
    /**
     * A replacement for {@link java.lang.Class#getMethod} with extended capability.
     * 
     * <p>
     * This method returns parameter-list assignment-compatible method as well as
     * exact-signature matching method.
     * 
     * @param clazz The class which will be queried for the method.
     * @param candidateMethodName Name of the method been looked for.
     * @param candidateParameterTypes Types of the parameters in the signature of the method being loooked for.
     * @return The Method found.
     * @throws NoSuchMethodException when the method cannot be found
     */
    public static Method getAssignmentCompatibleMethod( Class<?> clazz,
                                                        String candidateMethodName,
                                                        Class<?>[] candidateParameterTypes
                                                      ) throws NoSuchMethodException
    {
        if ( LOG.isDebugEnabled() )
        {
            StringBuilder buf = new StringBuilder();
            buf.append( "call to getAssignmentCompatibleMethod(): \n\tclazz = " );
            buf.append( clazz.getName() );
            buf.append( "\n\tcandidateMethodName = " );
            buf.append( candidateMethodName );
            buf.append( "\n\tcandidateParameterTypes = " );

            for ( Class<?> argClass : candidateParameterTypes )
            {
                buf.append( "\n\t\t" );
                buf.append( argClass.getName() );
            }

            LOG.debug( buf.toString() );
        }

        try
        {
            // Look for exactly the same signature.
            Method exactMethod = clazz.getMethod( candidateMethodName, candidateParameterTypes );
            
            if ( exactMethod != null )
            {
                return exactMethod;
            }
        }
        catch ( Exception e )
        {
            LOG.info( "Could not find accessible exact match for candidateMethod {}", candidateMethodName, e );
        }


        /**
         * Look for the assignment-compatible signature.
         */
        
        // Get all methods of the class.
        Method[] methods = clazz.getMethods();
        
        // For each method of the class...
        for ( int mx = 0; mx < methods.length; mx++ )
        {
            // If the method name does not match...
            if ( !candidateMethodName.equals( methods[ mx ].getName() ) )
            {
                // ... Go on with the next method.
                continue;
            }
            
            // ... Get parameter types list.
            Class<?>[] parameterTypes = methods[ mx ].getParameterTypes();
            
            // If parameter types list length mismatch...
            if ( parameterTypes.length != candidateParameterTypes.length )
            {
                // ... Go on with the next method.
                continue;
            }
            // If parameter types list length is OK...
            // ... For each parameter of the method...
            for ( int px = 0; px < parameterTypes.length; px++ )
            {
                // ... If the parameter is not assignment-compatible with the candidate parameter type...
                if ( ! parameterTypes[ px ].isAssignableFrom( candidateParameterTypes[ px ] ) )
                {
                    // ... Go on with the next method.
                    break;
                }
            }
            
            // Return the only one possible and found method.
            return methods[ mx ];
        }
        
        throw new NoSuchMethodException( clazz.getName() + "." + candidateMethodName
            + "(" + Arrays.toString( candidateParameterTypes ) + ")" );
    }
}
