/*
 * @(#)Base64.java
 *
 * Copyright 2003-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistribution of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *   2. Redistribution in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use in
 * the design, construction, operation or maintenance of any nuclear facility.
 */

package com.sun.xacml.attr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Class that knows how to encode and decode Base64 values. Base64 Content-Transfer-Encoding rules
 * are defined in Section 6.8 of IETF RFC 2045 <i>Multipurpose Internet Mail Extensions (MIME) Part
 * One: Format of Internet Message Bodies</i>, available at <a
 * href="ftp://ftp.isi.edu/in-notes/rfc2045.txt">
 * <code>ftp://ftp.isi.edu/in-notes/rfc2045.txt</code></a>.
 * <p>
 * All methods of this class are static and thread-safe.
 * 
 * @since 1.0
 * @author Anne Anderson
 */
class Base64 {
    /*
     * ASCII white-space characters. These are the ones recognized by the C and Java language
     * [pre-processors].
     */
    private static final char SPACE = 0x20; /* space, or blank, character */

    private static final char ETX = 0x04; /* end-of-text character */

    private static final char VTAB = 0x0b; /* vertical tab character */

    private static final char FF = 0x0c; /* form-feed character */

    private static final char HTAB = 0x09; /* horizontal tab character */

    private static final char LF = 0x0a; /* line feed character */

    private static final char ALTLF = 0x13; /* line feed on some systems */

    private static final char CR = 0x0d; /* carriage-return character */

    /*
     * The character used to pad out a 4-character Base64-encoded block, or "quantum".
     */
    // private static char PAD = '=';
    /*
     * String used for BASE64 encoding and decoding.
     * 
     * For index values 0-63, the character at each index is the Base-64 encoded value of the index
     * value. Index values beyond 63 are never referenced during encoding, but are used in this
     * implementation to help in decoding. The character at index 64 is the Base64 pad character
     * '='.
     * 
     * Charaters in index positions 0-64 MUST NOT be moved or altered, as this will break the
     * implementation.
     * 
     * The characters after index 64 are white space characters that should be ignored in
     * Base64-encoded input strings while doing decoding. Note that the white-space character set
     * should include values used on various platforms, since a Base64-encoded value may have been
     * generated on a non-Java platform. The values included here are those used in Java and in C.
     * 
     * The white-space character set may be modified without affecting the implementation of the
     * encoding algorithm.
     */
    private static final String BASE64EncodingString = "ABCDEFGHIJ" + "KLMNOPQRST" + "UVWXYZabcd"
            + "efghijklmn" + "opqrstuvwx" + "yz01234567" + "89+/" + "=" + SPACE + ETX + VTAB + FF
            + HTAB + LF + ALTLF + CR;

    // Index of pad character in Base64EncodingString
    private static final int PAD_INDEX = 64;

    /*
     * The character in Base64EncodingString with the maximum character value in Unicode.
     */
    private static final int MAX_BASE64_CHAR = 'z';

    /*
     * Array for mapping encoded characters to decoded values. This array is initialized when needed
     * by calling initDecodeArray(). Only includes entries up to MAX_BASE64_CHAR.
     */
    private static int[] Base64DecodeArray = null;

    /*
     * State values used for decoding a quantum of four encoded input characters as follows.
     * 
     * Initial state: NO_CHARS_DECODED NO_CHARS_DECODED: no characters have been decoded on encoded
     * char: decode char into output quantum; new state: ONE_CHAR_DECODED otherwise: Exception
     * ONE_CHAR_DECODED: one character has been decoded on encoded char: decode char into output
     * quantum; new state: TWO_CHARS_DECODED otherwise: Exception TWO_CHARS_DECODED: two characters
     * have been decoded on encoded char: decode char into output quantum; new state:
     * THREE_CHARS_DECODED on pad: write quantum byte 0 to output; new state: PAD_THREE_READ
     * THREE_CHARS_DECODED: three characters have been decoded on encoded char: decode char into
     * output quantum; write quantum bytes 0-2 to output; new state: NO_CHARS_DECODED on pad: write
     * quantum bytes 0-1 to output; new state: PAD_FOUR_READ PAD_THREE_READ: pad character has been
     * read as 3rd of 4 chars on pad: new state: PAD_FOUR_READ otherwise: Exception PAD_FOUR_READ:
     * pad character has been read as 4th of 4 char on any char: Exception
     * 
     * The valid terminal states are NO_CHARS_DECODED and PAD_FOUR_READ.
     */
    private static final int NO_CHARS_DECODED = 0;

    private static final int ONE_CHAR_DECODED = 1;

    private static final int TWO_CHARS_DECODED = 2;

    private static final int THREE_CHARS_DECODED = 3;

    private static final int PAD_THREE_READ = 5;

    private static final int PAD_FOUR_READ = 6;

    /**
     * The maximum number of groups that should be encoded onto a single line (so we don't exceed 76
     * characters per line).
     */
    private static final int MAX_GROUPS_PER_LINE = 76 / 4;

    /**
     * Encodes the input byte array into a Base64-encoded <code>String</code>. The output
     * <code>String</code> has a CR LF (0x0d 0x0a) after every 76 bytes, but not at the end.
     * <p>
     * <b>WARNING</b>: If the input byte array is modified while encoding is in progress, the output
     * is undefined.
     * 
     * @param binaryValue
     *            the byte array to be encoded
     * 
     * @return the Base64-encoded <code>String</code>
     */
    public static String encode(byte[] binaryValue) {

        int binaryValueLen = binaryValue.length;
        // Estimated output length (about 1.4x input, due to CRLF)
        int maxChars = (binaryValueLen * 7) / 5;
        // Buffer for encoded output
        StringBuffer sb = new StringBuffer(maxChars);

        int groupsToEOL = MAX_GROUPS_PER_LINE;
        // Convert groups of 3 input bytes, with pad if < 3 in final
        for (int binaryValueNdx = 0; binaryValueNdx < binaryValueLen; binaryValueNdx += 3) {

            // Encode 1st 6-bit group
            int group1 = (binaryValue[binaryValueNdx] >> 2) & 0x3F;
            sb.append(BASE64EncodingString.charAt(group1));

            // Encode 2nd 6-bit group
            int group2 = (binaryValue[binaryValueNdx] << 4) & 0x030;
            if ((binaryValueNdx + 1) < binaryValueLen) {
                group2 = group2 | ((binaryValue[binaryValueNdx + 1] >> 4) & 0xF);
            }
            sb.append(BASE64EncodingString.charAt(group2));

            // Encode 3rd 6-bit group
            int group3;
            if ((binaryValueNdx + 1) < binaryValueLen) {
                group3 = (binaryValue[binaryValueNdx + 1] << 2) & 0x03C;
                if ((binaryValueNdx + 2) < binaryValueLen) {
                    group3 = group3 | ((binaryValue[binaryValueNdx + 2] >> 6) & 0x3);
                }
            } else {
                group3 = PAD_INDEX;
            }
            sb.append(BASE64EncodingString.charAt(group3));

            // Encode 4th 6-bit group
            int group4;
            if ((binaryValueNdx + 2) < binaryValueLen) {
                group4 = binaryValue[binaryValueNdx + 2] & 0x3F;
            } else {
                group4 = PAD_INDEX;
            }
            sb.append(BASE64EncodingString.charAt(group4));

            // After every MAX_GROUPS_PER_LINE groups, insert CR LF.
            // Unless this is the final line, in which case we skip that.
            groupsToEOL = groupsToEOL - 1;
            if (groupsToEOL == 0) {
                groupsToEOL = MAX_GROUPS_PER_LINE;
                if ((binaryValueNdx + 3) <= binaryValueLen) {
                    sb.append(CR);
                    sb.append(LF);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Initializes Base64DecodeArray, if this hasn't already been done.
     */
    private static void initDecodeArray() {
        if (Base64DecodeArray != null)
            return;

        int[] ourArray = new int[MAX_BASE64_CHAR + 1];
        for (int i = 0; i <= MAX_BASE64_CHAR; i++)
            ourArray[i] = BASE64EncodingString.indexOf(i);

        Base64DecodeArray = ourArray;
    }

    /**
     * Decodes a Base64-encoded <code>String</code>. The result is returned in a byte array that
     * should match the original binary value (before encoding). Whitespace characters in the input
     * <code>String</code> are ignored.
     * <p>
     * If the <code>ignoreBadChars</code> parameter is <code>true</code>, characters that are not
     * allowed in a BASE64-encoded string are ignored. Otherwise, they cause an
     * <code>IOException</code> to be raised.
     * 
     * @param encoded
     *            a <code>String</code> containing a Base64-encoded value
     * @param ignoreBadChars
     *            If <code>true</code>, bad characters are ignored. Otherwise, they cause an
     *            <code>IOException</code> to be raised.
     * 
     * @return a byte array containing the decoded value
     * 
     * @throws IOException
     *             if the input <code>String</code> is not a valid Base64-encoded value
     */
    public static byte[] decode(String encoded, boolean ignoreBadChars) throws IOException {
        int encodedLen = encoded.length();
        int maxBytes = (encodedLen / 4) * 3; /* Maximum possible output bytes */
        ByteArrayOutputStream ba = /* Buffer for decoded output */
        new ByteArrayOutputStream(maxBytes);
        byte[] quantum = new byte[3]; /* one output quantum */

        // ensure Base64DecodeArray is initialized
        initDecodeArray();

        /*
         * Every 4 encoded characters in input are converted to 3 bytes of output. This is called
         * one "quantum". Each encoded character is mapped to one 6-bit unit of the output.
         * Whitespace characters in the input are passed over; they are not included in the output.
         */

        int state = NO_CHARS_DECODED;
        for (int encodedNdx = 0; encodedNdx < encodedLen; encodedNdx++) {
            // Turn encoded char into decoded value
            int encodedChar = encoded.charAt(encodedNdx);
            int decodedChar;
            if (encodedChar > MAX_BASE64_CHAR)
                decodedChar = -1;
            else
                decodedChar = Base64DecodeArray[encodedChar];

            // Handle white space and invalid characters
            if (decodedChar == -1) {
                if (ignoreBadChars)
                    continue;
                throw new IOException("Invalid character");
            }
            if (decodedChar > PAD_INDEX) { /* whitespace */
                continue;
            }

            // Handle valid characters
            switch (state) {
            case NO_CHARS_DECODED:
                if (decodedChar == PAD_INDEX) {
                    throw new IOException("Pad character in invalid position");
                }
                quantum[0] = (byte) ((decodedChar << 2) & 0xFC);
                state = ONE_CHAR_DECODED;
                break;
            case ONE_CHAR_DECODED:
                if (decodedChar == PAD_INDEX) {
                    throw new IOException("Pad character in invalid position");
                }
                quantum[0] = (byte) (quantum[0] | ((decodedChar >> 4) & 0x3));
                quantum[1] = (byte) ((decodedChar << 4) & 0xF0);
                state = TWO_CHARS_DECODED;
                break;
            case TWO_CHARS_DECODED:
                if (decodedChar == PAD_INDEX) {
                    ba.write(quantum, 0, 1);
                    state = PAD_THREE_READ;
                } else {
                    quantum[1] = (byte) (quantum[1] | ((decodedChar >> 2) & 0x0F));
                    quantum[2] = (byte) ((decodedChar << 6) & 0xC0);
                    state = THREE_CHARS_DECODED;
                }
                break;
            case THREE_CHARS_DECODED:
                if (decodedChar == PAD_INDEX) {
                    ba.write(quantum, 0, 2);
                    state = PAD_FOUR_READ;
                } else {
                    quantum[2] = (byte) (quantum[2] | decodedChar);
                    ba.write(quantum, 0, 3);
                    state = NO_CHARS_DECODED;
                }
                break;
            case PAD_THREE_READ:
                if (decodedChar != PAD_INDEX) {
                    throw new IOException("Missing pad character");
                }
                state = PAD_FOUR_READ;
                break;
            case PAD_FOUR_READ:
                throw new IOException("Invalid input follows pad character");
            }
        }

        // Test valid terminal states
        if (state != NO_CHARS_DECODED && state != PAD_FOUR_READ)
            throw new IOException("Invalid sequence of input characters");

        return ba.toByteArray();
    }
}
