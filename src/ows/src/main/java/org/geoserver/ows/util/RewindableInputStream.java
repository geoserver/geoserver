/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
//
// Borrowed from org.apache.xerces.impl.XMLEntityManager,
// slightly modified and commented out. -AK
//
// This class wraps the byte inputstreams we're presented with.
// We need it because java.io.InputStreams don't provide
// functionality to reread processed bytes, and they have a habit
// of reading more than one character when you call their read()
// methods.  This means that, once we discover the true (declared)
// encoding of a document, we can neither backtrack to read the
// whole doc again nor start reading where we are with a new
// reader.
//
// This class allows rewinding an inputStream by allowing a mark
// to be set, and the stream reset to that position.  <strong>The
// class assumes that it needs to read one character per
// invocation when it's read() method is inovked, but uses the
// underlying InputStream's read(char[], offset length) method--it
// won't buffer data read this way!</strong>
//
// @task TODO: How about implementing an ability to completely
//             disable buffering performed by this stream?
//             It is very unlikely that someone will read data from
//             the stream byte by byte with <code>read()</code>
//             method, but if they do, the whole content will
//             be unconditionally stored in internal buffer, resulting
//             in additional (and most probably vain) memory impact.
//
// @author Neil Graham, IBM
// @author Glenn Marcy, IBM
//
package org.geoserver.ows.util;

import java.io.IOException;
import java.io.InputStream;

public class RewindableInputStream extends InputStream {
    /**
     * Default buffer size before we've finished with the XMLDecl: I think the name should be left
     * unchanged to give a hint for possible use of this class :)
     */
    public static final int DEFAULT_XMLDECL_BUFFER_SIZE = 64;

    /**
     * Tells whether <code>read(byte[], int, int)</code> method is allowed to read multiple bytes
     * beyond the internal buffer (<code>true</code>) or not (<code>true</code> is the default)
     */
    protected boolean fMayReadChunks;

    /** Source input stream we are wrapping with rewindable one. */
    protected InputStream fInputStream;

    /**
     * Internal buffer of bytes already read from source input stream. Allows to access the same
     * byte more than once.
     */
    protected byte[] fData;

    /**
     * Position in the stream that to which stream pointer can be reset with <code>rewind</code>
     * method invocation.
     */
    protected int fStartOffset;

    /**
     * Position where the end of underlying stream was encountered. Potentially in <code>
     * RewindableInputStream</code> instance stream's "end" could be reached more than once, so it
     * is a good thing to know where original stream ended to avoid <code>IOExceptions</code>.
     */
    protected int fEndOffset;

    /**
     * Offset of the next byte to be read from the stream relative to the beginning of the stream
     * (and <code>fData</code> array as well)
     */
    protected int fOffset;

    /**
     * Number of read bytes currently stored in <code>fData</code> buffer. Size of the buffer itself
     * can be greater than this value, obviously.
     */
    protected int fLength;

    /** Offset of the "marked" position in the stream relative to its beginning. */
    protected int fMark;

    /**
     * Creates new <code>RewindableInputStream</code> object with internal buffer of default size
     * and default value of chunked reading flag (which is _currently_ <code>true</code>).
     *
     * @param is InputStream that needs basic reset/rewind functionality.
     */
    public RewindableInputStream(InputStream is) {
        this(is, true, DEFAULT_XMLDECL_BUFFER_SIZE);
    }

    /**
     * Creates new RewindableInputStream with internal buffer of specified size and no chunk reading
     * beyound the buffer limits allowed.
     *
     * @param is InputStream that needs some reset/rewind functionality.
     * @param chunkedMode See the <code>RewindableInputStream(InputStream,
     *                      boolean, int)</code> constructor description.
     */
    public RewindableInputStream(InputStream is, boolean chunkedMode) {
        this(is, chunkedMode, DEFAULT_XMLDECL_BUFFER_SIZE);
    }

    /**
     * Primary constructor that allows to specify all parameters exlicitly affecting class work
     * (initial size of the internal buffer and chunk read mode).
     *
     * @param is InputStream that needs some reset/rewind functionality.
     *     <p>Initial value of <code>fMayReadChunks</code> flag which determines whether multiple
     *     bytes can be read from the underlying stream in single reading operation or not. This
     *     value can be changed using <code>setChunkedMode</code> (or its aliases). For specific
     *     purpose of inferring encoding/charset of XML document typical usage policy is to disable
     *     chunked reads while obtaining XML declaration and then enable it to speed up reading the
     *     rest of document.
     * @param initialSize Initial size of the internal buffer array.
     */
    public RewindableInputStream(InputStream is, boolean chunkedMode, int initialSize) {
        if (0 >= initialSize) {
            initialSize = DEFAULT_XMLDECL_BUFFER_SIZE;
        }

        fData = new byte[initialSize];

        fInputStream = is;
        fStartOffset = 0;
        fMayReadChunks = chunkedMode;
        fEndOffset = -1;
        fOffset = 0;
        fLength = 0;
        fMark = 0;
    }

    /**
     * Sets the position somewhere in the stream to which the stream pointer will be reset after
     * <code>rewind</code> invocation. By default this position is the beginning of the stream.
     *
     * @param offset New value for "fStartOffset".
     */
    public void setStartOffset(int offset) {
        fStartOffset = offset;
    }

    /**
     * Allows to change the behavior of the stream regarding chunked reading at runtime. If you
     * allowed chunked reading and then read some data from the stream, you better forget about
     * <code>reset</code>ting or <code>rewind</code>ing it after that.
     *
     * @param chunkedMode New value for <code>fMayReadChunks</code>.
     */
    public void setChunkedMode(boolean chunkedMode) {
        fMayReadChunks = chunkedMode;
    }

    /**
     * More conscious alias for <code>setChunkedMode(true)</code>. While last method is a general
     * purpose mutator, code may look a bit more clear if you use specialized methods to
     * enable/disable chunk read mode.
     */
    public void enableChunkedMode() {
        fMayReadChunks = true;
    }

    /**
     * More conscious alias for <code>setChunkedMode(false)</code>. While last method is a general
     * purpose mutator, code may look a bit more clear if you use specialized methods to
     * enable/disable chunk read mode.
     */
    public void disableChunkedMode() {
        fMayReadChunks = false;
    }

    /**
     * Quickly reset stream pointer to the beginning of the stream or to position which offset was
     * specified during the last <code>setStartOffset</code> call.
     */
    public void rewind() {
        fOffset = fStartOffset;
    }

    /**
     * Reads next byte from this stream. This byte is either being read from underlying InputStream
     * or taken from the internal buffer in case it was already read at some point before.
     *
     * @return Next byte of data or <code>-1</code> if end of stream is reached.
     * @throws IOException in case of any I/O errors.
     */
    public int read() throws IOException {
        int b = 0;

        // Byte to be read is already in out buffer, simply returning it
        if (fOffset < fLength) {
            return fData[fOffset++] & 0xff;
        }

        /*
         * End of the stream is reached.
         * I also believe that in certain cases fOffset can point to the
         * position after the end of stream, for example, after invalid
         * `setStartOffset()` call followed by `rewind()`.
         * This situation is not handled currently.
         */
        if (fOffset == fEndOffset) {
            return -1;
        }

        /*
         * Ok, we should actually read data from underlying stream, but
         * first it will be good to check if buffer array should be
         * expanded. Each time buffer size is doubled.
         */
        if (fOffset == fData.length) {
            byte[] newData = new byte[fOffset << 1];
            System.arraycopy(fData, 0, newData, 0, fOffset);
            fData = newData;
        }

        // Reading byte from the underlying stream, storing it in buffer and
        // then returning it.
        b = fInputStream.read();

        if (b == -1) {
            fEndOffset = fOffset;

            return -1;
        }

        fData[fLength++] = (byte) b;
        fOffset++;

        return b & 0xff;
    } // END read()

    /**
     * Reads up to len bytes of data from the input stream into an array of bytes. In its current
     * implementation it cannot return more bytes than left in the buffer if in "non-chunked" mode (
     * <code>fMayReadChunks == false</code>). After reaching the end of the buffer, each invocation
     * of this method will read exactly 1 byte then. In "chunked" mode this method <em>may</em>
     * return more than 1 byte, but it doesn't buffer the result.
     *
     * <p>From the other hand, for the task of reading xml declaration, such behavior may be
     * desirable, as we probably don't need reset/rewind functionality after we finished with
     * charset deduction. It is good idea to call <code>enableChunkedMode</code> after that, in
     * order to improve perfomance and lessen memoery consumption when reading the rest of the data.
     *
     * @return Total number of bytes actually read or <code>-1</code> if end of stream has been
     *     reached.
     * @throws IOException when an I/O error occurs while reading data
     * @throws IndexOutOfBoundsException in case of invalid <code>off</code>, <code>len</code> and
     *     <code>b.length</code> combination
     */
    public int read(byte[] b, int off, int len) throws IOException {
        if (null == b) {
            throw new NullPointerException("Destination byte array is null.");
        } else if (0 == len) {
            return 0;
        } else if ((b.length < off) || (b.length < (off + len)) || (0 > off) || (0 > len)) {
            throw new IndexOutOfBoundsException();
        }

        int bytesLeft = fLength - fOffset;

        /*
         * There is no more bytes in the buffer. We either reading 1 byte
         * from underlying InputStream and saving it in the buffer, or
         * getting more bytes without saving them, depending on the value
         * of `fMayReadChunks` field.
         */
        if (bytesLeft == 0) {
            if (fOffset == fEndOffset) {
                return -1;
            }

            // better get some more for the voracious reader...
            if (fMayReadChunks) {
                // Hmm, this can be buffered in theory. But in many
                // cases this would be undesirable, so let it be as it is.
                return fInputStream.read(b, off, len);
            }

            int returnedVal = read();

            if (returnedVal == -1) {
                fEndOffset = fOffset;

                return -1;
            }

            b[off] = (byte) returnedVal;

            return 1;
        }

        /*
         * In non-chunked mode we shouldn't give out more bytes then left
         * in the buffer.
         */
        if (fMayReadChunks) {
            // Count of bytes to get form buffer
            int readFromBuffer = (len < bytesLeft) ? len : bytesLeft;

            System.arraycopy(fData, fOffset, b, off, readFromBuffer);

            int readFromStream = 0;

            if (len > bytesLeft) {
                readFromStream = fInputStream.read(b, off + bytesLeft, len - bytesLeft);
            }

            fOffset += readFromBuffer;

            return readFromBuffer + ((-1 == readFromStream) ? 0 : readFromStream);
        } else {
            //
            // This will prevent returning more bytes than the remainder of
            // the buffer array.
            if (len > bytesLeft) {
                len = bytesLeft;
            }

            System.arraycopy(fData, fOffset, b, off, len);

            fOffset += len;

            return len;
        }
    } // END read(byte[], int, int)

    /**
     * Skips over and discards <code>n</code> bytes of data from this input stream. The skip method
     * may, for a variety of reasons, end up skipping over some smaller number of bytes, possibly
     * <code>0</code>. The actual number of bytes skipped is returned. If <code>n</code> is
     * negative, no bytes are skipped.
     *
     * @param n Number of bytes to be skipped.
     * @return Number of bytes actually skipped.
     * @throws IOException if an I/O error occurs.
     */
    public long skip(long n) throws IOException {
        int bytesLeft;

        if (n <= 0) {
            return 0;
        }

        bytesLeft = fLength - fOffset;

        // If end of buffer is reached, using `skip()` of the underlying input
        // stream
        if (bytesLeft == 0) {
            if (fOffset == fEndOffset) {
                return 0;
            }

            return fInputStream.skip(n);
        }

        // Quickly "skipping" bytes in the buffer by modifying its pointer.
        if (n <= bytesLeft) {
            fOffset += n;

            return n;
        }

        fOffset += bytesLeft;

        if (fOffset == fEndOffset) {
            return bytesLeft;
        }

        n -= bytesLeft;

        return fInputStream.skip(n) + bytesLeft;
    } // END skip(long)

    /**
     * Returns the number of bytes that can be read (or skipped over) from this input stream without
     * blocking by the next caller of a method for this input stream. For <code>
     * RewindableInputStream</code> this can be:
     *
     * <ul>
     *   <li>Number of unread bytes in the <code>fData</code> buffer, i.e. those between current
     *       position (fOffset) and total bytes quantity in the buffer (fLength).
     *   <li>Result of underlying InputStream's <code>available</code> call if there are no unread
     *       bytes in the buffer.
     *   <li><code>-1</code> if end of stream is reached.
     * </ul>
     *
     * @return the number of bytes that can be read from this input stream without blocking.
     * @throws IOException when an I/O error occurs.
     */
    public int available() throws IOException {
        int bytesLeft = fLength - fOffset;

        if (bytesLeft == 0) {
            // Again, the same thing as in `read()`. Do we need to throw
            // an exception if fOffset > fEndOffset???
            if (fOffset == fEndOffset) {
                return -1;
            }

            /*
             * In a manner of speaking, when this class isn't permitting more
             * than one byte at a time to be read, it is "blocking".  The
             * available() method should indicate how much can be read without
             * blocking, so while we're in this mode, it should only indicate
             * that bytes in its buffer are available; otherwise, the result of
             * available() on the underlying InputStream is appropriate.
             */
            return fMayReadChunks ? fInputStream.available() : 0;
        }

        return bytesLeft;
    }

    /**
     * Sets a mark to the current position in the stream.
     *
     * @param howMuch Not used in this implementation I guess.
     */
    public void mark(int howMuch) {
        fMark = fOffset;
    }

    /**
     * Returns stream pointer to the position previously remembered using <code>mark</code> method
     * (or to beginning of the stream, if there were no <code>mark</code> method calls).
     */
    public void reset() {
        fOffset = fMark;
    }

    /**
     * Tells that this stream supports mark/reset capability. This one definitely supports it :)
     *
     * @return <code>true</code> if this stream instance supports the mark and reset methods; <code>
     *     false</code> otherwise.
     */
    public boolean markSupported() {
        return true;
    }

    /**
     * Closes underlying byte stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        if (fInputStream != null) {
            fInputStream.close();
            fInputStream = null;
            fData = null;
        }
    }
} // end of RewindableInputStream class
