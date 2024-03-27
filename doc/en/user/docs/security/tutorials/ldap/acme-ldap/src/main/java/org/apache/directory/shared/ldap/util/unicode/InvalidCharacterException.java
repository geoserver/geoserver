/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.directory.shared.ldap.util.unicode;


import java.io.IOException;

import org.apache.directory.shared.i18n.I18n;

/**
 * 
 * Exception thrown when a Character is invalid
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InvalidCharacterException extends IOException
{
    private static final long serialVersionUID = 1L;
    private int input;


    public InvalidCharacterException( int input )
    {
        this.input = input;
    }


    @Override
    public String getMessage()
    {
        return I18n.err( I18n.ERR_04335, Integer.toHexString( input ) );
    }
}