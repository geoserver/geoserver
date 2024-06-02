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
 * Utility class used by the DN Parser.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 918756 $, $Date: 2010-03-04 01:05:29 +0200 (Thu, 04 Mar 2010) $
 */
public class DNUtils
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------
    /** A value if we got an error while parsing */
    public static final int PARSING_ERROR = -1;

    /** A value if we got a correct parsing */
    public static final int PARSING_OK = 0;

    /** If an hex pair contains only one char, this value is returned */
    public static final int BAD_HEX_PAIR = -2;

    /** A constant representing one char length */
    public static final int ONE_CHAR = 1;

    /** A constant representing two chars length */
    public static final int TWO_CHARS = 2;

    /** A constant representing one byte length */
    public static final int ONE_BYTE = 1;

    /** A constant representing two bytes length */
    public static final int TWO_BYTES = 2;

    /**
     * &lt;safe-init-char&gt; ::= [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x1F] |
     * [0x21-0x39] | 0x3B | [0x3D-0x7F]
     */
    private static final boolean[] SAFE_INIT_CHAR =
        { 
            false, true,  true,  true,  true,  true,  true,  true, 
            true,  true,  false, true,  true,  false, true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            false, true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  false, true,  false, true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true 
        };

    /** &lt;safe-char&gt; ::= [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x7F] */
    private static final boolean[] SAFE_CHAR =
        { 
            false, true,  true,  true,  true,  true,  true,  true, 
            true,  true,  false, true,  true,  false, true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
        };

    /**
     * &lt;base64-char&gt; ::= 0x2B | 0x2F | [0x30-0x39] | 0x3D | [0x41-0x5A] |
     * [0x61-0x7A]
     */
    private static final boolean[] BASE64_CHAR =
        { 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, true,  false, false, false, true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  false, false, false, true,  false, false, 
            false, true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  false, false, false, false, false,
            false, true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  false, false, false, false, false 
        };

    /**
     * ' ' | '"' | '#' | '+' | ',' | [0-9] | ';' | '<' | '=' | '>' | [A-F] | '\' | [a-f]
     * 0x22 | 0x23 | 0x2B | 0x2C | [0x30-0x39] | 0x3B | 0x3C | 0x3D | 0x3E |
     * [0x41-0x46] | 0x5C | [0x61-0x66]
     */
    private static final boolean[] PAIR_CHAR =
        { 
            false, false, false, false, false, false, false, false, // 00 -> 07
            false, false, false, false, false, false, false, false, // 08 -> 0F
            false, false, false, false, false, false, false, false, // 10 -> 17
            false, false, false, false, false, false, false, false, // 18 -> 1F
            true,  false, true,  true,  false, false, false, false, // 20 -> 27 ( ' ', '"', '#' )
            false, false, false, true,  true,  false, false, false, // 28 -> 2F ( '+', ',' )
            true,  true,  true,  true,  true,  true,  true,  true,  // 30 -> 37 ( '0'..'7' )
            true,  true,  false, true,  true,  true,  true,  false, // 38 -> 3F ( '8', '9', ';', '<', '=', '>' ) 
            false, true,  true,  true,  true,  true,  true,  false, // 40 -> 47 ( 'A', 'B', 'C', 'D', 'E', 'F' )
            false, false, false, false, false, false, false, false, // 48 -> 4F
            false, false, false, false, false, false, false, false, // 50 -> 57
            false, false, false, false, true,  false, false, false, // 58 -> 5F ( '\' )
            false, true,  true,  true,  true,  true,  true,  false, // 60 -> 67 ( 'a', 'b', 'c', 'd', 'e', 'f' )
            false, false, false, false, false, false, false, false, // 68 -> 6F
            false, false, false, false, false, false, false, false, // 70 -> 77
            false, false, false, false, false, false, false, false  // 78 -> 7F
        };


    /**
     * [0x01-0x1F] | 0x21 | [0x24-0x2A] | [0x2D-0x3A] | 0x3D | [0x3F-0x5B] | [0x5D-0x7F]
     */
    private static final boolean[] LUTF1 =
        { 
            false, true,  true,  true,  true,  true,  true,  true, // 00 -> 07 '\0'
            true,  true,  true,  true,  true,  true,  true,  true, // 08 -> 0F
            true,  true,  true,  true,  true,  true,  true,  true, // 10 -> 17
            true,  true,  true,  true,  true,  true,  true,  true, // 18 -> 1F
            false, true,  false, false, true,  true,  true,  true, // 20 -> 27 ( ' ', '"', '#' )
            true,  true,  true,  false, false, true,  true,  true, // 28 -> 2F ( '+', ',' )
            true,  true,  true,  true,  true,  true,  true,  true, // 30 -> 37 
            true,  true,  true,  false, false, true,  false, true, // 38 -> 3F ( ';', '<', '>' ) 
            true,  true,  true,  true,  true,  true,  true,  true, // 40 -> 47 
            true,  true,  true,  true,  true,  true,  true,  true, // 48 -> 4F
            true,  true,  true,  true,  true,  true,  true,  true, // 50 -> 57
            true,  true,  true,  true,  false, true,  true,  true, // 58 -> 5F ( '\' )
            true,  true,  true,  true,  true,  true,  true,  true, // 60 -> 67 
            true,  true,  true,  true,  true,  true,  true,  true, // 68 -> 6F
            true,  true,  true,  true,  true,  true,  true,  true, // 70 -> 77
            true,  true,  true,  true,  true,  true,  true,  true  // 78 -> 7F
        };


    /**
     * [0x01-0x21] | [0x23-0x2A] | [0x2D-0x3A] | 0x3D | [0x3F-0x5B] | [0x5D-0x7F]
     */
    private static final boolean[] SUTF1 =
        { 
            false, true,  true,  true,  true,  true,  true,  true, // 00 -> 07 '\0'
            true,  true,  true,  true,  true,  true,  true,  true, // 08 -> 0F
            true,  true,  true,  true,  true,  true,  true,  true, // 10 -> 17
            true,  true,  true,  true,  true,  true,  true,  true, // 18 -> 1F
            true,  true,  false, true,  true,  true,  true,  true, // 20 -> 27 ( '"' )
            true,  true,  true,  false, false, true,  true,  true, // 28 -> 2F ( '+', ',' )
            true,  true,  true,  true,  true,  true,  true,  true, // 30 -> 37 
            true,  true,  true,  false, false, true,  false, true, // 38 -> 3F ( ';', '<', '>' ) 
            true,  true,  true,  true,  true,  true,  true,  true, // 40 -> 47 
            true,  true,  true,  true,  true,  true,  true,  true, // 48 -> 4F
            true,  true,  true,  true,  true,  true,  true,  true, // 50 -> 57
            true,  true,  true,  true,  false, true,  true,  true, // 58 -> 5F ( '\' )
            true,  true,  true,  true,  true,  true,  true,  true, // 60 -> 67 
            true,  true,  true,  true,  true,  true,  true,  true, // 68 -> 6F
            true,  true,  true,  true,  true,  true,  true,  true, // 70 -> 77
            true,  true,  true,  true,  true,  true,  true,  true  // 78 -> 7F
        };


    /**
     * ' ' | '"' | '#' | '+' | ',' | ';' | '<' | '=' | '>' | '\' |
     * 0x22 | 0x23 | 0x2B | 0x2C | 0x3B | 0x3C | 0x3D | 0x3E | 0x5C
     */
    private static final boolean[] PAIR_CHAR_ONLY =
        { 
            false, false, false, false, false, false, false, false, // 00 -> 07
            false, false, false, false, false, false, false, false, // 08 -> 0F
            false, false, false, false, false, false, false, false, // 10 -> 17
            false, false, false, false, false, false, false, false, // 18 -> 1F
            true,  false, true,  true,  false, false, false, false, // 20 -> 27 ( ' ', '"', '#' )
            false, false, false, true,  true,  false, false, false, // 28 -> 2F ( '+', ',' )
            false, false, false, false, false, false, false, false, // 30 -> 37
            false, false, false, true,  true,  true,  true,  false, // 38 -> 3F ( ';', '<', '=', '>' ) 
            false, false, false, false, false, false, false, false, // 40 -> 47
            false, false, false, false, false, false, false, false, // 48 -> 4F
            false, false, false, false, false, false, false, false, // 50 -> 57
            false, false, false, false, true,  false, false, false, // 58 -> 5F ( '\' )
            false, false, false, false, false, false, false, false, // 60 -> 67
            false, false, false, false, false, false, false, false, // 68 -> 6F
            false, false, false, false, false, false, false, false, // 70 -> 77
            false, false, false, false, false, false, false, false  // 78 -> 7F
        };

    /**
     * '"' | '#' | '+' | ',' | [0-9] | ';' | '<' | '=' | '>' | [A-F] | '\' |
     * [a-f] 0x22 | 0x23 | 0x2B | 0x2C | [0x30-0x39] | 0x3B | 0x3C | 0x3D | 0x3E |
     * [0x41-0x46] | 0x5C | [0x61-0x66]
     */
    private static final int[] STRING_CHAR =
        { 
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 00 -> 03
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 04 -> 07
            ONE_CHAR,      ONE_CHAR,      PARSING_ERROR, ONE_CHAR,     // 08 -> 0B
            ONE_CHAR,      PARSING_ERROR, ONE_CHAR,      ONE_CHAR,     // 0C -> 0F
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 10 -> 13
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 14 -> 17
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 18 -> 1B
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 1C -> 1F
            ONE_CHAR,      ONE_CHAR,      PARSING_ERROR, ONE_CHAR,     // 20 -> 23
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 24 -> 27
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      PARSING_ERROR,// 28 -> 2B
            PARSING_ERROR, ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 2C -> 2F
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 30 -> 33
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 34 -> 37
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      PARSING_ERROR,// 38 -> 3B
            PARSING_ERROR, ONE_CHAR,      PARSING_ERROR, ONE_CHAR      // 3C -> 3F
        };

    /** "oid." static */
    public static final String OID_LOWER = "oid.";

    /** "OID." static */
    public static final String OID_UPPER = "OID.";

    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Walk the buffer while characters are Safe String characters :
     * &lt;safe-string&gt; ::= &lt;safe-init-char&gt; &lt;safe-chars&gt; &lt;safe-init-char&gt; ::=
     * [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x1F] | [0x21-0x39] | 0x3B |
     * [0x3D-0x7F] &lt;safe-chars&gt; ::= &lt;safe-char&gt; &lt;safe-chars&gt; | &lt;safe-char&gt; ::=
     * [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x7F]
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return The position of the first character which is not a Safe Char
     */
    public static int parseSafeString( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return -1;
        }
        else
        {
            byte c = bytes[index];

            if ( ( ( c | 0x7F ) != 0x7F ) || ( !SAFE_INIT_CHAR[c] ) )
            {
                return -1;
            }

            index++;

            while ( index < bytes.length )
            {
                c = bytes[index];

                if ( ( ( c | 0x7F ) != 0x7F ) || ( !SAFE_CHAR[c] ) )
                {
                    break;
                }

                index++;
            }

            return index;
        }
    }


    /**
     * Walk the buffer while characters are Alpha characters : &lt;alpha&gt; ::=
     * [0x41-0x5A] | [0x61-0x7A]
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return The position of the first character which is not an Alpha Char
     */
    public static int parseAlphaASCII( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return -1;
        }
        else
        {
            byte b = bytes[index++];

            if ( StringTools.isAlpha( b ) )
            {
                return index-1;
            }
            else
            {
                return -1;
            }
        }
    }
    
    
    /**
     * Check if the current character is a LUTF1 (Lead UTF ascii char)<br/> 
     * &lt;LUTF1&gt; ::= 0x01-1F | 0x21 | 0x24-2A | 0x2D-3A | 0x3D | 0x3F-5B | 0x5D-7F
     * 
     * @param bytes The buffer containing the data
     * @param index Current position in the buffer
     * @return <code>true</code> if the current character is a LUTF1
     */
    public static boolean isLUTF1( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return false;
        }

        byte c = bytes[index];
        return ( ( ( c | 0x7F ) == 0x7F ) && LUTF1[c & 0x7f] );
    }

    
    /**
     * Check if the current character is a SUTF1 (Stringchar UTF ascii char)<br/> 
     * &lt;LUTF1&gt; ::= 0x01-20 | 0x23-2A | 0x2D-3A | 0x3D | 0x3F-5B | 0x5D-7F
     * 
     * @param bytes The buffer containing the data
     * @param index Current position in the buffer
     * @return <code>true</code> if the current character is a SUTF1
     */
    public static boolean isSUTF1( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return false;
        }

        byte c = bytes[index];
        return ( ( ( c | 0x7F ) == 0x7F ) && SUTF1[c & 0x7f] );
    }

    
    /**
     * Check if the given char is a pair char only
     * &lt;pairCharOnly&gt; ::= ' ' | ',' | '=' | '+' | '<' | '>' | '#' | ';' | '\' | '"'
     *
     * @param c the char we want to test
     * @return true if the char is a pair char only
     */
    public static boolean isPairCharOnly( char c )
    {
        return ( ( ( c | 0x7F ) == 0x7F ) && PAIR_CHAR_ONLY[c & 0x7f] );
    }


    /**
     * Check if the current character is a Pair Char 
     * &lt;pairchar&gt; ::= ' ' | ',' | '=' | '+' | '<' | '>' | '#' | ';' | '\' | '"' | [0-9a-fA-F] [0-9a-fA-F]
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return <code>true</code> if the current character is a Pair Char
     */
    public static boolean isPairChar( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return false;
        }
        else
        {
            byte c = bytes[index];

            if ( ( ( c | 0x7F ) != 0x7F )  || ( !PAIR_CHAR[c] ) )
            {
                return false;
            }
            else
            {
                if ( PAIR_CHAR_ONLY[c] )
                {
                    return true;
                }
                else if ( StringTools.isHex( bytes, index++ ) )
                {
                    return StringTools.isHex( bytes, index );
                }
                else
                {
                    return false;
                }
            }
        }
    }


    /**
     * Check if the current character is a Pair Char 
     * 
     * &lt;pairchar&gt; ::= ' ' | ',' | '=' | '+' | '<' | '>' | '#' | ';' | 
     *                  '\' | '"' | [0-9a-fA-F] [0-9a-fA-F]
     * 
     * @param bytes The byte array which contains the data
     * @param index Current position in the byte array
     * @return <code>true</code> if the current byte is a Pair Char
     */
    public static int countPairChar( byte[] bytes, int index )
    {
        if ( bytes == null )
        {
            return PARSING_ERROR;
        }

        int length = bytes.length;
        
        if ( ( length == 0 ) || ( index < 0 ) || ( index >= length ) )
        {
            return PARSING_ERROR;
        }
        else
        {
            byte c = bytes[index];

            if ( ( ( c | 0x7F ) != 0x7F )  || ( !PAIR_CHAR[c] ) )
            {
                return PARSING_ERROR;
            }
            else
            {
                if ( PAIR_CHAR_ONLY[c] )
                {
                    return 1;
                }
                else if ( StringTools.isHex( bytes, index++ ) )
                {
                    return StringTools.isHex( bytes, index ) ? 2 : PARSING_ERROR;
                }
                else
                {
                    return PARSING_ERROR;
                }
            }
        }
    }


    /**
     * Check if the current character is a String Char. Chars are Unicode, not
     * ASCII. &lt;stringchar&gt; ::= [0x00-0xFFFF] - [,=+<>#;\"\n\r]
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return The current char if it is a String Char, or '#' (this is simpler
     *         than throwing an exception :)
     */
    public static int isStringChar( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return -1;
        }
        else
        {
            byte c = bytes[index];

            if ( ( c | 0x3F ) == 0x3F )
            {
                return STRING_CHAR[ c ];
            }
            else
            {
                return StringTools.countBytesPerChar( bytes, index );
            }
        }
    }


    /**
     * Check if the current character is an ascii String Char.<br/>
     * <p> 
     * &lt;asciistringchar&gt; ::= [0x00-0x7F] - [,=+<>#;\"\n\r]
     * </p>
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return The current char if it is a String Char, or '#' (this is simpler
     *         than throwing an exception :)
     */
    public static int isAciiStringChar( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return -1;
        }
        else
        {
            byte c = bytes[index];

            if ( ( c | 0x3F ) == 0x3F )
            {
                return STRING_CHAR[ c ];
            }
            else
            {
                return StringTools.countBytesPerChar( bytes, index );
            }
        }
    }


    /**
     * Check if the current character is a Quote Char We are testing Unicode
     * chars &lt;quotechar&gt; ::= [0x00-0xFFFF] - [\"]
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     *
     * @return <code>true</code> if the current character is a Quote Char
     */
    public static int isQuoteChar( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return -1;
        }
        else
        {
            byte c = bytes[index];

            if ( ( c == '\\' ) || ( c == '"' ) )
            {
                return -1;
            }
            else
            {
                return StringTools.countBytesPerChar( bytes, index );
            }
        }
    }


    /**
     * Parse an hex pair &lt;hexpair&gt; ::= &lt;hex&gt; &lt;hex&gt;
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return The new position, -1 if the buffer does not contain an HexPair,
     *         -2 if the buffer contains an hex byte but not two.
     */
    public static int parseHexPair( byte[] bytes, int index )
    {
        if ( StringTools.isHex( bytes, index ) )
        {
            if ( StringTools.isHex( bytes, index + 1 ) )
            {
                return index + 2;
            }
            else
            {
                return -2;
            }
        }
        else
        {
            return -1;
        }
    }


    /**
     * Parse an hex pair &lt;hexpair&gt; ::= &lt;hex&gt; &lt;hex&gt;
     * 
     * @param bytes The byte array which contains the data
     * @param index Current position in the byte array
     * @return The new position, -1 if the byte array does not contain an HexPair,
     *         -2 if the byte array contains an hex byte but not two.
     */
    private static byte getHexPair( byte[] bytes, int index )
    {
        return StringTools.getHexValue( bytes[index], bytes[index + 1] );
    }

    
    /**
     * Parse an hex string, which is a list of hex pairs &lt;hexstring&gt; ::=
     * &lt;hexpair&gt; &lt;hexpairs&gt; &lt;hexpairs&gt; ::= &lt;hexpair&gt; &lt;hexpairs&gt; | e
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return Return the first position which is not an hex pair, or -1 if
     *         there is no hexpair at the beginning or if an hexpair is invalid
     *         (if we have only one hex instead of 2)
     */
    public static int parseHexString( byte[] bytes, int index )
    {
        int result = parseHexPair( bytes, index );

        if ( result < 0 )
        {
            return -1;
        }
        else
        {
            index += 2;
        }

        while ( ( result = parseHexPair( bytes, index ) ) >= 0 )
        {
            index += 2;
        }

        return ( ( result == -2 ) ? -1 : index );
    }


    /**
     * Parse an hex string, which is a list of hex pairs &lt;hexstring&gt; ::=
     * &lt;hexpair&gt; &lt;hexpairs&gt; &lt;hexpairs&gt; ::= &lt;hexpair&gt; &lt;hexpairs&gt; | e
     * 
     * @param bytes The byte array which contains the data
     * @param hex The result as a byte array
     * @param pos Current position in the string
     * @return Return the first position which is not an hex pair, or -1 if
     *         there is no hexpair at the beginning or if an hexpair is invalid
     *         (if we have only one hex instead of 2)
     */
    public static int parseHexString( byte[] bytes, byte[] hex, Position pos )
    {
        int i = 0;
        pos.end = pos.start;
        int result = parseHexPair( bytes, pos.start );

        if ( result < 0 )
        {
            return PARSING_ERROR;
        }
        else
        {
            hex[i++] = getHexPair( bytes, pos.end );
            pos.end += TWO_CHARS;
        }

        while ( ( result = parseHexPair( bytes, pos.end ) ) >= 0 )
        {
            hex[i++] = getHexPair( bytes, pos.end );
            pos.end += TWO_CHARS;
        }

        return ( ( result == BAD_HEX_PAIR ) ? PARSING_ERROR : PARSING_OK );
    }

    
    /**
     * Walk the buffer while characters are Base64 characters : &lt;base64-string&gt;
     * ::= &lt;base64-char&gt; &lt;base64-chars&gt; &lt;base64-chars&gt; ::= &lt;base64-char&gt;
     * &lt;base64-chars&gt; | &lt;base64-char&gt; ::= 0x2B | 0x2F | [0x30-0x39] | 0x3D |
     * [0x41-0x5A] | [0x61-0x7A]
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return The position of the first character which is not a Base64 Char
     */
    public static int parseBase64String( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return -1;
        }
        else
        {
            byte c = bytes[index];

            if ( ( ( c | 0x7F ) != 0x7F )  || ( !BASE64_CHAR[c] ) )
            {
                return -1;
            }

            index++;

            while ( index < bytes.length )
            {
                c = bytes[index];

                if ( ( ( c | 0x7F ) != 0x7F )  || ( !BASE64_CHAR[c] ) )
                {
                    break;
                }

                index++;
            }

            return index;
        }
    }
}
