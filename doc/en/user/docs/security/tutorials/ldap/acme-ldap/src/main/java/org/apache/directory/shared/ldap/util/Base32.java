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


/**
 * decoding of base32 characters to raw bytes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 664290 $
 */
public class Base32
{
    private static byte[] CHARS = new byte[]{ 
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 
        'Y', 'Z', '2', '3', '4', '5', '6', '7' };
    

    public static String encode( String str )
    {
        if ( StringTools.isEmpty( str ) )
        {
            return "";
        }
        
        byte[] data = StringTools.getBytesUtf8( str );
        int dataLength = data.length;
        int newLength = ( ( dataLength << 3 ) / 5 ) + ( ( dataLength % 5 ) == 0 ? 0 : 1 );
        newLength += ( ( newLength % 8 == 0 ) ? 0 : 8 - newLength % 8 );
        byte[] out = new byte[newLength];
        
        int roundLength = (dataLength/5) * 5;
        int posOut = 0;
        int posIn = 0;
        
        if ( roundLength != 0 )
        {
            for ( posIn = 0; posIn < roundLength; posIn += 5 )
            {
                byte b0 = data[posIn];
                byte b1 = data[posIn+1];
                byte b2 = data[posIn+2];
                byte b3 = data[posIn+3];
                byte b4 = data[posIn+4];
                
                out[posOut++] = CHARS[( b0 & 0xF8) >> 3];
                out[posOut++] = CHARS[( ( b0 & 0x07) << 2 ) | ( ( b1 & 0xC0 ) >> 6 ) ];
                out[posOut++] = CHARS[( b1 & 0x3E) >> 1];
                out[posOut++] = CHARS[( ( b1 & 0x01) << 4 ) | ( ( b2 & 0xF0 ) >> 4 ) ];
                out[posOut++] = CHARS[( ( b2 & 0x0F) << 1 ) | ( ( b3 & 0x80 ) >> 7 ) ];
                out[posOut++] = CHARS[( b3 & 0x7C) >> 2];
                out[posOut++] = CHARS[( ( b3 & 0x03) << 3 ) | ( ( b4 & 0x70 ) >> 5 )];
                out[posOut++] = CHARS[b4 & 0x1F];
            }
        }
        
        int remaining = dataLength - roundLength;
        
        switch ( remaining )
        {
            case 1 :
                byte b0 = data[posIn++];
                
                out[posOut++] = CHARS[( b0 & 0xF8) >> 3];
                out[posOut++] = CHARS[( ( b0 & 0x07) << 2 )];
                out[posOut++] = '=';
                out[posOut++] = '=';
                out[posOut++] = '=';
                out[posOut++] = '=';
                out[posOut++] = '=';
                out[posOut++] = '=';
                break;

            case 2 :
                b0 = data[posIn++];
                byte b1 = data[posIn++];

                out[posOut++] = CHARS[( b0 & 0xF8) >> 3];
                out[posOut++] = CHARS[( ( b0 & 0x07) << 2 ) | ( ( b1 & 0xC0 ) >> 6 ) ];
                out[posOut++] = CHARS[( b1 & 0x3E) >> 1];
                out[posOut++] = CHARS[( ( b1 & 0x01) << 4 )];
                out[posOut++] = '=';
                out[posOut++] = '=';
                out[posOut++] = '=';
                out[posOut++] = '=';
                break;
                
            case 3 :
                b0 = data[posIn++];
                b1 = data[posIn++];
                byte b2 = data[posIn++];

                out[posOut++] = CHARS[( b0 & 0xF8) >> 3];
                out[posOut++] = CHARS[( ( b0 & 0x07) << 2 ) | ( ( b1 & 0xC0 ) >> 6 ) ];
                out[posOut++] = CHARS[( b1 & 0x3E) >> 1];
                out[posOut++] = CHARS[( ( b1 & 0x01) << 4 ) | ( ( b2 & 0xF0 ) >> 4 ) ];
                out[posOut++] = CHARS[( ( b2 & 0x0F) << 1 ) ];
                out[posOut++] = '=';
                out[posOut++] = '=';
                out[posOut++] = '=';
                break;
                
            case 4 :
                b0 = data[posIn++];
                b1 = data[posIn++];
                b2 = data[posIn++];
                byte b3 = data[posIn++];

                out[posOut++] = CHARS[( b0 & 0xF8) >> 3];
                out[posOut++] = CHARS[( ( b0 & 0x07) << 2 ) | ( ( b1 & 0xC0 ) >> 6 ) ];
                out[posOut++] = CHARS[( b1 & 0x3E) >> 1];
                out[posOut++] = CHARS[( ( b1 & 0x01) << 4 ) | ( ( b2 & 0xF0 ) >> 4 ) ];
                out[posOut++] = CHARS[( ( b2 & 0x0F) << 1 ) | ( ( b3 & 0x80 ) >> 7 ) ];
                out[posOut++] = CHARS[( b3 & 0x7C) >> 2];
                out[posOut++] = CHARS[( ( b3 & 0x03) << 3 ) ];
                out[posOut++] = '=';
                break;
        }
        
        return StringTools.utf8ToString( out );
    }
}
