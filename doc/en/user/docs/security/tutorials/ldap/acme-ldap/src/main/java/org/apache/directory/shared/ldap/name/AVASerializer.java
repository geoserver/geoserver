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
package org.apache.directory.shared.ldap.name;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.directory.shared.ldap.entry.BinaryValue;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class which serialize and deserialize an AttributeTypeAndValue
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AVASerializer
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( AVASerializer.class );

    /**
     * Serialize an AttributeTypeAndValue object.
     * 
     * An AttributeTypeAndValue is composed of  a type and a value.
     * The data are stored following the structure :
     * 
     * <li>upName</li> The User provided ATAV
     * <li>start</li> The position of this ATAV in the DN
     * <li>length</li> The ATAV length
     * <li>upType</li> The user Provided Type
     * <li>normType</li> The normalized AttributeType
     * <li>isHR<li> Tells if the value is a String or not
     * <p>
     * if the value is a String :
     * <li>upValue</li> The User Provided value.
     * <li>value</li> The normalized value.
     * <p>
     * if the value is binary :
     * <li>upValueLength</li>
     * <li>upValue</li> The User Provided value.
     * <li>valueLength</li>
     * <li>value</li> The normalized value.
     *
     * @param atav the AttributeTypeAndValue to serialize
     * @param out the OutputStream in which the atav will be serialized
     * @throws IOException If we can't serialize the atav
     */
    public static void serialize( AVA atav, ObjectOutput out ) throws IOException
    {
        if ( StringTools.isEmpty( atav.getUpName() ) || 
             StringTools.isEmpty( atav.getUpType() ) ||
             StringTools.isEmpty( atav.getNormType() ) ||
             ( atav.getStart() < 0 ) ||
             ( atav.getLength() < 2 ) ||             // At least a type and '='
             ( atav.getUpValue().isNull() ) ||
             ( atav.getNormValue().isNull() ) )
        {
            String message = "Cannot serialize an wrong ATAV, ";
            
            if ( StringTools.isEmpty( atav.getUpName() ) )
            {
                message += "the upName should not be null or empty";
            }
            else if ( StringTools.isEmpty( atav.getUpType() ) )
            {
                message += "the upType should not be null or empty";
            }
            else if ( StringTools.isEmpty( atav.getNormType() ) )
            {
                message += "the normType should not be null or empty";
            }
            else if ( atav.getStart() < 0 )
            {
                message += "the start should not be < 0";
            }
            else if ( atav.getLength() < 2 )
            {
                message += "the length should not be < 2";
            }
            else if ( atav.getUpValue().isNull() )
            {
                message += "the upValue should not be null";
            }
            else if ( atav.getNormValue().isNull() )
            {
                message += "the value should not be null";
            }
                
            LOG.error( message );
            throw new IOException( message );
        }
        
        out.writeUTF( atav.getUpName() );
        out.writeInt( atav.getStart() );
        out.writeInt( atav.getLength() );
        out.writeUTF( atav.getUpType() );
        out.writeUTF( atav.getNormType() );
        
        boolean isHR = !atav.getNormValue().isBinary();
        
        out.writeBoolean( isHR );
        
        if ( isHR )
        {
            out.writeUTF( atav.getUpValue().getString() );
            out.writeUTF( atav.getNormValue().getString() );
        }
        else
        {
            out.writeInt( atav.getUpValue().length() );
            out.write( atav.getUpValue().getBytes() );
            out.writeInt( atav.getNormValue().length() );
            out.write( atav.getNormValue().getBytes() );
        }
    }
    
    
    /**
     * Deserialize an AttributeTypeAndValue object
     * 
     * We read back the data to create a new ATAV. The structure 
     * read is exposed in the {@link AVA#writeExternal(ObjectOutput)} 
     * method<p>
     * 
     * @param in the input stream
     * @throws IOException If the input stream can't be read
     * @return The constructed AttributeTypeAndValue
     */
    public static AVA deserialize( ObjectInput in ) throws IOException
    {
        String upName = in.readUTF();
        int start = in.readInt();
        int length = in.readInt();
        String upType = in.readUTF();
        String normType = in.readUTF();
        
        boolean isHR = in.readBoolean();

        try
        {
            if ( isHR )
            {
                Value<String> upValue = new StringValue( in.readUTF() );
                Value<String> normValue = new StringValue( in.readUTF() );
                
                AVA atav = 
                    new AVA( upType, normType, upValue, normValue, upName );
                
                return atav;
            }
            else
            {
                int upValueLength = in.readInt();
                byte[] upValue = new byte[upValueLength];
                in.readFully( upValue );
    
                int valueLength = in.readInt();
                byte[] normValue = new byte[valueLength];
                in.readFully( normValue );
    
                AVA atav = 
                    new AVA( upType, normType, 
                        new BinaryValue( upValue) , 
                        new BinaryValue( normValue ), upName );
                
                return atav;
            }
        }
        catch ( LdapInvalidDnException ine )
        {
            throw new IOException( ine.getMessage() );
        }
    }
}
