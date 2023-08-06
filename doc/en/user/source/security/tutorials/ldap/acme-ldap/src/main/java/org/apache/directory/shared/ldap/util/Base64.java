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

import org.apache.directory.shared.i18n.I18n;


/**
 * decoding of base64 characters to raw bytes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 919765 $
 */
public class Base64
{

    /**
     * passed data array.
     * 
     * @param a_data
     *            the array of bytes to encode
     * @return base64-coded character array.
     */
    public static char[] encode( byte[] a_data )
    {
        char[] l_out = new char[( ( a_data.length + 2 ) / 3 ) * 4];

        //
        // 3 bytes encode to 4 chars. Output is always an even
        // multiple of 4 characters.
        //
        for ( int ii = 0, l_index = 0; ii < a_data.length; ii += 3, l_index += 4 )
        {
            boolean l_quad = false;
            boolean l_trip = false;

            int l_val = ( 0xFF & a_data[ii] );
            l_val <<= 8;
            if ( ( ii + 1 ) < a_data.length )
            {
                l_val |= ( 0xFF & a_data[ii + 1] );
                l_trip = true;
            }

            l_val <<= 8;
            if ( ( ii + 2 ) < a_data.length )
            {
                l_val |= ( 0xFF & a_data[ii + 2] );
                l_quad = true;
            }

            l_out[l_index + 3] = s_alphabet[( l_quad ? ( l_val & 0x3F ) : 64 )];
            l_val >>= 6;
            l_out[l_index + 2] = s_alphabet[( l_trip ? ( l_val & 0x3F ) : 64 )];
            l_val >>= 6;
            l_out[l_index + 1] = s_alphabet[l_val & 0x3F];
            l_val >>= 6;
            l_out[l_index + 0] = s_alphabet[l_val & 0x3F];
        }
        return l_out;
    }


    /**
     * Decodes a BASE-64 encoded stream to recover the original data. White
     * space before and after will be trimmed away, but no other manipulation of
     * the input will be performed. As of version 1.2 this method will properly
     * handle input containing junk characters (newlines and the like) rather
     * than throwing an error. It does this by pre-parsing the input and
     * generating from that a count of VALID input characters.
     * 
     * @param a_data
     *            data to decode.
     * @return the decoded binary data.
     */
    public static byte[] decode( char[] data )
    {
        // as our input could contain non-BASE64 data (newlines,
        // whitespace of any sort, whatever) we must first adjust
        // our count of USABLE data so that...
        // (a) we don't misallocate the output array, and
        // (b) think that we miscalculated our data length
        // just because of extraneous throw-away junk

        int tempLen = data.length;
        
        for ( char c:data)
        {
            if ( ( c > 255 ) || s_codes[c] < 0 )
            {
                --tempLen; // ignore non-valid chars and padding
            }
        }
        // calculate required length:
        // -- 3 bytes for every 4 valid base64 chars
        // -- plus 2 bytes if there are 3 extra base64 chars,
        // or plus 1 byte if there are 2 extra.

        int l_len = ( tempLen / 4 ) * 3;

        if ( ( tempLen % 4 ) == 3 )
        {
            l_len += 2;
        }

        if ( ( tempLen % 4 ) == 2 )
        {
            l_len += 1;
        }

        byte[] l_out = new byte[l_len];

        int l_shift = 0; // # of excess bits stored in accum
        int l_accum = 0; // excess bits
        int l_index = 0;

        // we now go through the entire array (NOT using the 'tempLen' value)
        for ( char c:data )
        {
            int l_value = ( c > 255 ) ? -1 : s_codes[c];

            if ( l_value >= 0 ) // skip over non-code
            {
                l_accum <<= 6; // bits shift up by 6 each time thru
                l_shift += 6; // loop, with new bits being put in
                l_accum |= l_value; // at the bottom. whenever there
                if ( l_shift >= 8 ) // are 8 or more shifted in, write them
                {
                    l_shift -= 8; // out (from the top, leaving any excess
                    l_out[l_index++] = // at the bottom for next iteration.
                    ( byte ) ( ( l_accum >> l_shift ) & 0xff );
                }
            }
            // we will also have skipped processing a padding null byte ('=')
            // here;
            // these are used ONLY for padding to an even length and do not
            // legally
            // occur as encoded data. for this reason we can ignore the fact
            // that
            // no index++ operation occurs in that special case: the out[] array
            // is
            // initialized to all-zero bytes to start with and that works to our
            // advantage in this combination.
        }

        // if there is STILL something wrong we just have to throw up now!
        if ( l_index != l_out.length )
        {
            throw new Error( I18n.err( I18n.ERR_04348, l_index, l_out.length ) );
        }

        return l_out;
    }

    /** code characters for values 0..63 */
    private static char[] s_alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
        .toCharArray();

    /** lookup table for converting base64 characters to value in range 0..63 */
    private static byte[] s_codes = new byte[256];

    static
    {
        for ( int ii = 0; ii < 256; ii++ )
        {
            s_codes[ii] = -1;
        }

        for ( int ii = 'A'; ii <= 'Z'; ii++ )
        {
            s_codes[ii] = ( byte ) ( ii - 'A' );
        }

        for ( int ii = 'a'; ii <= 'z'; ii++ )
        {
            s_codes[ii] = ( byte ) ( 26 + ii - 'a' );
        }

        for ( int ii = '0'; ii <= '9'; ii++ )
        {
            s_codes[ii] = ( byte ) ( 52 + ii - '0' );
        }

        s_codes['+'] = 62;
        s_codes['/'] = 63;
    }
}
