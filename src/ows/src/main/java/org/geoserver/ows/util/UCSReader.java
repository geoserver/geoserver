/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xerces" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

// package org.apache.xerces.impl.io;
package org.geoserver.ows.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Reader for UCS-2 and UCS-4 encodings. (more precisely ISO-10646-UCS-(2|4) encodings).
 *
 * <p>This variant is modified to handle supplementary Unicode code points correctly. Though this
 * required a lot of new code and definitely reduced the perfomance comparing to original version. I
 * tried my best to preserve exsiting code and comments whenever it was possible. I performed some
 * basic tests, but not too thorough ones, so some bugs may still nest in the code. -AK
 *
 * @author Neil Graham, IBM
 * @version $Id$
 */
public class UCSReader extends Reader {
    //
    // Constants
    //

    /**
     * Default byte buffer size (8192, larger than that of ASCIIReader since it's reasonable to
     * surmise that the average UCS-4-encoded file should be 4 times as large as the average
     * ASCII-encoded file).
     */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Starting size of the internal char buffer. Internal char buffer is maintained to hold excess
     * chars that may left from previous read operation when working with UCS-4 data (never used for
     * UCS-2).
     */
    public static final int CHAR_BUFFER_INITIAL_SIZE = 1024;

    public static final short UCS2LE = 1;
    public static final short UCS2BE = 2;
    public static final short UCS4LE = 4;
    public static final short UCS4BE = 8;

    /** The minimum value of a supplementary code point. */
    public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;

    /** The minimum value of a Unicode code point. */
    public static final int MIN_CODE_POINT = 0x000000;

    /** The maximum value of a Unicode code point. */
    public static final int MAX_CODE_POINT = 0x10ffff;

    //
    // Data
    //

    /** Input stream. */
    protected InputStream fInputStream;

    /** Byte buffer. */
    protected byte[] fBuffer;

    /** what kind of data we're dealing with */
    protected short fEncoding;

    /**
     * Stores aforeread or "excess" characters that may appear during <code>read</code> methods
     * invocation due to the fact that one input UCS-4 supplementary character results in two output
     * Java <code>char</code>`s - high surrogate and low surrogate code units. Because of that, if
     * <code>read()</code> method encounters supplementary code point in the input stream, it
     * returns UTF-16-encoded high surrogate code unit and stores low surrogate in buffer. When
     * called next time, <code>read()</code> will return this low surrogate, instead of reading more
     * bytes from the <code>InputStream</code>. Similarly if <code>read(char[], int, int)</code> is
     * invoked to read, for example, 10 chars into specified buffer, and 4 of them turn out to be
     * supplementary Unicode characters, each written as two chars, then we end up having 4 excess
     * chars that we cannot immediately return or push back to the input stream. So we need to store
     * them in the buffer awaiting further <code>read</code> invocations. Note that char buffer
     * functions like a stack, i.e. chars and surrogate pairs are stored in reverse order.
     */
    protected char[] fCharBuf;

    /** Count of Java chars currently being stored in in the <code>fCharBuf</code> array. */
    protected int fCharCount;

    //
    // Constructors
    //

    /**
     * Constructs an <code>ISO-10646-UCS-(2|4)</code> reader from the specified input stream using
     * default buffer size. The Endianness and exact input encoding (<code>UCS-2</code> or <code>
     * UCS-4</code>) also should be known in advance.
     *
     * @param inputStream input stream with UCS-2|4 encoded data
     * @param encoding One of UCS2LE, UCS2BE, UCS4LE or UCS4BE.
     */
    public UCSReader(InputStream inputStream, short encoding) {
        this(inputStream, DEFAULT_BUFFER_SIZE, encoding);
    } // <init>(InputStream, short)

    /**
     * Constructs an <code>ISO-10646-UCS-(2|4)</code> reader from the source input stream using
     * explicitly specified initial buffer size. Endianness and exact input encoding (<code>UCS-2
     * </code> or <code>UCS-4</code>) also should be known in advance.
     *
     * @param inputStream input stream with UCS-2|4 encoded data
     * @param size The initial buffer size. You better make sure this number is divisible by 4 if
     *     you plan to to read UCS-4 with this class.
     * @param encoding One of UCS2LE, UCS2BE, UCS4LE or UCS4BE
     */
    public UCSReader(InputStream inputStream, int size, short encoding) {
        fInputStream = inputStream;
        fBuffer = new byte[size];
        fEncoding = encoding;

        fCharBuf = new char[CHAR_BUFFER_INITIAL_SIZE];
        fCharCount = 0;
    } // <init>(InputStream, int, short)

    //
    // Reader methods
    //

    /**
     * Read a single character. This method will block until a character is available, an I/O error
     * occurs, or the end of the stream is reached.
     *
     * <p>If supplementary Unicode character is encountered in <code>UCS-4</code> input, it will be
     * encoded into <code>UTF-16</code> surrogate pair according to RFC 2781. High surrogate code
     * unit will be returned immediately, and low surrogate saved in the internal buffer to be read
     * during next <code>read()</code> or <code>read(char[], int, int)</code> invocation. -AK
     *
     * @return Java 16-bit <code>char</code> value containing UTF-16 code unit which may be either
     *     code point from Basic Multilingual Plane or one of the surrogate code units (high or low)
     *     of the pair representing supplementary Unicode character (one in <code>0x10000 - 0x10FFFF
     *     </code> range) -AK
     * @exception IOException when I/O error occurs
     */
    public int read() throws IOException {
        // If we got something in the char buffer, let's use it.
        if (0 != fCharCount) {
            fCharCount--;

            return ((int) fCharBuf[fCharCount]) & 0xFFFF;
        }

        int b0 = fInputStream.read() & 0xff; // 1st byte

        if (b0 == 0xff) {
            return -1;
        }

        int b1 = fInputStream.read() & 0xff; // 2nd byte

        if (b1 == 0xff) {
            return -1;
        }

        if (fEncoding >= 4) { // UCS-4

            int b2 = fInputStream.read() & 0xff; // 3rd byte

            if (b2 == 0xff) {
                return -1;
            }

            int b3 = fInputStream.read() & 0xff; // 4th byte

            if (b3 == 0xff) {
                return -1;
            }

            int codepoint;

            if (UCS4BE == fEncoding) {
                codepoint = ((b0 << 24) + (b1 << 16) + (b2 << 8) + b3);
            } else {
                codepoint = ((b3 << 24) + (b2 << 16) + (b1 << 8) + b0);
            }

            /*
             * Encoding from UCS-4 to UTF-16 as described in RFC 2781
             * In theory there should be additional `isValidCodePoint()` check
             * but I simply don't know what to do if invalid one is encountered.
             */
            if (!isSupplementaryCodePoint(codepoint)) {
                return codepoint;
            } else {
                int cp1 = (codepoint - 0x10000) & 0xFFFFF;
                int highSurrogate = 0xD800 + (cp1 >>> 10); // ">>" should work too
                // Saving low surrogate for future use

                fCharBuf[fCharCount] = (char) (0xDC00 + (cp1 & 0x3FF));

                // low surrogate code unit will be returned during next call
                return highSurrogate;
            }
        } else { // UCS-2

            if (fEncoding == UCS2BE) {
                return (b0 << 8) + b1;
            } else {
                return (b1 << 8) + b0;
            }
        }
    } // read():int

    /**
     * Read characters into a portion of an array. This method will block until some input is
     * available, an I/O error occurs, or the end of the stream is reached.
     *
     * <p>I suspect that the whole stuff works awfully slow, so if you know for sure that your
     * <code>UCS-4</code> input does not contain any supplementary code points you probably should
     * use original <code>UCSReader</code> class from Xerces team (<code>
     * org.apache.xerces.impl.io.UCSReader</code>). -AK
     *
     * @param ch Destination buffer
     * @param offset Offset at which to start storing characters
     * @param length Maximum number of characters to read
     * @return The number of characters read, or <code>-1</code> if the end of the stream has been
     *     reached. Note that this is not a number of <code>UCS-4</code> characters read, but
     *     instead number of <code>UTF-16</code> code units. These two are equal only if there were
     *     no supplementary Unicode code points among read chars.
     * @exception IOException If an I/O error occurs
     */
    public int read(char[] ch, int offset, int length) throws IOException {
        /*
         * The behavior of this method is _intended_ to be like this:
         *
         * 1. In case if we are working with UCS-2 data, `readUCS2` method
         *    handles the stuff.
         *
         * 2. For UCS-4 data method first looks if there is some data stored in
         *    the internal character buffer (fCharBuf). Usually this data is
         *    left from previous reading operation if there were any
         *    supplementary Unicode (ISO-10646) characters.
         *
         * 3. If buffer holds something, these chars are put directly in passed
         *    `ch` buffer (maximum `length` of them).
         *
         * 4. If char buffer ends and more data can be put into `ch`,
         *    then they are read from the underlying byte stream.
         *
         * 5. Method tries to read maximum possible number of bytes from
         *    InputStream, as if all read code points were from BMP (Basic
         *    Multilingual Plane).
         *
         * 6. Read UCS-4 characters are encoded to UTF-16 (which is native Java
         *     encoding) ant put into `ch` array.
         *
         * 7. It is possible that we end up with more chars than we can
         *    currently put into passed buffer due to the fact that
         *    supplementary Unicode characters are encoded into _two_ Java
         *    char's each. In this situation excess chars are stored in the
         *    internal char buffer (in reverse order, i.e. those read last
         *    are at the beginning of the `fCharBuf`). They are usually picked
         *    up during next call(s) to one of the `read` methods.
         */
        if ((0 > offset)
                || (offset > ch.length)
                || (0 > length)
                || ((offset + length) > ch.length)
                || (0 > (offset + length))) {
            throw new IndexOutOfBoundsException();
        } else if (0 == length) {
            return 0;
        }

        /*
         * Well, it is clear that the code should be separated for
         * UCS-2 and UCS-4 now with all that char buffer stuff around.
         * Things are already getting nasty.
         */
        if (fEncoding < 4) {
            return readUCS2(ch, offset, length);
        }

        // First using chars from internal char buffer (if any)
        int charsRead = 0;

        while (charsRead <= length) {
            if (0 != fCharCount) {
                ch[offset + charsRead] = fCharBuf[--fCharCount];
                charsRead++;
            } else {
                break;
            }
        }

        // Reading remaining chars from InputStream.
        if (0 != (length - charsRead)) {
            /*
             * Each output char (two for supplementary characters) will require
             * us to read 4 input bytes. But as we cannot predict how many
             * supplementary chars we will encounter, so we should try to read
             * maximum possible number.
             */
            int byteLength = (length - charsRead) << 2;

            if (byteLength > fBuffer.length) {
                byteLength = fBuffer.length;
            }

            int count = fInputStream.read(fBuffer, 0, byteLength);

            if (-1 == count) {
                return (0 == charsRead) ? (-1) : charsRead;
            } else {
                // try and make count be a multiple of the number of bytes we're
                // looking for (simply reading 1 to 3 bytes from input stream to
                // ensure the last code point is complete)
                // this looks ugly, but it avoids an if at any rate...
                int numToRead = ((4 - (count & 3)) & 3);

                for (int i = 0; i < numToRead; i++) {
                    int charRead = fInputStream.read();

                    if (charRead == -1) {
                        // end of input; something likely went wrong! Pad buffer
                        // with zeros.
                        for (int j = i; j < numToRead; j++) fBuffer[count + j] = 0;

                        break;
                    } else {
                        fBuffer[count + i] = (byte) charRead;
                    }
                }

                count += numToRead;

                // now count is a multiple of the right number of bytes
                int numChars = count >> 2;
                int curPos = 0;

                /*
                 * `i` is index of currently processed char from InputStream.
                 * `charsCount` also counts number of chars that were (possibly)
                 * read from internal char buffer.
                 */
                int charsCount = charsRead;
                int i;

                for (i = 0; (i < numChars) && (length >= charsCount); i++) {
                    int b0 = fBuffer[curPos++] & 0xff;
                    int b1 = fBuffer[curPos++] & 0xff;
                    int b2 = fBuffer[curPos++] & 0xff;
                    int b3 = fBuffer[curPos++] & 0xff;

                    int codepoint;

                    if (UCS4BE == fEncoding) {
                        codepoint = ((b0 << 24) + (b1 << 16) + (b2 << 8) + b3);
                    } else {
                        codepoint = ((b3 << 24) + (b2 << 16) + (b1 << 8) + b0);
                    }

                    // Again, validity of this codepoint is never checked, this
                    // can yield problems sometimes.
                    if (!isSupplementaryCodePoint(codepoint)) {
                        ch[offset + charsCount] = (char) codepoint;
                        charsCount++;
                    } else {
                        // Checking if we can put another 2 chars in buffer.
                        if (2 <= (length - charsCount)) {
                            int cp1 = (codepoint - 0x10000) & 0xFFFFF;
                            ch[offset + charsCount] = (char) (0xD800 + (cp1 >>> 10));
                            ch[offset + charsCount + 1] = (char) (0xDC00 + (cp1 & 0x3FF));
                            charsCount += 2;
                        } else {
                            break; // END for
                        }
                    }
                } // END for

                // Storing data, that possibly remain in `fBuffer` into internal
                // char buffer for future use :)
                curPos = (numChars << 2) - 1;

                for (int k = numChars; k > i; k--) {
                    // Reading bytes in reverse order
                    int b3 = fBuffer[curPos--] & 0xff;
                    int b2 = fBuffer[curPos--] & 0xff;
                    int b1 = fBuffer[curPos--] & 0xff;
                    int b0 = fBuffer[curPos--] & 0xff;

                    int codepoint;

                    if (UCS4BE == fEncoding) {
                        codepoint = ((b0 << 24) + (b1 << 16) + (b2 << 8) + b3);
                    } else {
                        codepoint = ((b3 << 24) + (b2 << 16) + (b1 << 8) + b0);
                    }

                    // Look if we need to increase buffer size
                    if (2 > (fCharBuf.length - k)) {
                        char[] newBuf = new char[fCharBuf.length << 1];
                        System.arraycopy(fCharBuf, 0, newBuf, 0, fCharBuf.length);
                        fCharBuf = newBuf;
                    }

                    if (!isSupplementaryCodePoint(codepoint)) {
                        fCharBuf[fCharCount++] = (char) codepoint;
                    } else {
                        int cp1 = (codepoint - 0x10000) & 0xFFFFF;
                        // In this case store low surrogate code unit first, so that
                        // it can be read back after high one.
                        fCharBuf[fCharCount++] = (char) (0xDC00 + ((char) cp1 & 0x3FF));
                        fCharBuf[fCharCount++] = (char) (0xD800 + (cp1 >>> 10));
                    }
                } // END for

                return charsCount;
            } // END if (-1 == count) ELSE
        } // END if (0 != (length - charsRead))

        return charsRead;
    } // read(char[],int,int)

    /**
     * Read <code>UCS-2</code> characters into a portion of an array. This method will block until
     * some input is available, an I/O error occurs, or the end of the stream is reached.
     *
     * <p>In original <code>UCSReader</code> this code was part of <code>read(char[], int, int)
     * </code> method, but I removed it from there to reduce complexity of the latter.
     *
     * @param ch destination buffer
     * @param offset offset at which to start storing characters
     * @param length maximum number of characters to read
     * @return The number of characters read, or <code>-1</code> if the end of the stream has been
     *     reached
     * @exception IOException If an I/O error occurs
     */
    protected int readUCS2(char[] ch, int offset, int length) throws IOException {
        int byteLength = length << 1;

        if (byteLength > fBuffer.length) {
            byteLength = fBuffer.length;
        }

        int count = fInputStream.read(fBuffer, 0, byteLength);

        if (count == -1) {
            return -1;
        }

        // try and make count be a multiple of the number of bytes we're
        // looking for (simply reading 1 to 3 bytes from input stream to
        // ensure the last code point is complete)
        int numToRead = count & 1;

        if (numToRead != 0) {
            count++;

            int charRead = fInputStream.read();

            if (charRead == -1) { // end of input; something likely went
                // wrong! Pad buffer with nulls.
                fBuffer[count] = 0;
            } else {
                fBuffer[count] = (byte) charRead;
            }
        }

        // now count is a multiple of the right number of bytes
        int numChars = count >> 1;
        int curPos = 0;

        for (int i = 0; i < numChars; i++) {
            int b0 = fBuffer[curPos++] & 0xff;
            int b1 = fBuffer[curPos++] & 0xff;

            if (fEncoding == UCS2BE) {
                ch[offset + i] = (char) ((b0 << 8) + b1);
            } else {
                ch[offset + i] = (char) ((b1 << 8) + b0);
            }
        }

        return numChars;
    } // END readUCS2(char[], int, int)

    /**
     * Skip characters. This method will block until some characters are available, an I/O error
     * occurs, or the end of the stream is reached.
     *
     * @param n The number of characters to skip
     * @return The number of characters actually skipped
     * @exception IOException If an I/O error occurs
     */
    public long skip(long n) throws IOException {
        /*
         * charWidth will represent the number of bits to move
         * n leftward to get num of bytes to skip, and then move the result
         * rightward
         * to get num of chars effectively skipped.
         * The trick with &'ing, as with elsewhere in this dcode, is
         * intended to avoid an expensive use of / that might not be optimized
         * away.
         */
        int charWidth = (fEncoding >= 4) ? 2 : 1;
        long bytesSkipped = fInputStream.skip(n << charWidth);

        if ((bytesSkipped & (charWidth | 1)) == 0) {
            return bytesSkipped >>> charWidth;
        }

        return (bytesSkipped >>> charWidth) + 1;
    } // skip(long):long

    /**
     * Tell whether this stream is ready to be read.
     *
     * @return True if the next read() is guaranteed not to block for input, false otherwise. Note
     *     that returning false does not guarantee that the next read will block.
     * @exception IOException If an I/O error occurs
     */
    public boolean ready() throws IOException {
        return false;
    } // ready()

    /** Tell whether this stream supports the mark() operation. */
    public boolean markSupported() {
        return fInputStream.markSupported();
    } // markSupported()

    /**
     * Mark the present position in the stream. Subsequent calls to <code>reset</code> will attempt
     * to reposition the stream to this point. Not all character-input streams support the <code>
     * mark</code> operation. This is one of them :) It relies on marking facilities of underlying
     * byte stream.
     *
     * @param readAheadLimit Limit on the number of characters that may be read while still
     *     preserving the mark. After reading this many characters, attempting to reset the stream
     *     may fail.
     * @exception IOException If the stream does not support <code>mark</code>, or if some other I/O
     *     error occurs
     */
    public void mark(int readAheadLimit) throws IOException {
        fInputStream.mark(readAheadLimit);
    } // mark(int)

    /**
     * Reset the stream. If the stream has been marked, then attempt to reposition it at the mark.
     * If the stream has not been marked, then attempt to reset it in some way appropriate to the
     * particular stream, for example by repositioning it to its starting point. This stream
     * implementation does not support <code>mark</code>/<code>reset</code> by itself, it relies on
     * underlying byte stream in this matter.
     *
     * @exception IOException If the stream has not been marked, or if the mark has been
     *     invalidated, or if the stream does not support reset(), or if some other I/O error occurs
     */
    public void reset() throws IOException {
        fInputStream.reset();
    } // reset()

    /**
     * Close the stream. Once a stream has been closed, further <code>read</code>, <code>ready
     * </code>, <code>mark</code>, or <code>reset</code> invocations will throw an IOException.
     * Closing a previously-closed stream, however, has no effect.
     *
     * @exception IOException If an I/O error occurs
     */
    public void close() throws IOException {
        fInputStream.close();
        fInputStream = null;
        fCharBuf = null;
        fBuffer = null;
    } // close()

    /**
     * Returns the encoding currently in use by this character stream.
     *
     * @return Encoding of this stream. Either ISO-10646-UCS-2 or ISO-10646-UCS-4. Problem is that
     *     this string doesn't indicate the byte order of that encoding. What to do, then? Unlike
     *     UTF-16 byte order cannot be made part of the encoding name in this case and still can be
     *     critical. Currently you can find out the byte order by invoking <code>getByteOrder</code>
     *     method.
     */
    public String getEncoding() {
        if (4 > fEncoding) {
            return "ISO-10646-UCS-2";
        } else {
            return "ISO-10646-UCS-4";
        }
    }

    /**
     * Returns byte order ("endianness") of the encoding currently in use by this character stream.
     * This is a string with two possible values: <code>LITTLE_ENDIAN</code> and <code>BIG_ENDIAN
     * </code>. Maybe using a named constant is a better alternative, but I just don't like them.
     * But feel free to change this behavior if you think that would be better.
     *
     * @return <code>LITTLE_ENDIAN</code> or <code>BIG_ENDIAN</code> depending on byte order of
     *     current encoding of this stream.
     */
    public String getByteOrder() {
        if ((1 == fEncoding) || (4 == fEncoding)) {
            return "LITTLE_ENDIAN";
        } else {
            return "BIG_ENDIAN";
        }
    }

    /**
     * Determines whether the specified character (Unicode code point) is in the supplementary
     * character range. The method call is equivalent to the expression:
     *
     * <blockquote>
     *
     * <pre>
     * codePoint &gt;= 0x10000 &amp;&amp; codePoint &lt;= 0x10ffff
     * </pre>
     *
     * </blockquote>
     *
     * Stolen from JDK 1.5 <code>java.lang.Character</code> class in order to provide JDK 1.4
     * compatibility.
     *
     * @param codePoint the character (Unicode code point) to be tested
     * @return <code>true</code> if the specified character is in the Unicode supplementary
     *     character range; <code>false</code> otherwise.
     */
    protected boolean isSupplementaryCodePoint(int codePoint) {
        return (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT) && (codePoint <= MAX_CODE_POINT);
    }
} // class UCSReader
